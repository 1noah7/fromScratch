package com.fromscratch.model;

import java.time.Instant;
import java.util.UUID;
import org.junit.Test;

import static org.junit.Assert.*;

/** Header boundary tests; snapshot and accounting invariants enter in later milestones. */
public class RunStateTest
{
	private static final String CHARACTER = "test-character-a";
	private static final UUID RUN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

	@Test
	public void unstartedHeaderHasCurrentVersionAndNoActiveRun()
	{
		RunState state = RunState.notStarted(CHARACTER);
		assertEquals(1, state.getSchemaVersion());
		assertEquals(CHARACTER, state.getCharacterKey());
		assertEquals(RunStatus.NOT_STARTED, state.getStatus());
		assertNull(state.getRunId());
		assertNull(state.getStartedAt());
		assertNull(state.getCompletedAt());
	}

	@Test
	public void everyPersistedLifecycleStatusHasAValidHeader()
	{
		for (RunStatus status : RunStatus.values())
		{
			boolean activated = status != RunStatus.NOT_STARTED && status != RunStatus.INITIALIZING;
			RunState state = RunState.restore(1, CHARACTER,
				status == RunStatus.NOT_STARTED ? null : RUN_ID, status,
				activated ? START : null, status == RunStatus.COMPLETED ? START.plusSeconds(1) : null);
			assertEquals(status, state.getStatus());
		}
	}

	@Test
	public void validatedRestorePreservesHeaderValues()
	{
		RunState state = RunState.restore(1, CHARACTER, RUN_ID, RunStatus.COMPLETED, START, START.plusSeconds(30));
		assertEquals(RUN_ID, state.getRunId());
		assertEquals(START, state.getStartedAt());
		assertEquals(START.plusSeconds(30), state.getCompletedAt());
		assertEquals(state, RunState.restore(1, CHARACTER, RUN_ID, RunStatus.COMPLETED, START, START.plusSeconds(30)));
	}

	@Test
	public void characterIdentityIsPartOfStateEquality()
	{
		assertNotEquals(RunState.notStarted(CHARACTER), RunState.notStarted("test-character-b"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void futureSchemasRequireExplicitMigration()
	{
		RunState.restore(2, CHARACTER, null, RunStatus.NOT_STARTED, null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void oldSchemasRequireExplicitMigration()
	{
		RunState.restore(0, CHARACTER, null, RunStatus.NOT_STARTED, null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void blankCharacterKeyIsRejected()
	{
		RunState.notStarted("  ");
	}

	@Test(expected = IllegalArgumentException.class)
	public void missingCharacterKeyIsRejected()
	{
		RunState.notStarted(null);
	}

	@Test(expected = NullPointerException.class)
	public void missingStatusIsRejected()
	{
		RunState.restore(1, CHARACTER, null, null, null, null);
	}

	@Test(expected = NullPointerException.class)
	public void activatedRunRequiresIdentity()
	{
		RunState.restore(1, CHARACTER, null, RunStatus.ACTIVE, START, null);
	}

	@Test(expected = NullPointerException.class)
	public void activatedRunRequiresStartTime()
	{
		RunState.restore(1, CHARACTER, RUN_ID, RunStatus.ACTIVE, null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void unstartedRunCannotContainAnOldRunId()
	{
		RunState.restore(1, CHARACTER, RUN_ID, RunStatus.NOT_STARTED, null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void initializingRunIsNotYetActivated()
	{
		RunState.restore(1, CHARACTER, RUN_ID, RunStatus.INITIALIZING, START, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void ongoingRunCannotContainAnEndTime()
	{
		RunState.restore(1, CHARACTER, RUN_ID, RunStatus.NEEDS_RECONCILIATION, START, START);
	}

	@Test(expected = NullPointerException.class)
	public void completedRunRequiresEndTime()
	{
		RunState.restore(1, CHARACTER, RUN_ID, RunStatus.COMPLETED, START, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void completedRunCannotEndBeforeItStarts()
	{
		RunState.restore(1, CHARACTER, RUN_ID, RunStatus.COMPLETED, START, START.minusSeconds(1));
	}
}
