package com.fromscratch.tracking;

import com.fromscratch.model.ContainerSnapshot;
import com.fromscratch.model.LegacySnapshot;
import com.fromscratch.model.SnapshotLocation;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** Complete physical totals, independent of slot order, stack splitting and the frozen legacy snapshot. */
@Getter
@EqualsAndHashCode
public final class Holdings
{
	private final Map<SnapshotLocation, Map<Integer, Long>> locations;
	public Holdings(Map<SnapshotLocation, Map<Integer, Long>> locations)
	{
		EnumMap<SnapshotLocation, Map<Integer, Long>> copy = new EnumMap<>(SnapshotLocation.class);
		for (SnapshotLocation location : SnapshotLocation.values())
		{
			Map<Integer, Long> items = new TreeMap<>();
			if (locations.get(location) == null) { throw new IllegalArgumentException("All physical locations are required"); }
			locations.get(location).forEach((id, quantity) ->
			{
				if (id == null || id < 0 || quantity == null || quantity <= 0) { throw new IllegalArgumentException("Invalid physical quantity"); }
				items.put(id, quantity);
			});
			copy.put(location, Collections.unmodifiableMap(items));
		}
		this.locations = Collections.unmodifiableMap(copy);
		for (int id : ids()) { total(id); } // reject aggregate long overflow at the boundary
	}
	public static Holdings from(LegacySnapshot snapshot) { return fromContainers(snapshot.getContainers()); }
	public static Holdings fromContainers(Map<SnapshotLocation, ContainerSnapshot> containers)
	{
		Map<SnapshotLocation, Map<Integer, Long>> items = new EnumMap<>(SnapshotLocation.class);
		for (SnapshotLocation location : SnapshotLocation.values()) { items.put(location, containers.get(location).getQuantities()); }
		return new Holdings(items);
	}
	public long at(int id, SnapshotLocation location) { return locations.get(location).getOrDefault(id, 0L); }
	public long total(int id)
	{
		long total = 0;
		for (SnapshotLocation location : SnapshotLocation.values()) { total = Math.addExact(total, at(id, location)); }
		return total;
	}
	public Set<Integer> ids()
	{
		Set<Integer> ids = new TreeSet<>();
		locations.values().forEach(items -> ids.addAll(items.keySet()));
		return ids;
	}
}
