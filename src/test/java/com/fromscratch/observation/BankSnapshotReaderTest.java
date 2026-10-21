package com.fromscratch.observation;

import com.fromscratch.model.LegacySnapshot;
import com.fromscratch.model.SnapshotLocation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/** Real capture path with synthetic client reads; no reflection, game input or account required. */
public class BankSnapshotReaderTest
{
	private FakeAccess access;
	private BankSnapshotReader reader;
	private TestClock clock;

	@Before
	public void setUp()
	{
		access = new FakeAccess();
		clock = new TestClock();
		reader = new BankSnapshotReader(access, clock);
	}

	private void open()
	{
		access.interfaces.add(InterfaceID.BANKMAIN);
		access.visible.add(InterfaceID.Bankmain.ITEMS_CONTAINER);
		reader.interfaceChanged();
		reader.gameTick();
	}

	@Test
	public void beginBeforeBankOpensDoesNotReadCachedBankData()
	{
		access.containers.put(InventoryID.BANK, Collections.singletonList(new ObservedItem(ItemID.COINS, 1000, false)));
		expectUnavailable("Open the normal bank");
		assertFalse("A closed bank must not be read, but personal observations may be collected", access.containerReads.contains(InventoryID.BANK));
		open();
		assertEquals(1000, reader.capture().at(SnapshotLocation.BANK).quantity(ItemID.COINS));
	}

	@Test
	public void alreadyOpenBankWorksWithoutWidgetLoadedOrContainerChangedEvents()
	{
		access.interfaces.add(InterfaceID.BANKMAIN);
		access.visible.add(InterfaceID.Bankmain.ITEMS_CONTAINER);
		reader.gameTick();
		assertNotNull(reader.capture());
	}

	@Test
	public void openingWidgetWaitsForATickAndDelayedContainerData()
	{
		access.interfaces.add(InterfaceID.BANKMAIN);
		access.visible.add(InterfaceID.Bankmain.ITEMS_CONTAINER);
		reader.interfaceChanged();
		expectUnavailable("settling");
		access.containers.put(InventoryID.BANK, null);
		reader.gameTick();
		expectUnavailable("Bank container data is unavailable");
		access.containers.put(InventoryID.BANK, Collections.emptyList());
		reader.gameTick();
		assertNotNull(reader.capture());
	}

	@Test
	public void populatedBankReadsTheCompleteBankInventoryAndEquipmentContainers()
	{
		open();
		access.containers.put(InventoryID.BANK, Arrays.asList(new ObservedItem(ItemID.COINS, 2000000, false), new ObservedItem(ItemID.SHARK, 100, false)));
		access.containers.put(InventoryID.INV, Collections.singletonList(new ObservedItem(ItemID.SHARK, 2, false)));
		access.containers.put(InventoryID.WORN, Collections.singletonList(new ObservedItem(ItemID.ABYSSAL_WHIP, 1, false)));
		LegacySnapshot result = reader.capture();
		assertEquals(2000000, result.at(SnapshotLocation.BANK).quantity(ItemID.COINS));
		assertEquals(100, result.at(SnapshotLocation.BANK).quantity(ItemID.SHARK));
		assertEquals(2, result.at(SnapshotLocation.INVENTORY).quantity(ItemID.SHARK));
		assertEquals(1, result.at(SnapshotLocation.EQUIPMENT).quantity(ItemID.ABYSSAL_WHIP));
		assertEquals(Arrays.asList(InventoryID.BANK, InventoryID.INV, InventoryID.WORN), access.containerReads);
	}

	@Test
	public void genuinelyEmptyNonNullBankIsAccepted()
	{
		open();
		assertTrue(reader.capture().at(SnapshotLocation.BANK).getQuantities().isEmpty());
	}

	@Test
	public void nullBankContainerIsNeverCountedAsAnEmptyBank()
	{
		open();
		access.containers.put(InventoryID.BANK, null);
		expectUnavailable("Bank container data is unavailable");
	}

	@Test
	public void pinInterfaceRejectsEvenAnApparentlyPopulatedBank()
	{
		open();
		access.interfaces.add(InterfaceID.BANKPIN_KEYPAD);
		expectUnavailable("Bank PIN manually");
		assertFalse(access.containerReads.contains(InventoryID.BANK));
	}

	@Test
	public void bankPinSettingsAreNotTheNormalBankCaptureView()
	{
		open();
		access.interfaces.add(InterfaceID.BANKPIN_SETTINGS);
		expectUnavailable("Close Bank PIN settings");
		assertFalse(access.containerReads.contains(InventoryID.BANK));
	}

