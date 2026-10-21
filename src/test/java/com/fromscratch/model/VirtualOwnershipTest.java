package com.fromscratch.model;

import com.fromscratch.SnapshotFixtures;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class VirtualOwnershipTest
{
	public static VirtualOwnership fixture()
	{
		Map<VirtualLocation, Long> locations = new EnumMap<>(VirtualLocation.class);
		locations.put(VirtualLocation.BANK, 75L);
		locations.put(VirtualLocation.INVENTORY, 20L);
		locations.put(VirtualLocation.EQUIPMENT, 1L);
		locations.put(VirtualLocation.GRAND_EXCHANGE, 3L);
		return VirtualOwnership.empty().withItem(ItemID.SHARK, new VirtualItemOwnership(100, locations));
	}

	@Test public void initializationLeavesAllLegacyTypesVirtuallyZero()
	{
		RunRecord run = SnapshotFixtures.active(SnapshotFixtures.CHARACTER_A, 2000000);
		assertEquals(0, run.getOwnership().total(ItemID.COINS));
		assertEquals(0, run.getOwnership().at(ItemID.COINS, VirtualLocation.BANK));
		assertTrue(run.getOwnership().getItems().isEmpty());
	}

	@Test public void missingRecordsAndLocationsAreValidZero()
	{
		assertEquals(0, VirtualOwnership.empty().total(ItemID.SHARK));
		for (VirtualLocation location : VirtualLocation.values()) { assertEquals(0, VirtualOwnership.empty().at(ItemID.SHARK, location)); }
	}

	@Test public void bankAllocationIsIndependentOfTotalAndOtherLocations()
	{
		assertEquals(100, fixture().total(ItemID.SHARK));
		assertEquals(75, fixture().at(ItemID.SHARK, VirtualLocation.BANK));
		assertEquals(20, fixture().at(ItemID.SHARK, VirtualLocation.INVENTORY));
		assertEquals(1, fixture().item(ItemID.SHARK).unallocated());
	}

	@Test public void stacksUseLongQuantitiesWithoutPhysicalBounds()
	{
		VirtualOwnership owned = VirtualOwnership.empty().withItem(ItemID.COINS,
			new VirtualItemOwnership(Long.MAX_VALUE, Collections.singletonMap(VirtualLocation.BANK, Long.MAX_VALUE)));
		assertEquals(Long.MAX_VALUE, owned.at(ItemID.COINS, VirtualLocation.BANK));
	}

	@Test public void unstackableCopiesRemainAggregateQuantities()
	{
		VirtualOwnership owned = VirtualOwnership.empty().withItem(ItemID.ABYSSAL_WHIP,
			new VirtualItemOwnership(2, Collections.singletonMap(VirtualLocation.BANK, 1L)));
		assertEquals(2, owned.total(ItemID.ABYSSAL_WHIP));
		assertEquals(1, owned.at(ItemID.ABYSSAL_WHIP, VirtualLocation.BANK));
	}

	@Test public void distinctRawVariantIdsAreNeverMerged()
	{
		VirtualOwnership owned = VirtualOwnership.empty()
			.withItem(ItemID.ABYSSAL_WHIP, new VirtualItemOwnership(1, Collections.emptyMap()))
			.withItem(ItemID.Cert.ABYSSAL_WHIP, new VirtualItemOwnership(2, Collections.emptyMap()));
		assertEquals(1, owned.total(ItemID.ABYSSAL_WHIP));
		assertEquals(2, owned.total(ItemID.Cert.ABYSSAL_WHIP));
	}

	@Test(expected = IllegalArgumentException.class) public void negativeTotalIsRejected() { new VirtualItemOwnership(-1, Collections.emptyMap()); }
	@Test(expected = IllegalArgumentException.class) public void negativeAllocationIsRejected() { new VirtualItemOwnership(1, Collections.singletonMap(VirtualLocation.BANK, -1L)); }
	@Test(expected = IllegalArgumentException.class) public void allocationsCannotExceedTotal() { new VirtualItemOwnership(1, Collections.singletonMap(VirtualLocation.BANK, 2L)); }
	@Test(expected = ArithmeticException.class) public void allocationOverflowIsRejected()
	{
		Map<VirtualLocation, Long> values = new EnumMap<>(VirtualLocation.class);
		values.put(VirtualLocation.BANK, Long.MAX_VALUE);
		values.put(VirtualLocation.INVENTORY, 1L);
		new VirtualItemOwnership(Long.MAX_VALUE, values);
	}

	@Test public void ownershipUpdatesNeverModifyLegacySnapshotOrOriginalRun()
	{
		RunRecord original = SnapshotFixtures.active(SnapshotFixtures.CHARACTER_A, 100);
		RunRecord changed = original.withOwnership(fixture());
		assertSame(original.getSnapshot(), changed.getSnapshot());
		assertSame(original.getState(), changed.getState());
		assertEquals(100, changed.getSnapshot().at(SnapshotLocation.BANK).quantity(ItemID.COINS));
		assertTrue(original.getOwnership().getItems().isEmpty());
		assertEquals(100, changed.getOwnership().total(ItemID.SHARK));
		assertNotEquals(original.getRevision(), changed.getRevision());
	}

	@Test public void ownershipDefensivelyCopiesInputsAndExposesImmutableMaps()
	{
		Map<Integer, VirtualItemOwnership> input = new HashMap<>(fixture().getItems());
		VirtualOwnership owned = new VirtualOwnership(input);
		input.clear();
		assertEquals(100, owned.total(ItemID.SHARK));
		try { owned.getItems().clear(); fail(); } catch (UnsupportedOperationException expected) { }
		try { owned.item(ItemID.SHARK).getLocations().clear(); fail(); } catch (UnsupportedOperationException expected) { }
	}

	@Test public void zeroRecordsNormalizeToSparseEmptyOwnership()
	{
		assertEquals(VirtualOwnership.empty(), VirtualOwnership.empty().withItem(ItemID.SHARK, new VirtualItemOwnership(0, Collections.emptyMap())));
	}

	@Test(expected = IllegalArgumentException.class) public void resetCannotCarryVirtualAssets()
	{
		RunRecord.reset(SnapshotFixtures.active(SnapshotFixtures.CHARACTER_A, 100)).withOwnership(fixture());
	}
}
