package com.fromscratch.bank;

import com.fromscratch.FromScratchConfig;
import com.fromscratch.FromScratchPlugin;
import com.fromscratch.overlay.BankOverlayState;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Synthetic Swing input and pixels only. These tests never launch or control RuneScape. */
public class BankUiPolishPassTest
{
	private static <T extends Component> T named(BankPanel panel, String name, Class<T> type)
	{
		return type.cast(BankPolishTest.all(panel).stream().filter(c -> name.equals(c.getName())).findFirst().get());
	}
	private static JButton button(BankPanel panel, String title)
	{
		return (JButton) BankPolishTest.all(panel).stream().filter(c -> c instanceof JButton && title.equals(((JButton)c).getAccessibleContext().getAccessibleName())).findFirst().get();
	}
	private static BufferedImage icon(JCheckBox box)
	{
		BufferedImage image = new BufferedImage(22, 22, BufferedImage.TYPE_INT_ARGB); Graphics2D g = image.createGraphics(); box.getIcon().paintIcon(box, g, 0, 0); g.dispose(); return image;
	}
	private static void layout(Container parent) { parent.doLayout(); for (Component c : parent.getComponents()) { if (c instanceof Container) { layout((Container)c); } } }
	private static BankView setup()
	{
		return new BankView(null, new BankObservation(UUID.randomUUID(), 5, com.fromscratch.SnapshotFixtures.TIME, BankFixtures.bank(1), "Bank ready."), Collections.emptyMap(), false, false, "Bank ready.");
	}
	@Test public void defaultsUseReadableGreenAndHalfOpacityCharcoal()
	{
		FromScratchConfig config = new FromScratchConfig(){}; assertEquals(new Color(0x1DFF00), config.virtualQuantityColor()); assertEquals(new Color(0x333333), config.lockedItemBackgroundColor()); assertEquals(50, config.lockedItemBackgroundOpacity());
	}
	@Test public void storedCustomColorsRemainAuthoritativeAcrossLiveReconfiguration()
	{
		FromScratchConfig custom = new FromScratchConfig(){ public Color virtualQuantityColor(){return Color.MAGENTA;} public Color lockedItemBackgroundColor(){return Color.BLUE;} public int lockedItemBackgroundOpacity(){return 20;} };
		BankOverlayState state = new BankOverlayState(); state.enable(); state.updateBank(BankPolishTest.view()); BankOverlayState.Projection projection = state.getProjection();
		state.configure(custom); state.configure(custom); assertEquals(Color.MAGENTA, state.getSettings().getColor()); assertEquals(new Color(0,0,255,51), state.getSettings().getLockedTint()); assertSame(projection, state.getProjection());
	}
	@Test public void manualHistoryUsesTheHorrorFromTheDeepDiary() { assertEquals(3846, BankIcons.MANUAL_CORRECTIONS); }
	@Test public void automaticHistoryUsesBankersNote() { assertEquals(28767, BankIcons.AUTOMATIC_CHANGES); }
	@Test public void correctionsToolKeepsThePurplePartyhat() { assertEquals(1046, BankIcons.BANK_CORRECTIONS); }
	@Test public void spritesHaveConsistentCenteredBoundsAndAreOnlyRequestedOnce()
	{
		AtomicInteger requests = new AtomicInteger(); BankIcons icons = new BankIcons(id -> {requests.incrementAndGet(); return null;});
		for (int id : new int[]{BankIcons.AUTOMATIC_CHANGES, BankIcons.MANUAL_CORRECTIONS, BankIcons.BANK_CORRECTIONS}) { for(int n=0;n<3;n++){JLabel label=new JLabel(); icons.apply(label,id); assertEquals(new Dimension(36,32),label.getPreferredSize());assertEquals(SwingConstants.CENTER,label.getHorizontalAlignment());assertNotNull(label.getIcon());} } assertEquals(3,requests.get());
	}
	@Test public void navigationIconIsTransparentPurplePixelArtwork() throws Exception
	{
		BufferedImage image=ImageIO.read(FromScratchPlugin.class.getResource("icon.png")); assertEquals(24,image.getWidth()); assertEquals(24,image.getHeight()); int purple=0,transparent=0;
		for(int y=0;y<24;y++){for(int x=0;x<24;x++){Color pixel=new Color(image.getRGB(x,y),true);if(pixel.getAlpha()==0){transparent++;}else if(pixel.getRed()>pixel.getGreen()&&pixel.getBlue()>pixel.getGreen()){purple++;}}} assertTrue(purple>80); assertTrue(transparent>100);
	}
	@Test public void elsewhereHasExactlyTwoEqualColumnsAndOnlyRightTileIsClickable() throws Exception
	{
		SwingUtilities.invokeAndWait(() -> { BankPanel panel=new BankPanel(); panel.render(BankPolishTest.view(),null); JPanel row=named(panel,"playedElsewhereTiles",JPanel.class); assertEquals(2,((GridLayout)row.getLayout()).getColumns()); assertEquals(2,row.getComponentCount()); row.setSize(208,132); row.doLayout(); assertTrue(row.getComponent(0) instanceof JPanel); assertTrue(row.getComponent(1) instanceof JButton); assertEquals(row.getComponent(0).getSize(),row.getComponent(1).getSize()); });
	}
	@Test public void leftTileContainsExactConciseInstructions() throws Exception
	{
		SwingUtilities.invokeAndWait(() -> { BankPanel panel=new BankPanel(); JPanel instructions=named(panel,"elsewhereInstructions",JPanel.class); JLabel area=(JLabel)instructions.getComponent(0); assertTrue(area.getText().contains("Played on mobile or another PC?"));assertTrue(area.getText().contains("Bank your items and open your bank here."));assertEquals(SwingConstants.CENTER,area.getHorizontalAlignment());assertEquals(SwingConstants.CENTER,area.getVerticalAlignment()); });
	}
	@Test public void correctionsTileNavigatesToExistingCorrectionAction() throws Exception
	{
		SwingUtilities.invokeAndWait(() -> { AtomicInteger opened=new AtomicInteger(); BankPanel panel=new BankPanel(new BankIcons(id->null),manual->{},opened::incrementAndGet); panel.render(BankPolishTest.view(),null); button(panel,"Manual Bank Corrections").doClick(0); assertEquals(1,opened.get()); });
	}
	@Test public void resetButtonIsSolidRedWhiteCenteredAndChangesWithHoverAndPress() throws Exception
	{
		SwingUtilities.invokeAndWait(() -> { BankDangerButton reset=new BankDangerButton("Reset Progress"); reset.setSize(180,40); assertEquals(Color.WHITE,reset.getForeground()); assertEquals(SwingConstants.CENTER,reset.getHorizontalAlignment()); assertEquals(SwingConstants.CENTER,reset.getVerticalAlignment()); assertEquals(BankDangerButton.NORMAL.getRGB(),paint(reset).getRGB(5,5)); reset.getModel().setRollover(true); assertEquals(BankDangerButton.HOVER.getRGB(),paint(reset).getRGB(5,5)); reset.getModel().setArmed(true);reset.getModel().setPressed(true);assertEquals(BankDangerButton.PRESSED.getRGB(),paint(reset).getRGB(5,5));assertFalse(BankDialogs.resetConfirmed("")); });
	}
	private static BufferedImage paint(JComponent c) { BufferedImage image=new BufferedImage(c.getWidth(),c.getHeight(),BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();c.paint(g);g.dispose();return image; }
	@Test public void uncheckedAttestationHasVisibleOutlineAndDarkInterior() { BankAttestation box=new BankAttestation(); assertFalse(box.isSelected()); BufferedImage image=icon(box); assertEquals(new Color(201,190,213).getRGB(),image.getRGB(2,10));assertEquals(new Color(27,24,31).getRGB(),image.getRGB(10,10));assertEquals(22,box.getIcon().getIconWidth()); }
	@Test public void checkedAttestationShowsBrightDistinctCheckmark() { BankAttestation box=new BankAttestation();box.setSelected(true);BufferedImage image=icon(box);assertEquals(Color.WHITE.getRGB(),image.getRGB(10,15));assertEquals(new Color(0xB450FF).getRGB(),image.getRGB(2,10)); }
	@Test public void checkboxHoverAndFocusRemainDistinct() { BankAttestation box=new BankAttestation();box.getModel().setRollover(true);assertEquals(Color.WHITE.getRGB(),icon(box).getRGB(2,10));JCheckBox focused=new JCheckBox(){@Override public boolean hasFocus(){return true;}}; focused.setIcon(box.getIcon());assertEquals(new Color(0xFFD54F).getRGB(),icon(focused).getRGB(0,10)); }
	@Test public void clickingCheckboxLabelTogglesConfirmation() throws Exception
	{
		SwingUtilities.invokeAndWait(() -> { BankAttestation box=new BankAttestation();box.setSize(205,74);long time=System.currentTimeMillis();box.dispatchEvent(new MouseEvent(box,MouseEvent.MOUSE_PRESSED,time,0,95,35,1,false,MouseEvent.BUTTON1));box.dispatchEvent(new MouseEvent(box,MouseEvent.MOUSE_RELEASED,time+1,0,95,35,1,false,MouseEvent.BUTTON1));assertTrue(box.isSelected()); });
	}
	@Test public void checkboxSupportsKeyboardSpaceActivation() throws Exception
	{
		SwingUtilities.invokeAndWait(() -> { BankAttestation box=new BankAttestation();assertTrue(box.isFocusable());for(boolean released:new boolean[]{false,true}){Object key=box.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE,0,released));assertNotNull(key);Action action=box.getActionMap().get(key);assertNotNull(action);action.actionPerformed(new ActionEvent(box,ActionEvent.ACTION_PERFORMED,"test"));}assertTrue(box.isSelected()); });
	}
	@Test public void attestationStillGatesStartAndDoesNotModifySnapshot() throws Exception
	{
		SwingUtilities.invokeAndWait(() -> { BankPanel panel=new BankPanel();BankView view=setup();BankObservation original=view.getObservation();panel.render(view,null);JCheckBox box=(JCheckBox)BankPolishTest.all(panel).stream().filter(c->c instanceof JCheckBox).findFirst().get();JButton start=button(panel,"Start From Scratch");assertFalse(box.isSelected());assertFalse(start.isEnabled());box.doClick(0);assertTrue(start.isEnabled());panel.render(view,null);assertSame(original,view.getObservation());assertNull(view.getRun());assertEquals(1,original.getBank().quantity(BankFixtures.SHIELD)); });
	}
	@Test public void attestationCannotReplaceMissingBankObservation() throws Exception
	{
		SwingUtilities.invokeAndWait(() -> { BankPanel panel=new BankPanel();panel.render(new BankView(null,null,Collections.emptyMap(),false,false,"Open your bank."),null);JCheckBox box=(JCheckBox)BankPolishTest.all(panel).stream().filter(c->c instanceof JCheckBox).findFirst().get();box.doClick(0);assertFalse(button(panel,"Start From Scratch").isEnabled()); });
	}
	@Test public void setupCheckboxAndInstructionsFitMinimumSidebarWidth() throws Exception
	{
		SwingUtilities.invokeAndWait(() -> { BankPanel panel=new BankPanel();panel.render(setup(),null);panel.setSize(225,960);layout(panel);JCheckBox box=(JCheckBox)BankPolishTest.all(panel).stream().filter(c->c instanceof JCheckBox).findFirst().get();assertTrue(box.getWidth()>=box.getPreferredSize().width);assertTrue(box.getHeight()>=box.getPreferredSize().height);try{java.nio.file.Path file=java.nio.file.Paths.get("build/reports/setup-polish.png");java.nio.file.Files.createDirectories(file.getParent());ImageIO.write(paint(panel),"png",file.toFile());}catch(java.io.IOException ex){throw new AssertionError(ex);} });
	}
}
