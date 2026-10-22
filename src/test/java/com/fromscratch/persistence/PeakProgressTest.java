package com.fromscratch.persistence;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.bank.*;
import com.fromscratch.buyback.*;
import com.fromscratch.model.ContainerSnapshot;
import com.fromscratch.wealth.*;
import java.util.*;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.util.Filepath;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class PeakProgressTest
{
	@Rule public TemporaryFolder temp=new TemporaryFolder();
	private Filepath root;private BankRepository repository;private BankRunCodec codec;private PeakProgressRepository peak;
	@Before public void setup(){root=Filepath.Unchecked.getRooted(temp.getRoot().toPath());codec=new BankRunCodec(SnapshotFixtures.gson());repository=new BankRepository(codec,SnapshotFixtures.codec());peak=new PeakProgressRepository(codec);}
	private BankRun run()throws Exception
	{BankRun r=BankEngine.start(SnapshotFixtures.CHARACTER_A,null,new ContainerSnapshot(Collections.singletonMap(ItemID.TZHAAR_CAPE_FIRE,1L),1),true,UUID.randomUUID(),SnapshotFixtures.TIME);r=repository.save(root,null,r);Map<Integer,Long> p=new TreeMap<>(r.getPhysical());p.put(ItemID.COINS,2000000L);return repository.save(root,r,BankEngine.observe(r,new ContainerSnapshot(p,p.size()),UUID.randomUUID(),SnapshotFixtures.TIME));}
	@Test public void coinsAndBuybackPermanentSpendingCancelOnceWithoutValuingEntitlement()throws Exception
	{BankRun before=run();assertEquals(Long.valueOf(2000000),PeakProgressRepository.candidate(before,Collections.emptyMap()));assertEquals(Long.valueOf(2000000),peak.checkpoint(root,before,PeakProgressRepository.candidate(before,Collections.emptyMap())));BankRun after=repository.save(root,before,BuybackModelTest.buy(before,"fire_cape"));assertEquals(1000000,BuybackEngine.available(after));assertEquals(Long.valueOf(2000000),PeakProgressRepository.candidate(after,Collections.emptyMap()));assertEquals(Long.valueOf(2000000),peak.checkpoint(root,after,PeakProgressRepository.candidate(after,Collections.emptyMap())));}
	@Test public void peakIsMonotonicDurableAndDoesNotRewriteBankOrUnchangedCounter()throws Exception
	{BankRun r=run();assertEquals(Long.valueOf(2000000),peak.checkpoint(root,r,2000000L));Filepath f=root.join("characters",r.getCharacter(),"peak-"+r.getRunId()+".json");String bytes=BankStorageFiles.readText(f);assertEquals(Long.valueOf(2000000),new PeakProgressRepository(codec).checkpoint(root,r,100L));assertEquals(bytes,BankStorageFiles.readText(f));assertEquals(r,repository.load(root,r.getCharacter()).get());assertEquals(Long.valueOf(2000000),peak.checkpoint(root,r,null));}
	@Test public void missingStaleAndOverflowQuotesCannotRaisePeak()throws Exception
	{BankRun r=run();Map<Integer,Long> p=new TreeMap<>(r.getPhysical());p.put(ItemID.WOODEN_SHIELD,1L);r=repository.save(root,r,BankEngine.observe(r,new ContainerSnapshot(p,p.size()),UUID.randomUUID(),SnapshotFixtures.TIME));assertNull(PeakProgressRepository.candidate(r,Collections.emptyMap()));PriceQuote stale=new PriceQuote(ItemID.WOODEN_SHIELD,100L,SnapshotFixtures.TIME,PriceQuote.Source.RUNELITE_GE_GUIDE,true);assertNull(PeakProgressRepository.candidate(r,Collections.singletonMap(ItemID.WOODEN_SHIELD,stale)));PriceQuote fresh=new PriceQuote(ItemID.WOODEN_SHIELD,100L,SnapshotFixtures.TIME,PriceQuote.Source.RUNELITE_GE_GUIDE,false);assertEquals(Long.valueOf(2000100),PeakProgressRepository.candidate(r,Collections.singletonMap(ItemID.WOODEN_SHIELD,fresh)));PriceQuote huge=new PriceQuote(ItemID.WOODEN_SHIELD,Long.MAX_VALUE,SnapshotFixtures.TIME,PriceQuote.Source.RUNELITE_GE_GUIDE,false);assertNull(PeakProgressRepository.candidate(r,Collections.singletonMap(ItemID.WOODEN_SHIELD,huge)));}
	@Test public void interruptedOrCorruptPeakNeverReplacesEconomicProgressOrResetsCounter()throws Exception
	{BankRun r=run();peak.checkpoint(root,r,100L);try{Thread.currentThread().interrupt();peak.checkpoint(root,r,200L);fail();}catch(java.io.IOException expected){}finally{Thread.interrupted();}assertEquals(Long.valueOf(100),peak.checkpoint(root,r,null));Filepath f=root.join("characters",r.getCharacter(),"peak-"+r.getRunId()+".json");f.write("broken");try{peak.checkpoint(root,r,200L);fail();}catch(SaveException expected){}assertEquals("broken",BankStorageFiles.readText(f));assertEquals(r,repository.load(root,r.getCharacter()).get());}
	@Test public void exportImportIncludesPeakAndNewerPriceOnlyPeakRejectsStaleRestore()throws Exception
	{BankRun r=run();peak.checkpoint(root,r,200L);Filepath exported=Filepath.Unchecked.getRooted(temp.getRoot().toPath().resolveSibling("peak-"+UUID.randomUUID()+".fsp"));try{repository.backups().exportProgress(root,r,exported);ProgressBackup.Candidate preview=repository.backups().preview(root,r.getCharacter(),exported);peak.checkpoint(root,r,300L);try{repository.backups().restore(root,preview);fail();}catch(SaveException expected){}repository.backups().discard(preview);preview=repository.backups().preview(root,r.getCharacter(),exported);repository.backups().restore(root,preview);assertEquals(Long.valueOf(200),peak.checkpoint(root,r,null));}finally{exported.delete();}}
}
