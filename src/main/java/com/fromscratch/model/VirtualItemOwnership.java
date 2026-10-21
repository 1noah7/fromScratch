package com.fromscratch.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Immutable ownership of one exact item ID, independent of physical/legacy copies.
 * total >= sum(locations); the difference is owned but not yet allocated to a
 * tracked location. Missing location entries mean zero, never an inferred loss.
 */
@Getter
@EqualsAndHashCode
public final class VirtualItemOwnership
{
	private final long total;
	private final Map<VirtualLocation, Long> locations;

	public VirtualItemOwnership(long total, Map<VirtualLocation, Long> locations)
	{
		if (total < 0) { throw new IllegalArgumentException("Virtual item totals cannot be negative"); }
		Objects.requireNonNull(locations, "Virtual locations are required");
		EnumMap<VirtualLocation, Long> copy = new EnumMap<>(VirtualLocation.class);
		long allocated = 0;
		for (Map.Entry<VirtualLocation, Long> entry : locations.entrySet())
		{
			Objects.requireNonNull(entry.getKey(), "Virtual location is required");
			Long quantity = Objects.requireNonNull(entry.getValue(), "Virtual quantity is required");
			if (quantity < 0) { throw new IllegalArgumentException("Virtual allocations cannot be negative"); }
			allocated = Math.addExact(allocated, quantity);
			if (quantity > 0) { copy.put(entry.getKey(), quantity); }
		}
		if (allocated > total) { throw new IllegalArgumentException("Virtual allocations exceed total ownership"); }
		this.total = total;
		this.locations = Collections.unmodifiableMap(copy);
	}

	public long at(VirtualLocation location) { return locations.getOrDefault(Objects.requireNonNull(location), 0L); }

	public long unallocated()
	{
		long allocated = 0;
		for (long quantity : locations.values()) { allocated = Math.addExact(allocated, quantity); }
		return Math.subtractExact(total, allocated);
	}
}
