package com.fromscratch.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

/** Foundation preview. Values are placeholders and never read from the game. */
public final class FromScratchPanel extends PluginPanel
{
	static final Color PURPLE = new Color(0xB450FF);
	static final Color GOLD = new Color(0xFFD54F);

	private final JPanel help;

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
		add(message("Foundation preview", "No active run. Values below are placeholders."));

		add(metric("Total Value Gained", "0 GP", GOLD));
		add(metric("Current Net Worth", "0 GP", GOLD));
		add(metric("Spendable GP", "0 GP", GOLD));
		add(metric("Active run duration", "0h 00m", Color.WHITE));
		add(metric("Items Unlocked", "0", PURPLE));
		add(metric("Total Value Lost / Spent", "0 GP", GOLD));
		add(message("Recent acquisitions", "No acquisitions recorded."));

		help = message("Milestone 1", "Starting runs, bank snapshots and accounting arrive in later milestones.");
		add(help);
		setFoundationHelpVisible(showHelp);
	}

	/** Called on the EDT; hiding help never hides the placeholder disclosure. */
	void setFoundationHelpVisible(boolean visible)
	{
		help.setVisible(visible);
		revalidate();
		repaint();
	}

	private static JPanel metric(String name, String value, Color color)
	{
		JPanel card = new JPanel(new GridLayout(0, 1, 0, 4));
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		card.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		card.add(new JLabel(name));
		JLabel amount = new JLabel(value);
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
		// Fixed text only; this HTML does not contain character names or user input.
		section.add(new JLabel("<html><body style='width: 153px'>" + text + "</body></html>"), BorderLayout.CENTER);
		return section;
	}
}
