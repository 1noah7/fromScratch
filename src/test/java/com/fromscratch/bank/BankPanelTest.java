package com.fromscratch.bank;

import com.fromscratch.SnapshotFixtures;
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import net.runelite.client.ui.NavigationButton;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.fromscratch.bank.BankFixtures.*;

/** Real Swing component structure and search model; never interacts with the game. */
public class BankPanelTest
{
	private static BankView active() { return new BankView(run(1), new BankObservation(UUID.randomUUID(), 5, SnapshotFixtures.TIME, bank(1), "Bank ready."), Collections.singletonMap(SHIELD, "Wooden shield"), false, false, "Bank progress updates automatically."); }
	private static final BankActions NONE = new BankActions() { public void startRun(boolean attested) { } public void anchor(UUID id, com.fromscratch.model.ContainerSnapshot b, boolean approve) { } public void correct(UUID revision, int id, long desired) { } public void reset(UUID revision) { } public void retry() { } };
	private static List<String> text(Container c)
	{
		List<String> result = new ArrayList<>(); for (Component child : c.getComponents()) { if (!child.isVisible()) { continue; } if (child instanceof JLabel) { result.add(((JLabel) child).getText()); } if (child instanceof AbstractButton) { result.add(((AbstractButton) child).getText()); } if (child instanceof JTextArea) { result.add(((JTextArea) child).getText()); } if (child instanceof Container) { result.addAll(text((Container) child)); } } return result;
	}
	private static java.util.List<Component> all(Container root) { java.util.List<Component> result = new ArrayList<>(); for (Component c : root.getComponents()) { result.add(c); if(c instanceof Container) { result.addAll(all((Container)c)); } } return result; }
	private static JButton button(BankPanel panel, String text) { for (Component c : all(panel)) { if (c instanceof JButton && (text.equals(((JButton) c).getText()) || text.equals(((JButton) c).getAccessibleContext().getAccessibleName()))) { return (JButton) c; } } throw new AssertionError(text); }
	@Test public void setupRequiresAttestationButNoPersonalObservationLabels() throws Exception { SwingUtilities.invokeAndWait(() -> { BankPanel p = new BankPanel(); BankView a = active(); p.render(new BankView(null, a.getObservation(), a.getNames(), false, false, "Bank ready."), NONE); assertFalse(button(p, "Start From Scratch").isEnabled()); for (Component c : all(p)) { if (c instanceof JCheckBox) { ((JCheckBox) c).doClick(); } } assertTrue(button(p, "Start From Scratch").isEnabled()); assertFalse(String.join(" ", text(p)).contains("Equipment: Waiting")); }); }
	@Test public void activeDashboardHidesLegacyAndUnsupportedWalletClaims() throws Exception { SwingUtilities.invokeAndWait(() -> { BankPanel p = new BankPanel(); p.render(active(), NONE); String t = String.join(" ", text(p)); assertTrue(t.contains("<html><center>Items Unlocked</center></html>")); assertFalse(t.contains("Recent Bank Changes")); assertTrue(t.contains("<html><center>Peak Progress<br>Value</center></html>")); assertFalse(t.contains("Total Value<br>Gained")); assertTrue(t.contains("0 GP"));assertFalse(t.contains("Spendable GP")); assertFalse(t.contains("Original")); assertFalse(t.contains("Before Starting")); assertFalse(t.contains("Reconcile")); }); }
	@Test public void resetIsLastAndHistoryHasPermanentTiles() throws Exception { SwingUtilities.invokeAndWait(() -> { BankPanel p = new BankPanel(); p.render(active(), NONE); List<String> t = text(p); assertTrue(t.contains("Bank History")); assertTrue(t.contains("Reset Progress")); assertEquals("Reset Progress", t.get(t.size() - 1)); assertFalse(t.contains("On return:")); }); }
	@Test public void correctionsAvailableWithNoAutomaticDifferences() throws Exception { SwingUtilities.invokeAndWait(() -> { BankPanel p = new BankPanel(); p.render(active(), NONE); assertTrue(button(p, "Manual Bank Corrections").isEnabled()); assertEquals(Collections.singletonList(SHIELD), BankCorrections.search(active(), "wooden")); }); }
	@Test public void searchMatchesNamesIdsAndSortsDeterministically() { BankView a = active(); Map<Integer, String> names = new TreeMap<>(); names.put(SHIELD, "Wooden shield"); names.put(OTHER, "Coins"); BankView v = new BankView(a.getRun(), a.getObservation(), names, false, false, ""); assertEquals(Arrays.asList(OTHER, SHIELD), BankCorrections.search(v, "")); assertEquals(Collections.singletonList(SHIELD), BankCorrections.search(v, Integer.toString(SHIELD))); assertTrue(BankCorrections.search(v, "absent").isEmpty()); }
	@Test public void newSidebarRegistrationIsIdempotent() throws Exception { SwingUtilities.invokeAndWait(() -> { List<NavigationButton> add = new ArrayList<>(), remove = new ArrayList<>(); BankSidebar sidebar = new BankSidebar(add::add, remove::add); sidebar.bind(NONE); sidebar.show(); sidebar.show(); assertEquals(1, add.size()); assertEquals(24, add.get(0).getIcon().getWidth()); sidebar.hide(); sidebar.hide(); assertEquals(1, remove.size()); }); }
	@Test public void playedElsewhereInstructionsRequireOnlyBank() throws Exception { SwingUtilities.invokeAndWait(() -> { BankPanel p = new BankPanel(); p.render(active(), NONE); String t = String.join(" ", text(p)); assertTrue(t.contains("Played on mobile or another PC?"));assertTrue(t.contains("Bank your items and open your bank here.")); assertFalse(t.contains("Update Progress")); }); }
}
