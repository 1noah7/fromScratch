package com.fromscratch.observation;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.model.LegacySnapshot;
import com.fromscratch.model.SnapshotLocation;
import java.util.EnumMap;
import java.util.List;
import net.runelite.api.gameval.ItemID;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class SnapshotAssemblerTest
{
	private EnumMap<SnapshotLocation, List<ObservedItem>> observations;

	@Before
	public void setUp() { observations = SnapshotFixtures.observations(); }

	private LegacySnapshot capture() { return SnapshotAssembler.capture(true, true, observations, SnapshotFixtures.TIME); }
	private void item(SnapshotLocation location, int id, long quantity) { observations.get(location).add(new ObservedItem(id, quantity, false)); }

	@Test
	public void emptyBankIsACompleteValidSnapshot()
	{
		assertTrue(capture().at(SnapshotLocation.BANK).getQuantities().isEmpty());
	}

	@Test
	public void populatedBankPreservesEveryTypeAndQuantity()
	{
		item(SnapshotLocation.BANK, ItemID.SHARK, 100);
		item(SnapshotLocation.BANK, ItemID.ABYSSAL_WHIP, 1);
		assertEquals(2, capture().at(SnapshotLocation.BANK).getQuantities().size());
		assertEquals(100, capture().at(SnapshotLocation.BANK).quantity(ItemID.SHARK));
	}

	@Test
	public void inventoryItemsRemainInInventoryBaseline()
	{
		item(SnapshotLocation.INVENTORY, ItemID.SHARK, 5);
		assertEquals(5, capture().at(SnapshotLocation.INVENTORY).quantity(ItemID.SHARK));
		assertEquals(0, capture().at(SnapshotLocation.BANK).quantity(ItemID.SHARK));
	}

	@Test
	public void equippedItemsRemainInEquipmentBaseline()
	{
		item(SnapshotLocation.EQUIPMENT, ItemID.ABYSSAL_WHIP, 1);
		assertEquals(1, capture().at(SnapshotLocation.EQUIPMENT).getOccupiedSlots());
		assertEquals(1, capture().at(SnapshotLocation.EQUIPMENT).quantity(ItemID.ABYSSAL_WHIP));
	}

	@Test
	public void coinsArePreservedSeparatelyInAllThreeLegacyLocations()
	{
		for (SnapshotLocation location : SnapshotLocation.values()) { item(location, ItemID.COINS, 100); }
		for (SnapshotLocation location : SnapshotLocation.values()) { assertEquals(100, capture().at(location).quantity(ItemID.COINS)); }
	}

	@Test
	public void placeholdersAndEmptySlotsAreNotOwnedItems()
	{
		observations.get(SnapshotLocation.BANK).add(new ObservedItem(ItemID.Placeholder.ABYSSAL_WHIP, 1, true));
		observations.get(SnapshotLocation.BANK).add(new ObservedItem(ItemID.BANK_FILLER, 1, true));
		item(SnapshotLocation.BANK, -1, 0);
		item(SnapshotLocation.BANK, ItemID.SHARK, 0);
		assertEquals(0, capture().at(SnapshotLocation.BANK).getOccupiedSlots());
		assertTrue(capture().at(SnapshotLocation.BANK).getQuantities().isEmpty());
	}

	@Test
	public void largeStacksAggregateBeyondSignedIntegerRange()
	{
		item(SnapshotLocation.BANK, ItemID.COINS, Integer.MAX_VALUE);
		item(SnapshotLocation.BANK, ItemID.COINS, Integer.MAX_VALUE);
		assertEquals(4294967294L, capture().at(SnapshotLocation.BANK).quantity(ItemID.COINS));
	}

	@Test
	public void multipleUnstackableCopiesAggregateWithoutLosingSlotCount()
	{
		for (int copy = 0; copy < 3; copy++) { item(SnapshotLocation.INVENTORY, ItemID.ABYSSAL_WHIP, 1); }
		assertEquals(3, capture().at(SnapshotLocation.INVENTORY).quantity(ItemID.ABYSSAL_WHIP));
		assertEquals(3, capture().at(SnapshotLocation.INVENTORY).getOccupiedSlots());
	}

	@Test
	public void distinctNotedVariantsKeepTheirExactIds()
	{
		item(SnapshotLocation.BANK, ItemID.ABYSSAL_WHIP, 1);
		item(SnapshotLocation.INVENTORY, ItemID.Cert.ABYSSAL_WHIP, 2);
		assertEquals(0, capture().at(SnapshotLocation.INVENTORY).quantity(ItemID.ABYSSAL_WHIP));
		assertEquals(2, capture().at(SnapshotLocation.INVENTORY).quantity(ItemID.Cert.ABYSSAL_WHIP));
	}

	@Test(expected = IllegalStateException.class)
	public void initializationWithoutAnOpenBankIsRejected()
	{
		SnapshotAssembler.capture(false, true, observations, SnapshotFixtures.TIME);
	}

	@Test(expected = IllegalStateException.class)
	public void staleOrIncompleteBankObservationIsRejected()
	{
		SnapshotAssembler.capture(true, false, observations, SnapshotFixtures.TIME);
	}

	@Test(expected = IllegalStateException.class)
	public void absentBankCannotBeInterpretedAsAnEmptyBank()
	{
		observations.remove(SnapshotLocation.BANK);
		capture();
	}

	@Test(expected = IllegalStateException.class)
	public void absentInventoryIsNotAssumedEmpty()
	{
		observations.remove(SnapshotLocation.INVENTORY);
		capture();
	}

	@Test(expected = IllegalStateException.class)
	public void absentEquipmentIsNotAssumedEmpty()
	{
		observations.remove(SnapshotLocation.EQUIPMENT);
		capture();
	}

	@Test(expected = IllegalStateException.class)
	public void incompleteItemSlotIsRejected()
	{
		observations.get(SnapshotLocation.BANK).add(null);
		capture();
	}

	@Test(expected = IllegalStateException.class)
	public void negativeQuantityIsRejected()
	{
		item(SnapshotLocation.BANK, ItemID.SHARK, -1);
		capture();
	}

	@Test(expected = ArithmeticException.class)
	public void quantityOverflowCannotWrapIntoAValidSnapshot()
	{
		item(SnapshotLocation.BANK, ItemID.COINS, Long.MAX_VALUE);
		item(SnapshotLocation.BANK, ItemID.COINS, 1);
		capture();
	}

	@Test
	public void subsequentPhysicalChangesCannotModifyTheOriginalBaseline()
	{
		item(SnapshotLocation.BANK, ItemID.SHARK, 10);
		LegacySnapshot original = capture();
		observations.get(SnapshotLocation.BANK).clear();
		assertEquals(10, original.at(SnapshotLocation.BANK).quantity(ItemID.SHARK));
		assertEquals(0, capture().at(SnapshotLocation.BANK).quantity(ItemID.SHARK));
	}

	@Test(expected = UnsupportedOperationException.class)
	public void legacyQuantityMapsAreImmutable()
	{
		capture().at(SnapshotLocation.BANK).getQuantities().put(ItemID.SHARK, 1L);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void legacyContainerMapsAreImmutable()
	{
		capture().getContainers().clear();
	}
}
