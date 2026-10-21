package com.fromscratch.ui;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.NavigationButton;
import org.junit.Test;

import static org.junit.Assert.*;

/** Exercises real Swing components and the navigation boundary, without starting RuneScape. */
public class FromScratchSidebarTest
{
	@Test
	public void repeatedShowRegistersOnlyOneButton() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			List<NavigationButton> added = new ArrayList<>();
			FromScratchSidebar sidebar = new FromScratchSidebar(added::add, button -> fail("Unexpected removal"));
			sidebar.show(true);
			sidebar.show(false);
			assertEquals(1, added.size());
			assertEquals("From Scratch", added.get(0).getTooltip());
			assertNotNull(added.get(0).getPanel());
			assertEquals(24, added.get(0).getIcon().getWidth());
		});
	}

	@Test
	public void disableRemovesTheRegisteredButtonExactlyOnce() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			List<NavigationButton> added = new ArrayList<>();
			List<NavigationButton> removed = new ArrayList<>();
			FromScratchSidebar sidebar = new FromScratchSidebar(added::add, removed::add);
			sidebar.hide();
			sidebar.show(true);
			sidebar.hide();
			sidebar.hide();
			sidebar.refresh(false);
			assertEquals(1, added.size());
			assertEquals(added, removed);
		});
	}

	@Test
	public void reenableCreatesOneFreshRegistration() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			List<NavigationButton> added = new ArrayList<>();
			List<NavigationButton> removed = new ArrayList<>();
			FromScratchSidebar sidebar = new FromScratchSidebar(added::add, removed::add);
			sidebar.show(true);
			sidebar.hide();
			sidebar.show(true);
			assertEquals(2, added.size());
			assertEquals(1, removed.size());
			assertNotSame(added.get(0), added.get(1));
		});
	}

	@Test
	public void helpSettingNeverHidesThePlaceholderWarning() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			FromScratchPanel panel = new FromScratchPanel(true);
			assertTrue(visibleText(panel).contains("Milestone 1"));
			panel.setFoundationHelpVisible(false);
			List<String> text = visibleText(panel);
			assertFalse(text.contains("Milestone 1"));
			assertTrue(text.contains("Foundation preview"));
			assertTrue(text.stream().anyMatch(s -> s.contains("Values below are placeholders")));
			panel.setFoundationHelpVisible(true);
			assertTrue(visibleText(panel).contains("Milestone 1"));
		});
	}

	@Test
	public void dashboardDisplaysAllSixZeroMetrics() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			List<String> text = visibleText(new FromScratchPanel(true));
			for (String label : new String[]{"Total Value Gained", "Current Net Worth", "Spendable GP",
				"Active run duration", "Items Unlocked", "Total Value Lost / Spent", "Recent acquisitions"})
			{
				assertTrue(label, text.contains(label));
			}
			assertEquals(4, text.stream().filter("0 GP"::equals).count());
			assertTrue(text.contains("0"));
			assertTrue(text.contains("0h 00m"));
		});
	}

	@Test(expected = IllegalStateException.class)
	public void sidebarRejectsUiWorkOutsideTheEdt()
	{
		new FromScratchSidebar(button -> fail(), button -> fail()).show(true);
	}

	private static List<String> visibleText(Container container)
	{
		List<String> result = new ArrayList<>();
		for (Component component : container.getComponents())
		{
			if (!component.isVisible())
			{
				continue;
			}
			if (component instanceof JLabel)
			{
				result.add(((JLabel) component).getText());
			}
			if (component instanceof Container)
			{
				result.addAll(visibleText((Container) component));
			}
		}
		return result;
	}
}
