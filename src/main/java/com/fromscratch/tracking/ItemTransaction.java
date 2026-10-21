package com.fromscratch.tracking;

import com.fromscratch.model.SnapshotLocation;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.Value;

/** Auditable item-only entry; signed virtual deltas are separate from the physical movement quantity. */
@Value
public class ItemTransaction
{
	public enum Category { ACQUISITION, TRANSFER, CONSUMPTION, LOSS, RECONCILIATION_ADJUSTMENT }
	public enum Confidence { OBSERVED, NET_RECONCILED, MANUAL }
	UUID id;
	Instant time;
	int itemId;
	long quantity;
	long virtualChange;
	Map<SnapshotLocation, Long> locationChanges;
	SnapshotLocation source;
	SnapshotLocation destination;
	Category category;
	Confidence confidence;
	String reason;
	long legacyWithdrawalChange;
	public ItemTransaction(UUID id, Instant time, int itemId, long quantity, long virtualChange,
		Map<SnapshotLocation, Long> changes, SnapshotLocation source, SnapshotLocation destination,
		Category category, Confidence confidence, String reason)
	{
		this(id, time, itemId, quantity, virtualChange, changes, source, destination, category, confidence, reason, 0);
	}
	public ItemTransaction(UUID id, Instant time, int itemId, long quantity, long virtualChange,
		Map<SnapshotLocation, Long> changes, SnapshotLocation source, SnapshotLocation destination,
		Category category, Confidence confidence, String reason, long legacyWithdrawalChange)
	{
		this.id = Objects.requireNonNull(id); this.time = Objects.requireNonNull(time);
		if (itemId < -1 || quantity < 0 || (itemId == -1 && (quantity != 0 || virtualChange != 0 || legacyWithdrawalChange != 0 || !changes.isEmpty())))
		{ throw new IllegalArgumentException("Invalid item transaction"); }
		this.itemId = itemId; this.quantity = quantity; this.virtualChange = virtualChange;
		EnumMap<SnapshotLocation, Long> copy = new EnumMap<>(SnapshotLocation.class);
		changes.forEach((location, delta) -> { Objects.requireNonNull(location); Objects.requireNonNull(delta); if (delta != 0) { copy.put(location, delta); } });
		this.locationChanges = Collections.unmodifiableMap(copy);
		this.source = source; this.destination = destination; this.category = Objects.requireNonNull(category);
		this.confidence = Objects.requireNonNull(confidence);
		if (reason == null || reason.isBlank() || reason.length() > 500) { throw new IllegalArgumentException("A short accounting reason is required"); }
		this.reason = reason;
		this.legacyWithdrawalChange = legacyWithdrawalChange;
	}
}
