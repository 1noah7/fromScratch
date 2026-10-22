package com.fromscratch.bank;

import com.fromscratch.FromScratchConfig;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class Milestone9PanelTest
{
	@Test public void virtualBankSettingIsFirstDefaultOnAndAbsentFromSidebar()throws Exception
	{
		SwingUtilities.invokeAndWait(()->
		{
			BankPanel panel=new BankPanel();BankRun run=BankFixtures.run(1);
			panel.render(new BankView(null,null,Collections.emptyMap(),false,false,"Open your bank."),null);assertNull(find(panel,"virtualBankView"));
			panel.render(new BankView(run,null,Collections.emptyMap(),false,false,"Bank progress updates automatically."),null);assertNull(find(panel,"virtualBankView"));
			assertFalse(BankPolishTest.all(panel).stream().anyMatch(c->c instanceof JCheckBox&&((JCheckBox)c).getText().contains("Virtual Bank")));
			JPanel tiles=(JPanel)find(panel,"statisticTiles");assertEquals(2,((GridLayout)tiles.getLayout()).getColumns());assertEquals(6,tiles.getComponentCount());
		});
		assertTrue(new FromScratchConfig(){}.virtualBankView());assertEquals(0,FromScratchConfig.class.getMethod("virtualBankView").getAnnotation(net.runelite.client.config.ConfigItem.class).position());assertFalse(FromScratchConfig.class.getMethod("virtualBankView").getAnnotation(net.runelite.client.config.ConfigItem.class).hidden());assertFalse(new FromScratchConfig(){@Override public boolean virtualBankView(){return false;}}.virtualBankView());
	}
	@Test public void progressToolsAreCompactAndDeveloperControlIsHidden()throws Exception
	{
		assertNull(ProgressDialogs.selection(null));assertNull(ProgressDialogs.selection(Collections.emptyList()));
		SwingUtilities.invokeAndWait(()->
		{
			JPopupMenu ordinary=ProgressDialogs.menu(new JPanel(),()->null,()->null);assertEquals(2,ordinary.getComponentCount());assertEquals("Export Progress",((JMenuItem)ordinary.getComponent(0)).getText());assertEquals("Import Progress",((JMenuItem)ordinary.getComponent(1)).getText());
			BankView error=new BankView(null,null,Collections.emptyMap(),false,true,"Save could not be loaded.");JPopupMenu recovery=ProgressDialogs.menu(new JPanel(),()->null,()->error);assertEquals("Recover Previous Save",((JMenuItem)recovery.getComponent(2)).getText());
		});
		assertTrue(FromScratchConfig.class.getMethod("developmentItemTrace").getAnnotation(net.runelite.client.config.ConfigItem.class).hidden());
	}
	private static Component find(Container parent,String name)
	{
		for(Component child:parent.getComponents()){if(name.equals(child.getName())){return child;}if(child instanceof Container){Component found=find((Container)child,name);if(found!=null){return found;}}}return null;
	}
	@Test public void spendingStartsAtExactZeroAndEstimatedFeesHaveLeadingTilde()throws Exception
	{
		BankRun start=BankFixtures.run(1);UUID session=UUID.randomUUID();com.fromscratch.ge.GeObservation placed=new com.fromscratch.ge.GeObservation(UUID.randomUUID(),session,java.time.Instant.now(),0,BankFixtures.SHIELD,"Shield",net.runelite.api.GrandExchangeOfferState.SELLING,1,0,1000,0,false);
		BankRun selling=BankEngine.exchange(start,placed),sold=BankEngine.exchange(selling,new com.fromscratch.ge.GeObservation(UUID.randomUUID(),session,java.time.Instant.now(),0,BankFixtures.SHIELD,"Shield",net.runelite.api.GrandExchangeOfferState.SOLD,1,1,1000,1000,false));
		SwingUtilities.invokeAndWait(()->{BankPanel p=new BankPanel();p.render(new BankView(start,null,Collections.emptyMap(),false,false,"Bank progress saved."),null);assertTrue(BankPolishTest.all(p).stream().anyMatch(c->c instanceof JLabel&&"0 GP".equals(((JLabel)c).getText())));p.render(new BankView(sold,null,Collections.emptyMap(),false,false,"Bank progress saved."),null);assertTrue(BankPolishTest.all(p).stream().anyMatch(c->c instanceof JLabel&&"~20 GP".equals(((JLabel)c).getText())));assertEquals(6,((JPanel)find(p,"statisticTiles")).getComponentCount());});
	}
}
