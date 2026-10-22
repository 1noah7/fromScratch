package com.fromscratch.persistence;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.bank.*;
import com.fromscratch.model.ContainerSnapshot;
import com.fromscratch.pouch.*;
import java.lang.management.ManagementFactory;
import java.util.*;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.util.Filepath;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

/** Synthetic worker benchmark: one ordinary atomic commit per snapshot, not one write per rune. */
public class PouchPerformanceTest
{
	@Rule public TemporaryFolder temporary=new TemporaryFolder();
	@Test public void largeBankWithPendingRunesKeepsFiniteMatchingAndOneOrderedCommitPerChange()throws Exception
	{
		Map<Integer,Long> q=new TreeMap<>();for(int i=0;i<1200;i++){q.put(100000+i,1L);}q.put(ItemID.BH_RUNE_POUCH,1L);
		Filepath root=Filepath.Unchecked.getRooted(temporary.getRoot().toPath());BankRepository repo=new BankRepository(new BankRunCodec(SnapshotFixtures.gson()),SnapshotFixtures.codec());UUID session=UUID.randomUUID();
		BankRun run=repo.save(root,null,BankEngine.start(SnapshotFixtures.CHARACTER_A,null,new ContainerSnapshot(q,q.size()),true,session,SnapshotFixtures.TIME));
		run=repo.save(root,run,BankEngine.pouch(run,new PouchAction(PouchAction.Kind.REGISTER,Collections.singletonMap(ItemID.NATURERUNE,500L),false,false,"Player verified legacy stack."),session,SnapshotFixtures.TIME,Collections.emptyMap()));long sequence=run.getSequence();
		long start=System.nanoTime(),cpu=ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
		for(int i=1;i<=64;i++){q.put(ItemID.NATURERUNE,10L*i);run=repo.save(root,run,BankEngine.observe(run,new ContainerSnapshot(q,q.size()),session,SnapshotFixtures.TIME));}
		double wallMs=(System.nanoTime()-start)/1e6,cpuMs=(ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime()-cpu)/1e6;
		assertEquals(sequence+64,run.getSequence());assertEquals(640,run.physical(ItemID.NATURERUNE));assertEquals(140,run.balance(ItemID.NATURERUNE));assertEquals(Long.valueOf(500),run.getBuybacks().getPouch().getHeld().get(ItemID.NATURERUNE));assertTrue(run.getBuybacks().getPouch().getRemaining().isEmpty());assertEquals(run,repo.load(root,run.getCharacter()).get());
		System.out.printf(Locale.ROOT,"POUCH_1200_ITEMS commits=64 worker_wall_ms=%.3f worker_cpu_ms=%.3f io_scheduling_ms=%.3f mean_commit_ms=%.3f%n",wallMs,cpuMs,wallMs-cpuMs,wallMs/64);
	}
}
