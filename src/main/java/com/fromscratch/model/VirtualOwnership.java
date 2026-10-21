package com.fromscratch.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** Sparse, immutable exact-ID ownership. An absent item is valid zero ownership. */
@Getter
@EqualsAndHashCode
public final class VirtualOwnership
{
	private static final VirtualItemOwnership ZERO = new VirtualItemOwnership(0, Collections.emptyMap());
	private static final VirtualOwnership EMPTY = new VirtualOwnership(Collections.emptyMap());
	private final Map<Integer, VirtualItemOwnership> items;
	private final Map<Integer, Long> totals;

	public VirtualOwnership(Map<Integer, VirtualItemOwnership> items)
	{
		Map<Integer, VirtualItemOwnership> copy = new HashMap<>();
		Map<Integer, Long> totals = new HashMap<>();
		for (Map.Entry<Integer, VirtualItemOwnership> entry : Objects.requireNonNull(items).entrySet())
		{
			if (entry.getKey() == null || entry.getKey() < 0) { throw new IllegalArgumentException("Invalid virtual item ID"); }
			VirtualItemOwnership item = Objects.requireNonNull(entry.getValue(), "Virtual item is required");
			if (item.getTotal() > 0)
			{
				copy.put(entry.getKey(), item);
				totals.put(entry.getKey(), item.getTotal());
			}
		}
		this.items = Collections.unmodifiableMap(copy);
		this.totals = Collections.unmodifiableMap(totals);
	}

	public static VirtualOwnership empty() { return EMPTY; }
	public VirtualItemOwnership item(int itemId) { return items.getOrDefault(itemId, ZERO); }
	public long total(int itemId) { return item(itemId).getTotal(); }
	public long at(int itemId, VirtualLocation location) { return item(itemId).at(location); }

	/** Pure replacement for a future validated accounting command; no game observation or earning inference. */
	public VirtualOwnership withItem(int id, VirtualItemOwnership item)
	{
		Map<Integer, VirtualItemOwnership> next = new HashMap<>(items);
		next.put(id, item);
		return new VirtualOwnership(next);
	}
}
