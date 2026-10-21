package com.fromscratch.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** A durable activation or reset checkpoint, distinct from an unsaved setup preview. */
@Getter
@EqualsAndHashCode
public final class RunRecord
{
	private final RunState state;
	private final UUID revision;
	private final UUID previousRunId;
	private final LegacySnapshot snapshot;

	public RunRecord(RunState state, UUID revision, UUID previousRunId, LegacySnapshot snapshot)
	{
		this.state = Objects.requireNonNull(state);
		this.revision = Objects.requireNonNull(revision);
		this.previousRunId = previousRunId;
		this.snapshot = snapshot;
		if (state.getStatus() == RunStatus.ACTIVE)
		{
			Objects.requireNonNull(snapshot, "An active run requires its complete baseline");
			if (previousRunId != null || snapshot.getCapturedAt().isAfter(state.getStartedAt()))
			{
				throw new IllegalArgumentException("Invalid activation checkpoint");
			}
		}
		else if (state.getStatus() != RunStatus.NOT_STARTED || snapshot != null || previousRunId == null)
		{
			throw new IllegalArgumentException("Only activation or an explicit reset can be saved in M2");
		}
	}

	public static RunRecord activate(String characterKey, UUID runId, LegacySnapshot snapshot, Instant startedAt)
	{
		return new RunRecord(RunState.restore(RunState.CURRENT_SCHEMA_VERSION, characterKey, runId,
			RunStatus.ACTIVE, startedAt, null), UUID.randomUUID(), null, snapshot);
	}

	public static RunRecord reset(RunRecord active)
	{
		if (active.getState().getStatus() != RunStatus.ACTIVE)
		{
			throw new IllegalStateException("There is no active run to reset");
		}
		return new RunRecord(RunState.notStarted(active.getState().getCharacterKey()),
			UUID.randomUUID(), active.getState().getRunId(), null);
	}

	public VirtualEconomy getEconomy()
	{
		return new VirtualEconomy();
	}
}
