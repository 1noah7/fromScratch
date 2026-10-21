package com.fromscratch.wealth;

import java.io.IOException;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.*;
import javax.inject.*;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemClient;
import net.runelite.http.api.item.ItemPrice;

/** Bulk RuneLite GE-guide cache. Only its dedicated worker performs HTTP; banking never waits for prices. */
@Singleton
public final class PriceService
{
	@FunctionalInterface public interface Feed { Map<Integer,Long> fetch() throws IOException; }
	private final Feed feed;
	private final Supplier<Instant> clock;
	private final Map<Integer,Integer> identities=new ConcurrentHashMap<>();
	private final AtomicLong version=new AtomicLong();
	private volatile Market market=new Market(Collections.emptyMap(),null,false);
	private ScheduledExecutorService executor;
	private ScheduledFuture<?> task;
	@Inject public PriceService(ItemClient client)
	{
		this(()->{ItemPrice[] items=client.getPrices();if(items==null){throw new IOException("Price feed unavailable");}Map<Integer,Long> prices=new HashMap<>();for(ItemPrice p:items){if(p.getId()<0||p.getPrice()<0){throw new IOException("Invalid price feed");}prices.put(p.getId(),(long)p.getPrice());}return prices;},Instant::now);
	}
	public PriceService(Feed feed,Supplier<Instant> clock) { this.feed=feed;this.clock=clock; }
	public static PriceService unavailable() { return new PriceService(()->{throw new IOException("No live pricing in this fixture");},Instant::now); }
	public void start()
	{
		if(executor!=null){return;}
		executor=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"fromscratch-market-prices");t.setDaemon(true);return t;});
		task=executor.scheduleWithFixedDelay(()->{Market current=market;if(current.received==null||current.failed||!clock.get().isBefore(current.received.plus(Duration.ofMinutes(30)))){refresh();}},0,60,TimeUnit.SECONDS);
	}
	public void stop() { if(task!=null){task.cancel(true);task=null;}if(executor!=null){executor.shutdownNow();executor=null;} }
	/** Worker-only refresh; a failed fetch retains prior verified records and marks them stale. */
	public void refresh()
	{
		try
		{
			Map<Integer,Long> result=new HashMap<>(Objects.requireNonNull(feed.fetch()));
			result.forEach((id,p)->{if(id==null||id<0||p==null||p<0){throw new IllegalArgumentException("Invalid price record");}});
			market=new Market(Collections.unmodifiableMap(result),clock.get(),false);
		}
		catch(IOException|RuntimeException ex){Market old=market;market=new Market(old.prices,old.received,true);}
		version.incrementAndGet();
	}
	/** Called on the client thread; resolve only new item definitions, never perform pricing/network here. */
	public void register(Collection<Integer> ids,IntUnaryOperator notedIdentity)
	{
		for(int id:ids){if(!identities.containsKey(id)){try{int normalized=notedIdentity.applyAsInt(id);if(normalized>=0){identities.put(id,normalized);}}catch(RuntimeException ex){/* Definition unavailable: leave unknown and retry on a later publication. */}}}
	}
	public long version() { return version.get(); }
	public Map<Integer,PriceQuote> snapshot(Collection<Integer> ids)
	{
		Map<Integer,PriceQuote> result=new HashMap<>();Market captured=market;Map<Integer,Long> records=captured.prices;Instant at=captured.received,now=clock.get();
		boolean stale=captured.failed||at==null||!now.isBefore(at.plus(Duration.ofMinutes(30)));
		for(int id:ids)
		{
			if(id==ItemID.COINS){result.put(id,PriceQuote.coins(now));continue;}
			Integer normalized=identities.get(id);Long price=normalized==null?null:records.get(normalized);
			result.put(id,price==null?PriceQuote.unknown(normalized==null?id:normalized):new PriceQuote(normalized,price,at,PriceQuote.Source.RUNELITE_GE_GUIDE,stale));
		}
		return Collections.unmodifiableMap(result);
	}
	private static final class Market
	{
		final Map<Integer,Long> prices;final Instant received;final boolean failed;
		Market(Map<Integer,Long> prices,Instant received,boolean failed){this.prices=prices;this.received=received;this.failed=failed;}
	}
}
