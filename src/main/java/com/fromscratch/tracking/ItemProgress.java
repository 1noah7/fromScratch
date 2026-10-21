package com.fromscratch.tracking;

import com.fromscratch.model.LegacySnapshot;
import com.fromscratch.model.SnapshotLocation;
import com.fromscratch.model.VirtualLocation;
import com.fromscratch.model.VirtualOwnership;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** Bounded current checkpoint; the unbounded audit journal lives in separate immutable ledger files. */
@Getter
@EqualsAndHashCode
public final class ItemProgress
{
	public static final int RECENT_LIMIT = 50;
	private final Holdings physical;
	private final Map<Integer, ReturnBalance> returns;
	private final Set<Integer> unlocked;
	private final List<ItemTransaction> recent;
	private final List<ItemTransaction> acquisitions;
	private final UUID sessionId;
	private final long tick;
	private final Instant observedAt;
	private final UUID ledgerHead;
	private final long sequence;
	private final boolean established;
	private final LegacyBankDeficits legacyDeficits;
	public ItemProgress(Holdings physical, Map<Integer, ReturnBalance> returns, Set<Integer> unlocked,
		List<ItemTransaction> recent, List<ItemTransaction> acquisitions, UUID sessionId, long tick,
		Instant observedAt, UUID ledgerHead, long sequence, boolean established)
	{
		this(physical, returns, unlocked, recent, acquisitions, sessionId, tick, observedAt, ledgerHead, sequence, established, LegacyBankDeficits.empty());
	}
	public ItemProgress(Holdings physical, Map<Integer, ReturnBalance> returns, Set<Integer> unlocked,
		List<ItemTransaction> recent, List<ItemTransaction> acquisitions, UUID sessionId, long tick,
		Instant observedAt, UUID ledgerHead, long sequence, boolean established, LegacyBankDeficits legacyDeficits)
	{
		this.legacyDeficits = Objects.requireNonNull(legacyDeficits);
		this.physical = Objects.requireNonNull(physical); this.sessionId = Objects.requireNonNull(sessionId);
		this.observedAt = Objects.requireNonNull(observedAt);
		if (tick < 0 || sequence < 0 || (sequence == 0) != (ledgerHead == null) || established != (sequence > 0))
		{ throw new IllegalArgumentException("Invalid ledger cursor"); }
		Map<Integer, ReturnBalance> copy = new HashMap<>();
		returns.forEach((id, balance) ->
		{
			if (id == null || id < 0 || balance == null) { throw new IllegalArgumentException("Invalid return record"); }
			if (balance.getLegacy() != 0 || balance.getVirtual() != 0) { copy.put(id, balance); }
		});
		Set<Integer> ids = new HashSet<>(unlocked);
		if (ids.stream().anyMatch(id -> id == null || id < 0)) { throw new IllegalArgumentException("Invalid lifetime item ID"); }
		this.returns = Collections.unmodifiableMap(copy); this.unlocked = Collections.unmodifiableSet(ids);
		this.recent = immutableRecent(recent); this.acquisitions = immutableRecent(acquisitions);
		if (!established && (!returns.isEmpty() || !unlocked.isEmpty() || !recent.isEmpty() || !acquisitions.isEmpty() || !legacyDeficits.getItems().isEmpty()))
		{ throw new IllegalArgumentException("An unestablished checkpoint cannot contain tracking history"); }
		for (ItemTransaction acquisition : acquisitions)
		{
			if (acquisition.getVirtualChange() <= 0 || (acquisition.getCategory() != ItemTransaction.Category.ACQUISITION
				&& acquisition.getConfidence() != ItemTransaction.Confidence.MANUAL)) { throw new IllegalArgumentException("Invalid acquisition history"); }
		}
		this.tick = tick; this.ledgerHead = ledgerHead; this.sequence = sequence; this.established = established;
	}
	private static List<ItemTransaction> immutableRecent(List<ItemTransaction> entries)
	{
		if (entries.size() > RECENT_LIMIT || entries.stream().anyMatch(Objects::isNull)) { throw new IllegalArgumentException("Invalid recent history"); }
		return Collections.unmodifiableList(new ArrayList<>(entries));
	}
	/** M2/M3 migration retains its baseline but cannot claim a current host-session observation. */
	public static ItemProgress initial(LegacySnapshot snapshot)
	{
		return new ItemProgress(Holdings.from(snapshot), Collections.emptyMap(), Collections.emptySet(),
			Collections.emptyList(), Collections.emptyList(), new UUID(0, 0), 0, snapshot.getCapturedAt(), null, 0, false);
	}
	public void validateOwnership(VirtualOwnership ownership)
	{
		ownership.getItems().forEach((id, item) ->
		{
			if (item.at(VirtualLocation.GRAND_EXCHANGE) != 0) { throw new IllegalArgumentException("GE ownership requires a later accounting engine"); }
			for (SnapshotLocation location : SnapshotLocation.values())
			{
				if (item.at(VirtualLocation.valueOf(location.name())) > physical.at(id, location))
				{ throw new IllegalArgumentException("Virtual ownership exceeds observed physical holdings"); }
			}
		});
	}
}
