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
	private final JPanel tools = new JPanel(new DynamicGridLayout(0, 1, 0, 4));
	private boolean historyExpanded;
	private final JLabel durationValue = new JLabel("0h 00m");
	private final JLabel unlockedValue = new JLabel("0");
	private final JLabel recentValue = new JLabel("No acquisitions recorded.");

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
		add(metric("Items Unlocked", unlockedValue, PURPLE));
		add(new JLabel("Recent acquisitions")); add(recentValue);
		add(new JLabel("GP and value metrics are not tracked yet."));
		tools.setBackground(ColorScheme.DARK_GRAY_COLOR); add(tools);

		setFoundationHelpVisible(showHelp);
		render(SetupView.signedOut(), SetupActions.NONE);
	}

	/** Replaces presentation only; button callbacks return local requests to the controller. */
	void render(SetupView view, SetupActions actions)
	{
		setup.removeAll();
		tools.removeAll();
		durationValue.setText(RunDuration.format(view.getElapsedMinutes()));
		durationValue.setToolTipText("Elapsed time since run start, including time away.");
		unlockedValue.setText(view.getSaved() == null ? "0" : Long.toString(view.getSaved().getEconomy().getItemsUnlocked()));
		recentValue.setText(recentAcquisitions(view));
		String status;
		switch (view.getMode())
		{
			case ACTIVE: status = "Active"; break;
			case RECONCILING: status = "Active — item changes need review"; break;
			case INITIALIZING: status = "Initializing"; break;
			case SAVING: status = view.getSaved() != null && view.getSaved().getSnapshot() != null
				? "Active (resetting)" : "Initializing (saving)"; break;
			case LOADING: status = "Loading"; break;
			case ERROR: status = "Storage error"; break;
			default: status = "Not Started";
		}
		setup.add(new JLabel("Status: " + status));
		if (view.getMode() != SetupView.Mode.ACTIVE)
		{
			setup.add(message("Run status", view.getMode() == SetupView.Mode.INITIALIZING
				? PreparationChecklist.instruction(view.getProgress()) : view.getMode() == SetupView.Mode.RECONCILING
				? "Review item changes in Account Tools below." : view.getMessage()));
		}
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
			setup.add(new JLabel("Prepare Your Account"));
			for (String step : PreparationChecklist.steps(view.getProgress())) { setup.add(new JLabel(step)); }
			setup.add(new JLabel(PreparationChecklist.step(false, "Review and confirm your snapshot.")));
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
		if (view.getMode() == SetupView.Mode.ACTIVE || view.getMode() == SetupView.Mode.RECONCILING)
		{
			tools.add(new JLabel("Account Tools"));
			tools.add(message("Update Progress", "Played on mobile or another computer? Update your From Scratch progress when you return to this PC."));
			tools.add(button("Update Progress", () ->
			{
				if (view.getItemReview() != null) { ItemReviewDialog.show(this, view, actions); }
				else { actions.reviewItems(); }
			}));
			if (view.getMode() == SetupView.Mode.ACTIVE && view.getProgress().getContainers().size() < SnapshotLocation.values().length)
			{ tools.add(message("Item observation", PreparationChecklist.instruction(view.getProgress()))); }
			if (view.getMode() == SetupView.Mode.RECONCILING)
			{
				for (String step : PreparationChecklist.steps(view.getProgress())) { tools.add(new JLabel(step)); }
				tools.add(message("Next step", view.getItemReview() == null ? PreparationChecklist.instruction(view.getProgress()) : "Review the changed items, then apply your decisions."));
				JButton review = button("Review changes", () -> ItemReviewDialog.show(this, view, actions));
				review.setEnabled(view.getItemReview() != null); tools.add(review);
			}
			addLegacyDeficits(view);
			tools.add(button("Reset From Scratch", () ->
			{
				if (JOptionPane.showConfirmDialog(this, "Back up and reset this From Scratch run?\n"
					+ "You must begin again and confirm a fresh snapshot. Real OSRS items are unchanged.",
					"Confirm From Scratch reset", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION)
				{
					actions.reset(view.getSaved().getState().getRunId());
				}
			}));
			tools.add(button((historyExpanded ? "▼ " : "▶ ") + "Transaction History", () -> { historyExpanded = !historyExpanded; render(view, actions); }));
			if (historyExpanded) { tools.add(button("View recent transactions", () -> showHistory(view))); }
		}
		if (view.getMode() == SetupView.Mode.ERROR) { setup.add(button("Retry Load", actions::retry)); }
		if (view.isWriting())
		{
			for (java.awt.Component component : setup.getComponents()) { if (component instanceof JButton) { component.setEnabled(false); } }
			for (java.awt.Component component : tools.getComponents()) { if (component instanceof JButton) { component.setEnabled(false); } }
		}
		revalidate();
		repaint();
	}

	private static String recentAcquisitions(SetupView view)
	{
		if (view.getSaved() == null || view.getSaved().getItemProgress() == null || view.getSaved().getItemProgress().getAcquisitions().isEmpty()) { return "No acquisitions recorded."; }
		StringBuilder text = new StringBuilder("<html>"); int count = 0;
		for (com.fromscratch.tracking.ItemTransaction entry : view.getSaved().getItemProgress().getAcquisitions())
		{
			if (count++ == 5) { break; }
			String name = view.getItemNames().getOrDefault(entry.getItemId(), "Item #" + entry.getItemId());
			text.append(name.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"))
				.append(" × ").append(entry.getVirtualChange()).append("<br>");
		}
		return text.append("</html>").toString();
	}
	private void showHistory(SetupView view)
	{
		StringBuilder text = new StringBuilder("Most recent 50 item changes. Full history is saved locally on this PC.\n\n");
		for (com.fromscratch.tracking.ItemTransaction entry : view.getSaved().getItemProgress().getRecent())
		{
			text.append(entry.getTime()).append(" | ").append(entry.getCategory()).append(" | ")
				.append(entry.getItemId() < 0 ? "Progress check" : view.getItemNames().getOrDefault(entry.getItemId(), "Item #" + entry.getItemId()))
				.append(" | quantity ").append(entry.getQuantity()).append(" | virtual change ").append(entry.getVirtualChange())
				.append(" | ").append(entry.getConfidence()).append("\n").append(entry.getReason()).append("\n\n");
		}
		javax.swing.JTextArea area = new javax.swing.JTextArea(text.toString()); area.setEditable(false);
		javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(area); scroll.setPreferredSize(new java.awt.Dimension(800, 400));
		JOptionPane.showMessageDialog(this, scroll, "From Scratch transaction history", JOptionPane.PLAIN_MESSAGE);
	}
	private void addLegacyDeficits(SetupView view)
	{
		if (view.getSaved() == null || view.getSaved().getItemProgress() == null) { return; }
		view.getSaved().getItemProgress().getLegacyDeficits().getItems().forEach((id, quantity) ->
		{
			String name = view.getItemNames().getOrDefault(id, "Unknown item #" + id);
			boolean absent = view.getSaved().getItemProgress().getPhysical().at(id, SnapshotLocation.BANK) == 0;
			tools.add(message("Legacy withdrawal", name + ": -" + quantity + (absent ? " (not currently in bank)" : " outstanding")
				+ ". Return legacy copies or resolve this in Update Progress."));
		});
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