	@Test
	public void finishingPinAllowsCaptureWithoutAQuantityChange()
	{
		open();
		access.interfaces.add(InterfaceID.BANKPIN_KEYPAD);
		expectUnavailable("Bank PIN");
		access.interfaces.remove(InterfaceID.BANKPIN_KEYPAD);
		reader.gameTick();
		assertNotNull(reader.capture());
	}

	@Test
	public void incompleteBankInterfaceRejectsEvenWhenContainerExists()
	{
		access.interfaces.add(InterfaceID.BANKMAIN);
		reader.gameTick();
		expectUnavailable("item interface is not ready");
		assertFalse(access.containerReads.contains(InventoryID.BANK));
	}

	@Test
	public void cachedVisibleWidgetWithoutMountedBankIsRejected()
	{
		access.visible.add(InterfaceID.Bankmain.ITEMS_CONTAINER);
		reader.gameTick();
		expectUnavailable("Open the normal bank");
	}

	@Test
	public void searchingOrChangingTabsNeverRestrictsTheCompleteContainer()
	{
		open();
		access.visible.add(InterfaceID.Bankmain.ITEMS);
		access.containers.put(InventoryID.BANK, Arrays.asList(new ObservedItem(ItemID.SHARK, 100, false), new ObservedItem(ItemID.ABYSSAL_WHIP, 2, false)));
		LegacySnapshot all = reader.capture();
		// An empty search or another layout may hide the displayed items. Only
		// the normal bank item layer and full API container determine readiness.
		access.visible.remove(InterfaceID.Bankmain.ITEMS);
		LegacySnapshot filtered = reader.capture();
		assertTrue(all.sameContents(filtered));
		assertEquals(2, filtered.at(SnapshotLocation.BANK).getQuantities().size());
	}

	@Test
	public void bankClosureRejectsAFormerlyReadyCapture()
	{
		open();
		assertNotNull(reader.capture());
		access.interfaces.remove(InterfaceID.BANKMAIN);
		reader.interfaceChanged();
		expectUnavailable("Open the normal bank");
	}

	@Test
	public void reopeningUnchangedBankNeedsNoContainerChangeEvent()
	{
		open();
		LegacySnapshot first = reader.capture();
		access.interfaces.remove(InterfaceID.BANKMAIN);
		reader.interfaceChanged();
		expectUnavailable("Open the normal bank");
		open();
		assertTrue(first.sameContents(reader.capture()));
	}

	@Test
	public void currentContainerIsReadAgainWithoutMutatingTheOldSnapshot()
	{
		open();
		access.containers.put(InventoryID.BANK, Collections.singletonList(new ObservedItem(ItemID.SHARK, 10, false)));
		LegacySnapshot first = reader.capture();
		access.containers.put(InventoryID.BANK, Collections.singletonList(new ObservedItem(ItemID.SHARK, 20, false)));
		assertEquals(20, reader.capture().at(SnapshotLocation.BANK).quantity(ItemID.SHARK));
		assertEquals(10, first.at(SnapshotLocation.BANK).quantity(ItemID.SHARK));
	}

	@Test
	public void missingInventoryRemainsAnActionableFailure()
	{
		open();
		access.containers.put(InventoryID.INV, null);
		expectUnavailable("Inventory container data is unavailable");
	}

	@Test
	public void missingEquipmentRemainsAnActionableFailure()
	{
		open();
		access.containers.put(InventoryID.WORN, null);
		expectUnavailable("Equipment container data is unavailable");
	}

	@Test
	public void partialItemSlotStillRejectsTheSnapshot()
	{
		open();
		access.containers.put(InventoryID.BANK, Collections.singletonList(null));
		expectUnavailable("incomplete");
	}

	@Test
	public void bankReadyAndNullInventoryReportsIndependentAvailability()
	{
		open();
		access.containers.put(InventoryID.INV, null);
		expectUnavailable("Inventory container data is unavailable");
		assertTrue(reader.progress().ready(SnapshotLocation.BANK));
		assertFalse(reader.progress().ready(SnapshotLocation.INVENTORY));
		assertTrue(reader.progress().ready(SnapshotLocation.EQUIPMENT));
	}

