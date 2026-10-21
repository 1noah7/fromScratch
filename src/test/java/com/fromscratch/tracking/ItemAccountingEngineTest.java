package com.fromscratch.tracking;

import com.fromscratch.model.*;
import com.fromscratch.observation.ItemObservation;
import java.util.*;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.fromscratch.tracking.TrackingFixtures.*;

public class ItemAccountingEngineTest
{
	@Test public void legacyWithdrawalAndRedepositNeverAcquireOrUnlock()
	{
		RunRecord run = run(1000, 0, 0, 0, 0, 0);
		AccountingPlan withdraw = plan(run, holdings(960, 40, 0), 2);
		assertEquals(0, withdraw.getOwnership().total(ItemID.SHARK)); assertFalse(withdraw.isReviewRequired());
		AccountingPlan deposit = plan(run.withItems(withdraw), holdings(1000, 0, 0), 3);
		assertEquals(0, deposit.getOwnership().total(ItemID.SHARK)); assertTrue(deposit.getProgress().getUnlocked().isEmpty());
		assertTrue(deposit.getTransactions().stream().anyMatch(entry -> entry.getCategory() == ItemTransaction.Category.TRANSFER));
		assertEquals(0, deposit.getProgress().getLegacyDeficits().quantity(ItemID.SHARK));
	}
	@Test public void legitimateNewItemAndExistingLegacyTypeAreCredited()
	{
		RunRecord run = run(1000, 0, 0, 0, 0, 0);
		AccountingPlan gained = plan(run, holdings(1000, 100, 0), 2);
		assertEquals(100, gained.getOwnership().at(ItemID.SHARK, VirtualLocation.INVENTORY));
		assertEquals(1, gained.getProgress().getUnlocked().size()); assertFalse(gained.isReviewRequired());
		assertEquals(ItemTransaction.Category.ACQUISITION, gained.getTransactions().get(0).getCategory());
	}
	@Test public void mixedStackWithdrawalsUseVirtualFirstAndConserveTotals()
	{
		RunRecord run = run(1100, 0, 0, 100, 0, 0);
		AccountingPlan moved = plan(run, holdings(1060, 40, 0), 2);
		assertEquals(60, moved.getOwnership().at(ItemID.SHARK, VirtualLocation.BANK));
		assertEquals(40, moved.getOwnership().at(ItemID.SHARK, VirtualLocation.INVENTORY));
		assertEquals(100, moved.getOwnership().total(ItemID.SHARK)); assertTrue(moved.getProgress().getAcquisitions().isEmpty());
	}
	@Test public void inventoryEquipmentAndDirectEquipmentBankTransfersConserveOwnership()
	{
		RunRecord run = run(0, 10, 0, 0, 4, 0);
		AccountingPlan equip = plan(run, holdings(0, 8, 2), 2);
		assertEquals(2, equip.getOwnership().at(ItemID.SHARK, VirtualLocation.EQUIPMENT));
		AccountingPlan bank = plan(run.withItems(equip), holdings(2, 8, 0), 3);
		assertEquals(2, bank.getOwnership().at(ItemID.SHARK, VirtualLocation.BANK)); assertEquals(4, bank.getOwnership().total(ItemID.SHARK));
	}
	@Test public void confirmedEatingReducesVirtualFirstAndRetainsLifetimeUnlock()
	{
		RunRecord base = run(0, 0, 0, 0, 0, 0);
		RunRecord earned = base.withItems(plan(base, holdings(0, 1, 0), 2));
		ItemObservation eat = observation(holdings(0, 0, 0), 3, Collections.singletonMap(ItemID.SHARK, ItemObservation.Action.CONSUME), SESSION);
		AccountingPlan consumed = ENGINE.plan(earned, eat, id -> id, false, false, Collections.emptyMap(), "");
		assertFalse(consumed.isReviewRequired()); assertEquals(0, consumed.getOwnership().total(ItemID.SHARK));
		assertEquals(1, consumed.getProgress().getUnlocked().size()); assertTrue(consumed.getProgress().getReturns().isEmpty());
		assertEquals(ItemTransaction.Category.CONSUMPTION, consumed.getTransactions().get(0).getCategory());
	}
	@Test public void legacyDropAndPickupDoNotCreateOwnership()
	{
		RunRecord run = run(0, 50, 0, 0, 0, 0);
		ItemObservation drop = observation(holdings(0, 0, 0), 2, Collections.singletonMap(ItemID.SHARK, ItemObservation.Action.DROP), SESSION);
		AccountingPlan dropped = ENGINE.plan(run, drop, id -> id, false, false, Collections.emptyMap(), "");
		assertFalse(dropped.isReviewRequired()); assertEquals(50, dropped.getProgress().getReturns().get(ItemID.SHARK).getLegacy());
		AccountingPlan pickup = plan(run.withItems(dropped), holdings(0, 50, 0), 3);
		assertEquals(0, pickup.getOwnership().total(ItemID.SHARK)); assertTrue(pickup.getProgress().getUnlocked().isEmpty());
	}
	@Test public void virtualDropAndPickupRestoreCopiesWithoutDuplicateAcquisition()
	{
		RunRecord base = run(0, 0, 0, 0, 0, 0);
		RunRecord earned = base.withItems(plan(base, holdings(0, 5, 0), 2));
		AccountingPlan dropped = ENGINE.plan(earned, observation(holdings(0, 0, 0), 3,
			Collections.singletonMap(ItemID.SHARK, ItemObservation.Action.DROP), SESSION), id -> id, false, false, Collections.emptyMap(), "");
		AccountingPlan pickup = plan(earned.withItems(dropped), holdings(0, 5, 0), 4);
		assertEquals(5, pickup.getOwnership().total(ItemID.SHARK)); assertEquals(1, pickup.getProgress().getAcquisitions().size());
		assertEquals(ItemTransaction.Category.RECONCILIATION_ADJUSTMENT, pickup.getTransactions().get(0).getCategory());
	}
	@Test public void unknownReductionRequiresReviewAndDoesNotSilentlyDestroyTrustedRun()
	{
		RunRecord run = run(0, 50, 0, 0, 20, 0); AccountingPlan candidate = plan(run, holdings(0, 40, 0), 2);
		assertTrue(candidate.isReviewRequired()); assertEquals(20, run.getOwnership().total(ItemID.SHARK));
		assertEquals(10, candidate.getOwnership().total(ItemID.SHARK));
	}
	@Test public void offlineNetIncreaseCreditsOnlyTheNetGain()
	{
		RunRecord run = run(100, 0, 0, 0, 0, 0);
		AccountingPlan candidate = ENGINE.plan(run, observation(holdings(130, 0, 0), 20, Collections.emptyMap(), UUID.randomUUID()),
			id -> id, true, false, Collections.emptyMap(), "");
		assertEquals(30, candidate.getOwnership().total(ItemID.SHARK)); assertTrue(candidate.isReviewRequired());
		assertEquals(ItemTransaction.Confidence.NET_RECONCILED, candidate.getTransactions().get(0).getConfidence());
	}
	@Test public void offlineNetLossUsesVirtualFirstAndRequiresConfirmation()
	{
		RunRecord run = run(130, 0, 0, 100, 0, 0);
		AccountingPlan candidate = ENGINE.plan(run, observation(holdings(80, 0, 0), 20, Collections.emptyMap(), UUID.randomUUID()),
			id -> id, true, false, Collections.emptyMap(), "");
		assertEquals(50, candidate.getOwnership().total(ItemID.SHARK)); assertTrue(candidate.isReviewRequired());
	}
	@Test public void unknownTransformationOutputStaysLockedUntilCorrected()
	{
		RunRecord run = run(0, 1, 0, 0, 0, 0);
		Holdings crafted = holdings(Collections.emptyMap(), items(ItemID.ABYSSAL_WHIP, 1), Collections.emptyMap());
		AccountingPlan candidate = plan(run, crafted, 2);
		assertTrue(candidate.isReviewRequired()); assertEquals(0, candidate.getOwnership().total(ItemID.ABYSSAL_WHIP));
	}
	@Test public void explicitNoteConversionPreservesVirtualCopiesWithoutAcquisition()
	{
		int noted = ItemID.SHARK + 1; // synthetic resolver, not a production item-ID assumption
		RunRecord run = run(100, 0, 0, 30, 0, 0);
		Holdings next = holdings(items(ItemID.SHARK, 80), items(noted, 20), Collections.emptyMap());
		AccountingPlan converted = ENGINE.plan(run, observation(next, 2), id -> id == noted ? ItemID.SHARK : id,
			false, false, Collections.emptyMap(), "");
		assertEquals(10, converted.getOwnership().total(ItemID.SHARK)); assertEquals(20, converted.getOwnership().total(noted));
		assertFalse(converted.isReviewRequired()); assertTrue(converted.getProgress().getAcquisitions().isEmpty());
	}
	@Test(expected = IllegalStateException.class) public void duplicateObservationIsRejected()
	{
		RunRecord run = run(0, 0, 0, 0, 0, 0); RunRecord next = run.withItems(plan(run, holdings(0, 1, 0), 2));
		plan(next, holdings(0, 1, 0), 2);
	}
	@Test(expected = IllegalStateException.class) public void outOfOrderObservationIsRejected() { plan(run(0, 0, 0, 0, 0, 0), holdings(0, 1, 0), 0); }
	@Test(expected = IllegalStateException.class) public void newHostSessionCannotAutomaticallyCredit()
	{
		ENGINE.plan(run(0, 0, 0, 0, 0, 0), observation(holdings(0, 1, 0), 2, Collections.emptyMap(), UUID.randomUUID()), id -> id,
			false, false, Collections.emptyMap(), "");
	}
	@Test public void legacyBaselineRemainsImmutableAcrossAcquisitionAndTransfer()
	{
		RunRecord run = run(100, 0, 0, 0, 0, 0); LegacySnapshot snapshot = run.getSnapshot();
		RunRecord acquired = run.withItems(plan(run, holdings(100, 5, 0), 2));
		RunRecord moved = acquired.withItems(plan(acquired, holdings(105, 0, 0), 3));
		assertSame(snapshot, moved.getSnapshot()); assertEquals(100, snapshot.at(SnapshotLocation.BANK).quantity(ItemID.SHARK));
	}
	@Test public void checkedLongStackAndUnstackableCopiesAreNotLimitedToInts()
	{
		RunRecord run = run(0, 0, 0, 0, 0, 0); long q = (long) Integer.MAX_VALUE + 100;
		assertEquals(q, plan(run, holdings(q, 0, 0), 2).getOwnership().total(ItemID.SHARK));
		assertEquals(3, plan(run, holdings(0, 3, 0), 2).getOwnership().total(ItemID.SHARK));
	}
	@Test public void ledgerDeltasAgreeWithOwnershipChanges()
	{
		RunRecord run = run(1100, 0, 0, 100, 0, 0); AccountingPlan transfer = plan(run, holdings(1060, 40, 0), 2);
		long total = run.getOwnership().total(ItemID.SHARK);
		for (ItemTransaction entry : transfer.getTransactions()) { total = Math.addExact(total, entry.getVirtualChange()); }
		assertEquals(transfer.getOwnership().total(ItemID.SHARK), total);
	}
	@Test public void unpairedBankingIncreaseRequiresReviewInsteadOfAutomaticCredit()
	{
		RunRecord run = run(100, 0, 0, 0, 0, 0); ItemObservation ordinary = observation(holdings(100, 40, 0), 2);
		ItemObservation banking = new ItemObservation(SESSION, 2, ordinary.getTime(), ordinary.getProgress(), ordinary.getHoldings(),
			true, true, false, Collections.emptyMap(), 2, true, false);
		assertTrue(ENGINE.plan(run, banking, id -> id, false, false, Collections.emptyMap(), "").isReviewRequired());
	}
	@Test public void unsupportedStorageRetrievalRemainsLockedUntilReviewed()
	{
		RunRecord run = run(100, 0, 0, 0, 0, 0); ItemObservation ordinary = observation(holdings(100, 40, 0), 2);
		ItemObservation storage = new ItemObservation(SESSION, 2, ordinary.getTime(), ordinary.getProgress(), ordinary.getHoldings(),
			true, false, false, Collections.emptyMap(), 2, false, true);
		AccountingPlan candidate = ENGINE.plan(run, storage, id -> id, false, false, Collections.emptyMap(), "");
		assertTrue(candidate.isReviewRequired()); assertEquals(0, candidate.getOwnership().total(ItemID.SHARK));
	}
	@Test public void coinsLootedAlongsideAnItemDoNotBlockOrdinaryItemCreditOrCreateVirtualGp()
	{
		RunRecord run = run(0, 0, 0, 0, 0, 0); Map<Integer, Long> loot = new HashMap<>(); loot.put(ItemID.COINS, 100L); loot.put(ItemID.SHARK, 5L);
		AccountingPlan candidate = plan(run, holdings(Collections.emptyMap(), loot, Collections.emptyMap()), 2);
		assertFalse(candidate.isReviewRequired()); assertEquals(5, candidate.getOwnership().total(ItemID.SHARK)); assertEquals(0, candidate.getOwnership().total(ItemID.COINS));
	}
	@Test public void equipmentToInventoryConservesVirtualOwnership()
	{
		RunRecord run = run(0, 0, 1, 0, 0, 1); AccountingPlan unequipped = plan(run, holdings(0, 1, 0), 2);
		assertEquals(1, unequipped.getOwnership().at(ItemID.SHARK, VirtualLocation.INVENTORY));
		assertEquals(0, unequipped.getOwnership().at(ItemID.SHARK, VirtualLocation.EQUIPMENT)); assertFalse(unequipped.isReviewRequired());
	}
	@Test public void copiesPreservedElsewhereReturnWithoutDuplicateOwnershipOrAcquisitions()
	{
		RunRecord base = run(0, 0, 0, 0, 0, 0);
		RunRecord earned = base.withItems(plan(base, holdings(0, 5, 0), 2));
		AccountingPlan retained = ENGINE.plan(earned, observation(holdings(0, 0, 0), 3), id -> id, true, false,
			Collections.singletonMap(ItemID.SHARK, new VirtualItemOwnership(5, Collections.emptyMap())), "Moved to unobserved storage.");
		assertEquals(5, retained.getOwnership().total(ItemID.SHARK)); assertEquals(1, retained.getProgress().getAcquisitions().size());
		RunRecord elsewhere = earned.withItems(retained);
		AccountingPlan returned = plan(elsewhere, holdings(0, 5, 0), 4);
		assertTrue(returned.isReviewRequired()); assertEquals(5, returned.getOwnership().total(ItemID.SHARK));
		assertEquals(5, returned.getOwnership().at(ItemID.SHARK, VirtualLocation.INVENTORY));
		assertEquals(0, returned.getOwnership().item(ItemID.SHARK).unallocated());
		assertEquals(1, returned.getProgress().getAcquisitions().size());
		assertEquals(0, returned.getTransactions().stream().mapToLong(ItemTransaction::getVirtualChange).sum());
	}
	@Test public void elsewhereReturnWithoutAnOutstandingDropReserveIsNotNewOwnership()
	{
		RunRecord base = run(0, 0, 0, 0, 0, 0);
		RunRecord earned = base.withItems(plan(base, holdings(0, 5, 0), 2));
		AccountingPlan retained = ENGINE.plan(earned, observation(holdings(0, 0, 0), 3), id -> id, true, true,
			Collections.singletonMap(ItemID.SHARK, new VirtualItemOwnership(5, Collections.emptyMap())), "Preserve existing copies elsewhere.");
		assertTrue(retained.getProgress().getReturns().isEmpty());
		AccountingPlan returned = plan(earned.withItems(retained), holdings(0, 5, 0), 4);
		assertTrue(returned.isReviewRequired()); assertEquals(5, returned.getOwnership().total(ItemID.SHARK));
		assertEquals(0, returned.getOwnership().item(ItemID.SHARK).unallocated());
		assertEquals(1, returned.getProgress().getAcquisitions().size());
	}
}
