package com.fromscratch.model;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** Immutable exact-ID quantities, aggregated across duplicate physical slots. */
@Getter
@EqualsAndHashCode
public final class ContainerSnapshot
{
	private final Map<Integer, Long> quantities;
	private final int occupiedSlots;
	private final long totalQuantity;

	public ContainerSnapshot(Map<Integer, Long> quantities, int occupiedSlots)
	{
		TreeMap<Integer, Long> copy = new TreeMap<>();
		long total = 0;
		for (Map.Entry<Integer, Long> entry : quantities.entrySet())
		{
			if (entry.getKey() == null || entry.getKey() < 0 || entry.getValue() == null || entry.getValue() <= 0)
			{
				throw new IllegalArgumentException("Snapshot item IDs and quantities must be valid");
			}
			total = Math.addExact(total, entry.getValue());
			copy.put(entry.getKey(), entry.getValue());
		}
		if (occupiedSlots < copy.size() || occupiedSlots > total || (copy.isEmpty() && occupiedSlots != 0))
		{
			throw new IllegalArgumentException("Snapshot slot count does not match its quantities");
		}
		this.quantities = Collections.unmodifiableMap(copy);
		this.occupiedSlots = occupiedSlots;
		this.totalQuantity = total;
	}

	public long quantity(int itemId)
	{
		return quantities.getOrDefault(itemId, 0L);
	}
}
