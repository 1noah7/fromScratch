package com.fromscratch.bank;

import com.fromscratch.persistence.BankRepository;
import com.fromscratch.overlay.BankOverlayState;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.*;
import javax.inject.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.Filepath;
import com.fromscratch.wealth.*;
import com.fromscratch.ge.*;

/** Client-thread coordinator with serialized background storage and lifecycle/identity guarded callbacks. */
@Singleton
public final class BankController implements BankActions
{
	private final BankSource source;
	private final BankRepository repository;
	private final Consumer<Runnable> dispatch;
	private final Consumer<BankView> display;
	private final AtomicLong epoch = new AtomicLong();
	private volatile boolean enabled;
	private ExecutorService worker;
	private DirectoryProvider directory;
	private String character;
	private long context;
	private BankRun run;
	private BankObservation observation;
	private boolean busy;
	private boolean error;
	private String message = "Log in to load this character's local progress.";
	private String notice;
	private final Map<Integer, String> names = new TreeMap<>();
	private BankView last;
	private final Deque<Pending> pending = new ArrayDeque<>();
	private final Map<Integer,GeObservation> exchangeBaseline=new TreeMap<>();
	private final Map<Integer,GeObservation> latestOffers=new TreeMap<>();
	private static final class Pending
	{
		final BankObservation bank;final GeObservation offer;
		Pending(BankObservation bank,GeObservation offer){this.bank=bank;this.offer=offer;}
	}
	private boolean automatic;
	private BankTimings.Trace committedTrace;
	private final PriceService prices;
	private BankValuation valuation;
	private boolean valuationQueued;
	private UUID valuedRevision;
	private long valuedPrices=-1;
	@Inject public BankController(RuneLiteBankSource source, BankRepository repository, ClientThread clientThread, BankSidebar sidebar, BankOverlayState overlay,PriceService prices)
	{ this(source, repository, clientThread::invokeLater, v -> { overlay.updateBank(v); sidebar.update(v); },prices); }
	public BankController(BankSource source, BankRepository repository, Consumer<Runnable> dispatch, Consumer<BankView> display)
	{ this(source,repository,dispatch,display,PriceService.unavailable()); }
	public BankController(BankSource source, BankRepository repository, Consumer<Runnable> dispatch, Consumer<BankView> display,PriceService prices)
	{ this.source = source; this.repository = repository; this.dispatch = dispatch; this.display = display;this.prices=prices; }
	public void start(DirectoryProvider directory)
	{
		start(directory, new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(8), task -> { Thread t = new Thread(task, "fromscratch-bank-storage"); t.setDaemon(true); return t; }));
	}
	public void start(DirectoryProvider directory, ExecutorService worker)
	{
		this.directory = directory; this.worker = worker; enabled = true; long token = epoch.incrementAndGet();
			dispatch.accept(() -> { if (!enabled || epoch.get() != token) { return; } character = null; run = null; busy = false; error = false; automatic = false; committedTrace = null; notice = null; pending.clear(); exchangeBaseline.clear(); latestOffers.clear(); observation = null; last = null; context++; valuation=null;valuationQueued=false;valuedRevision=null;valuedPrices=-1; names.clear(); publish(); tick(); });
	}
	public void stop() { enabled = false; epoch.incrementAndGet(); if (worker != null) { worker.shutdownNow(); } }
	public void tick()
	{
		if (!enabled) { return; }
		String key = source.characterKey();
		if (!Objects.equals(character, key))
		{
			character = key; context++; run = null; observation = null; busy = false; error = false; automatic = false; pending.clear(); exchangeBaseline.clear(); latestOffers.clear(); notice = null; committedTrace = null; names.clear();
			valuation=null;valuationQueued=false;valuedRevision=null;valuedPrices=-1;
			if (key == null) { message = "Log in to load this character's local progress."; publish(); }
			else { load(); } return;
		}
		if (character == null || error || (busy && !automatic)) { return; }
		flushExchangeBaseline();
		observation = source.observe();
		try
		{
			if (run != null && run.isActive() && run.isAnchored() && observation.isReady())
			{
				enqueue(observation);
				if (busy) { return; }
			}
			message = run != null && run.isActive() ? (run.isAnchored() ? "Bank progress updates automatically." : "Confirm a one-time bank anchor to finish upgrading this run.") : observation.getInstruction();
			publish();
		}
		catch (IllegalArgumentException | ArithmeticException ex) { error = true; message = "Bank quantities could not be safely updated: " + ex.getMessage(); publish(); }
	}
	/** Queue complete tick snapshots, not raw container events. A slow disk never blocks or reorders client events. */
	private void enqueue(BankObservation observed)
	{
		Pending tail = pending.peekLast();
		if (tail != null && tail.bank!=null && tail.bank.getSession().equals(observed.getSession()) && tail.bank.getBank().getQuantities().equals(observed.getBank().getQuantities())) { return; }
		if (pending.size() >= 256) { error = true; message = "Bank storage cannot keep up. Progress was preserved; retry loading before continuing."; publish(); return; }
		pending.addLast(new Pending(observed,null)); drain();
	}
	/** Passive copied GE packets share the same durable FIFO as bank snapshots, never a second quantity ledger. */
	public void exchange(GeObservation observed)
	{
		if(!enabled){return;}String key=source.characterKey();if(!Objects.equals(character,key)){tick();}
		if(key==null||!key.equals(character)||error){return;}
		latestOffers.put(observed.getSlot(),observed);
		if(run==null||!run.isActive()){exchangeBaseline.put(observed.getSlot(),observed);return;}
		// Keep room for ordinary bank snapshots during an extreme offer burst. The latest
		// cumulative value per slot survives overload; omitted intermediate fills remain uncertain.
		if(pending.size()>=128||exchangeBaseline.containsKey(observed.getSlot()))
		{
			exchangeBaseline.put(observed.getSlot(),new GeObservation(observed.getEventId(),observed.getSession(),observed.getAt(),observed.getSlot(),observed.getItemId(),observed.getName(),observed.getState(),observed.getTotal(),observed.getFilled(),observed.getLimitPrice(),observed.getReportedGp(),true,observed.isBeforeStart()));
			notice="GE updates arrived faster than storage. Their latest totals were retained; intermediate trade details may be incomplete.";publish();return;
		}
		pending.addLast(new Pending(null,observed));drain();
	}
	public void exchangeGap()
	{if(enabled){notice="Some GE history could not be observed. Bank progress continues automatically; trade details may be incomplete.";publish();}}
	private void flushExchangeBaseline()
	{
		if(run==null||!run.isActive()||exchangeBaseline.isEmpty()||pending.size()>=128){return;}
		List<GeObservation> baseline=new ArrayList<>(exchangeBaseline.values());exchangeBaseline.clear();baseline.sort(Comparator.comparing(GeObservation::getAt));
		for(GeObservation offer:baseline){exchange(offer);}
	}
	private void drain()
	{
		if (busy || error || run == null) { return; }
		while (!pending.isEmpty())
		{
			Pending update=pending.removeFirst();BankObservation next=update.bank;
			if (next!=null && run.getPhysical().equals(next.getBank().getQuantities()) && run.getHostSummary() != null && run.getHostSummary().getSession().equals(next.getSession())) { continue; }
			BankRun expected = run; busy = true; automatic = true;
			publish();
			// No speculative balances are projected; accounting/JSON/disk work all happen on this worker.
			submit(root ->
			{
				BankRun candidate = next==null?BankEngine.exchange(expected,update.offer):BankEngine.observe(expected, next.getBank(), next.getSession(), next.getAt(), next.getTrace(),prices.snapshot(next.getBank().getQuantities().keySet()));
				BankTimings.Trace trace=next==null?null:next.getTrace();BankTimings.mark(trace, BankTimings.Phase.WRITE_START);
				BankRun saved;
				try { saved = candidate == expected ? expected : repository.save(root, expected, candidate); }
				catch (IOException | RuntimeException ex) { if (trace != null) { trace.failed(); } throw ex; }
				BankTimings.mark(trace, BankTimings.Phase.WRITE_END); return saved;
			}, saved ->
			{
				run = saved; busy = false; automatic = false; committedTrace = next==null?null:next.getTrace(); notice = null; message = "Bank progress saved.";
				if(saved.getRecent().get(0).getExchange().getMatches().stream().anyMatch(m->m.isDeposit()&&m.getOrigin()==GeOffer.Origin.PREEXISTING))
				{notice="Possible return from a preexisting GE offer. Use Manual Bank Corrections if these bank counts should remain legacy.";}
				BankTimings.mark(committedTrace, BankTimings.Phase.PUBLISHED); publish(); drain();
			});
			return;
		}
		flushExchangeBaseline();
	}
	private void load()
	{
		busy = true; automatic = false; pending.clear(); message = "Loading local bank progress…"; publish();
		String key = character;
		submit(root -> repository.load(root, key).orElse(null), loaded -> { run = loaded; busy = false; error = false; notice = null; tick(); });
	}
	public void startRun(boolean attested)
	{
		act(() ->
		{
			if (run != null && run.isActive()) { throw new IllegalArgumentException("A run is already active."); }
			BankObservation fresh = requireBank();
			if (observation == null || !observation.isReady() || !observation.getBank().getQuantities().equals(fresh.getBank().getQuantities())) { throw new IllegalArgumentException("Your bank changed. Review the fresh bank before starting."); }
			BankRun candidate = BankEngine.start(character, run == null ? null : run.getPreviousRunId(), fresh.getBank(), attested, fresh.getSession(), Instant.now());
			latestOffers.forEach((slot,offer)->exchangeBaseline.put(slot,offer.beforeStart()));
			if (run != null)
			{
				candidate = new BankRun(candidate.getCharacter(), candidate.getRunId(), run.getPreviousRunId(), candidate.getStartedAt(), candidate.getRevision(), Math.incrementExact(run.getSequence()), candidate.getInitialBank(), candidate.getPhysical(), candidate.getBalances(), true, null, null, candidate.getRecent(), null);
			}
			save(candidate);
		});
	}
	public void anchor(UUID revision, com.fromscratch.model.ContainerSnapshot reviewedBank, boolean approveReductions)
	{ act(() -> { requireRevision(revision); BankObservation fresh = requireBank(); if (!fresh.getBank().getQuantities().equals(reviewedBank.getQuantities())) { throw new IllegalArgumentException("Your bank changed. Review the bank anchor again."); } save(BankEngine.anchor(run, fresh.getBank(), approveReductions, fresh.getSession(), Instant.now())); }); }
	public void correct(UUID revision, int id, long desired)
	{
		act(() ->
		{
			requireRevision(revision); BankObservation fresh = requireBank();
			if (!run.getPhysical().equals(fresh.getBank().getQuantities())) { throw new IllegalArgumentException("Your bank changed. Wait for the automatic update, then reopen Bank Corrections."); }
			save(BankEngine.correct(run, id, desired, fresh.getSession(), Instant.now()));
		});
	}
	public void reset(UUID revision)
	{ act(() -> { requireRevision(revision); save(BankEngine.reset(run, UUID.randomUUID(), Instant.now())); }); }
	public void buyback(UUID revision,String catalogId,UUID purchaseId)
	{buyback(revision,catalogId,purchaseId,null,p->{});}
	public void buyback(UUID revision,String catalogId,UUID purchaseId,com.fromscratch.pouch.PouchObservation preview,Consumer<com.fromscratch.buyback.BuybackPurchase> committed)
	{
		act(()->
		{
			requireRevision(revision);BankObservation fresh=requireBank();
			if(!run.getPhysical().equals(fresh.getBank().getQuantities())){throw new IllegalArgumentException("Your bank changed. Wait for the automatic update, then reopen Legacy Buybacks.");}
			com.fromscratch.buyback.BuybackEntry entry=com.fromscratch.buyback.BuybackCatalog.entry(catalogId);
			for(Map.Entry<String,Integer> role:entry.getRoles().entrySet())
			{
				boolean verified=com.fromscratch.buyback.BuybackCatalog.variants(role.getKey()).entrySet().stream().anyMatch(v->v.getValue().equals(role.getValue())&&run.physical(v.getKey())>0&&source.buybackDefinition(v.getKey()));
				if(!verified){throw new IllegalArgumentException("A required item definition is unavailable or is not qualifying untradeable equipment. No GP was spent.");}
			}
			com.fromscratch.pouch.PouchObservation contents=entry.getId().equals("rune_pouch_family")?source.pouch():com.fromscratch.pouch.PouchObservation.unknown();
			if(preview!=null&&(!contents.verified()||!preview.getQuantities().equals(contents.getQuantities())||preview.getPouchId()!=contents.getPouchId())){throw new IllegalArgumentException(contents.verified()?"Rune Pouch contents changed. Review them again with the bank open.":contents.getProblem());}
			BankRun candidate=BankEngine.buyback(run,catalogId,purchaseId,fresh.getSession(),Instant.now(),contents);
			save(candidate,()->committed.accept(candidate.getBuybacks().getPurchases().get(candidate.getBuybacks().getPurchases().size()-1)));
		});
	}
	public void recordExpense(UUID revision,long gp,String reason)
	{ act(()->{requireRevision(revision);save(BankEngine.expense(run,gp,reason,UUID.randomUUID(),Instant.now()));}); }
	/** Read varbits only on a relevant user request, never a personal-container observation loop. */
	public void pouchPreview(Consumer<com.fromscratch.pouch.PouchObservation> callback)
	{
		dispatch.accept(()->{callback.accept(enabled&&Objects.equals(character,source.characterKey())?source.pouch():com.fromscratch.pouch.PouchObservation.unknown());});
	}
	public void retry() { dispatch.accept(() -> { if (enabled && !busy && character != null) { error = false; load(); } }); }
	public void history(boolean manual, BankHistory.Cursor cursor, Consumer<BankHistory.Page> callback)
	{
		dispatch.accept(() ->
		{
			if (!enabled || run == null || !run.isActive()) { callback.accept(new BankHistory.Page(Collections.emptyList(), null, "No active bank history.")); return; }
			BankRun captured = run; long token = epoch.get(); long capturedContext = context; DirectoryProvider provider = directory;
			try { worker.execute(() ->
			{
				BankHistory.Page page;
				try { page = repository.history(provider.get(), captured, cursor, manual); }
				catch (IOException | RuntimeException ex) { page = new BankHistory.Page(Collections.emptyList(), null, "History could not be read. Existing records were preserved."); }
				BankHistory.Page result = page;
				dispatch.accept(() ->
				{
					if (!enabled || epoch.get() != token || context != capturedContext || run == null || !Objects.equals(run.getRunId(), captured.getRunId()) || !captured.getCharacter().equals(source.characterKey())) { callback.accept(new BankHistory.Page(Collections.emptyList(), null, "Character or run changed. Reopen Bank History.")); return; }
					List<BankHistory.Row> rows = new ArrayList<>();
					for (BankHistory.Row row : result.getRows()) { String name = names.computeIfAbsent(row.getItemId(), source::itemName); rows.add(new BankHistory.Row(row.getAt(), row.getItemId(), name == null ? "Unknown item" : name, row.getChange(),row.getCategory(),row.getPurchase())); }
					callback.accept(new BankHistory.Page(rows, result.getNext(), result.getMessage()));
				});
			}); }
			catch (RejectedExecutionException ex) { callback.accept(new BankHistory.Page(Collections.emptyList(), null, "Storage is busy. Close this view and retry.")); }
		});
	}
	private void requireRevision(UUID revision)
	{
		if (run == null || !run.isActive() || !run.getRevision().equals(revision)) { throw new IllegalArgumentException("Progress changed. Reopen this tool before applying changes."); }
	}
	public void exchangeHistory(boolean buy,BankHistory.Cursor cursor,Consumer<GeHistory.Page> callback)
	{
		dispatch.accept(() ->
		{
			if(!enabled||run==null||!run.isActive()){callback.accept(new GeHistory.Page(Collections.emptyList(),null,"No active GE history."));return;}
			BankRun captured=run;long token=epoch.get(),capturedContext=context;DirectoryProvider provider=directory;
			try{worker.execute(() ->
			{
				GeHistory.Page page;try{page=repository.exchangeHistory(provider.get(),captured,cursor,buy);}catch(IOException|RuntimeException ex){page=new GeHistory.Page(Collections.emptyList(),null,"GE history could not be read; existing records were preserved.");}
				GeHistory.Page result=page;dispatch.accept(() ->
				{
					if(!enabled||epoch.get()!=token||context!=capturedContext||run==null||!Objects.equals(run.getRunId(),captured.getRunId())||!captured.getCharacter().equals(source.characterKey())){callback.accept(new GeHistory.Page(Collections.emptyList(),null,"Character or run changed. Reopen GE history."));return;}callback.accept(result);
				});
			});}catch(RejectedExecutionException ex){callback.accept(new GeHistory.Page(Collections.emptyList(),null,"History is busy. Retry shortly."));}
		});
	}
	private BankObservation requireBank()
	{
		if (character == null || !character.equals(source.characterKey())) { throw new IllegalArgumentException("Character changed. Wait for local progress to reload."); }
		BankObservation fresh = source.observe(); if (!fresh.isReady()) { throw new IllegalArgumentException(fresh.getInstruction()); } return fresh;
	}
	private void save(BankRun next){save(next,()->{});}
	private void save(BankRun next,Runnable acknowledged)
	{
		if (next == run) { publish(); return; }
		BankRun expected = run; busy = true; automatic = false; message = "Saving bank progress…"; publish();
		submit(root -> repository.save(root, expected, next), saved -> { run = saved; busy = false; notice = null; message = "Bank progress saved."; publish();acknowledged.run(); flushExchangeBaseline();drain(); });
	}
	private void act(Runnable action)
	{
		dispatch.accept(() ->
		{
			if (!enabled || busy || error || character == null) { return; }
			if (!character.equals(source.characterKey())) { tick(); return; }
			try { notice = null; action.run(); }
			catch (IllegalArgumentException | ArithmeticException ex) { notice = ex.getMessage(); publish(); }
		});
	}
	private void publish()
	{
		if(run!=null){prices.register(com.fromscratch.buyback.BuybackEngine.balances(run).keySet(),source::priceId);}if(observation!=null&&observation.isReady()){prices.register(observation.getBank().getQuantities().keySet(),source::priceId);}
		refreshValuation();
		String visibleMessage = notice == null ? message : notice;
		boolean ready = !busy && !error && observation != null && observation.isReady();
		boolean previewChanged = (run == null || !run.isAnchored()) && last != null && observation != null && observation.isReady()
			&& (last.getObservation() == null || !last.getObservation().isReady() || !last.getObservation().getBank().getQuantities().equals(observation.getBank().getQuantities()));
		if (last != null && Objects.equals(last.getRun(), run) && last.isBusy() == busy && last.isError() == error && last.ready() == ready
			&& last.getMessage().equals(visibleMessage) && Objects.equals(last.getValuation(),valuation) && !previewChanged) { return; }
		Set<Integer> ids = new TreeSet<>();
		if (run != null) { ids.addAll(run.getPhysical().keySet()); ids.addAll(run.getBalances().keySet()); if (run.getInitialBank() != null) { ids.addAll(run.getInitialBank().getQuantities().keySet()); } run.getRecent().forEach(a -> { ids.addAll(a.getBalanceChanges().keySet()); ids.addAll(a.getPhysicalChanges().keySet()); }); }
		if (observation != null && observation.isReady()) { ids.addAll(observation.getBank().getQuantities().keySet()); }
		for (int id : ids) { names.computeIfAbsent(id, key -> { String name = source.itemName(key); return name == null || name.isEmpty() ? "Item #" + key : name; }); }
		BankView view = new BankView(run, observation, names, busy, error, visibleMessage, committedTrace,valuation);
		// Tick/time changes alone need not rebuild the entire Swing panel every game tick.
		if (last == null || !Objects.equals(last.getRun(), view.getRun()) || !last.getNames().equals(view.getNames()) || last.isBusy() != busy || last.isError() != error
			|| !last.getMessage().equals(visibleMessage) || !Objects.equals(last.getValuation(),valuation) || last.ready() != view.ready() || previewChanged) { last = view; display.accept(view); }
	}
	/** Derived values use the storage worker's ordering, but never perform network/disk work or create a save. */
	private void refreshValuation()
	{
		if(busy||error||run==null||!run.isActive()||!run.isAnchored()||valuationQueued||(run.getRevision().equals(valuedRevision)&&prices.version()==valuedPrices)){return;}
		BankRun captured=run;long priceVersion=prices.version(),token=epoch.get(),capturedContext=context;valuationQueued=true;
		try{worker.execute(()->{BankValuation result=BankValuation.calculate(captured,prices.snapshot(com.fromscratch.buyback.BuybackEngine.balances(captured).keySet()));dispatch.accept(()->{
			if(!enabled||epoch.get()!=token||context!=capturedContext){return;}valuationQueued=false;
			if(run!=null&&run.getRevision().equals(captured.getRevision())&&captured.getCharacter().equals(source.characterKey())){valuation=result;valuedRevision=captured.getRevision();valuedPrices=priceVersion;publish();}
		});});}catch(RejectedExecutionException ex){valuationQueued=false;}
	}
	private <T> void submit(Storage<T> work, Consumer<T> success)
	{
		long token = epoch.get(); long capturedContext = context; String key = character;
		DirectoryProvider capturedDirectory = directory;
		try
		{
			worker.execute(() ->
			{
				if (!enabled || token != epoch.get() || Thread.currentThread().isInterrupted()) { return; }
				T result = null; String failure = null;
				try { result = work.run(capturedDirectory.get()); } catch (IOException | RuntimeException ex) { failure = ex instanceof IOException ? ex.getMessage() : "Bank storage failed validation. Existing files were preserved."; }
				T value = result; String errorMessage = failure;
				dispatch.accept(() ->
				{
					if (!enabled || token != epoch.get() || capturedContext != context || !Objects.equals(key, character) || !Objects.equals(key, source.characterKey())) { return; }
					if (errorMessage == null) { success.accept(value); }
					else { busy = false; automatic = false; error = true; message = errorMessage; publish(); }
				});
			});
		}
		catch (RejectedExecutionException ex) { busy = false; error = true; message = "Bank storage is busy. Retry loading progress."; publish(); }
	}
	@FunctionalInterface public interface DirectoryProvider { Filepath get() throws IOException; }
	@FunctionalInterface private interface Storage<T> { T run(Filepath root) throws IOException; }
}
