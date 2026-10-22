package com.fromscratch.ge;

import com.fromscratch.bank.*;
import com.fromscratch.wealth.GpFormatter;
import java.awt.*;
import java.math.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;

/** Bounded, read-only trade page. Filtering/sorting never rescans or rewrites the journal. */
public final class GeHistoryView extends JPanel
{
	private final BankIcons icons;
	private final List<GeTrade> rows=new ArrayList<>();
	final JTextField search=new JTextField(16);
	final JComboBox<String> filter=new JComboBox<>(new String[]{"All completed","Full","Partial"});
	private final DefaultTableModel model=new DefaultTableModel(new String[]{"Completed / observed","Item","Quantity","Reported GP","Average GP","Tax","Status"},0)
	{
		@Override public boolean isCellEditable(int r,int c){return false;}
		@Override public Class<?> getColumnClass(int c){return c==1?JLabel.class:c==2||c==3?Long.class:String.class;}
	};
	final JTable table=new JTable(model);
	private final JTextArea details=BankDialogs.text("Select a trade for details. Collection destination and profit may be unknown.",55);
	private final TableRowSorter<DefaultTableModel> sorter=new TableRowSorter<>(model);
	public GeHistoryView(BankIcons icons)
	{
		this.icons=icons;setLayout(new BorderLayout(6,8));JPanel controls=new JPanel(new BorderLayout(8,0));controls.add(search,BorderLayout.CENTER);controls.add(filter,BorderLayout.EAST);add(controls,BorderLayout.NORTH);search.setToolTipText("Search item names on this page");
		table.setRowSorter(sorter);table.setRowHeight(36);table.setFillsViewportHeight(true);table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		int[] widths={165,185,100,150,145,150,220};for(int i=0;i<widths.length;i++){table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);}
		table.setDefaultRenderer(JLabel.class,(t,value,selected,focus,r,c)->{JLabel label=(JLabel)value;label.setOpaque(true);label.setBackground(selected?t.getSelectionBackground():t.getBackground());label.setForeground(selected?t.getSelectionForeground():t.getForeground());return label;});
		sorter.setComparator(1,Comparator.comparing(value->((JLabel)value).getText(),String.CASE_INSENSITIVE_ORDER));
		table.setDefaultRenderer(Long.class,new DefaultTableCellRenderer(){@Override protected void setValue(Object value){setHorizontalAlignment(CENTER);super.setValue(String.format(Locale.ROOT,"%,d",value));}});
		DefaultTableCellRenderer numeric=new DefaultTableCellRenderer();numeric.setHorizontalAlignment(SwingConstants.CENTER);
		for(int column:new int[]{4,5}){table.getColumnModel().getColumn(column).setCellRenderer(numeric);}
		add(new JScrollPane(table),BorderLayout.CENTER);add(details,BorderLayout.SOUTH);
		Runnable filtering=()->sorter.setRowFilter(new RowFilter<DefaultTableModel,Integer>(){@Override public boolean include(Entry<? extends DefaultTableModel,? extends Integer> entry){GeTrade trade=rows.get(entry.getIdentifier());return trade.getName().toLowerCase(Locale.ROOT).contains(search.getText().trim().toLowerCase(Locale.ROOT))&&(filter.getSelectedIndex()==0||(filter.getSelectedIndex()==2)==trade.getStatus().endsWith(" (Partial)"));}});
		search.getDocument().addDocumentListener(BankDialogs.changes(filtering));filter.addActionListener(e->filtering.run());
		table.getSelectionModel().addListSelectionListener(e->{if(!e.getValueIsAdjusting()&&table.getSelectedRow()>=0){details.setText(detail(rows.get(table.convertRowIndexToModel(table.getSelectedRow()))));}});
	}
	public void page(List<GeTrade> values)
	{
		if(values.size()>25){throw new IllegalArgumentException("GE pages are limited to 25 rows");}table.clearSelection();model.setRowCount(0);rows.clear();values.stream().filter(GeTrade::visibleInHistory).forEach(rows::add);
		for(GeTrade row:rows)
		{
			JLabel item=new JLabel();item.putClientProperty("html.disable",Boolean.TRUE);item.setText(row.getName());icons.apply(item,row.getItemId());item.setHorizontalAlignment(SwingConstants.LEFT);
			item.addPropertyChangeListener("icon",event->table.repaint());
			model.addRow(new Object[]{DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault()).format(row.getAt()),item,row.getQuantity(),row.getReportedGp(),average(row),row.isBuy()?"N/A":row.getEstimatedTax()==null?"Unknown":"~"+GpFormatter.exact(row.getEstimatedTax()),row.getStatus()});
		}
		// Large grouped longs must remain readable; AUTO_RESIZE_OFF provides horizontal scrolling.
		for(int column:new int[]{2,3,4,5})
		{
			int width=column==2?100:150;
			for(int row=0;row<model.getRowCount();row++){Object value=model.getValueAt(row,column);String text=value instanceof Long?String.format(Locale.ROOT,"%,d",value):value.toString();width=Math.max(width,table.getFontMetrics(table.getFont()).stringWidth(text)+24);}
			table.getColumnModel().getColumn(column).setPreferredWidth(width);
		}
		details.setText(rows.isEmpty()?"No completed Grand Exchange trades recorded.":"Select a trade. Completion time is when it was observed here; execution GP comes from RuneLite. Fees are estimated. Profit and collection destination are unknown.");
	}
	static String average(GeTrade trade)
	{
		if(trade.getQuantity()==0){return "N/A";}BigDecimal average=BigDecimal.valueOf(trade.getReportedGp()).divide(BigDecimal.valueOf(trade.getQuantity()),2,RoundingMode.DOWN).stripTrailingZeros();
		return (trade.getReportedGp()%trade.getQuantity()==0?"":"~")+new java.text.DecimalFormat("#,##0.##",java.text.DecimalFormatSymbols.getInstance(Locale.ROOT)).format(average)+" GP";
	}
	static String detail(GeTrade trade)
	{
		return (trade.isBuy()?"Buy":"Sell")+" — "+trade.getName()+" | "+trade.getQuantity()+" units | Reported executed total: "+GpFormatter.exact(trade.getReportedGp())+" | Derived average: "+average(trade)
			+"\n"+(trade.isBuy()?"No seller tax on purchases.":trade.getEstimatedTax()==null?"Tax not observed.":"Estimated seller tax: "+GpFormatter.exact(trade.getEstimatedTax())+" ("+trade.getTaxMethod()+").")
			+" Displayed tax uses the completed offer's average price; financial totals retain their original per-fill estimates. Gross/net settlement and realized profit are not verified. Collection destination is unknown; bank associations are possible matches only."
			+"\n"+(trade.getOrigin()==GeOffer.Origin.PREEXISTING?"Preexisting offer.":trade.getOrigin()==GeOffer.Origin.UNKNOWN?"Offer origin unknown.":"Offer generation observed on this PC.")+" "+trade.getStatus();
	}
}
