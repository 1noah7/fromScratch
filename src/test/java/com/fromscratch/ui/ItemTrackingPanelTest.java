package com.fromscratch.ui;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.model.*;
import com.fromscratch.observation.CaptureProgress;
import com.fromscratch.tracking.*;
import java.awt.Component;
import java.awt.Container;
import java.time.Instant;
import java.util.*;
import javax.swing.*;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.fromscratch.tracking.TrackingFixtures.*;

public class ItemTrackingPanelTest
{
	@Test public void observedEmptyLocationsCompleteChecklistWithoutTechnicalWaitingLabels()
	{
		List<String> steps = PreparationChecklist.steps(CaptureProgress.from(snapshot(holdings(0, 0, 0))));
		assertEquals(3, steps.stream().filter(step -> step.startsWith("\u2713")).count());
		assertFalse(steps.stream().anyMatch(step -> step.contains("Waiting")));
	}
	@Test public void unknownLocationsRemainUnchecked() { assertEquals(3, PreparationChecklist.steps(CaptureProgress.from(null)).stream().filter(step -> step.startsWith("\u25cb")).count()); }
	@Test public void hostReviewWorkflowIsVisibleOnlyWhileReconciling() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			FromScratchPanel panel = new FromScratchPanel(true); RunRecord run = run(100, 0, 0, 0, 0, 0);
			panel.render(new SetupView(SetupView.Mode.RECONCILING, "Confirm this session", run, null, null), SetupActions.NONE);
			assertTrue(labels(panel).contains("Account Tools")); assertTrue(labels(panel).contains("Update Progress"));
			assertFalse(button(panel, "Review changes").isEnabled());
			panel.render(new SetupView(SetupView.Mode.ACTIVE, "Tracking", run, null, null), SetupActions.NONE);
			assertFalse(labels(panel).contains("Reconcile Progress")); assertTrue(labels(panel).contains("Items Unlocked"));
			assertNotNull(button(panel, "Update Progress")); assertNotNull(button(panel, "▶ Transaction History"));
		});
	}
	@Test public void reviewEditorPreservesProposalUntilExplicitlyChanged()
	{
		RunRecord run = run(100, 0, 0, 0, 0, 0); AccountingPlan plan = plan(run, holdings(100, 5, 0), 2);
		ItemReview review = review(run, plan); ItemReviewTable table = new ItemReviewTable(review, Collections.singletonMap(ItemID.SHARK, "Shark"));
		assertTrue(table.corrections().isEmpty()); assertEquals("Shark", table.getValueAt(0, 0));
		table.setValueAt("3", 0, 6); assertEquals(3, table.corrections().get(ItemID.SHARK).at(VirtualLocation.INVENTORY));
		assertEquals(5, plan.getOwnership().at(ItemID.SHARK, VirtualLocation.INVENTORY));
	}
	@Test(expected = IllegalArgumentException.class) public void reviewEditorRejectsNegativeQuantities()
	{
		RunRecord run = run(100, 0, 0, 0, 0, 0); ItemReviewTable table = new ItemReviewTable(review(run, plan(run, holdings(100, 5, 0), 2)), Collections.emptyMap());
		table.setValueAt("-1", 0, 6); table.corrections();
	}
	@Test(expected = IllegalArgumentException.class) public void reviewEditorRejectsUnobservedPhysicalQuantity()
	{
		RunRecord run = run(100, 0, 0, 0, 0, 0); ItemReviewTable table = new ItemReviewTable(review(run, plan(run, holdings(100, 5, 0), 2)), Collections.emptyMap());
		table.setValueAt("6", 0, 6); table.corrections();
	}
	@Test public void reviewEditorNeverAllowsCoinOwnershipEditing()
	{
		RunRecord run = run(100, 0, 0, 0, 0, 0); AccountingPlan plan = plan(run, holdings(items(ItemID.COINS, 100), items(ItemID.SHARK, 5), Collections.emptyMap()), 2);
		ItemReviewTable table = new ItemReviewTable(review(run, plan), Collections.emptyMap()); assertEquals(1, table.getRowCount());
	}
	@Test public void runDurationUsesSavedStartAndClampsClockRollback()
	{
		assertEquals("1d 2h 03m", RunDuration.format(RunDuration.minutes(TIME, TIME.plusSeconds((26 * 60 + 3) * 60))));
		assertEquals("0h 00m", RunDuration.format(RunDuration.minutes(TIME, TIME.minusSeconds(1))));
	}
	private static ItemReview review(RunRecord run, AccountingPlan plan)
	{
		return new ItemReview(UUID.randomUUID(), run.getItemProgress().getPhysical(), plan.getProgress().getPhysical(), plan.getOwnership(), run.getOwnership(), plan.getExplanation());
	}
	private static List<Component> components(Container parent)
	{
		List<Component> list = new ArrayList<>(); for (Component c : parent.getComponents()) { list.add(c); if (c instanceof Container) { list.addAll(components((Container) c)); } } return list;
	}
	private static List<String> labels(Container panel)
	{
		List<String> labels = new ArrayList<>(); for (Component c : components(panel)) { if (c instanceof JLabel) { labels.add(((JLabel) c).getText()); } } return labels;
	}
	private static JButton button(Container panel, String title)
	{
		return components(panel).stream().filter(c -> c instanceof JButton && title.equals(((JButton) c).getText())).map(c -> (JButton) c).findFirst().get();
	}
}