	@Test
	public void bankReadyAndNullEquipmentReportsIndependentAvailability()
	{
		open();
		access.containers.put(InventoryID.WORN, null);
		expectUnavailable("Equipment container data is unavailable");
		assertTrue(reader.progress().ready(SnapshotLocation.BANK));
		assertTrue(reader.progress().ready(SnapshotLocation.INVENTORY));
		assertFalse(reader.progress().ready(SnapshotLocation.EQUIPMENT));
	}

	@Test
	public void personalObservationsBeforeBankRemainUsableWhenGettersBecomeNull()
	{
		access.containers.put(InventoryID.INV, Collections.singletonList(new ObservedItem(ItemID.SHARK, 2, false)));
		access.containers.put(InventoryID.WORN, Collections.singletonList(new ObservedItem(ItemID.ABYSSAL_WHIP, 1, false)));
		expectUnavailable("Open the normal bank");
		access.containers.put(InventoryID.INV, null);
		access.containers.put(InventoryID.WORN, null);
		open();
		LegacySnapshot snapshot = reader.capture();
		assertEquals(2, snapshot.at(SnapshotLocation.INVENTORY).quantity(ItemID.SHARK));
		assertEquals(1, snapshot.at(SnapshotLocation.EQUIPMENT).quantity(ItemID.ABYSSAL_WHIP));
	}

	@Test
	public void lateBankContainerCombinesIndependentPersonalObservations()
	{
		open();
		access.containers.put(InventoryID.BANK, null);
		expectUnavailable("Bank container data is unavailable");
		access.containers.put(InventoryID.INV, null);
		access.containers.put(InventoryID.WORN, null);
		access.containers.put(InventoryID.BANK, Collections.singletonList(new ObservedItem(ItemID.COINS, 123, false)));
		assertEquals(123, reader.capture().at(SnapshotLocation.BANK).quantity(ItemID.COINS));
	}

	@Test
	public void emptyValidInventoryIsReadyAndCanBeRetained()
	{
		expectUnavailable("Open the normal bank");
		assertTrue(reader.progress().ready(SnapshotLocation.INVENTORY));
		access.containers.put(InventoryID.INV, null);
		open();
		assertEquals(0, reader.capture().at(SnapshotLocation.INVENTORY).getTotalQuantity());
	}

	@Test
	public void nullInventoryNeverRefreshesTheAgeOfAnEarlierValidObservation()
	{
		expectUnavailable("Open the normal bank");
		access.containers.put(InventoryID.INV, null);
		clock.seconds(61);
		open();
		expectUnavailable("Inventory container data is unavailable");
		assertFalse(reader.progress().ready(SnapshotLocation.INVENTORY));
		assertTrue(reader.diagnostics().contains("ageMs=61000"));
	}

	@Test
	public void equipmentObservationExpiresWhileAReadyPreviewIsAwaitingConfirmation()
	{
		open();
		assertNotNull(reader.capture());
		access.containers.put(InventoryID.WORN, null);
		clock.seconds(61);
		expectUnavailable("Equipment container data is unavailable");
	}

	@Test
	public void refreshingAnExpiredContainerRestoresReadiness()
	{
		open();
		reader.capture();
		access.containers.put(InventoryID.INV, null);
		clock.seconds(61);
		expectUnavailable("Inventory container data is unavailable");
		access.containers.put(InventoryID.INV, Collections.emptyList());
		assertNotNull(reader.capture());
	}

	@Test
	public void unknownInventoryChangeInvalidatesCachedContents()
	{
		expectUnavailable("Open the normal bank");
		access.containers.put(InventoryID.INV, null);
		reader.containerChanged(InventoryID.INV, null);
		open();
		expectUnavailable("Inventory container data is unavailable");
	}

	@Test
	public void eventPayloadRemainsUsableIfTheGetterIsUnavailable()
	{
		open();
		access.containers.put(InventoryID.INV, null);
		reader.containerChanged(InventoryID.INV, Collections.singletonList(new ObservedItem(ItemID.SHARK, 4, false)));
		expectUnavailable("settling");
		reader.gameTick();
		assertEquals(4, reader.capture().at(SnapshotLocation.INVENTORY).quantity(ItemID.SHARK));
	}

	@Test
	public void bankChangesInvalidateUnavailablePersonalObservations()
	{
		open();
		reader.capture();
		access.containers.put(InventoryID.INV, null);
		access.containers.put(InventoryID.BANK, Collections.singletonList(new ObservedItem(ItemID.SHARK, 4, false)));
		expectUnavailable("Inventory container data is unavailable");
	}

