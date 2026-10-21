package com.fromscratch.tracking;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.Value;

/** Common character/run/session envelope for immutable transaction entries in one atomic checkpoint. */
@Value
public class ItemLedgerBatch
{
	UUID id;
	UUID previousHead;
	UUID previousRevision;
	String character;
	UUID runId;
	UUID sessionId;
	long sequence;
	Instant time;
	String resultSha256;
	List<ItemTransaction> entries;
	public ItemLedgerBatch(UUID id, UUID previousHead, UUID previousRevision, String character, UUID runId,
		UUID sessionId, long sequence, Instant time, String resultSha256, List<ItemTransaction> entries)
	{
		this.id = Objects.requireNonNull(id); this.previousHead = previousHead; this.previousRevision = Objects.requireNonNull(previousRevision);
		if (character == null || !character.matches("[a-f0-9]{64}") || sequence <= 0 || (sequence == 1) != (previousHead == null)
			|| resultSha256 == null || !resultSha256.matches("[a-f0-9]{64}") || entries.isEmpty()) { throw new IllegalArgumentException("Invalid ledger batch"); }
		this.character = character; this.runId = Objects.requireNonNull(runId); this.sessionId = Objects.requireNonNull(sessionId);
		this.sequence = sequence; this.time = Objects.requireNonNull(time); this.resultSha256 = resultSha256;
		HashSet<UUID> unique = new HashSet<>();
		for (ItemTransaction entry : entries)
		{
			if (entry == null || !unique.add(entry.getId()) || !time.equals(entry.getTime())) { throw new IllegalArgumentException("Invalid or duplicate transaction entry"); }
		}
		this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
	}
}
