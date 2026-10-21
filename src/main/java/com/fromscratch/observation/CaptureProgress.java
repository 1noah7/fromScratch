package com.fromscratch.observation;

import com.fromscratch.model.ContainerSnapshot;
import com.fromscratch.model.LegacySnapshot;
import com.fromscratch.model.SnapshotLocation;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import lombok.Value;

/** Immutable partial setup results; absent locations are unknown, never empty. */
@Value
public class CaptureProgress
{
	Map<SnapshotLocation, ContainerSnapshot> containers;

	public CaptureProgress(Map<SnapshotLocation, ContainerSnapshot> containers)
	{
		EnumMap<SnapshotLocation, ContainerSnapshot> copy = new EnumMap<>(SnapshotLocation.class);
		copy.putAll(containers);
		this.containers = Collections.unmodifiableMap(copy);
	}

	public boolean ready(SnapshotLocation location) { return containers.containsKey(location); }

	public static CaptureProgress from(LegacySnapshot snapshot)
	{
		EnumMap<SnapshotLocation, ContainerSnapshot> result = new EnumMap<>(SnapshotLocation.class);
		if (snapshot != null)
		{
			for (SnapshotLocation location : SnapshotLocation.values()) { result.put(location, snapshot.at(location)); }
		}
		return new CaptureProgress(result);
	}
}
