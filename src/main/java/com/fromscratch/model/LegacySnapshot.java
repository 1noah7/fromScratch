package com.fromscratch.model;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** Permanent baseline. No live observation is allowed to modify a confirmed snapshot. */
@Getter
@EqualsAndHashCode
public final class LegacySnapshot
{
	private final Instant capturedAt;
	private final Map<SnapshotLocation, ContainerSnapshot> containers;

	public LegacySnapshot(Instant capturedAt, Map<SnapshotLocation, ContainerSnapshot> containers)
	{
		this.capturedAt = Objects.requireNonNull(capturedAt, "Snapshot time is required");
		EnumMap<SnapshotLocation, ContainerSnapshot> copy = new EnumMap<>(SnapshotLocation.class);
		for (SnapshotLocation location : SnapshotLocation.values())
		{
			copy.put(location, Objects.requireNonNull(containers.get(location), "All three containers are required"));
		}
		this.containers = Collections.unmodifiableMap(copy);
	}

	/** Confirmation compares contents rather than the timestamp of a second read. */
	public boolean sameContents(LegacySnapshot other)
	{
		return other != null && containers.equals(other.containers);
	}

	public ContainerSnapshot at(SnapshotLocation location)
	{
		return containers.get(location);
	}
}
