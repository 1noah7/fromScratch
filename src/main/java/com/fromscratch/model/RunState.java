package com.fromscratch.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Immutable, versioned lifecycle header for a single character's run.
 *
 * <p>This is a domain object, not a Gson deserialization target. A future storage
 * DTO must pass through {@link #restore} after schema migration so malformed data
 * cannot bypass constructor validation. No lifecycle transitions or storage are
 * enabled in the foundation milestone.</p>
 */
@Getter
@EqualsAndHashCode
public final class RunState
{
	public static final int CURRENT_SCHEMA_VERSION = 1;

	private final int schemaVersion;
	// Opaque RuneLite RS profile identity; never use a display name as a save key.
	private final String characterKey;
	private final UUID runId;
	private final RunStatus status;
	private final Instant startedAt;
	private final Instant completedAt;

	private RunState(int schemaVersion, String characterKey, UUID runId,
		RunStatus status, Instant startedAt, Instant completedAt)
	{
		if (schemaVersion != CURRENT_SCHEMA_VERSION)
		{
			throw new IllegalArgumentException("Unsupported run schema; migrate before restoring");
		}
		if (characterKey == null || characterKey.trim().isEmpty())
		{
			throw new IllegalArgumentException("A character profile key is required");
		}
		Objects.requireNonNull(status, "Run status is required");
		if (status == RunStatus.NOT_STARTED)
		{
			if (runId != null || startedAt != null || completedAt != null)
			{
				throw new IllegalArgumentException("An unstarted run cannot contain active run identity or times");
			}
		}
		else
		{
			Objects.requireNonNull(runId, "An initialized run requires a run ID");
			if (status == RunStatus.INITIALIZING)
			{
				if (startedAt != null || completedAt != null)
				{
					throw new IllegalArgumentException("Initialization is not an activated run");
				}
			}
			else
			{
				Objects.requireNonNull(startedAt, "An activated run requires a start time");
				if (status == RunStatus.COMPLETED)
				{
					Objects.requireNonNull(completedAt, "A completed run requires an end time");
					if (completedAt.isBefore(startedAt))
					{
						throw new IllegalArgumentException("Run end precedes its start");
					}
				}
				else if (completedAt != null)
				{
					throw new IllegalArgumentException("An ongoing run cannot have an end time");
				}
			}
		}
		this.schemaVersion = schemaVersion;
		this.characterKey = characterKey;
		this.runId = runId;
		this.status = status;
		this.startedAt = startedAt;
		this.completedAt = completedAt;
	}

	/** Creates an explicit empty header only after a character has been identified. */
	public static RunState notStarted(String characterKey)
	{
		return restore(CURRENT_SCHEMA_VERSION, characterKey, null, RunStatus.NOT_STARTED, null, null);
	}

	/** Validates a migrated header without silently replacing invalid data with defaults. */
	public static RunState restore(int schemaVersion, String characterKey, UUID runId,
		RunStatus status, Instant startedAt, Instant completedAt)
	{
		return new RunState(schemaVersion, characterKey, runId, status, startedAt, completedAt);
	}
}