	@Test
	public void bankChangeEventInvalidatesPersonalCacheBeforeConfirmation()
	{
		open();
		reader.capture();
		access.containers.put(InventoryID.INV, null);
		List<ObservedItem> bank = Collections.singletonList(new ObservedItem(ItemID.SHARK, 4, false));
		reader.containerChanged(InventoryID.BANK, bank);
		access.containers.put(InventoryID.BANK, bank);
		reader.gameTick();
		expectUnavailable("Inventory container data is unavailable");
	}

	@Test
	public void unknownBankChangeInvalidatesPreviouslyCachedPersonalData()
	{
		open();
		reader.capture();
		access.containers.put(InventoryID.INV, null);
		reader.containerChanged(InventoryID.BANK, null);
		reader.gameTick();
		expectUnavailable("Inventory container data is unavailable");
	}

	@Test
	public void malformedPersonalObservationCannotFallBackToEarlierValidContents()
	{
		open();
		reader.capture();
		access.containers.put(InventoryID.INV, Collections.singletonList(null));
		expectUnavailable("incomplete");
		assertFalse(reader.progress().ready(SnapshotLocation.INVENTORY));
	}

	@Test
	public void resetBetweenSessionsDiscardsAllCachedPersonalObservations()
	{
		open();
		reader.capture();
		reader.reset();
		access.containers.put(InventoryID.INV, null);
		reader.gameTick();
		expectUnavailable("Inventory container data is unavailable");
	}

	@Test
	public void copiedObservationIsImmutableWhenTheSourceListChanges()
	{
		List<ObservedItem> inventory = new ArrayList<>();
		inventory.add(new ObservedItem(ItemID.SHARK, 4, false));
		access.containers.put(InventoryID.INV, inventory);
		expectUnavailable("Open the normal bank");
		inventory.clear();
		access.containers.put(InventoryID.INV, null);
		open();
		assertEquals(4, reader.capture().at(SnapshotLocation.INVENTORY).quantity(ItemID.SHARK));
	}

	@Test
	public void diagnosticsExposeSizesAgesAndPredicateWithoutAccountItems()
	{
		open();
		access.containers.put(InventoryID.INV, null);
		expectUnavailable("Inventory container data is unavailable");
		String diagnostic = reader.diagnostics();
		assertTrue(diagnostic.contains("bankMounted=true"));
		assertTrue(diagnostic.contains("predicate=INVENTORY_UNAVAILABLE_OR_STALE"));
		assertTrue(diagnostic.contains("inventory={liveSize=-1,observed=false,ageMs=unknown}"));
		assertTrue(diagnostic.contains("equipment={liveSize=0,observed=true,ageMs=0}"));
		assertFalse(diagnostic.contains("quantity"));
	}

	@Test
	public void backwardsClockDoesNotMakeAnObservationTrustworthy()
	{
		expectUnavailable("Open the normal bank");
		access.containers.put(InventoryID.INV, null);
		clock.seconds(-1);
		open();
		expectUnavailable("Inventory container data is unavailable");
	}

	private static final class TestClock extends Clock
	{
		private Instant now = Instant.parse("2026-01-01T00:00:00Z");
		void seconds(long seconds) { now = now.plusSeconds(seconds); }
		public ZoneId getZone() { return ZoneOffset.UTC; }
		public Clock withZone(ZoneId zone) { return this; }
		public Instant instant() { return now; }
	}

	private void expectUnavailable(String message)
	{
		try { reader.capture(); fail("Unavailable bank must not produce a preview"); }
		catch (IllegalStateException expected) { assertTrue(expected.getMessage(), expected.getMessage().contains(message)); }
	}

	private static final class FakeAccess implements BankSnapshotReader.Access
	{
		private final Set<Integer> interfaces = new HashSet<>();
		private final Set<Integer> visible = new HashSet<>();
		private final Map<Integer, List<ObservedItem>> containers = new HashMap<>();
		private final List<Integer> containerReads = new ArrayList<>();

		FakeAccess()
		{
			containers.put(InventoryID.BANK, Collections.emptyList());
			containers.put(InventoryID.INV, Collections.emptyList());
			containers.put(InventoryID.WORN, Collections.emptyList());
		}

		public boolean isInterfaceOpen(int interfaceId) { return interfaces.contains(interfaceId); }
		public boolean isVisible(int componentId) { return visible.contains(componentId); }
		public List<ObservedItem> items(int inventoryId) { containerReads.add(inventoryId); return containers.get(inventoryId); }
	}
}
