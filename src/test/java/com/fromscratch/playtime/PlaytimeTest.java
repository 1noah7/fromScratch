package com.fromscratch.playtime;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.bank.*;
import com.fromscratch.persistence.*;
import java.util.*;
import net.runelite.client.util.Filepath;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

/** Real small-checkpoint IO plus deterministic GameTick and concurrent-client sequences; no sleeps. */
public class PlaytimeTest
{
	@Rule public TemporaryFolder temporary=new TemporaryFolder();
	private final PlaytimeRepository repo=new PlaytimeRepository(new BankRunCodec(SnapshotFixtures.gson()));
	private final BankRun run=BankFixtures.run(1);
	private Filepath root;
	@Before public void setup() { root=Filepath.Unchecked.getRooted(temporary.getRoot().toPath()); }
	private Filepath current(UUID runId) { return root.join("characters",run.getCharacter(),"playtime","playtime-"+runId+".json"); }
	private static String read(Filepath file) throws java.io.IOException
	{ try(java.io.Reader reader=file.openReader()) { StringBuilder text=new StringBuilder();char[] buffer=new char[4096];int n;while((n=reader.read(buffer))!=-1) { text.append(buffer,0,n); }return text.toString(); } }
	@Test public void truncatesMinutesHoursDaysWithSingularPluralAndNoWallClock()
	{
		long[] ticks={0,99,100,700,5999,6000,9000,143999,144000,192000,1008000,Long.MAX_VALUE};
		String[] labels={"0 minutes","0 minutes","1 minute","7 minutes","59 minutes","1 hour","1 hour","23 hours","1 day","1 day","7 days",(Long.MAX_VALUE/100/1440)+" days"};
		for(int i=0;i<ticks.length;i++) { assertEquals(labels[i],Playtime.format(ticks[i])); }
	}
	@Test public void onlyLoadedLoggedInTicksCountAndReconnectDoesNotDuplicate() throws Exception
	{
		BankFixtures.Worker worker=new BankFixtures.Worker();List<PlaytimeTracker.View> views=new ArrayList<>();
		PlaytimeTracker tracker=new PlaytimeTracker(repo,Runnable::run,views::add);tracker.start(()->root,worker);
		tracker.tick(run,false,0);worker.runAll();
		for(int i=1;i<=50;i++) { tracker.tick(run,true,i);tracker.tick(run,true,i); }
		tracker.interrupted();worker.runAll();tracker.tick(run,false,50);tracker.tick(run,true,50);
		for(int i=51;i<=100;i++) { tracker.tick(run,true,i); }
		tracker.stop();worker.runAll();assertEquals(100,repo.load(root,run.getCharacter(),run.getRunId()).getTicks());
	}
	@Test public void batchesCheckpointsAndRestartsWithoutConvertingOldRunAge() throws Exception
	{
		BankFixtures.Worker worker=new BankFixtures.Worker();PlaytimeTracker tracker=new PlaytimeTracker(repo,Runnable::run,v->{});tracker.start(()->root,worker);
		tracker.tick(run,true,0);worker.runAll();assertEquals(0,repo.load(root,run.getCharacter(),run.getRunId()).getTicks());
		for(int i=1;i<=99;i++) { tracker.tick(run,true,i); }
		assertTrue(worker.tasks.isEmpty());assertEquals(0,repo.load(root,run.getCharacter(),run.getRunId()).getTicks());
		tracker.tick(run,true,100);assertEquals(1,worker.tasks.size());worker.runAll();
		tracker.tick(run,true,101);tracker.stop();worker.runAll();assertEquals(101,repo.load(root,run.getCharacter(),run.getRunId()).getTicks());
		BankFixtures.Worker second=new BankFixtures.Worker();List<PlaytimeTracker.View> restored=new ArrayList<>();PlaytimeTracker again=new PlaytimeTracker(repo,Runnable::run,restored::add);again.start(()->root,second);again.tick(run,true,500);second.runAll();assertEquals(101,restored.get(0).getTicks());again.stop();second.runAll();
	}
	@Test public void staleConcurrentIntervalsAndRepeatedBatchesCannotDoubleCount() throws Exception
	{
		Playtime initial=repo.load(root,run.getCharacter(),run.getRunId());UUID batch=UUID.randomUUID();
		Playtime saved=repo.add(root,initial,100,batch);
		assertEquals(saved,repo.add(root,initial,100,batch));assertEquals(saved,repo.add(root,initial,200,UUID.randomUUID()));
		assertEquals(0,repo.load(root,run.getCharacter(),UUID.randomUUID()).getTicks());
		assertEquals(0,repo.load(root,SnapshotFixtures.CHARACTER_B,run.getRunId()).getTicks());
	}
	@Test public void exportCheckpointAndRestoreBarrierFlushPartialTicksWithoutCountingDuringRestore()throws Exception
	{
		BankFixtures.Worker worker=new BankFixtures.Worker();PlaytimeTracker tracker=new PlaytimeTracker(repo,Runnable::run,v->{});tracker.start(()->root,worker);tracker.tick(run,true,0);worker.runAll();
		for(int i=1;i<=17;i++){tracker.tick(run,true,i);}boolean[] ready={false};tracker.checkpoint(()->ready[0]=true);assertFalse(ready[0]);worker.runAll();assertTrue(ready[0]);assertEquals(17,repo.load(root,run.getCharacter(),run.getRunId()).getTicks());
		tracker.tick(run,true,18);tracker.beforeRestore(()->{});tracker.tick(run,true,19);worker.runAll();assertEquals(18,repo.load(root,run.getCharacter(),run.getRunId()).getTicks());
		tracker.afterRestore();tracker.tick(run,true,20);worker.runAll();tracker.tick(run,true,21);tracker.stop();worker.runAll();assertEquals(19,repo.load(root,run.getCharacter(),run.getRunId()).getTicks());
	}
	@Test public void corruptOrMissingCounterPreservesRecoveryEvidence() throws Exception
	{
		Playtime initial=repo.load(root,run.getCharacter(),run.getRunId());repo.add(root,initial,100,UUID.randomUUID());
		Filepath counter=current(run.getRunId()),backup=root.join("characters",run.getCharacter(),"playtime","playtime-"+run.getRunId()+"-backup.json");
		String prior=read(backup);counter.write("corrupt");
		try { repo.load(root,run.getCharacter(),run.getRunId());fail(); } catch(SaveException expected) { }
		assertEquals("corrupt",read(counter));assertEquals(prior,read(backup));counter.delete();
		try { repo.load(root,run.getCharacter(),run.getRunId());fail(); } catch(SaveException expected) { }
		assertFalse(counter.exists());assertTrue(backup.exists());
	}
	@Test public void interruptedCandidateNeverReplacesCommittedCounter() throws Exception
	{
		Playtime initial=repo.load(root,run.getCharacter(),run.getRunId());UUID batch=UUID.randomUUID();
		Filepath pending=root.join("characters",run.getCharacter(),"playtime","playtime-"+run.getRunId()+"-pending-"+batch+".json");pending.write("interrupted candidate");
		try { repo.add(root,initial,100,batch);fail(); } catch(java.io.IOException expected) { }
		assertEquals(initial,repo.load(root,run.getCharacter(),run.getRunId()));assertEquals("interrupted candidate",read(pending));
	}
}
