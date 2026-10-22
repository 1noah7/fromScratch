package com.fromscratch.bank;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.buyback.*;
import com.fromscratch.pouch.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

/** Offscreen Swing validation only; user acceptance still verifies RuneLite scaling/appearance. */
public class PostM7UiTest
{
	private static JPanel named(Container root,String name){return (JPanel)BankPolishTest.all(root).stream().filter(c->name.equals(c.getName())).findFirst().get();}
	private static JButton button(Container root,String text){return (JButton)BankPolishTest.all(root).stream().filter(c->c instanceof JButton&&(text.equals(((JButton)c).getText())||text.equals(((JButton)c).getAccessibleContext().getAccessibleName()))).findFirst().get();}
	private static JCheckBox check(Container root,String text){return (JCheckBox)BankPolishTest.all(root).stream().filter(c->c instanceof JCheckBox&&((JCheckBox)c).getText().contains(text)).findFirst().get();}
	private static BankView pouchView(){BankRun run=BankEngine.start(SnapshotFixtures.CHARACTER_A,null,BuybackModelTest.bank(ItemID.BH_RUNE_POUCH),true,UUID.randomUUID(),SnapshotFixtures.TIME);return new BankView(run,new BankObservation(UUID.randomUUID(),1,SnapshotFixtures.TIME,run.getInitialBank(),""),Collections.emptyMap(),false,false,"");}
	private static void layout(Container root){root.doLayout();for(Component c:root.getComponents()){if(c instanceof Container){layout((Container)c);}}}
	@Test public void sixSectionsAppearInExactOrderWithResetLast()throws Exception{SwingUtilities.invokeAndWait(()->{BankPanel panel=new BankPanel();panel.render(BankPolishTest.view(),null);JPanel stats=named(panel,"statisticTiles");Container active=stats.getParent();List<String> sections=new ArrayList<>();sections.add("From Scratch");for(Component c:active.getComponents()){if(c instanceof JLabel){sections.add(((JLabel)c).getText());}else if("playedElsewhere".equals(c.getName())){sections.add("Played Elsewhere?");}else if("resetDangerArea".equals(c.getName())){sections.add("Reset Progress");}}assertEquals(Arrays.asList("From Scratch","Legacy Buybacks","Played Elsewhere?","Grand Exchange History","Bank History","Reset Progress"),sections);assertEquals("resetDangerArea",active.getComponent(active.getComponentCount()-1).getName());});}
	@Test public void statisticAndActionHeightsAreCompactButContentsStillFit()throws Exception
	{
		SwingUtilities.invokeAndWait(()->
		{
			BankPanel panel=new BankPanel();panel.render(BankPolishTest.view(),null);panel.setSize(225,1300);layout(panel);
			JPanel stats=named(panel,"statisticTiles");assertEquals(306,stats.getPreferredSize().height);
			for(Component tile:stats.getComponents())
			{
				assertEquals(98,tile.getHeight());
				for(Component c:BankPolishTest.all((Container)tile))
				{
					if(c instanceof JLabel&&c.isVisible()){assertTrue("Statistic label must fit: "+((JLabel)c).getText(),c.getPreferredSize().height<=c.getHeight());}
				}
			}
			for(String name:new String[]{"legacyBuybackTiles","grandExchangeTiles"})
			{
				JPanel row=named(panel,name);assertEquals(90,row.getPreferredSize().height);
				assertEquals(row.getComponent(0).getBounds().height,row.getComponent(1).getBounds().height);
				assertEquals(row.getComponent(0).getWidth(),row.getComponent(1).getWidth());
			}
			assertPlayedElsewhereFits(panel);
		});
	}
	private static void assertPlayedElsewhereFits(BankPanel panel)
	{
		JPanel row=named(panel,"playedElsewhereTiles");
		// Logical Dialog fonts have different HTML wrapping/metrics on Windows and Linux.
		// Keep the exact 116px compact minimum, allowing only the height the content needs.
		int contentHeight=row.getLayout().preferredLayoutSize(row).height;
		assertEquals("Compact minimum or full natural content height",Math.max(116,contentHeight),row.getPreferredSize().height);
		assertEquals("Compact tile padding stays unchanged",new Insets(5,4,5,4),((JPanel)row.getComponent(0)).getInsets());
		assertEquals(6,((GridLayout)row.getLayout()).getHgap());
		assertEquals(6,((GridLayout)row.getLayout()).getVgap());
		assertEquals(row.getPreferredSize().height,row.getHeight());
		assertEquals(row.getComponent(0).getSize(),row.getComponent(1).getSize());
		JLabel help=(JLabel)BankPolishTest.all(row).stream().filter(c->"elsewhereHelp".equals(c.getName())).findFirst().get();
		assertTrue("Full help text must fit its host font: "+help.getFont(),help.getPreferredSize().height<=help.getHeight());
	}
	@Test public void playedElsewhereFitsDifferentHostFontMetrics()throws Exception
	{
		SwingUtilities.invokeAndWait(()->
		{
			for(Font font:new Font[]{new Font(Font.DIALOG,Font.PLAIN,11),new Font(Font.MONOSPACED,Font.PLAIN,11),new Font(Font.DIALOG,Font.PLAIN,18)})
			{
				BankPanel panel=new BankPanel();panel.render(BankPolishTest.view(),null);
				JLabel help=(JLabel)BankPolishTest.all(panel).stream().filter(c->"elsewhereHelp".equals(c.getName())).findFirst().get();
				help.setFont(font);panel.setSize(225,1800);layout(panel);assertPlayedElsewhereFits(panel);
				if(font.getSize()==18){assertTrue("Large text must exercise expansion beyond the old fixed cap",named(panel,"playedElsewhereTiles").getPreferredSize().height>=132);}
			}
			BankPanel shortCopy=new BankPanel();shortCopy.render(BankPolishTest.view(),null);
			JLabel help=(JLabel)BankPolishTest.all(shortCopy).stream().filter(c->"elsewhereHelp".equals(c.getName())).findFirst().get();
			for(Component c:BankPolishTest.all(named(shortCopy,"playedElsewhereTiles")))
			{if(c instanceof JLabel){c.setFont(new Font(Font.DIALOG,Font.PLAIN,8));}}
			help.setText("Bank your items.");shortCopy.setSize(225,1800);layout(shortCopy);assertPlayedElsewhereFits(shortCopy);
			assertEquals("Short content keeps the compact minimum",116,named(shortCopy,"playedElsewhereTiles").getPreferredSize().height);
		});
	}
	@Test public void largerFontsAndLongCaptionsCanExpandTileHeight()throws Exception{SwingUtilities.invokeAndWait(()->{BankTile tile=new BankTile("Value","Value",ItemID.COINS,new BankIcons(id->null));tile.value("123 GP","Exact value","Priced subtotal");for(Component c:BankPolishTest.all(tile)){if(c instanceof JLabel){c.setFont(new Font(Font.DIALOG,Font.PLAIN,26));}}assertTrue(tile.getPreferredSize().height>98);tile.setSize(230,tile.getPreferredSize().height);layout(tile);for(Component c:BankPolishTest.all(tile)){if(c instanceof JLabel&&c.isVisible()){assertTrue(c.getPreferredSize().height<=c.getHeight());}}});}
	@Test public void tileActionsAndSpriteIdentitiesRemainSeparateAfterReorder()throws Exception{SwingUtilities.invokeAndWait(()->{List<Boolean> history=new ArrayList<>(),ge=new ArrayList<>();int[] corrections={0};BankPanel panel=new BankPanel(new BankIcons(id->null),history::add,()->corrections[0]++,ge::add);panel.render(BankPolishTest.view(),null);button(panel,"Automatic Changes").doClick(0);button(panel,"Manual Corrections").doClick(0);button(panel,"Purchases").doClick(0);button(panel,"Sales").doClick(0);button(panel,"Manual Bank Corrections").doClick(0);assertEquals(Arrays.asList(false,true),history);assertEquals(Arrays.asList(true,false),ge);assertEquals(1,corrections[0]);assertTrue(BankPolishTest.all(button(panel,"Manual Corrections")).stream().anyMatch(c->c instanceof JLabel&&Integer.valueOf(ItemID.HORROR_DIARY2).equals(((JLabel)c).getClientProperty("fromscratch.itemId"))));assertTrue(BankPolishTest.all(button(panel,"Manual Bank Corrections")).stream().anyMatch(c->c instanceof JLabel&&Integer.valueOf(ItemID.PURPLE_PARTYHAT).equals(((JLabel)c).getClientProperty("fromscratch.itemId"))));});}
}
