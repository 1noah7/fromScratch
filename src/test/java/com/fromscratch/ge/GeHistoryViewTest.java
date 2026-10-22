package com.fromscratch.ge;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.bank.BankIcons;
import java.util.*;
import javax.swing.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Page-local controls, readable uncertainty and bounded icon requests without opening a game window. */
public class GeHistoryViewTest
{
	private static GeTrade trade(String name,GeTrade.Evidence evidence){return new GeTrade(UUID.randomUUID(),UUID.randomUUID(),0,net.runelite.api.gameval.ItemID.WOODEN_SHIELD,name,false,3,1000,18L,"Estimated 2% average price",evidence==GeTrade.Evidence.EXISTING_SNAPSHOT?"Sold (Partial)":"Sold",GeOffer.Origin.UNKNOWN,evidence,SnapshotFixtures.TIME);}
	@Test public void searchFiltersOnlyCurrentPageByItemName()throws Exception{SwingUtilities.invokeAndWait(()->{GeHistoryView v=new GeHistoryView(new BankIcons(id->null));v.page(Arrays.asList(trade("Wooden shield",GeTrade.Evidence.FILL),trade("Oak logs",GeTrade.Evidence.FILL)));v.search.setText("WOODEN");assertEquals(1,v.table.getRowCount());v.search.setText("missing");assertEquals(0,v.table.getRowCount());});}
	@Test public void completedAndPartialFiltersAreAvailable()throws Exception{SwingUtilities.invokeAndWait(()->{GeHistoryView v=new GeHistoryView(new BankIcons(id->null));v.page(Arrays.asList(trade("Shield",GeTrade.Evidence.FILL),trade("Logs",GeTrade.Evidence.EXISTING_SNAPSHOT)));v.filter.setSelectedIndex(1);assertEquals(1,v.table.getRowCount());assertEquals("Sold",v.table.getValueAt(0,6));v.filter.setSelectedIndex(2);assertEquals(1,v.table.getRowCount());assertEquals("Sold (Partial)",v.table.getValueAt(0,6));});}
	@Test public void numericQuantitiesSortNumericallyAndRowsAreReadOnly()throws Exception{SwingUtilities.invokeAndWait(()->{GeHistoryView v=new GeHistoryView(new BankIcons(id->null));v.page(Collections.singletonList(trade("Shield",GeTrade.Evidence.FILL)));assertFalse(v.table.isCellEditable(0,0));assertEquals(Long.class,v.table.getColumnClass(2));v.table.getRowSorter().toggleSortOrder(2);assertEquals(1,v.table.getRowCount());});}
	@Test public void detailsSeparateReportedGpEstimatedTaxAndUnknownProfit(){String detail=GeHistoryView.detail(trade("Shield",GeTrade.Evidence.FILL));assertTrue(detail.contains("1,000 GP"));assertTrue(detail.contains("Estimated seller tax"));assertTrue(detail.contains("realized profit are not verified"));assertTrue(detail.contains("Collection destination is unknown"));assertFalse(detail.contains("generation="));}
	@Test public void derivedAverageDoesNotPretendToBeOfferLimit(){assertEquals("~333.33 GP",GeHistoryView.average(trade("Shield",GeTrade.Evidence.FILL)));}
	@Test public void oversizedPageIsRejectedBeforeRendering(){GeHistoryView v=new GeHistoryView(new BankIcons(id->null));try{v.page(Collections.nCopies(26,trade("Shield",GeTrade.Evidence.FILL)));fail();}catch(IllegalArgumentException expected){assertEquals(0,v.table.getRowCount());}}
	@Test public void rawLifecycleRowsStayHiddenAndFinalNumericCellsAreCentered()throws Exception
	{SwingUtilities.invokeAndWait(()->{GeTrade fill=trade("Shield",GeTrade.Evidence.FILL);GeTrade cleared=new GeTrade(UUID.randomUUID(),fill.getGeneration(),0,fill.getItemId(),"Shield",false,0,0,null,"Not observed","Slot cleared; collection destination unknown.",fill.getOrigin(),GeTrade.Evidence.STATUS,fill.getAt());GeTrade pending=new GeTrade(UUID.randomUUID(),fill.getGeneration(),0,fill.getItemId(),"Shield",false,2,200,4L,"Estimated","SELLING",fill.getOrigin(),GeTrade.Evidence.FILL,fill.getAt());GeTrade canceled=new GeTrade(UUID.randomUUID(),fill.getGeneration(),0,fill.getItemId(),"Shield",false,0,0,null,"Not observed","CANCELLED_SELL",fill.getOrigin(),GeTrade.Evidence.STATUS,fill.getAt());GeHistoryView view=new GeHistoryView(new BankIcons(id->null));view.page(Arrays.asList(fill,cleared,pending,canceled));assertEquals(1,view.table.getRowCount());for(int column:new int[]{2,3,4,5}){JLabel renderer=(JLabel)view.table.prepareRenderer(view.table.getCellRenderer(0,column),0,column);assertEquals(SwingConstants.CENTER,renderer.getHorizontalAlignment());}assertTrue(view.table.getColumnModel().getColumn(3).getPreferredWidth()>=150);});}
	@Test public void averagePricesHaveGroupedDigits(){GeTrade row=new GeTrade(UUID.randomUUID(),UUID.randomUUID(),0,1,"Item",true,1,7465,null,"Not observed","BOUGHT",GeOffer.Origin.OBSERVED_NEW,GeTrade.Evidence.FILL,SnapshotFixtures.TIME);assertEquals("7,465 GP",GeHistoryView.average(row));}
}
