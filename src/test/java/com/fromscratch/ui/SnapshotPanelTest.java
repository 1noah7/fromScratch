package com.fromscratch.ui;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.model.ContainerSnapshot;
import com.fromscratch.model.SnapshotLocation;
import com.fromscratch.observation.CaptureProgress;
import java.util.EnumMap;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.Test;

import static org.junit.Assert.*;

public class SnapshotPanelTest
{
	@Test
	public void setupInstructionsAppearExactlyOnceBeforeInitialization() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			List<String> labels = text(new FromScratchPanel(true));
			assertEquals(1, labels.stream().filter("Setup"::equals).count());
			assertEquals(1, labels.stream().filter(value -> value.contains("Open your bank to capture existing bank, inventory, and equipped items. Special storage is excluded.")).count());
			assertFalse(labels.contains("Snapshot scope"));
			assertFalse(labels.contains("Setup instructions"));
		});
	}

	@Test
	public void initializationShowsCaptureStatusInsteadOfSetupInstructions() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			FromScratchPanel panel = new FromScratchPanel(true);
			panel.render(new SetupView(SetupView.Mode.INITIALIZING, "Bank container unavailable; retrying", null, null, null), SetupActions.NONE);
			assertTrue(text(panel).contains("\u25cb Open your bank."));
			assertFalse(text(panel).contains("Setup"));
			assertNull(panel.getToolTipText());
			assertFalse(button(panel, "Confirm Snapshot").isEnabled());
		});
	}

	@Test
	public void activationHidesAllSetupInstructionsAndTheirTooltips() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			FromScratchPanel panel = new FromScratchPanel(true);
			panel.render(new SetupView(SetupView.Mode.ACTIVE, "Baseline saved", SnapshotFixtures.active(SnapshotFixtures.CHARACTER_A, 10), null, null), SetupActions.NONE);
			List<String> labels = text(panel);
			assertFalse(labels.contains("Setup"));
			assertFalse(labels.contains("Snapshot scope"));
			assertFalse(labels.contains("Setup instructions"));
			assertFalse(labels.stream().anyMatch(value -> value.contains("Open your bank to capture")));
			assertNull(panel.getToolTipText());
		});
	}

	@Test
	public void signedOutPanelShowsNotStartedButDisablesBegin() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			FromScratchPanel panel = new FromScratchPanel(true);
			assertTrue(text(panel).contains("Status: Not Started"));
			assertFalse(button(panel, "Begin From Scratch").isEnabled());
		});
	}

	@Test
	public void verifiedNotStartedPanelProvidesBeginAndSetupInstructions() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			FromScratchPanel panel = new FromScratchPanel(true);
			panel.render(new SetupView(SetupView.Mode.NOT_STARTED, "Ready", null, null, null), SetupActions.NONE);
			assertTrue(button(panel, "Begin From Scratch").isEnabled());
			assertTrue(text(panel).stream().anyMatch(value -> value.contains("Open your bank to capture existing bank, inventory, and equipped items. Special storage is excluded.")));
			assertTrue(panel.getToolTipText().contains("POH, seed vault, death storage, minigame rewards"));
		});
	}

	@Test
	public void hidingOptionalHelpNeverHidesSnapshotScope() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			FromScratchPanel panel = new FromScratchPanel(false);
			assertTrue(text(panel).contains("Setup"));
			assertTrue(text(panel).stream().anyMatch(value -> value.contains("Special storage is excluded.")));
		});
	}

	@Test
	public void initializationConfirmationIsDisabledUntilCompletePreviewExists() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			FromScratchPanel panel = new FromScratchPanel(true);
			panel.render(new SetupView(SetupView.Mode.INITIALIZING, "Open bank", null, null, null), SetupActions.NONE);
			assertFalse(button(panel, "Confirm Snapshot").isEnabled());
			panel.render(new SetupView(SetupView.Mode.INITIALIZING, "Ready", null, SnapshotFixtures.snapshot(1000), UUID.randomUUID()), SetupActions.NONE);
			assertTrue(button(panel, "Confirm Snapshot").isEnabled());
			assertTrue(text(panel).contains("\u2713 Open your bank."));
			panel.render(new SetupView(SetupView.Mode.INITIALIZING, "Bank closed", null, null, null), SetupActions.NONE);
			assertFalse(button(panel, "Confirm Snapshot").isEnabled());
		});
	}

	@Test
	public void activeDashboardHidesLegacyTotalsButKeepsRealStartTime() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			FromScratchPanel panel = new FromScratchPanel(true);
			panel.render(new SetupView(SetupView.Mode.ACTIVE, "Saved", SnapshotFixtures.active(SnapshotFixtures.CHARACTER_A, 2000000), null, null), SetupActions.NONE);
			List<String> labels = text(panel);
			assertTrue(labels.contains("Status: Active"));
			assertTrue(labels.contains("Run started"));
			assertFalse(labels.contains("Legacy bank"));
			assertFalse(labels.contains("Legacy inventory"));
			assertFalse(labels.contains("Legacy equipment"));
			assertFalse(labels.stream().anyMatch(value -> value.contains("2,000,000 GP") || value.contains("Legacy coins")));
			assertFalse(labels.contains("Not tracked yet"));
			assertNotNull(button(panel, "Reset From Scratch"));
			assertEquals(3, labels.stream().filter("0 GP"::equals).count());
		});
	}

	@Test
	public void storageErrorOffersRetryWithoutAnInitializationButton() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			FromScratchPanel panel = new FromScratchPanel(true);
			panel.render(new SetupView(SetupView.Mode.ERROR, "Corrupt save preserved", null, null, null), SetupActions.NONE);
			assertNotNull(button(panel, "Retry Load"));
			assertFalse(components(panel).stream().filter(component -> component instanceof JButton)
				.map(component -> ((JButton) component).getText()).anyMatch("Begin From Scratch"::equals));
		});
	}

	@Test
	public void partialCaptureShowsEachLocationAndOnlyObservedSummaries() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			FromScratchPanel panel = new FromScratchPanel(true);
			EnumMap<SnapshotLocation, ContainerSnapshot> partial = new EnumMap<>(SnapshotLocation.class);
			partial.put(SnapshotLocation.BANK, SnapshotFixtures.snapshot(50).at(SnapshotLocation.BANK));
			partial.put(SnapshotLocation.EQUIPMENT, SnapshotFixtures.snapshot(0).at(SnapshotLocation.EQUIPMENT));
			panel.render(new SetupView(SetupView.Mode.INITIALIZING, "Close the bank and open your normal inventory interface.",
				null, null, null, new CaptureProgress(partial)), SetupActions.NONE);
			List<String> labels = text(panel);
			assertTrue(labels.contains("\u2713 Open your bank."));
			assertTrue(labels.contains("\u25cb Open your inventory."));
			assertTrue(labels.contains("\u2713 Open your worn equipment tab."));
			assertTrue(labels.contains("Observed bank"));
			assertFalse(labels.contains("Observed inventory"));
			assertFalse(labels.contains("Setup"));
			assertFalse(button(panel, "Confirm Snapshot").isEnabled());
		});
	}

	@Test
	public void completePreviewShowsAllLocationsReady() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			FromScratchPanel panel = new FromScratchPanel(true);
			panel.render(new SetupView(SetupView.Mode.INITIALIZING, "Ready", null, SnapshotFixtures.snapshot(0), UUID.randomUUID()), SetupActions.NONE);
			assertTrue(text(panel).contains("\u2713 Open your bank."));
			assertTrue(text(panel).contains("\u2713 Open your inventory."));
			assertTrue(text(panel).contains("\u2713 Open your worn equipment tab."));
			assertTrue(button(panel, "Confirm Snapshot").isEnabled());
		});
	}

	@Test
	public void stalePartialProgressCannotEnableACompletePreview() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			FromScratchPanel panel = new FromScratchPanel(true);
			panel.render(new SetupView(SetupView.Mode.INITIALIZING, "Observation expired", null, SnapshotFixtures.snapshot(0),
				UUID.randomUUID(), CaptureProgress.from(null)), SetupActions.NONE);
			assertFalse(button(panel, "Confirm Snapshot").isEnabled());
		});
	}

	private static JButton button(Container panel, String name)
	{
		return components(panel).stream().filter(component -> component instanceof JButton)
			.map(component -> (JButton) component).filter(component -> name.equals(component.getText())).findFirst().get();
	}

	private static List<String> text(Container panel)
	{
		List<String> labels = new ArrayList<>();
		for (Component component : components(panel)) { if (component instanceof JLabel) { labels.add(((JLabel) component).getText()); } }
		return labels;
	}

	private static List<Component> components(Container panel)
	{
		List<Component> result = new ArrayList<>();
		for (Component component : panel.getComponents())
		{
			result.add(component);
			if (component instanceof Container) { result.addAll(components((Container) component)); }
		}
		return result;
	}
}
