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
	private final Deque<BankObservation> pending = new ArrayDeque<>();
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
			dispatch.accept(() -> { if (!enabled || epoch.get() != token) { return; } character = null; run = null; busy = false; error = false; automatic = false; committedTrace = null; notice = null; pending.clear(); observation = null; last = null; context++; valuation=null;valuationQueued=false;valuedRevision=null;valuedPrices=-1; names.clear(); publish(); tick(); });
	}
	public void stop() { enabled = false; epoch.incrementAndGet(); if (worker != null) { worker.shutdownNow(); } }
	public void tick()
	{
		if (!enabled) { return; }
		String key = source.characterKey();
		if (!Objects.equals(character, key))
		{
			character = key; context++; run = null; observation = null; busy = false; error = false; automatic = false; pending.clear(); notice = null; committedTrace = null; names.clear();
			valuation=null;valuationQueued=false;valuedRevision=null;valuedPrices=-1;
			if (key == null) { message = "Log in to load this character's local progress."; publish(); }
			else { load(); } return;
		}
		if (character == null || error || (busy && !automatic)) { return; }
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
		BankObservation tail = pending.peekLast();
		if (tail != null && tail.getSession().equals(observed.getSession()) && tail.getBank().getQuantities().equals(observed.getBank().getQuantities())) { return; }
		if (pending.size() >= 256) { error = true; message = "Bank storage cannot keep up. Progress was preserved; retry loading before continuing."; publish(); return; }
		pending.addLast(observed); drain();
	}
	private void drain()
	{
		if (busy || error || run == null) { return; }
		while (!pending.isEmpty())
		{
			BankObservation next = pending.removeFirst();
			if (run.getPhysical().equals(next.getBank().getQuantities()) && run.getHostSummary() != null && run.getHostSummary().getSession().equals(next.getSession())) { continue; }
			BankRun expected = run; busy = true; automatic = true;
			publish();
			// No speculative balances are projected; accounting/JSON/disk work all happen on this worker.
			submit(root ->
			{
				BankRun candidate = BankEngine.observe(expected, next.getBank(), next.getSession(), next.getAt(), next.getTrace(),prices.snapshot(next.getBank().getQuantities().keySet()));
				BankTimings.mark(next.getTrace(), BankTimings.Phase.WRITE_START);
				BankRun saved;
				try { saved = candidate == expected ? expected : repository.save(root, expected, candidate); }
				catch (IOException | RuntimeException ex) { if (next.getTrace() != null) { next.getTrace().failed(); } throw ex; }
				BankTimings.mark(next.getTrace(), BankTimings.Phase.WRITE_END); return saved;
			}, saved ->
			{
				run = saved; busy = false; automatic = false; committedTrace = next.getTrace(); notice = null; message = "Bank progress saved.";
				BankTimings.mark(committedTrace, BankTimings.Phase.PUBLISHED); publish(); drain();
			});
			return;
		}
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
	public void recordExpense(UUID revision,long gp,String reason)
	{ act(()->{requireRevision(revision);save(BankEngine.expense(run,gp,reason,UUID.randomUUID(),Instant.now()));}); }
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
					for (BankHistory.Row row : result.getRows()) { String name = names.computeIfAbsent(row.getItemId(), source::itemName); rows.add(new BankHistory.Row(row.getAt(), row.getItemId(), name == null ? "Unknown item" : name, row.getChange())); }
					callback.accept(new BankHistory.Page(rows, result.getNext(), result.getMessage()));
				});
			}); }
			catch (RejectedExecutionException ex) { callback.accept(new BankHistory.Page(Collections.emptyList(), null, "Storage is busy. Close this view and retry.")); }
		});
	}
	private void requireRevision(UUID revision)
	{ if (run == null || !run.isActive() || !run.getRevision().equals(revision)) { throw new IllegalArgumentException("Progress changed. Reopen this tool before applying changes."); } }
	private BankObservation requireBank()
	{
		if (character == null || !character.equals(source.characterKey())) { throw new IllegalArgumentException("Character changed. Wait for local progress to reload."); }
		BankObservation fresh = source.observe(); if (!fresh.isReady()) { throw new IllegalArgumentException(fresh.getInstruction()); } return fresh;
	}
	private void save(BankRun next)
	{
		if (next == run) { publish(); return; }
		BankRun expected = run; busy = true; automatic = false; message = "Saving bank progress…"; publish();
		submit(root -> repository.save(root, expected, next), saved -> { run = saved; busy = false; notice = null; message = "Bank progress saved."; publish(); });
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
		if(run!=null){prices.register(run.getBalances().keySet(),source::priceId);}if(observation!=null&&observation.isReady()){prices.register(observation.getBank().getQuantities().keySet(),source::priceId);}
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
		try{worker.execute(()->{BankValuation result=BankValuation.calculate(captured,prices.snapshot(captured.getBalances().keySet()));dispatch.accept(()->{
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
