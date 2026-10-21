package com.fromscratch.tracking;

import com.fromscratch.model.VirtualItemOwnership;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Getter;

/** Explicit player decisions bound by the review token; these never mutate a live run themselves. */
@Getter
public final class ItemReviewEdits
{
	private final Map<Integer, VirtualItemOwnership> ownership;
	private final Set<Integer> returned;
	private final Set<Integer> permanentlyRemoved;
	private final String reason;
	private final Map<Integer, String> decisions;
	public ItemReviewEdits(Map<Integer, VirtualItemOwnership> ownership, Map<Integer, Long> legacyDeficits,
		Set<Integer> returned, Set<Integer> permanentlyRemoved, String reason)
	{
		this(ownership, legacyDeficits, returned, permanentlyRemoved, reason, Collections.emptyMap());
	}
	public ItemReviewEdits(Map<Integer, VirtualItemOwnership> ownership, Map<Integer, Long> legacyDeficits,
		Set<Integer> returned, Set<Integer> permanentlyRemoved, String reason, Map<Integer, String> decisions)
	{
		this.ownership = Collections.unmodifiableMap(new HashMap<>(ownership));
		new LegacyBankDeficits(legacyDeficits); // Validate IDs/ranges, while retaining meaningful zero overrides below.
		// Zero overrides are meaningful, so keep them rather than normalizing them away.
		Map<Integer, Long> overrides = new HashMap<>(legacyDeficits);
		this.deficitOverrides = Collections.unmodifiableMap(overrides);
		this.returned = Collections.unmodifiableSet(new HashSet<>(returned));
		this.permanentlyRemoved = Collections.unmodifiableSet(new HashSet<>(permanentlyRemoved));
		if (reason == null || reason.isBlank() || reason.length() > 300) { throw new IllegalArgumentException("A short explanation is required for item decisions"); }
		this.reason = reason;
		Map<Integer, String> choices = new HashMap<>();
		decisions.forEach((id, choice) ->
		{
			if (id == null || id < 0 || choice == null || choice.isBlank() || choice.length() > 60) { throw new IllegalArgumentException("Invalid item decision"); }
			choices.put(id, choice);
		});
		this.decisions = Collections.unmodifiableMap(choices);
	}
	private final Map<Integer, Long> deficitOverrides;
}
