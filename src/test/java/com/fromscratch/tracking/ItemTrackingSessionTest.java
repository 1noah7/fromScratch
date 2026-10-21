package com.fromscratch.tracking;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.model.RunRecord;
import com.fromscratch.model.VirtualItemOwnership;
import com.fromscratch.model.VirtualLocation;
import com.fromscratch.observation.ItemObservation;
import java.util.*;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.fromscratch.tracking.TrackingFixtures.*;

public class ItemTrackingSessionTest
{
	private ItemTrackingSession ready()
	{
		ItemTrackingSession session = new ItemTrackingSession(run(100, 0, 0, 0, 0, 0), id -> id, false);
		ItemObservation observation = observation(holdings(100, 0, 0), 2);
		AccountingPlan confirmed = session.observe(observation); assertNotNull(confirmed); assertNull(session.getReview());
		session.committed(session.getRun().withItems(confirmed)); return session;
	}
	@Test public void restoredRunNeverCreditsBeforeHostReviewConfirmation()
	{
		ItemTrackingSession session = new ItemTrackingSession(run(100, 0, 0, 0, 0, 0), id -> id, false);
		assertNull(session.observe(observation(holdings(130, 0, 0), 2)));
		assertEquals(0, session.getRun().getOwnership().total(ItemID.SHARK)); assertNotNull(session.getReview());
		assertEquals(30, session.getReview().getProposed().total(ItemID.SHARK));
	}
	@Test public void unchangedSessionAutomaticallyReopensTrackingAndNeverRepeatsDuringPlay()
	{
		ItemTrackingSession session = ready(); assertFalse(session.isPreparing());
		assertNull(session.observe(observation(holdings(100, 0, 0), 3))); assertNull(session.getReview());
		assertFalse(session.isPreparing());
	}
	@Test public void normalAcquisitionProposesDurableUpdateOnlyAfterBaselineIsEstablished()
	{
		ItemTrackingSession session = ready(); AccountingPlan plan = session.observe(observation(holdings(100, 20, 0), 3));
		assertNotNull(plan); assertFalse(plan.isReviewRequired()); assertEquals(0, session.getRun().getOwnership().total(ItemID.SHARK));
		session.committed(session.getRun().withItems(plan)); assertEquals(20, session.getRun().getOwnership().total(ItemID.SHARK));
	}
	@Test public void ambiguousLossReturnsToGuidedReview()
	{
		ItemTrackingSession session = ready(); assertNull(session.observe(observation(holdings(99, 0, 0), 3)));
		assertTrue(session.isPreparing()); assertEquals(100, session.getRun().getItemProgress().getPhysical().total(ItemID.SHARK));
	}
	@Test(expected = IllegalStateException.class) public void changedContentsInvalidateTheConfirmationToken()
	{
		ItemTrackingSession session = ready(); session.requestReview(); session.observe(observation(holdings(100, 0, 0), 3));
		session.confirm(session.getReview().getToken(), observation(holdings(101, 0, 0), 4), Collections.emptyMap(), "", false);
	}
	@Test public void tabChangesDoNotReplaceAnUnchangedReviewToken()
	{
		ItemTrackingSession session = ready(); session.requestReview(); session.observe(observation(holdings(100, 0, 0), 3));
		UUID token = session.getReview().getToken(); session.observe(observation(holdings(100, 0, 0), 4));
		assertEquals(token, session.getReview().getToken());
	}
	@Test public void freshIdenticalInitialSnapshotNeedsNoSecondUserApproval()
	{
		RunRecord run = RunRecord.activate(SnapshotFixtures.CHARACTER_A, UUID.randomUUID(), snapshot(holdings(100, 0, 0)), TIME);
		ItemTrackingSession session = new ItemTrackingSession(run, id -> id, true);
		assertNotNull(session.observe(observation(holdings(100, 0, 0), 1)));
	}
	@Test public void freshActivationWithInterveningChangesRequiresReview()
	{
		RunRecord run = RunRecord.activate(SnapshotFixtures.CHARACTER_A, UUID.randomUUID(), snapshot(holdings(100, 0, 0)), TIME);
		ItemTrackingSession session = new ItemTrackingSession(run, id -> id, true);
		assertNull(session.observe(observation(holdings(101, 0, 0), 1))); assertNotNull(session.getReview());
	}
	@Test public void manualCorrectionIsBoundedAndAuditable()
	{
		ItemTrackingSession session = ready(); session.requestReview(); ItemObservation observation = observation(holdings(100, 0, 0), 3); session.observe(observation);
		VirtualItemOwnership correction = new VirtualItemOwnership(1, Collections.singletonMap(VirtualLocation.BANK, 1L));
		AccountingPlan plan = session.confirm(session.getReview().getToken(), observation, Collections.singletonMap(ItemID.SHARK, correction), "Correcting an ambiguous earned copy", false);
		assertEquals(1, plan.getOwnership().total(ItemID.SHARK));
		assertEquals(ItemTransaction.Confidence.MANUAL, plan.getTransactions().get(0).getConfidence());
	}
	@Test(expected = IllegalArgumentException.class) public void manualCorrectionsCannotExceedPhysicalQuantities()
	{
		ItemTrackingSession session = ready(); session.requestReview(); ItemObservation observation = observation(holdings(100, 0, 0), 3); session.observe(observation);
		session.confirm(session.getReview().getToken(), observation, Collections.singletonMap(ItemID.SHARK,
			new VirtualItemOwnership(101, Collections.singletonMap(VirtualLocation.BANK, 101L))), "Invalid", false);
	}
}
