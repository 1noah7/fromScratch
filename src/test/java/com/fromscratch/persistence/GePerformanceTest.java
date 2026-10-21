package com.fromscratch.persistence;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.bank.*;
import com.fromscratch.ge.*;
import com.fromscratch.model.ContainerSnapshot;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.*;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.util.Filepath;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

/** Diagnostic worker workload, not native gameplay latency or a fragile filesystem speed threshold. */
public class GePerformanceTest
{
	@Rule public TemporaryFolder temporary=new TemporaryFolder();
	@Test public void largeBankAndGeJournalStayBoundedAndPageWithoutFullHistoryReconstruction()throws Exception
	{
		Map<Integer,Long> bank=new TreeMap<>();bank.put(ItemID.WOODEN_SHIELD,1L);for(int i=0;i<1199;i++){bank.put(100000+i,1L);}
		Filepath root=Filepath.Unchecked.getRooted(temporary.getRoot().toPath());BankRepository repository=new BankRepository(new BankRunCodec(SnapshotFixtures.gson()),SnapshotFixtures.codec());
		BankRun run=repository.save(root,null,BankEngine.start(SnapshotFixtures.CHARACTER_A,null,new ContainerSnapshot(bank,1200),true,UUID.randomUUID(),Instant.now()));UUID session=UUID.randomUUID();
		run=repository.save(root,run,BankEngine.exchange(run,new GeObservation(UUID.randomUUID(),session,Instant.now(),0,ItemID.WOODEN_SHIELD,"Wooden shield",GrandExchangeOfferState.BUYING,1000,0,100,0,false)));
		long wall=System.nanoTime(),cpu=ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
		for(int q=1;q<=128;q++){run=repository.save(root,run,BankEngine.exchange(run,new GeObservation(UUID.randomUUID(),session,Instant.now(),0,ItemID.WOODEN_SHIELD,"Wooden shield",GrandExchangeOfferState.BUYING,1000,q,100,q*100L,false)));}
		double wallMs=(System.nanoTime()-wall)/1e6,cpuMs=(ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime()-cpu)/1e6;
		assertEquals(128,run.getExchange().getPurchases());assertTrue(run.getExchange().getRecent().size()<=30);assertTrue(run.getExchange().getCredits().size()<=256);assertEquals(25,repository.exchangeHistory(root,run,null,true).getRows().size());assertEquals(run,repository.load(root,run.getCharacter()).get());
		System.out.printf(Locale.ROOT,"GE_1200_ITEMS commits=128 worker_wall_ms=%.3f worker_cpu_ms=%.3f io_scheduling_ms=%.3f retained_rows=%d%n",wallMs,cpuMs,wallMs-cpuMs,run.getExchange().getRecent().size());
	}
}
