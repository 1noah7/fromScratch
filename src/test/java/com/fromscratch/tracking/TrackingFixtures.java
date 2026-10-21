package com.fromscratch.tracking;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.model.*;
import com.fromscratch.observation.CaptureProgress;
import com.fromscratch.observation.ItemObservation;
import java.time.Instant;
import java.util.*;
import net.runelite.api.gameval.ItemID;

/** Synthetic character/run/container state only; never reads local account data. */
public final class TrackingFixtures
{
	public static final Instant TIME = Instant.parse("2026-10-21T12:00:00Z");
	public static final UUID SESSION = UUID.fromString("00000000-0000-0000-0000-000000000004");
	public static final ItemAccountingEngine ENGINE = new ItemAccountingEngine();
	private TrackingFixtures() { }
	public static Holdings holdings(long bank, long inventory, long equipment)
	{
		return holdings(items(ItemID.SHARK, bank), items(ItemID.SHARK, inventory), items(ItemID.SHARK, equipment));
	}
	public static Map<Integer, Long> items(int id, long quantity) { return quantity == 0 ? Collections.emptyMap() : Collections.singletonMap(id, quantity); }
	public static Holdings holdings(Map<Integer, Long> bank, Map<Integer, Long> inventory, Map<Integer, Long> equipment)
	{
		Map<SnapshotLocation, Map<Integer, Long>> locations = new EnumMap<>(SnapshotLocation.class);
		locations.put(SnapshotLocation.BANK, bank); locations.put(SnapshotLocation.INVENTORY, inventory); locations.put(SnapshotLocation.EQUIPMENT, equipment);
		return new Holdings(locations);
	}
	public static LegacySnapshot snapshot(Holdings holdings)
	{
		Map<SnapshotLocation, ContainerSnapshot> locations = new EnumMap<>(SnapshotLocation.class);
		holdings.getLocations().forEach((location, items) -> locations.put(location, new ContainerSnapshot(items, items.size())));
		return new LegacySnapshot(TIME, locations);
	}
	public static ItemObservation observation(Holdings holdings, long tick)
	{
		return observation(holdings, tick, Collections.emptyMap(), SESSION);
	}
	public static ItemObservation observation(Holdings holdings, long tick, Map<Integer, ItemObservation.Action> actions, UUID session)
	{
		return new ItemObservation(session, tick, TIME.plusSeconds(tick), CaptureProgress.from(snapshot(holdings)), holdings, true, true, false, actions);
	}
	public static RunRecord run(long bank, long inventory, long equipment, long vb, long vi, long ve)
	{
		RunRecord run = RunRecord.activate(SnapshotFixtures.CHARACTER_A, UUID.randomUUID(), snapshot(holdings(bank, inventory, equipment)), TIME);
		Map<VirtualLocation, Long> locations = new EnumMap<>(VirtualLocation.class);
		locations.put(VirtualLocation.BANK, vb); locations.put(VirtualLocation.INVENTORY, vi); locations.put(VirtualLocation.EQUIPMENT, ve);
		run = run.withOwnership(VirtualOwnership.empty().withItem(ItemID.SHARK, new VirtualItemOwnership(vb + vi + ve, locations)));
		return run.withItems(ENGINE.plan(run, observation(holdings(bank, inventory, equipment), 1), id -> id, true, false, Collections.emptyMap(), ""));
	}
	public static AccountingPlan plan(RunRecord run, Holdings next, long tick)
	{
		return ENGINE.plan(run, observation(next, tick), id -> id, false, false, Collections.emptyMap(), "");
	}
}
