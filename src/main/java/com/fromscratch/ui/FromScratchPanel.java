package com.fromscratch.ui;

import com.fromscratch.model.ContainerSnapshot;
import com.fromscratch.model.LegacySnapshot;
import com.fromscratch.model.SnapshotLocation;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.text.NumberFormat;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.PluginPanel;
import net.runelite.api.gameval.ItemID;

/** Setup previews and the progression dashboard. Rendering and accounting stay outside Swing. */
public final class FromScratchPanel extends PluginPanel
{
	static final Color PURPLE = new Color(0xB450FF);
	static final Color GOLD = new Color(0xFFD54F);

	private static final String SETUP_TEXT = "Open your bank to capture existing bank, inventory, and equipped items. Special storage is excluded.";
	private static final String SETUP_DETAILS = "All captured items and coins become legacy. POH, seed vault, death storage, minigame rewards and other special storage are excluded. Nothing moves automatically.";
	private boolean showSetupDetails;
	private boolean beforeInitialization;
	private final JPanel setup = new JPanel(new DynamicGridLayout(0, 1, 0, 4));
	private final JLabel durationValue = new JLabel("0h 00m");

	public FromScratchPanel(boolean showHelp)
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			throw new IllegalStateException("Create the From Scratch panel on the EDT");
		}
		JLabel title = new JLabel("From Scratch");
		title.setForeground(PURPLE);
		title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
		add(title);
		add(new JLabel("Dashboard"));
		setup.setBackground(ColorScheme.DARK_GRAY_COLOR);
		add(setup);

		add(metric("Total Value Gained", "0 GP", GOLD));
		add(metric("Current Net Worth", "0 GP", GOLD));
		add(metric("Spendable GP", "0 GP", GOLD));
		add(metric("Active run duration", durationValue, Color.WHITE));
		add(metric("Items Unlocked", "0", PURPLE));
		add(metric("Total Value Lost / Spent", "0 GP", GOLD));
		add(message("Recent acquisitions", "No acquisitions recorded."));

		setFoundationHelpVisible(showHelp);
		render(SetupView.signedOut(), SetupActions.NONE);
	}

	/** Replaces presentation only; button callbacks return local requests to the controller. */
	void render(SetupView view, SetupActions actions)
	{
		setup.removeAll();
		durationValue.setText(view.getSaved() != null && view.getSaved().getSnapshot() != null ? "Not tracked yet" : "0h 00m");
		String status;
		switch (view.getMode())
		{
			case ACTIVE: status = "Active"; break;
			case INITIALIZING: status = "Initializing"; break;
			case SAVING: status = view.getSaved() != null && view.getSaved().getSnapshot() != null
				? "Active (resetting)" : "Initializing (saving)"; break;
			case LOADING: status = "Loading"; break;
			case ERROR: status = "Storage error"; break;
			default: status = "Not Started";
		}
		setup.add(new JLabel("Status: " + status));
		setup.add(message("Run status", view.getMessage()));
		beforeInitialization = view.getMode() == SetupView.Mode.NOT_STARTED || view.getMode() == SetupView.Mode.SIGNED_OUT;
		if (beforeInitialization)
		{
			setup.add(message("Setup", SETUP_TEXT));
		}
		refreshSetupTooltip();
		LegacySnapshot summary = view.getMode() == SetupView.Mode.INITIALIZING ? view.getPreview() : null;
		if (view.getSaved() != null && view.getSaved().getSnapshot() != null && view.getMode() != SetupView.Mode.INITIALIZING)
		{
			String started = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
				.withZone(ZoneId.systemDefault()).format(view.getSaved().getState().getStartedAt());
			setup.add(message("Run started", started));
		}
		if (view.getMode() == SetupView.Mode.INITIALIZING)
		{
			setup.add(new JLabel("Bank: " + (view.getProgress().ready(SnapshotLocation.BANK) ? "Ready" : "Waiting")));
			setup.add(new JLabel("Inventory: " + (view.getProgress().ready(SnapshotLocation.INVENTORY) ? "Ready" : "Waiting")));
			setup.add(new JLabel("Equipment: " + (view.getProgress().ready(SnapshotLocation.EQUIPMENT) ? "Ready" : "Waiting")));
			if (summary == null)
			{
				for (SnapshotLocation location : SnapshotLocation.values())
				{
					ContainerSnapshot partial = view.getProgress().getContainers().get(location);
					if (partial != null) { addSummary("Observed " + location.name().toLowerCase(Locale.ROOT), partial); }
				}
			}
		}
		if (summary != null)
		{
			addSummary("Legacy bank", summary.at(SnapshotLocation.BANK));
			addSummary("Legacy inventory", summary.at(SnapshotLocation.INVENTORY));
			addSummary("Legacy equipment", summary.at(SnapshotLocation.EQUIPMENT));
		}
		if (view.getMode() == SetupView.Mode.NOT_STARTED || view.getMode() == SetupView.Mode.SIGNED_OUT)
		{
			JButton begin = button("Begin From Scratch", actions::begin);
			begin.setEnabled(view.getMode() == SetupView.Mode.NOT_STARTED);
			setup.add(begin);
		}
		if (view.getMode() == SetupView.Mode.INITIALIZING)
		{
			JButton confirm = button("Confirm Snapshot", () ->
			{
				if (JOptionPane.showConfirmDialog(this, "Activate this run using the displayed bank, inventory and equipment?\n"
					+ "All captured items and coins become legacy; virtual ownership and GP start at zero.",
					"Confirm From Scratch snapshot", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION)
				{
					actions.confirm(view.getReviewToken());
				}
			});
			confirm.setEnabled(view.getPreview() != null && view.getReviewToken() != null
				&& view.getProgress().getContainers().size() == SnapshotLocation.values().length);
			setup.add(confirm);
			setup.add(button("Cancel Setup", actions::cancel));
		}
		if (view.getMode() == SetupView.Mode.ACTIVE)
		{
			setup.add(button("Reset From Scratch", () ->
			{
				if (JOptionPane.showConfirmDialog(this, "Back up and reset this From Scratch run?\n"
					+ "You must begin again and confirm a fresh snapshot. Real OSRS items are unchanged.",
					"Confirm From Scratch reset", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION)
				{
					actions.reset(view.getSaved().getState().getRunId());
				}
			}));
		}
		if (view.getMode() == SetupView.Mode.ERROR) { setup.add(button("Retry Load", actions::retry)); }
		revalidate();
		repaint();
	}

	private void addSummary(String title, ContainerSnapshot container)
	{
		NumberFormat numbers = NumberFormat.getIntegerInstance(Locale.US);
		setup.add(message(title, numbers.format(container.getQuantities().size()) + " item types; "
			+ numbers.format(container.getOccupiedSlots()) + " occupied slots; " + numbers.format(container.getTotalQuantity())
			+ " units. Legacy coins: " + numbers.format(container.quantity(ItemID.COINS)) + " GP."));
	}

	private static JButton button(String title, Runnable action)
	{
		JButton button = new JButton(title);
		button.addActionListener(event -> action.run());
		return button;
	}

	/** Preserves the existing config key; optional detail uses a tooltip, never a second instruction section. */
	void setFoundationHelpVisible(boolean visible)
	{
		showSetupDetails = visible;
		refreshSetupTooltip();
	}

	private void refreshSetupTooltip()
	{
		setToolTipText(beforeInitialization && showSetupDetails ? SETUP_DETAILS : null);
	}

	private static JPanel metric(String name, String value, Color color)
	{
		return metric(name, new JLabel(value), color);
	}

	private static JPanel metric(String name, JLabel amount, Color color)
	{
		JPanel card = new JPanel(new GridLayout(0, 1, 0, 4));
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		card.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		card.add(new JLabel(name));
		amount.setForeground(color);
		amount.setFont(amount.getFont().deriveFont(Font.BOLD, 18f));
		card.add(amount);
		return card;
	}

	private static JPanel message(String title, String text)
	{
		JPanel section = new JPanel(new BorderLayout(0, 4));
		section.setBackground(ColorScheme.DARK_GRAY_COLOR);
		section.setBorder(BorderFactory.createEmptyBorder(8, 2, 8, 2));
		section.add(new JLabel(title), BorderLayout.NORTH);
		String escaped = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
		section.add(new JLabel("<html><body style='width: 153px'>" + escaped + "</body></html>"), BorderLayout.CENTER);
		return section;
	}
}
