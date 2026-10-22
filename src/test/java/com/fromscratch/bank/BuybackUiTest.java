package com.fromscratch.bank;

import com.fromscratch.buyback.*;
import com.fromscratch.SnapshotFixtures;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

/** Headless presentation tests; actual DPI, sprites, confirmation/Cancel require user acceptance. */
public class BuybackUiTest
{
	private BankRun run(){return BuybackModelTest.funded(2000000,ItemID.BH_RUNE_POUCH,ItemID.TZHAAR_CAPE_FIRE);}
	private BankView view(BankRun r){return new BankView(r,new BankObservation(UUID.randomUUID(),1,SnapshotFixtures.TIME,r.getInitialBank(),"Ready"),Collections.emptyMap(),false,false,"Bank progress saved.");}
	@Test public void shopSearchFindsOneSharedPouchEntry()throws Exception{SwingUtilities.invokeAndWait(()->{BuybackView panel=new BuybackView(view(run()),new BankIcons(id->null),e->{});panel.filter("Rune pouch","All categories");assertEquals(1,panel.rows());panel.filter("Divine rune pouch","All categories");assertEquals(1,panel.rows());panel.filter("Rune pouch","Void Knight");assertEquals(0,panel.rows());});}
	@Test public void catalogCategoriesHideUnrelatedEntries()throws Exception{SwingUtilities.invokeAndWait(()->{BuybackView panel=new BuybackView(view(run()),new BankIcons(id->null),e->{});panel.filter("","Skilling Outfits");assertEquals(6,panel.rows());panel.filter("","Void Knight");assertEquals(8,panel.rows());});}
	@Test public void legacySectionHasTwoMatchingTilesInRequiredLocation()throws Exception{SwingUtilities.invokeAndWait(()->{BankPanel panel=new BankPanel();java.util.List<Component> all=BankPolishTest.all(panel);JPanel row=(JPanel)all.stream().filter(c->"legacyBuybackTiles".equals(c.getName())).findFirst().get();assertEquals(2,((GridLayout)row.getLayout()).getColumns());assertEquals(2,row.getComponentCount());int ge=index(all,"Purchases"),shop=index(all,"Buyback Shop"),elsewhere=index(all,"Manual Bank Corrections");assertTrue(shop<elsewhere&&elsewhere<ge);row.setSize(208,102);row.doLayout();assertEquals(row.getComponent(0).getSize(),row.getComponent(1).getSize());});}
	private static int index(java.util.List<Component> all,String title){return all.indexOf(all.stream().filter(c->c instanceof JButton&&title.equals(((JButton)c).getAccessibleContext().getAccessibleName())).findFirst().get());}
	@Test public void dashboardShowsAvailableGpAndConfirmedSpendingOnce()throws Exception{SwingUtilities.invokeAndWait(()->{BankRun before=run(),after=BuybackModelTest.buy(before,"fire_cape");BankPanel panel=new BankPanel();panel.render(view(after),null);assertTrue(BankTooltips.bankGp(after).contains("1,000,000 GP"));assertTrue(BankTooltips.bankGp(after).contains("2,000,000 GP"));assertTrue(BankTooltips.expenses(after).contains("Legacy buybacks"));assertEquals(before.getFinancial().getGainedGp(),after.getFinancial().getGainedGp());assertTrue(BankPolishTest.all(panel).stream().anyMatch(c->c instanceof JLabel&&"1000K GP".equals(((JLabel)c).getText())));});}
	@Test public void purchaseHistoryShowsOnePouchPaymentAndReadableDate()throws Exception{SwingUtilities.invokeAndWait(()->{JComponent history=BuybackDialog.history(BuybackModelTest.buy(run(),"rune_pouch_family"),new BankIcons(id->null));JTable table=(JTable)BankPolishTest.all(history).stream().filter(c->c instanceof JTable).findFirst().get();assertEquals(1,table.getRowCount());assertEquals("Rune Pouch / Divine Rune Pouch",table.getValueAt(0,1));assertEquals("1000K GP",table.getValueAt(0,2));assertEquals("1",table.getValueAt(0,3));assertFalse(table.getValueAt(0,4).toString().contains("T"));assertFalse(table.isCellEditable(0,1));});}
	@Test public void confirmationHasSpriteAndLatestPriceBudgetText()throws Exception{SwingUtilities.invokeAndWait(()->{java.util.List<Integer> ids=new ArrayList<>();BankIcons icons=new BankIcons(id->{ids.add(id);return null;});new BuybackView(view(run()),icons,e->{});assertTrue(ids.contains(ItemID.BH_RUNE_POUCH));String text=BuybackView.confirmation(run(),BuybackCatalog.entry("rune_pouch_family"));assertTrue(text.contains("1,000,000 GP"));assertTrue(text.contains("Pay once"));assertTrue(text.contains("Remaining"));});}
}
