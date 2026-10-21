package com.fromscratch.model;

import com.fromscratch.SnapshotFixtures;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class InitializationSessionTest
{
	private InitializationSession session;

	@Before
	public void setUp() { session = new InitializationSession(SnapshotFixtures.CHARACTER_A, null); }

	@Test
	public void completeEmptyBankCanBeExplicitlyActivatedWithZeroVirtualWealth()
	{
		session.begin();
		LegacySnapshot empty = SnapshotFixtures.snapshot(0);
		session.observe(empty);
		RunRecord candidate = session.prepareActivation(session.getReviewToken(), empty, SnapshotFixtures.TIME.plusSeconds(1));
		session.committed(candidate);
		assertTrue(session.getSaved().getSnapshot().at(SnapshotLocation.BANK).getQuantities().isEmpty());
		assertEquals(RunStatus.ACTIVE, session.getSaved().getState().getStatus());
		assertEquals(0, session.getSaved().getEconomy().getNetWorth());
	}

	@Test
	public void previewDoesNotActivateOrSaveARun()
	{
		session.begin();
		session.observe(SnapshotFixtures.snapshot(1000));
		assertTrue(session.isInitializing());
		assertNull(session.getSaved());
	}

	@Test(expected = IllegalStateException.class)
	public void duplicateBeginDuringSetupIsRejected() { session.begin(); session.begin(); }

	@Test(expected = IllegalStateException.class)
	public void duplicateInitializationOfActiveRunIsRejected()
	{
		new InitializationSession(SnapshotFixtures.CHARACTER_A, SnapshotFixtures.active(SnapshotFixtures.CHARACTER_A, 1)).begin();
	}

	@Test(expected = IllegalStateException.class)
	public void activationWithoutACompletePreviewIsRejected()
	{
		session.begin();
		session.prepareActivation(null, SnapshotFixtures.snapshot(0), SnapshotFixtures.TIME.plusSeconds(1));
	}

	@Test(expected = IllegalStateException.class)
	public void confirmationOfAnExpiredReviewIsRejected()
	{
		session.begin();
		session.observe(SnapshotFixtures.snapshot(1));
		session.prepareActivation(UUID.randomUUID(), SnapshotFixtures.snapshot(1), SnapshotFixtures.TIME.plusSeconds(1));
	}

	@Test
	public void changedPhysicalContentsRequireAnotherExplicitConfirmation()
	{
		session.begin();
		session.observe(SnapshotFixtures.snapshot(1));
		UUID token = session.getReviewToken();
		try
		{
			session.prepareActivation(token, SnapshotFixtures.snapshot(2), SnapshotFixtures.TIME.plusSeconds(1));
			fail("Changed snapshot must not activate");
		}
		catch (IllegalStateException expected) { assertTrue(expected.getMessage().contains("changed")); }
		assertNull(session.getSaved());
		assertNotEquals(token, session.getReviewToken());
	}

	@Test
	public void unchangedContainersKeepTheirReviewTokenAcrossBankDisplayChanges()
	{
		session.begin();
		session.observe(SnapshotFixtures.snapshot(1));
		UUID token = session.getReviewToken();
		session.observe(SnapshotFixtures.snapshot(1));
		assertEquals(token, session.getReviewToken());
	}

	@Test
	public void cancellingIncompleteSetupLeavesNoRun()
	{
		session.begin();
		session.cancel();
		assertFalse(session.isInitializing());
		assertNull(session.getPreview());
		assertNull(session.getSaved());
	}

	@Test
	public void onlyCommittedActivationBecomesTheSavedRun()
	{
		session.begin();
		session.observe(SnapshotFixtures.snapshot(1000000));
		RunRecord candidate = session.prepareActivation(session.getReviewToken(), SnapshotFixtures.snapshot(1000000), SnapshotFixtures.TIME.plusSeconds(1));
		assertNull(session.getSaved());
		session.committed(candidate);
		assertEquals(RunStatus.ACTIVE, session.getSaved().getState().getStatus());
		assertFalse(session.isInitializing());
	}

	@Test
	public void initialVirtualEconomyIsCompletelyEmptyDespiteLegacyCoins()
	{
		VirtualEconomy economy = SnapshotFixtures.active(SnapshotFixtures.CHARACTER_A, Long.MAX_VALUE).getEconomy();
		assertEquals(0, economy.getSpendableGp());
		assertEquals(0, economy.getNetWorth());
		assertEquals(0, economy.getTotalValueGained());
		assertEquals(0, economy.getItemsUnlocked());
		assertTrue(economy.getItems().isEmpty());
	}

	@Test
	public void resetReturnsNotStartedWithoutAutomaticallyCreatingANewRun()
	{
		RunRecord reset = RunRecord.reset(SnapshotFixtures.active(SnapshotFixtures.CHARACTER_A, 1));
		assertEquals(RunStatus.NOT_STARTED, reset.getState().getStatus());
		assertNull(reset.getState().getRunId());
		assertNull(reset.getSnapshot());
		assertNotNull(reset.getPreviousRunId());
	}

	@Test(expected = IllegalArgumentException.class)
	public void loadedRunFromAnotherCharacterIsRejected()
	{
		new InitializationSession(SnapshotFixtures.CHARACTER_A, SnapshotFixtures.active(SnapshotFixtures.CHARACTER_B, 1));
	}

	@Test(expected = IllegalArgumentException.class)
	public void anotherCharactersCommitCannotChangeTheSession()
	{
		session.committed(SnapshotFixtures.active(SnapshotFixtures.CHARACTER_B, 1));
	}
}
