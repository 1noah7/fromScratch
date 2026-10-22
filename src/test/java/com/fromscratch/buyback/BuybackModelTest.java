package com.fromscratch.buyback;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.bank.*;
import com.fromscratch.model.ContainerSnapshot;
import com.fromscratch.wealth.*;
import java.util.*;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

/** Explicit copy budgets, prices, family substitutions and zero physical game mutations. */
public class BuybackModelTest
{
	private static final int P=ItemID.BH_RUNE_POUCH,D=ItemID.DIVINE_RUNE_POUCH,F=ItemID.TZHAAR_CAPE_FIRE,C=ItemID.COINS;
	public static ContainerSnapshot bank(int...ids){Map<Integer,Long> q=new TreeMap<>();for(int id:ids){q.merge(id,1L,Math::addExact);}return new ContainerSnapshot(q,ids.length);}
	public static BankRun funded(long gp,int...legacy){BankRun r=BankEngine.start(SnapshotFixtures.CHARACTER_A,null,bank(legacy),true,UUID.randomUUID(),SnapshotFixtures.TIME);Map<Integer,Long> q=new TreeMap<>(r.getPhysical());q.put(C,gp);r=BankEngine.observe(r,new ContainerSnapshot(q,q.size()),UUID.randomUUID(),SnapshotFixtures.TIME);return BuybackCatalog.variants("rune_pouch_family").keySet().stream().anyMatch(q::containsKey)?emptyPouch(r):r;}
	public static BankRun emptyPouch(BankRun r){return BankEngine.pouch(r,new com.fromscratch.pouch.PouchAction(com.fromscratch.pouch.PouchAction.Kind.FINISH,Collections.emptyMap(),true,false,"Fixture player explicitly verified empty pouch and accounted for earlier deposits."),UUID.randomUUID(),SnapshotFixtures.TIME,Collections.emptyMap());}
 public static BankRun buy(BankRun r,String id){if(id.equals("looting_bag_family")){int bag=BuybackCatalog.variants(id).keySet().stream().filter(i->r.physical(i)>0).findFirst().orElse(ItemID.LOOTING_BAG);return BankEngine.buyBag(r,UUID.randomUUID(),UUID.randomUUID(),SnapshotFixtures.TIME,new BagObservation(bag,Collections.emptyMap(),Collections.emptyMap(),SnapshotFixtures.TIME));}int pouch=BuybackCatalog.variants("rune_pouch_family").keySet().stream().filter(i->r.physical(i)>0).findFirst().orElse(ItemID.BH_RUNE_POUCH);return BankEngine.buyback(r,id,UUID.randomUUID(),UUID.randomUUID(),SnapshotFixtures.TIME,new com.fromscratch.pouch.PouchObservation(true,Collections.emptyMap(),SnapshotFixtures.TIME,pouch));}
	private static BankRun observe(BankRun r,int...ids){Map<Integer,Long> q=new TreeMap<>(bank(ids).getQuantities());q.put(C,r.physical(C));return BankEngine.observe(r,new ContainerSnapshot(q,q.size()),UUID.randomUUID(),SnapshotFixtures.TIME);}
	private static long visible(BankRun r,int id){return BuybackEngine.balances(r).getOrDefault(id,0L);}
	private static void rejected(Runnable r){try{r.run();fail("Expected rejection");}catch(IllegalArgumentException|ArithmeticException expected){}}
	@Test public void fireCapeUsesExactlyOneMillionWithoutMovingPhysicalCoins(){BankRun a=funded(1500000,F),b=buy(a,"fire_cape");assertEquals(a.getPhysical(),b.getPhysical());assertEquals(a.getBalances(),b.getBalances());assertEquals(500000,BuybackEngine.available(b));assertEquals(1,visible(b,F));assertEquals(a.getFinancial(),b.getFinancial());}
	@Test public void insufficientFundsRejectEntirePurchase(){BankRun r=funded(999999,F);rejected(()->buy(r,"fire_cape"));assertTrue(r.getBuybacks().getPurchases().isEmpty());}
	@Test public void exactPriceLeavesNoAvailableGp(){assertEquals(0,BuybackEngine.available(buy(funded(1000000,F),"fire_cape")));}
	@Test public void missingPhysicalItemRejectsPurchase(){rejected(()->buy(funded(2000000),"fire_cape"));}
	@Test public void duplicateEntitlementRejectsEvenWithMoreMoney(){BankRun r=buy(funded(3000000,F),"fire_cape");rejected(()->buy(r,"fire_cape"));}
	@Test public void standardPouchQualifies(){assertEquals(1,visible(buy(funded(1000000,P),"rune_pouch_family"),P));}
	@Test public void divinePouchQualifies(){assertEquals(1,visible(buy(funded(1000000,D),"rune_pouch_family"),D));}
	@Test public void runeNoteDoesNotQualify(){rejected(()->buy(funded(2000000,ItemID.PROMISSORY_NOTE_RUNEPOUCH),"rune_pouch_family"));}
	@Test public void pouchFamilyIsOneRecordAndOneCopy(){BankRun r=buy(funded(2000000,P),"rune_pouch_family");assertEquals(1,r.getBuybacks().getEntitlements().size());assertEquals(1,r.getBuybacks().getPurchases().size());assertEquals(1000000,r.getBuybacks().getCommittedGp());assertEquals(1,visible(r,P)+visible(r,D));assertEquals(1,r.unlocked()-1);}
	@Test public void upgradingPouchPreservesPaidCopy(){BankRun r=observe(buy(funded(1000000,P),"rune_pouch_family"),D);assertEquals(0,visible(r,P));assertEquals(1,visible(r,D));assertEquals(1000000,r.getBuybacks().getCommittedGp());}
	@Test public void revertingPouchPreservesPaidCopy(){BankRun r=observe(buy(funded(1000000,D),"rune_pouch_family"),P);assertEquals(1,visible(r,P));assertEquals(0,visible(r,D));assertEquals(1,r.getBuybacks().getPurchases().size());}
	@Test public void withdrawingAndReturningDoesNotLoseEntitlement(){BankRun r=buy(funded(1000000,P),"rune_pouch_family");r=observe(r);assertEquals(0,visible(r,P));assertEquals(1,r.getBuybacks().getEntitlements().size());r=observe(r,P);assertEquals(1,visible(r,P));}
	@Test public void multiplePouchesDoNotGrantUnlimitedCopies(){BankRun r=funded(1000000,P,P,D);rejected(()->buy(r,"rune_pouch_family"));assertEquals(0,r.getBuybacks().getCommittedGp());}
	@Test public void genuinelyNewCopyRemainsIndependent(){BankRun r=buy(funded(1000000,P),"rune_pouch_family");r=observe(r,P,P);assertEquals(2,visible(r,P));assertEquals(1,r.getBuybacks().getEntitlements().size());}
	@Test public void naturalFireCapeDoesNotUnlockOriginalCopy(){BankRun r=observe(funded(1000000,F),F,F);assertEquals(1,visible(r,F));r=buy(r,"fire_cape");assertEquals(2,visible(r,F));}
	@Test public void newUntradeableAloneDoesNotRequireBuyback(){BankRun r=observe(funded(1000000),F);assertEquals(1,visible(r,F));rejected(()->buy(r,"fire_cape"));}
	@Test public void trouverPouchesAreTheSameSingleFamily(){BankRun r=buy(funded(1000000,ItemID.BH_RUNE_POUCH_TROUVER),"rune_pouch_family");r=observe(r,ItemID.DIVINE_RUNE_POUCH_TROUVER);assertEquals(1,visible(r,ItemID.DIVINE_RUNE_POUCH_TROUVER));assertEquals(4,BuybackCatalog.variants("rune_pouch_family").size());}
	@Test public void pouchRunesAreNeverTouched(){BankRun r=buy(funded(1000000,P,ItemID.MINDRUNE),"rune_pouch_family");assertEquals(0,visible(r,ItemID.MINDRUNE));assertEquals(1,r.physical(ItemID.MINDRUNE));}
	@Test public void infernalIsFifteenMillion(){assertEquals(15000000,BuybackCatalog.entry("infernal_cape").getPrice());}
	@Test public void ancientSceptreHasNoComponentFee(){assertEquals(500000,buy(funded(500000,ItemID.ANCIENT_SCEPTRE),"ancient_sceptre").getBuybacks().getCommittedGp());}
	@Test public void exclusionsAndArbitraryItemsAreNotCatalogEntries(){rejected(()->BuybackCatalog.entry("barrows_gloves"));rejected(()->BuybackCatalog.entry("royal_seed_pod"));rejected(()->BuybackCatalog.entry("wooden_shield"));}
	@Test public void regularVoidOneHelmTotalsTwoPointOneMillion(){long sum=0;for(String k:new String[]{"void_melee","void_top","void_robe","void_gloves"}){sum+=BuybackCatalog.entry(k).getPrice();}assertEquals(2100000,sum);}
	@Test public void eliteOneHelmTotalsFourPointOneMillion(){assertEquals(4100000,BuybackCatalog.entry("void_mage").getPrice()+BuybackCatalog.entry("elite_void_top").getPrice()+BuybackCatalog.entry("elite_void_robe").getPrice()+BuybackCatalog.entry("void_gloves").getPrice());}
	@Test public void eliteOutrightIncludesBase(){BankRun r=buy(funded(1800000,ItemID.ELITE_VOID_KNIGHT_TOP),"elite_void_top");assertEquals(1800000,r.getBuybacks().getCommittedGp());assertEquals(1,visible(r,ItemID.ELITE_VOID_KNIGHT_TOP));rejected(()->buy(r,"void_top"));}
	@Test public void eliteUpgradeChargesOnlyDifferenceAndSelectsEliteCopy(){BankRun r=buy(funded(1800000,ItemID.PEST_VOID_KNIGHT_TOP,ItemID.ELITE_VOID_KNIGHT_TOP),"void_top");r=buy(r,"elite_void_top");assertEquals(1800000,r.getBuybacks().getCommittedGp());assertEquals(0,visible(r,ItemID.PEST_VOID_KNIGHT_TOP));assertEquals(1,visible(r,ItemID.ELITE_VOID_KNIGHT_TOP));}
	@Test public void baseVoidDoesNotUnlockEliteBeforeUpgrade(){BankRun r=observe(buy(funded(1800000,ItemID.PEST_VOID_KNIGHT_TOP),"void_top"),ItemID.ELITE_VOID_KNIGHT_TOP);assertEquals(0,visible(r,ItemID.ELITE_VOID_KNIGHT_TOP));assertEquals("Available",BuybackEngine.status(r,BuybackCatalog.entry("elite_void_top")));}
	@Test public void gracefulIsSixPieceAtomicSet(){int[] ids={ItemID.GRACEFUL_HOOD,ItemID.GRACEFUL_CAPE,ItemID.GRACEFUL_TOP,ItemID.GRACEFUL_LEGS,ItemID.GRACEFUL_GLOVES,ItemID.GRACEFUL_BOOTS};BankRun r=buy(funded(1000000,ids),"graceful");assertEquals(6,r.getBuybacks().getEntitlements().size());assertEquals(1,r.getBuybacks().getPurchases().size());for(int id:ids){assertEquals(1,visible(r,id));}}
	@Test public void incompleteOutfitChargesNothing(){BankRun r=funded(1000000,ItemID.GRACEFUL_HOOD,ItemID.GRACEFUL_CAPE);rejected(()->buy(r,"graceful"));assertEquals(0,r.getBuybacks().getCommittedGp());}
	@Test public void eachOutfitHasVerifiedDistinctRoles(){for(String key:new String[]{"graceful","prospector","angler","rogue","lumberjack","farmer"}){BuybackEntry e=BuybackCatalog.entry(key);int[] ids=e.getRoles().keySet().stream().mapToInt(k->BuybackCatalog.variants(k).keySet().iterator().next()).toArray();BankRun r=buy(funded(e.getPrice(),ids),key);assertEquals(ids.length,r.getBuybacks().getEntitlements().size());assertEquals(0,BuybackEngine.available(r));}}
	@Test public void spendingReducesWorthOnceAndNeverCreatesGains(){BankRun a=funded(2000000,F);BankRun b=buy(a,"fire_cape");Map<Integer,PriceQuote> prices=Collections.singletonMap(F,new PriceQuote(F,0L,SnapshotFixtures.TIME,PriceQuote.Source.RUNELITE_GE_GUIDE,false));assertEquals(Long.valueOf(1000000),BankValuation.calculate(b,prices).getSubtotalGp());assertEquals(a.getFinancial().getGainedGp(),b.getFinancial().getGainedGp());}
	@Test public void bankCorrectionsNeverRefundSpentGp(){BankRun r=buy(funded(1000000,F),"fire_cape");r=BankEngine.correct(r,C,0,UUID.randomUUID(),SnapshotFixtures.TIME);assertEquals(-1000000,BuybackEngine.available(r));assertEquals(1000000,r.getBuybacks().getCommittedGp());}
	@Test public void immutableLegacySnapshotRemainsUnchanged(){BankRun r=funded(1000000,F,P);assertEquals(r.getInitialBank(),buy(r,"fire_cape").getInitialBank());}
	@Test public void pouchSearchFindsSameEntryFromBothNames(){assertEquals("rune_pouch_family",BuybackCatalog.search("Divine rune pouch").get(0).getId());assertEquals("rune_pouch_family",BuybackCatalog.search("Rune pouch").get(0).getId());}
	@Test public void exactRuneLitePouchConstantsMatchVerifiedIds(){assertEquals(12791,P);assertEquals(27281,D);assertEquals(24416,ItemID.BH_RUNE_POUCH_TROUVER);assertEquals(27509,ItemID.DIVINE_RUNE_POUCH_TROUVER);}
	@Test public void pouchConfirmationExplainsOneSharedPayment(){String text=BuybackView.confirmation(funded(1500000,P),BuybackCatalog.entry("rune_pouch_family"));assertTrue(text.contains("Pay once"));assertTrue(text.contains("Divine Rune Pouch"));assertTrue(text.contains("500,000 GP"));}
	@Test public void exactPouchPaymentCountsOneUnlockAndNoSpentCoinType(){assertEquals(1,buy(funded(1000000,P),"rune_pouch_family").unlocked());}
	@Test public void priorLegacyWithdrawalsDoNotAbsorbThePaidBankedCopy()
	{
		BankRun r=observe(funded(1000000,F,F,F,F),F);assertEquals(-3,r.balance(F));r=buy(r,"fire_cape");assertEquals(1,visible(r,F));assertEquals(-3,r.balance(F));assertEquals(Long.valueOf(-3),r.getBuybacks().getBankFloors().get("fire_cape"));
		r=observe(r,F,F,F,F);assertEquals(1,visible(r,F));assertEquals(Long.valueOf(0),r.getBuybacks().getBankFloors().get("fire_cape"));r=observe(r,F,F,F);assertEquals(0,visible(r,F));r=observe(r,F,F,F,F);assertEquals(1,visible(r,F));
	}
	@Test public void returningOldWithdrawalsCannotMultiplyOnePaidPouch()
	{
		BankRun r=observe(funded(1000000,P,P,P,P),D);r=buy(r,"rune_pouch_family");assertEquals(1,visible(r,D));r=observe(r,P,P,P,D);assertEquals(1,visible(r,P)+visible(r,D));r=observe(r,P,P,P,D,D);assertEquals(2,visible(r,P)+visible(r,D));assertEquals(1,r.getBuybacks().getPurchases().size());
	}
	@Test public void eliteUpgradeCannotRefillAnOutsidePaidCopy()
	{
		BankRun r=buy(funded(1800000,ItemID.PEST_VOID_KNIGHT_TOP,ItemID.ELITE_VOID_KNIGHT_TOP),"void_top");r=observe(r,ItemID.ELITE_VOID_KNIGHT_TOP);BankRun captured=r;rejected(()->buy(captured,"elite_void_top"));assertEquals(800000,r.getBuybacks().getCommittedGp());
	}
}
