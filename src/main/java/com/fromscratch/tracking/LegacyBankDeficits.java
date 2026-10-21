package com.fromscratch.tracking;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Outstanding legacy copies removed from the bank, independent of earned ownership.
 * A bank's signed informational balance is earned bank copies minus this deficit.
 */
@Getter
@EqualsAndHashCode
public final class LegacyBankDeficits
{
	private final Map<Integer, Long> items;
	public LegacyBankDeficits(Map<Integer, Long> items)
	{
		Map<Integer, Long> copy = new TreeMap<>();
		items.forEach((id, quantity) ->
		{
			if (id == null || id < 0 || quantity == null || quantity < 0)
			{ throw new IllegalArgumentException("Legacy withdrawal counts must be nonnegative"); }
			if (quantity != 0) { copy.put(id, quantity); }
		});
		this.items = Collections.unmodifiableMap(copy);
	}
	public static LegacyBankDeficits empty() { return new LegacyBankDeficits(Collections.emptyMap()); }
	public long quantity(int id) { return items.getOrDefault(id, 0L); }
	public long display(int id, long earnedBank) { return Math.subtractExact(earnedBank, quantity(id)); }
}
