package com.fromscratch.bank;

import com.fromscratch.ge.*;
import com.fromscratch.SnapshotFixtures;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import net.runelite.api.GrandExchangeOfferState;
import org.junit.Test;
import static org.junit.Assert.*;

/** The GE row reuses the accepted two-column tile system and distinct read-only navigation. */
public class GeDashboardTest
{
	private static JButton button(BankPanel panel,String name){return (JButton)BankPolishTest.all(panel).stream().filter(c->c instanceof JButton&&name.equals(((JButton)c).getAccessibleContext().getAccessibleName())).findFirst().get();}
	@Test public void purchasesAndSalesHaveExactlyTwoMatchingColumns()throws Exception
	{SwingUtilities.invokeAndWait(()->{BankPanel panel=new BankPanel();JPanel row=(JPanel)BankPolishTest.all(panel).stream().filter(c->"grandExchangeTiles".equals(c.getName())).findFirst().get();assertEquals(2,((GridLayout)row.getLayout()).getColumns());assertEquals(2,row.getComponentCount());row.setSize(208,102);row.doLayout();assertEquals(row.getComponent(0).getSize(),row.getComponent(1).getSize());assertNotNull(button(panel,"Purchases"));assertNotNull(button(panel,"Sales"));});}
	@Test public void geNavigationDoesNotOpenBankCorrectionsOrBankHistory()throws Exception
	{SwingUtilities.invokeAndWait(()->{java.util.List<Boolean> sides=new ArrayList<>();BankPanel panel=new BankPanel(new BankIcons(id->null),manual->{throw new AssertionError("bank history");},()->{throw new AssertionError("corrections");},sides::add);button(panel,"Purchases").doClick(0);button(panel,"Sales").doClick(0);assertEquals(Arrays.asList(true,false),sides);});}
	@Test public void geSectionIsBetweenBankHistoryAndPlayedElsewhere()throws Exception
	{SwingUtilities.invokeAndWait(()->{BankPanel panel=new BankPanel();java.util.List<Component> all=BankPolishTest.all(panel);int history=all.indexOf(button(panel,"Manual Corrections")),ge=all.indexOf(button(panel,"Purchases"));int elsewhere=all.indexOf(button(panel,"Manual Bank Corrections"));assertTrue(history<ge&&ge<elsewhere);});}
	@Test public void estimatedFeesAppearOnceWithoutChangingBankGp()throws Exception
	{
		SwingUtilities.invokeAndWait(()->
		{
			BankRun run=BankFixtures.run(1);UUID session=UUID.randomUUID();run=BankEngine.exchange(run,new GeObservation(UUID.randomUUID(),session,SnapshotFixtures.TIME.plusSeconds(1),0,BankFixtures.SHIELD,"Wooden shield",GrandExchangeOfferState.SELLING,10,0,100,0,false));run=BankEngine.exchange(run,new GeObservation(UUID.randomUUID(),session,SnapshotFixtures.TIME.plusSeconds(2),0,BankFixtures.SHIELD,"Wooden shield",GrandExchangeOfferState.SOLD,10,10,100,1000,false));
			BankPanel panel=new BankPanel();panel.render(new BankView(run,null,Collections.emptyMap(),false,false,"Bank progress saved."),null);assertEquals(0,run.balance(net.runelite.api.gameval.ItemID.COINS));
			assertTrue(BankPolishTest.all(panel).stream().anyMatch(c->c instanceof JLabel&&"20 GP~".equals(((JLabel)c).getText())));assertTrue(BankTooltips.expenses(run.getFinancial(),run.getExchange()).contains("Estimated GE seller fees"));
		});
	}
}
