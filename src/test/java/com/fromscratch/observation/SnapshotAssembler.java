package com.fromscratch.observation;

import com.fromscratch.model.ContainerSnapshot;
import com.fromscratch.model.LegacySnapshot;
import com.fromscratch.model.SnapshotLocation;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Pure validation/aggregation, independent of RuneLite widgets and persistence. */
public final class SnapshotAssembler
{
	private SnapshotAssembler() { }

	public static LegacySnapshot capture(boolean bankOpen, boolean completeBankData,
		Map<SnapshotLocation, List<ObservedItem>> observations, Instant capturedAt)
	{
		if (!bankOpen || !completeBankData)
		{
			throw new IllegalStateException("The normal bank must be open with complete current bank data");
		}
		EnumMap<SnapshotLocation, ContainerSnapshot> containers = new EnumMap<>(SnapshotLocation.class);
		for (SnapshotLocation location : SnapshotLocation.values())
		{
			List<ObservedItem> slots = observations.get(location);
			if (slots == null)
			{
				throw new IllegalStateException("Bank, inventory and equipment must all be available");
			}
			containers.put(location, container(slots));
		}
		return new LegacySnapshot(capturedAt, containers);
	}

	/** Validates a single complete observation before it can enter the setup cache. */
	public static ContainerSnapshot container(List<ObservedItem> slots)
	{
		if (slots == null) { throw new IllegalStateException("Container data is unavailable"); }
		Map<Integer, Long> quantities = new TreeMap<>();
		int occupied = 0;
		for (ObservedItem item : slots)
		{
			if (item == null || item.getQuantity() < 0)
			{
				throw new IllegalStateException("An incomplete or invalid item slot was observed. Wait for bank data or close and reopen the bank.");
			}
			if (item.getItemId() < 0 || item.getQuantity() == 0 || item.isPlaceholder()) { continue; }
			quantities.merge(item.getItemId(), item.getQuantity(), Math::addExact);
			occupied = Math.incrementExact(occupied);
		}
		return new ContainerSnapshot(quantities, occupied);
	}
}
