package com.fromscratch.bank;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.model.ContainerSnapshot;
import java.util.*;
import net.runelite.api.Item;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.fromscratch.bank.BankFixtures.*;

public class BankContractTest
{
	@Test public void balancesAndBaselineAreImmutable() { BankRun run = run(1); try { run.getBalances().put(SHIELD, 1L); fail(); } catch (UnsupportedOperationException expected) { } try { run.getInitialBank().getQuantities().clear(); fail(); } catch (UnsupportedOperationException expected) { } }
	@Test public void duplicateUnstackableSlotsAggregate() { ContainerSnapshot s = BankItems.aggregate(new Item[]{new Item(SHIELD, 1), new Item(SHIELD, 1)}, id -> false); assertEquals(2, s.quantity(SHIELD)); assertEquals(2, s.getOccupiedSlots()); }
	@Test public void largeStacksExceedingIntAreSafe() { ContainerSnapshot s = BankItems.aggregate(new Item[]{new Item(SHIELD, Integer.MAX_VALUE), new Item(SHIELD, Integer.MAX_VALUE)}, id -> false); assertEquals(4294967294L, s.quantity(SHIELD)); }
	@Test public void nullAndEmptyRemainDistinct() { assertNull(BankItems.aggregate(null, id -> false)); assertNotNull(BankItems.aggregate(new Item[0], id -> false)); }
	@Test public void negativeApiQuantityRejected() { try { BankItems.aggregate(new Item[]{new Item(SHIELD, -1)}, id -> false); fail(); } catch (IllegalArgumentException expected) { } }
	@Test public void positiveBalancesCannotExceedPhysicalQuantity() { BankRun r = run(1); try { BankEngine.correct(r, SHIELD, 2, UUID.randomUUID(), SnapshotFixtures.TIME); fail(); } catch (IllegalArgumentException expected) { } }
	@Test public void negativeCorrectionCanExistWithoutPhysicalSlot() { BankRun r = BankEngine.correct(run(0), SHIELD, -5, UUID.randomUUID(), SnapshotFixtures.TIME); assertEquals(-5, r.balance(SHIELD)); assertEquals(0, r.physical(SHIELD)); }
	@Test public void correctionOffsetSurvivesLaterDeltas() { BankRun r = BankEngine.correct(run(10), SHIELD, -2, UUID.randomUUID(), SnapshotFixtures.TIME); r = BankEngine.observe(r, bank(12), UUID.randomUUID(), SnapshotFixtures.TIME); assertEquals(0, r.balance(SHIELD)); assertEquals(10, r.getInitialBank().quantity(SHIELD)); }
	@Test public void signedAdjustmentPreviewUsesCurrentBalance() { BankRun r = BankEngine.correct(run(10), SHIELD, 2, UUID.randomUUID(), SnapshotFixtures.TIME); assertEquals(-1, BankCorrections.desired(r, SHIELD, "-3", true)); }
	@Test public void minimumLongRejected() { try { BankEngine.correct(run(0), SHIELD, Long.MIN_VALUE, UUID.randomUUID(), SnapshotFixtures.TIME); fail(); } catch (IllegalArgumentException expected) { } }
	@Test public void arithmeticOverflowRejectsEntireOperation() { BankRun r = BankEngine.correct(run(1), SHIELD, -Long.MAX_VALUE, UUID.randomUUID(), SnapshotFixtures.TIME); try { BankEngine.observe(r, bank(0), UUID.randomUUID(), SnapshotFixtures.TIME); fail(); } catch (IllegalArgumentException expected) { } assertEquals(-Long.MAX_VALUE, r.balance(SHIELD)); }
	@Test public void overflowingCorrectionDeltaRejected() { BankRun r = BankEngine.correct(run(1), SHIELD, -Long.MAX_VALUE, UUID.randomUUID(), SnapshotFixtures.TIME); try { BankEngine.correct(r, SHIELD, 1, UUID.randomUUID(), SnapshotFixtures.TIME); fail(); } catch (ArithmeticException expected) { } }
	@Test public void sessionSummaryDoesNotDisappearAfterOrdinaryChanges() { BankRun r = run(1); UUID session = UUID.randomUUID(); r = BankEngine.observe(r, bank(3), session, SnapshotFixtures.TIME); BankAudit summary = r.getHostSummary(); r = BankEngine.observe(r, bank(4), session, SnapshotFixtures.TIME); assertEquals(summary, r.getHostSummary()); }
	@Test public void unlockedCountsCurrentPositiveTypesOnly() { BankRun r = BankEngine.observe(run(1), bank(2), UUID.randomUUID(), SnapshotFixtures.TIME); assertEquals(1, r.unlocked()); r = BankEngine.observe(r, bank(1), UUID.randomUUID(), SnapshotFixtures.TIME); assertEquals(0, r.unlocked()); }
}
