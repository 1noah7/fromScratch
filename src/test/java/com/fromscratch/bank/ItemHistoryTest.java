package com.fromscratch.bank;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.persistence.*;
import com.fromscratch.ge.*;
import com.fromscratch.pouch.PouchObservation;
import com.fromscratch.model.ContainerSnapshot;
import java.util.*;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.util.Filepath;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import static com.fromscratch.bank.BankFixtures.*;

public class ItemHistoryTest
{
	@Rule public TemporaryFolder temporary=new TemporaryFolder();
	@Test public void itemFilterPrecedesPaginationAndLeavesStateUntouched() throws Exception
	{
		Filepath root=Filepath.Unchecked.getRooted(temporary.getRoot().toPath());BankRepository repo=new BankRepository(new BankRunCodec(SnapshotFixtures.gson()),SnapshotFixtures.codec());
		BankRun saved=repo.save(root,null,run(1));
		for(int i=0;i<32;i++) { saved=repo.save(root,saved,BankEngine.observe(saved,bank(i%2==0?2:1),UUID.randomUUID(),SnapshotFixtures.TIME)); }
		saved=repo.save(root,saved,BankEngine.correct(saved,SHIELD,-2,UUID.randomUUID(),SnapshotFixtures.TIME));
		ItemHistory.Page first=repo.itemHistory(root,saved,null,SHIELD);assertEquals(25,first.getRows().size());assertEquals("Manual correction",first.getRows().get(0).getAction());
		ItemHistory.Page next=repo.itemHistory(root,saved,first.getNext(),SHIELD);assertEquals(8,next.getRows().size());assertNull(next.getNext());
		assertTrue(repo.itemHistory(root,saved,null,OTHER).getRows().isEmpty());assertEquals(saved,repo.load(root,saved.getCharacter()).get());
	}
	@Test public void pouchOffsetsAndSharedBuybacksAreDistinctFromBankMovements()
	{
		Map<Integer,Long> initial=Collections.singletonMap(ItemID.BH_RUNE_POUCH,1L);
		BankRun run=BankEngine.start(SnapshotFixtures.CHARACTER_A,null,new ContainerSnapshot(initial,1),true,UUID.randomUUID(),SnapshotFixtures.TIME);
		Map<Integer,Long> bank=new TreeMap<>(initial);bank.put(ItemID.COINS,2000000L);run=BankEngine.observe(run,new ContainerSnapshot(bank,2),UUID.randomUUID(),SnapshotFixtures.TIME);
		PouchObservation proof=new PouchObservation(true,Collections.singletonMap(ItemID.NATURERUNE,500L),SnapshotFixtures.TIME,ItemID.BH_RUNE_POUCH);
		run=BankEngine.buyback(run,"rune_pouch_family",UUID.randomUUID(),UUID.randomUUID(),SnapshotFixtures.TIME,proof);
		BankAudit purchase=run.getRecent().get(0);
		assertEquals("Legacy Rune Pouch offset",ItemHistory.rows(purchase,ItemID.NATURERUNE).get(0).getAction());
		assertEquals(-500,ItemHistory.rows(purchase,ItemID.NATURERUNE).get(0).getQuantity());
		assertTrue(ItemHistory.rows(purchase,ItemID.BH_RUNE_POUCH).get(0).getDetails().contains("1,000,000 GP"));
	}
	@Test public void geEvidenceIsShownWithoutInventingVerifiedBankOriginOrProfit()
	{
		GeTrade trade=new GeTrade(UUID.randomUUID(),UUID.randomUUID(),0,SHIELD,"Wooden shield",true,4,800L,null,"","Filled",GeOffer.Origin.UNKNOWN,GeTrade.Evidence.FILL,SnapshotFixtures.TIME);
		GeUpdate update=new GeUpdate(null,Collections.singletonList(trade),Collections.emptyList(),Collections.emptyMap());
		BankAudit audit=new BankAudit(UUID.randomUUID(),UUID.randomUUID(),SnapshotFixtures.TIME,BankAudit.Kind.EXCHANGE,Collections.emptyMap(),Collections.emptyMap(),"",Collections.emptyList(),update);
		List<ItemHistory.Row> rows=ItemHistory.rows(audit,SHIELD);assertEquals(1,rows.size());assertEquals("GE purchase",rows.get(0).getAction());assertTrue(rows.get(0).getDetails().contains("bank association unverified"));assertTrue(ItemHistory.rows(audit,OTHER).isEmpty());
	}
}
