package com.fromscratch.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * Pure setup workflow. Previews are ephemeral; only a successful repository commit
 * can replace the loaded run. Callers must supply fresh, verified observations.
 */
@Getter
public final class InitializationSession
{
	private final String characterKey;
	private RunRecord saved;
	private boolean initializing;
	private UUID pendingRunId;
	private UUID reviewToken;
	private LegacySnapshot preview;

	public InitializationSession(String characterKey, RunRecord saved)
	{
		this.characterKey = Objects.requireNonNull(characterKey);
		if (saved != null && !characterKey.equals(saved.getState().getCharacterKey()))
		{
			throw new IllegalArgumentException("Character mismatch");
		}
		this.saved = saved;
	}

	public void begin()
	{
		if (initializing || (saved != null && saved.getState().getStatus() == RunStatus.ACTIVE))
		{
			throw new IllegalStateException("A run or initialization already exists");
		}
		initializing = true;
		pendingRunId = UUID.randomUUID();
	}

	public void observe(LegacySnapshot observation)
	{
		if (!initializing) { throw new IllegalStateException("Begin setup before capturing a snapshot"); }
		if (preview == null || !preview.sameContents(observation))
		{
			preview = Objects.requireNonNull(observation);
			reviewToken = UUID.randomUUID();
		}
	}

	public void invalidatePreview()
	{
		preview = null;
		reviewToken = null;
	}

	public void cancel()
	{
		initializing = false;
		pendingRunId = null;
		invalidatePreview();
	}

	public RunRecord prepareActivation(UUID expectedReview, LegacySnapshot fresh, Instant confirmedAt)
	{
		if (!initializing || preview == null || !Objects.equals(reviewToken, expectedReview))
		{
			throw new IllegalStateException("Snapshot review expired. Review the current snapshot before confirming");
		}
		if (!preview.sameContents(fresh))
		{
			observe(fresh);
			throw new IllegalStateException("Snapshot changed. Review the updated summary and confirm again");
		}
		return RunRecord.activate(characterKey, pendingRunId, preview, confirmedAt);
	}

	public void committed(RunRecord record)
	{
		if (!characterKey.equals(record.getState().getCharacterKey())) { throw new IllegalArgumentException("Character mismatch"); }
		saved = record;
		cancel();
	}
}
