package com.fromscratch.ui;

import com.fromscratch.model.*;
import com.fromscratch.tracking.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.TableRowSorter;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.fromscratch.tracking.TrackingFixtures.*;

public class UpdateProgressEditorTest
{
	private ItemReview largeReview()
	{
		Map<Integer, Long> old = new TreeMap<>();
		// Deliberately synthetic IDs: no real bank data or item definitions are required.
		for (int id = 100_000; id < 101_000; id++) { old.put(id, 1L); }
		Map<Integer, Long> now = new TreeMap<>(old); now.remove(100_003); now.remove(100_004); now.put(ItemID.WOODEN_SHIELD, 3L);
		return new ItemReview(UUID.randomUUID(), holdings(old, Collections.emptyMap(), Collections.emptyMap()),
			holdings(now, items(100_004, 1), Collections.emptyMap()), VirtualOwnership.empty(), VirtualOwnership.empty(), "Review changes.");
	}
	private Map<Integer, String> names()
	{
		Map<Integer, String> names = new HashMap<>(); names.put(ItemID.WOODEN_SHIELD, "Wooden shield");
		names.put(100_003, "Lost resource"); names.put(100_004, "Moved resource"); return names;
	}
	@Test public void thousandUnchangedItemsAndThreeDifferencesShowOnlyThreeRows()
	{
		ItemReviewTable table = new ItemReviewTable(largeReview(), names()); assertEquals(3, table.getRowCount());
		table.filter(ItemReviewTable.Filter.ALL, "", true); assertEquals(1001, table.getRowCount());
	}
	@Test public void filtersSeparateGainsLossesAndUnresolvedDecisions()
	{
		ItemReviewTable table = new ItemReviewTable(largeReview(), names());
		table.filter(ItemReviewTable.Filter.GAINS, "", false); assertEquals(1, table.getRowCount()); assertEquals(3L, table.getValueAt(0, 1));
		table.filter(ItemReviewTable.Filter.LOSSES, "", false); assertEquals(1, table.getRowCount()); assertEquals(-1L, table.getValueAt(0, 1));
		table.filter(ItemReviewTable.Filter.REVIEW, "", false); assertEquals(2, table.getRowCount());
		table.decide(new int[]{0}, ItemReviewTable.Decision.LEGACY); table.filter(ItemReviewTable.Filter.REVIEW, "", false); assertEquals(1, table.getRowCount());
	}
	@Test public void nameSearchIsCaseInsensitiveAndLiteral()
	{
		ItemReviewTable table = new ItemReviewTable(largeReview(), names());
		table.filter(ItemReviewTable.Filter.ALL, "WOODEN", false); assertEquals(1, table.getRowCount()); assertEquals(ItemID.WOODEN_SHIELD, table.itemId(0));
		table.filter(ItemReviewTable.Filter.ALL, "[.*]", false); assertEquals(0, table.getRowCount());
	}
	@Test public void sortedViewBulkActionsTargetTheSelectedItemIds() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			ItemReviewTable table = new ItemReviewTable(largeReview(), names()); JTable view = new JTable(table);
			TableRowSorter<ItemReviewTable> sorter = new TableRowSorter<>(table); view.setRowSorter(sorter);
			sorter.setSortKeys(Collections.singletonList(new RowSorter.SortKey(0, SortOrder.DESCENDING)));
			int model = view.convertRowIndexToModel(0); assertEquals(ItemID.WOODEN_SHIELD, table.itemId(model));
			table.decide(new int[]{model}, ItemReviewTable.Decision.EARNED);
			assertEquals(3, table.edits("Reviewed gain.").getOwnership().get(ItemID.WOODEN_SHIELD).at(VirtualLocation.BANK));
			assertEquals(0, largeReview().getPrevious().total(ItemID.WOODEN_SHIELD));
		});
	}
	@Test public void bulkLegacyActionsRejectProposedAcquisitionsWithoutEditingTheOriginal()
	{
		RunRecord run = run(100, 0, 0, 0, 0, 0); AccountingPlan plan = plan(run, holdings(100, 5, 0), 2);
		ItemReview review = new ItemReview(UUID.randomUUID(), run.getItemProgress().getPhysical(), plan.getProgress().getPhysical(),
			run.getOwnership(), run.getItemProgress().getLegacyDeficits(), plan);
		ItemReviewTable table = new ItemReviewTable(review, Collections.singletonMap(ItemID.SHARK, "Shark"));
		table.decide(new int[]{0}, ItemReviewTable.Decision.LEGACY);
		assertEquals(0, table.edits("").getOwnership().get(ItemID.SHARK).getTotal());
		assertEquals(5, review.getProposed().total(ItemID.SHARK)); assertEquals(0, run.getOwnership().total(ItemID.SHARK));
	}
	@Test public void newlyEarnedDecisionUsesTheLocationThatActuallyGainedCopies()
	{
		RunRecord run = run(100, 0, 0, 0, 0, 0);
		ItemReview review = new ItemReview(UUID.randomUUID(), run.getItemProgress().getPhysical(), holdings(100, 5, 0),
			VirtualOwnership.empty(), run.getOwnership(), "Ambiguous output is locked.");
		ItemReviewTable table = new ItemReviewTable(review, Collections.emptyMap()); table.decide(new int[]{0}, ItemReviewTable.Decision.EARNED);
		VirtualItemOwnership result = table.edits("").getOwnership().get(ItemID.SHARK);
		assertEquals(0, result.at(VirtualLocation.BANK)); assertEquals(5, result.at(VirtualLocation.INVENTORY));
	}
	@Test public void unchangedItemRequiresExplicitRevealAndEarnedDecisionMeansOneCopy()
	{
		RunRecord run = run(100, 0, 0, 0, 0, 0);
		ItemReview review = new ItemReview(UUID.randomUUID(), run.getItemProgress().getPhysical(), run.getItemProgress().getPhysical(),
			run.getOwnership(), run.getOwnership(), "No changes.");
		ItemReviewTable table = new ItemReviewTable(review, Collections.emptyMap()); assertEquals(0, table.getRowCount()); assertFalse(table.hasChanges());
		table.filter(ItemReviewTable.Filter.ALL, "", true); table.decide(new int[]{0}, ItemReviewTable.Decision.EARNED);
		assertEquals(1, table.edits("").getOwnership().get(ItemID.SHARK).getTotal()); assertTrue(table.hasChanges());
		assertTrue(table.details().contains("0 → 1"));
	}
	@Test public void returnedItemDoesNotRepayTheSameLegacyDeficitTwice()
	{
		RunRecord run = run(100, 0, 0, 0, 0, 0);
		RunRecord withdrawn = run.withItems(plan(run, holdings(98, 2, 0), 2));
		AccountingPlan returned = plan(withdrawn, holdings(99, 1, 0), 3);
		ItemReview review = new ItemReview(UUID.randomUUID(), withdrawn.getItemProgress().getPhysical(), returned.getProgress().getPhysical(),
			withdrawn.getOwnership(), withdrawn.getItemProgress().getLegacyDeficits(), returned);
		ItemReviewTable table = new ItemReviewTable(review, Collections.emptyMap()); table.decide(new int[]{0}, ItemReviewTable.Decision.RETURNED);
		assertTrue(table.edits("").getDeficitOverrides().isEmpty()); assertEquals(1L, table.getValueAt(0, 9));
	}
	@Test public void lostRowsIncludeItemsAbsentFromCurrentHoldingsAndOwnership()
	{
		ItemReviewTable table = new ItemReviewTable(largeReview(), names()); table.filter(ItemReviewTable.Filter.LOSSES, "", false);
		assertEquals(100_003, table.itemId(0)); table.decide(new int[]{0}, ItemReviewTable.Decision.LOST);
		assertTrue(table.edits("").getPermanentlyRemoved().contains(100_003));
	}
	@Test public void existingUnchangedEditorHasNoChangesAndNoCorrections()
	{
		RunRecord run = run(100, 0, 0, 0, 0, 0);
		ItemReviewTable table = new ItemReviewTable(new ItemReview(UUID.randomUUID(), run.getItemProgress().getPhysical(), run.getItemProgress().getPhysical(),
			run.getOwnership(), run.getOwnership(), "No changes."), Collections.emptyMap());
		assertFalse(table.hasChanges()); assertTrue(table.corrections().isEmpty()); assertTrue(table.summary().startsWith("0 changed item types"));
	}
	@Test public void accountToolsFollowThePrimaryDashboardAndHistoryStartsCollapsed() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			FromScratchPanel panel = new FromScratchPanel(true); RunRecord run = run(100, 0, 0, 0, 0, 0);
			SetupView view = new SetupView(SetupView.Mode.ACTIVE, "Tracking", run, null, null);
			panel.render(view, SetupActions.NONE); List<String> labels = labels(panel);
			assertTrue(labels.indexOf("Recent acquisitions") < labels.indexOf("Account Tools"));
			assertFalse(labels.contains("Reconcile Progress")); assertFalse(labels.contains("Total Value Lost / Spent"));
			assertFalse(components(panel).stream().anyMatch(c -> c instanceof JButton && "View recent transactions".equals(((JButton) c).getText())));
			JButton expand = (JButton) components(panel).stream().filter(c -> c instanceof JButton && "▶ Transaction History".equals(((JButton) c).getText())).findFirst().get();
			expand.doClick(); assertTrue(components(panel).stream().anyMatch(c -> c instanceof JButton && "View recent transactions".equals(((JButton) c).getText())));
		});
	}
	@Test public void absentBankItemStillShowsItsOutstandingDeficitInAccountTools() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			RunRecord initial = run(1, 0, 0, 0, 0, 0); RunRecord withdrawn = initial.withItems(plan(initial, holdings(0, 1, 0), 2));
			FromScratchPanel panel = new FromScratchPanel(true);
			panel.render(new SetupView(SetupView.Mode.ACTIVE, "Tracking", withdrawn, null, null, com.fromscratch.observation.CaptureProgress.from(null),
				null, Collections.singletonMap(ItemID.SHARK, "Shark")), SetupActions.NONE);
			assertTrue(labels(panel).stream().anyMatch(text -> text.contains("Shark: -1 (not currently in bank)")));
		});
	}
	@Test public void developmentTraceIsDisabledByDefaultAndTargetsOnlyWoodenShield()
	{
		com.fromscratch.FromScratchConfig config = new com.fromscratch.FromScratchConfig() { };
		assertFalse(config.developmentItemTrace()); assertEquals(ItemID.WOODEN_SHIELD, config.developmentTraceItemId());
	}
	@Test public void classifyingANewCopyPreservesKnownEarnedItemsElsewhere()
	{
		VirtualOwnership elsewhere = VirtualOwnership.empty().withItem(ItemID.SHARK, new VirtualItemOwnership(5, Collections.emptyMap()));
		ItemReview review = new ItemReview(UUID.randomUUID(), holdings(100, 0, 0), holdings(100, 1, 0), elsewhere, elsewhere, "Review origin.");
		ItemReviewTable table = new ItemReviewTable(review, Collections.emptyMap()); table.decide(new int[]{0}, ItemReviewTable.Decision.EARNED);
		VirtualItemOwnership earned = table.edits("").getOwnership().get(ItemID.SHARK);
		assertEquals(6, earned.getTotal()); assertEquals(5, earned.unallocated()); assertEquals(1, earned.at(VirtualLocation.INVENTORY));
		table.decide(new int[]{0}, ItemReviewTable.Decision.LEGACY); assertTrue(table.edits("").getOwnership().isEmpty());
	}
	@Test public void bulkActionsRejectInapplicableRowsBeforeChangingAnyDecision()
	{
		ItemReviewTable table = new ItemReviewTable(largeReview(), names());
		try { table.decide(new int[]{0, 1}, ItemReviewTable.Decision.LOST); fail(); } catch (IllegalArgumentException expected) { }
		assertEquals(ItemReviewTable.Decision.DETECTED, table.getValueAt(0, 3)); assertEquals(ItemReviewTable.Decision.DETECTED, table.getValueAt(1, 3));
	}
	private static List<java.awt.Component> components(java.awt.Container parent)
	{
		List<java.awt.Component> result = new ArrayList<>();
		for (java.awt.Component component : parent.getComponents()) { result.add(component); if (component instanceof java.awt.Container) { result.addAll(components((java.awt.Container) component)); } }
		return result;
	}
	private static List<String> labels(java.awt.Container parent)
	{
		List<String> result = new ArrayList<>();
		for (java.awt.Component component : components(parent)) { if (component instanceof JLabel) { result.add(((JLabel) component).getText()); } }
		return result;
	}
}
