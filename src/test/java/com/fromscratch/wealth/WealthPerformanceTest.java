package com.fromscratch.wealth;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.bank.*;
import com.fromscratch.model.ContainerSnapshot;
import com.fromscratch.persistence.*;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.*;
import net.runelite.client.util.Filepath;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

/** Diagnostic timings rather than fragile machine-dependent deadlines. All inputs are synthetic. */
public class WealthPerformanceTest
{
	@Rule public TemporaryFolder temporary=new TemporaryFolder();
	@Test public void twelveHundredPricedItemsAndLargeFinancialHistory()throws Exception
	{
		Map<Integer,Long> physical=new TreeMap<>(),values=new HashMap<>();for(int i=0;i<1200;i++){physical.put(100000+i,1L);values.put(100000+i,100L);}
		PriceService prices=new PriceService(()->values,()->SnapshotFixtures.TIME);prices.register(physical.keySet(),id->id);prices.refresh();UUID session=UUID.randomUUID();BankRepository repository=new BankRepository(new BankRunCodec(SnapshotFixtures.gson()),SnapshotFixtures.codec());Filepath root=Filepath.Unchecked.getRooted(temporary.getRoot().toPath());
		BankRun run=BankEngine.start(SnapshotFixtures.CHARACTER_A,null,new ContainerSnapshot(Collections.emptyMap(),0),true,session,SnapshotFixtures.TIME);repository.save(root,null,run);
		BankRun next=BankEngine.observe(run,new ContainerSnapshot(physical,1200),session,SnapshotFixtures.TIME,null,prices.snapshot(physical.keySet()));repository.save(root,run,next);run=next;
		long begin=System.nanoTime(),cpu=ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();for(int i=0;i<100;i++){assertEquals(Long.valueOf(120000),BankValuation.calculate(run,prices.snapshot(physical.keySet())).getSubtotalGp());}
		double valueMs=(System.nanoTime()-begin)/1e6,valueCpu=(ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime()-cpu)/1e6;begin=System.nanoTime();cpu=ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
		for(int i=0;i<96;i++){physical.put(100000,(long)i+2);next=BankEngine.observe(run,new ContainerSnapshot(physical,1200),session,SnapshotFixtures.TIME.plusSeconds(i+1),null,prices.snapshot(physical.keySet()));repository.save(root,run,next);run=next;}
		double writeMs=(System.nanoTime()-begin)/1e6,writeCpu=(ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime()-cpu)/1e6;BankRun saved=repository.load(root,SnapshotFixtures.CHARACTER_A).get();assertEquals(1296,saved.getFinancial().getEventCount());assertEquals(129600,saved.getFinancial().getGainedGp());
		System.out.printf(Locale.ROOT,"WEALTH_1200_ITEMS value_100_wall_ms=%.3f value_cpu_ms=%.3f financial_commits=96 worker_wall_ms=%.3f worker_cpu_ms=%.3f io_scheduling_ms=%.3f durable_events=%d%n",valueMs,valueCpu,writeMs,writeCpu,Math.max(0,writeMs-writeCpu),saved.getFinancial().getEventCount());
		begin=System.nanoTime();cpu=ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
		for(int i=0;i<100;i++){Long candidate=com.fromscratch.persistence.PeakProgressRepository.candidate(saved,prices.snapshot(physical.keySet()));assertEquals(Long.valueOf(129600),repository.peak(root,saved,candidate));}
		System.out.printf(Locale.ROOT,"PEAK_1200_ITEMS history_operations=98 durable_events=1296 checkpoints=100 counter_replacements=1 worker_wall_ms=%.3f worker_cpu_ms=%.3f%n",(System.nanoTime()-begin)/1e6,(ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime()-cpu)/1e6);
	}
	@Test public void actualPriceSchedulerUsesItsOwnWorkerAndStops()throws Exception
	{
		CompletableFuture<String> fetched=new CompletableFuture<>();PriceService prices=new PriceService(()->{fetched.complete(Thread.currentThread().getName());return Collections.emptyMap();},java.time.Instant::now);
		try{prices.start();assertEquals("fromscratch-market-prices",fetched.get(5,TimeUnit.SECONDS));}finally{prices.stop();}
	}
}
