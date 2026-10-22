package com.fromscratch.pouch;

import java.util.*;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;
import static org.junit.Assert.*;

/** Actual varbit/enum contract, including reported zeros that still need human confirmation. */
public class RuneLitePouchReaderTest
{
	private static class Access implements RuneLitePouchReader.Access
	{
		boolean live=true;Map<Integer,Integer> bits=new HashMap<>(),types=new HashMap<>();
		public boolean live(){return live;}public int varbit(int id){return bits.getOrDefault(id,0);}public int rune(int type){return types.getOrDefault(type,-1);}
		void slot(int slot,int rune,int count){bits.put(RuneLitePouchReader.TYPES[slot],slot+1);bits.put(RuneLitePouchReader.QUANTITIES[slot],count);types.put(slot+1,rune);}
		PouchObservation read(){return new RuneLitePouchReader(this).read();}
	}
	@Test public void standardThreeSlotsResolveRealRuneIdsAndAmounts(){Access a=new Access();a.slot(0,ItemID.NATURERUNE,500);a.slot(1,ItemID.FIRERUNE,2000);a.slot(2,ItemID.LAWRUNE,7);assertEquals(Long.valueOf(500),a.read().getQuantities().get(ItemID.NATURERUNE));assertEquals(3,a.read().getQuantities().size());}
	@Test public void divineFourthSlotIsRead(){Access a=new Access();a.slot(3,ItemID.SOULRUNE,800);assertEquals(Long.valueOf(800),a.read().getQuantities().get(ItemID.SOULRUNE));}
	@Test public void allSixSupportedCorePairsAreInspected(){Access a=new Access();a.slot(4,ItemID.WRATHRUNE,3);a.slot(5,ItemID.COSMICRUNE,9);assertEquals(2,a.read().getQuantities().size());assertEquals(VarbitID.RUNE_POUCH_TYPE_6,RuneLitePouchReader.TYPES[5]);}
	@Test public void emptySlotsWithRetainedTypeAreIgnored(){Access a=new Access();a.slot(0,ItemID.NATURERUNE,0);assertTrue(a.read().isAvailable());assertTrue(a.read().getQuantities().isEmpty());assertEquals(PouchState.Phase.UNREVIEWED,PouchState.unreviewed().getPhase());}
	@Test public void unavailableSessionNeverBecomesVerifiedEmpty(){Access a=new Access();a.live=false;assertFalse(a.read().isAvailable());}
	@Test public void incompleteTypeWithPositiveQuantityIsUnknown(){Access a=new Access();a.bits.put(RuneLitePouchReader.QUANTITIES[0],500);assertFalse(a.read().isAvailable());}
	@Test public void unsupportedEnumValueIsUnknown(){Access a=new Access();a.slot(0,ItemID.WOODEN_SHIELD,500);assertFalse(a.read().isAvailable());}
	@Test public void missingDefinitionsAreUnknown(){RuneLitePouchReader reader=new RuneLitePouchReader(new Access(){@Override public int varbit(int id){throw new IllegalStateException("unavailable");}});assertFalse(reader.read().isAvailable());}
	@Test public void repeatedRuneSlotsAggregateSafely(){Access a=new Access();a.slot(0,ItemID.NATURERUNE,Integer.MAX_VALUE);a.slot(1,ItemID.NATURERUNE,Integer.MAX_VALUE);assertEquals(Long.valueOf(4294967294L),a.read().getQuantities().get(ItemID.NATURERUNE));}
	private static com.fromscratch.bank.BankObservation bank(int pouch,long tick){return new com.fromscratch.bank.BankObservation(UUID.randomUUID(),tick,java.time.Instant.EPOCH,com.fromscratch.buyback.BuybackModelTest.bank(pouch),"");}
	@Test public void bankedValuesNeedNoInventoryOrPouchWindowAndDoNotExpireWithOldBankTimestamp(){Access a=new Access();a.slot(0,ItemID.NATURERUNE,500);PouchObservation current=new RuneLitePouchReader(a).banked(bank(ItemID.BH_RUNE_POUCH,0));assertTrue(current.verified());assertTrue(current.fresh(java.time.Instant.now()));assertEquals(Long.valueOf(500),current.getQuantities().get(ItemID.NATURERUNE));}
	@Test public void observedEmptyBankedDivineIsValidButMissingBankOrDefinitionsAreNot(){RuneLitePouchReader reader=new RuneLitePouchReader(new Access());assertFalse(reader.banked(null).verified());PouchObservation empty=reader.banked(bank(ItemID.DIVINE_RUNE_POUCH,1));assertTrue(empty.verified());assertTrue(empty.getQuantities().isEmpty());assertFalse(new RuneLitePouchReader(new Access(){@Override public int serverVarbit(int id){throw new IllegalStateException("Unavailable server data");}}).banked(bank(ItemID.BH_RUNE_POUCH,1)).verified());}
	@Test public void clientPredictionThatDiffersFromServerIsNotVerifiedEmpty(){Access a=new Access(){@Override public int serverVarbit(int id){return id==RuneLitePouchReader.QUANTITIES[0]?500:super.serverVarbit(id);}};PouchObservation unknown=new RuneLitePouchReader(a).banked(bank(ItemID.BH_RUNE_POUCH,1));assertFalse(unknown.verified());assertTrue(unknown.getProblem().contains("updating"));}
}