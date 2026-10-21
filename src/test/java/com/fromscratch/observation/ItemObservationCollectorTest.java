package com.fromscratch.observation;

import com.fromscratch.model.SnapshotLocation;
import java.time.*;
import java.util.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class ItemObservationCollectorTest
{
	private final Access access = new Access();
	private final Time clock = new Time();
	private final ItemObservationCollector collector = new ItemObservationCollector(access, clock);
	@Before public void start() { collector.gameTick(); collector.gameTick(); }
	private ItemObservation poll() { return collector.poll(true); }
	private static List<ObservedItem> shark(long count) { return count == 0 ? Collections.emptyList() : Collections.singletonList(new ObservedItem(ItemID.SHARK, count, false)); }
	@Test public void emptyInventoryAndEquipmentAreCompleteObservations() { assertNotNull(poll().getHoldings()); assertEquals(0, poll().getHoldings().total(ItemID.SHARK)); }
	@Test public void populatedInventoryIsRecordedByActualId()
	{
		access.items.put(InventoryID.INV, shark(20)); assertEquals(20, poll().getHoldings().at(ItemID.SHARK, SnapshotLocation.INVENTORY));
	}
	@Test public void populatedEquipmentIsRecordedByActualId()
	{
		access.items.put(InventoryID.WORN, shark(2)); assertEquals(2, poll().getHoldings().at(ItemID.SHARK, SnapshotLocation.EQUIPMENT));
	}
	@Test public void allFourEmptyPopulatedCombinationsAreAccepted()
	{
		for (int inv = 0; inv <= 1; inv++) { for (int eq = 0; eq <= 1; eq++)
		{
			access.items.put(InventoryID.INV, shark(inv)); access.items.put(InventoryID.WORN, shark(eq));
			assertEquals(inv + eq, poll().getHoldings().total(ItemID.SHARK));
		} }
	}
	@Test public void neverObservedNullInventoryIsUnknown()
	{
		access.items.put(InventoryID.INV, null); assertNull(poll().getHoldings()); assertFalse(poll().getProgress().ready(SnapshotLocation.INVENTORY));
	}
	@Test public void neverObservedNullEquipmentIsUnknown()
	{
		access.items.put(InventoryID.WORN, null); assertNull(poll().getHoldings());
	}
	@Test public void bankPinAndClosedBankCannotEstablishBaseline()
	{
		access.pin = true; assertNull(poll().getHoldings()); access.pin = false; access.open = false; assertNull(poll().getHoldings());
	}
	@Test public void nullBankIsNeverAcceptedAsEmpty() { access.items.put(InventoryID.BANK, null); assertNull(poll().getHoldings()); }
	@Test public void temporarilyMissingPersonalContainerKeepsReliableUnchangedObservation()
	{
		access.items.put(InventoryID.INV, shark(2)); poll(); access.items.put(InventoryID.INV, null);
		assertEquals(2, collector.poll(false).getHoldings().at(ItemID.SHARK, SnapshotLocation.INVENTORY));
	}
	@Test public void changedButUnavailableContainerInvalidatesItsOldValue()
	{
		poll(); access.items.put(InventoryID.INV, null); collector.changed(InventoryID.INV, null);
		assertNull(collector.poll(false).getHoldings());
	}
	@Test public void personalEventPayloadSurvivesUnavailableGetter()
	{
		access.items.put(InventoryID.INV, null); collector.changed(InventoryID.INV, shark(7));
		assertEquals(7, poll().getHoldings().at(ItemID.SHARK, SnapshotLocation.INVENTORY));
	}
	@Test public void relatedBankingUpdatesMustPassAQuietTickBoundary()
	{
		access.items.put(InventoryID.BANK, shark(100)); poll();
		access.items.put(InventoryID.BANK, shark(60)); collector.changed(InventoryID.BANK, shark(60));
		assertFalse(poll().isStable());
		access.items.put(InventoryID.INV, shark(40)); collector.changed(InventoryID.INV, shark(40));
		collector.gameTick(); assertFalse(poll().isStable()); collector.gameTick(); assertTrue(poll().isStable());
		assertEquals(100, poll().getHoldings().total(ItemID.SHARK));
	}
	@Test public void closedBankUsesOnlyItsReliableSessionObservation()
	{
		access.items.put(InventoryID.BANK, shark(100)); poll(); access.open = false; collector.bankInterfaceChanged();
		assertEquals(100, collector.poll(false).getHoldings().at(ItemID.SHARK, SnapshotLocation.BANK));
		assertNull(collector.poll(true).getHoldings());
	}
	@Test public void bankChangeWhileClosedRequiresReviewRatherThanUsingPayload()
	{
		poll(); access.open = false; collector.changed(InventoryID.BANK, shark(999));
		ItemObservation observation = collector.poll(false); assertNull(observation.getHoldings()); assertTrue(observation.isUncertainBankChange());
	}
	@Test public void withdrawalAndImmediateBankClosureRetainBothTransferLegs()
	{
		access.items.put(InventoryID.BANK, shark(100)); poll();
		collector.changed(InventoryID.BANK, shark(60)); collector.changed(InventoryID.INV, shark(40));
		access.items.put(InventoryID.INV, shark(40)); access.open = false; collector.bankInterfaceChanged();
		collector.gameTick(); collector.gameTick();
		ItemObservation observation = collector.poll(false);
		assertTrue(observation.isStable()); assertEquals(100, observation.getHoldings().total(ItemID.SHARK));
		assertEquals(60, observation.getHoldings().at(ItemID.SHARK, SnapshotLocation.BANK));
	}
	@Test public void oldPersonalObservationsExpireDuringPreparation()
	{
		poll(); access.items.put(InventoryID.WORN, null); clock.now = clock.now.plusSeconds(61);
		assertFalse(poll().getProgress().ready(SnapshotLocation.EQUIPMENT));
	}
	@Test public void resetChangesSessionAndCannotReuseOldBank()
	{
		UUID first = poll().getSessionId(); collector.reset(); access.open = false;
		assertNotEquals(first, poll().getSessionId()); assertNull(poll().getHoldings());
	}
	@Test public void duplicateAndOutOfOrderPayloadsCannotOverrideTheCurrentGetter()
	{
		access.items.put(InventoryID.INV, shark(5)); collector.changed(InventoryID.INV, shark(10)); collector.changed(InventoryID.INV, shark(1));
		assertEquals(5, poll().getHoldings().at(ItemID.SHARK, SnapshotLocation.INVENTORY));
	}
	@Test public void slotReorderingDoesNotChangeAggregateHoldings()
	{
		access.items.put(InventoryID.BANK, Arrays.asList(new ObservedItem(ItemID.SHARK, 2, false), new ObservedItem(-1, 0, false)));
		com.fromscratch.tracking.Holdings first = poll().getHoldings();
		access.items.put(InventoryID.BANK, Arrays.asList(new ObservedItem(-1, 0, false), new ObservedItem(ItemID.SHARK, 2, false)));
		assertEquals(first, poll().getHoldings());
	}
	@Test public void invalidPartialSlotsCannotReplaceReliableData()
	{
		poll(); access.items.put(InventoryID.WORN, Arrays.asList((ObservedItem) null)); assertNull(poll().getHoldings());
	}
	@Test public void actionHintsExpireAndAreAcknowledgedOnlyAfterCommit()
	{
		collector.action(ItemID.SHARK, ItemObservation.Action.DROP); assertTrue(poll().getActions().containsKey(ItemID.SHARK));
		collector.acknowledge(poll().getEventVersion()); assertTrue(poll().getActions().isEmpty());
		collector.action(ItemID.SHARK, ItemObservation.Action.CONSUME);
		for (int i = 0; i < 5; i++) { collector.gameTick(); }
		assertTrue(poll().getActions().isEmpty());
	}
	@Test public void acknowledgingAnOlderObservationDoesNotEraseANewerSameTickAction()
	{
		collector.action(ItemID.SHARK, ItemObservation.Action.DROP); long prior = poll().getEventVersion();
		collector.action(ItemID.SHARK, ItemObservation.Action.CONSUME); collector.acknowledge(prior);
		assertEquals(ItemObservation.Action.CONSUME, poll().getActions().get(ItemID.SHARK));
	}
	@Test public void confirmedEmptyPersonalObservationsSurviveTheTransitionFromSetupToTracking()
	{
		collector.seedConfirmed(com.fromscratch.tracking.TrackingFixtures.snapshot(com.fromscratch.tracking.TrackingFixtures.holdings(0, 0, 0)));
		access.items.put(InventoryID.INV, null); access.items.put(InventoryID.WORN, null);
		assertNotNull(poll().getHoldings());
		collector.changed(InventoryID.WORN, null); assertNull(poll().getHoldings());
	}
	private static final class Access implements BankSnapshotReader.Access
	{
		private final Map<Integer, List<ObservedItem>> items = new HashMap<>(); private boolean open = true; private boolean pin;
		Access() { for (int id : new int[]{InventoryID.INV, InventoryID.WORN, InventoryID.BANK}) { items.put(id, Collections.emptyList()); } }
		public boolean isInterfaceOpen(int id) { return id == InterfaceID.BANKMAIN ? open : id == InterfaceID.BANKPIN_KEYPAD && pin; }
		public boolean isVisible(int id) { return open; }
		public List<ObservedItem> items(int id) { return items.get(id); }
	}
	private static final class Time extends Clock
	{
		private Instant now = Instant.parse("2026-10-21T12:00:00Z");
		public Instant instant() { return now; } public ZoneId getZone() { return ZoneOffset.UTC; } public Clock withZone(ZoneId zone) { return this; }
	}
}
