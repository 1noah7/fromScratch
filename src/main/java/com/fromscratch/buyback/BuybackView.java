package com.fromscratch.buyback;

import com.fromscratch.bank.*;
import com.fromscratch.wealth.GpFormatter;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Small curated shop/history. Sprites are cached by the same supported RuneLite item-image service. */
public final class BuybackView extends JPanel
{
	private final BankView view;
	private final BankIcons icons;
	private final JTextField search=new JTextField();
	private final JComboBox<String> category=new JComboBox<>(new String[]{"All categories","Combat & Utility","Void Knight","Skilling Outfits"});
	private final DefaultTableModel model=new DefaultTableModel(new String[]{"","Item / Family","Price","Status"},0){public boolean isCellEditable(int r,int c){return false;}public Class<?> getColumnClass(int c){return c==0?JLabel.class:String.class;}};
	private final JTable table=new JTable(model);
	private final java.util.List<BuybackEntry> entries=new ArrayList<>();
	private final JButton purchase=new JButton("Buyback");
	public BuybackView(BankView view,BankIcons icons,java.util.function.Consumer<BuybackEntry> confirm)
	{
		super(new BorderLayout(8,8));this.view=view;this.icons=icons;
		JPanel top=new JPanel(new BorderLayout(4,6));top.add(new JLabel("Available GP: "+GpFormatter.compact(BuybackEngine.available(view.getRun()))),BorderLayout.NORTH);
		JPanel controls=new JPanel(new GridLayout(1,2,8,0));search.setToolTipText("Search item or family name");controls.add(search);controls.add(category);top.add(controls,BorderLayout.SOUTH);add(top,BorderLayout.NORTH);
		table.setRowHeight(38);table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);table.setAutoCreateRowSorter(true);iconRenderer(table);table.getColumnModel().getColumn(0).setMaxWidth(42);table.getColumnModel().getColumn(1).setPreferredWidth(310);table.getColumnModel().getColumn(3).setPreferredWidth(160);add(new JScrollPane(table),BorderLayout.CENTER);
		purchase.setEnabled(false);add(purchase,BorderLayout.SOUTH);
		search.getDocument().addDocumentListener(BankDialogs.changes(this::refresh));category.addActionListener(e->refresh());table.getSelectionModel().addListSelectionListener(e->selection());
		purchase.addActionListener(e->{BuybackEntry entry=selected();if(entry!=null&&purchase.isEnabled()){confirm.accept(entry);}});refresh();
	}
	public void filter(String query,String category){search.setText(query);this.category.setSelectedItem(category);refresh();}
	public int rows(){return model.getRowCount();}
	public BuybackEntry selected(){int row=table.getSelectedRow();return row<0?null:entries.get(table.convertRowIndexToModel(row));}
	private void selection(){BuybackEntry entry=selected();purchase.setEnabled(view.ready()&&entry!=null&&"Available".equals(BuybackEngine.status(view.getRun(),entry)));}
	private void refresh()
	{
		model.setRowCount(0);entries.clear();String chosen=(String)category.getSelectedItem();
		for(BuybackEntry entry:BuybackCatalog.search(search.getText()))
		{
			if(!"All categories".equals(chosen)&&!entry.getCategory().equals(chosen)){continue;}
			JLabel icon=new JLabel();icons.apply(icon,entry.getIcon());icon.addPropertyChangeListener("icon",e->table.repaint());entries.add(entry);
			model.addRow(new Object[]{icon,entry.getName(),GpFormatter.compact(BuybackCatalog.cost(entry,view.getRun().getBuybacks().getEntitlements())),view.ready()?BuybackEngine.status(view.getRun(),entry):"Open your bank"});
		}
		selection();
	}
	static void iconRenderer(JTable table)
	{
		table.setDefaultRenderer(JLabel.class,(t,value,selected,focus,row,column)->{JLabel label=(JLabel)value;label.setOpaque(true);label.setBackground(selected?t.getSelectionBackground():t.getBackground());return label;});
	}
	public static String confirmation(BankRun run,BuybackEntry entry)
	{
		long price=BuybackCatalog.cost(entry,run.getBuybacks().getEntitlements()),available=BuybackEngine.available(run);
		String explanation=entry.getId().equals("rune_pouch_family")?"Unlocks both Rune Pouch and Divine Rune Pouch for 1M GP. Pay once, even if you upgrade or revert your pouch.":"Unlocks one qualifying legacy copy per listed equipment piece. Real items and coins stay untouched.";
		return entry.getName()+"\nPrice: "+GpFormatter.exact(price)+"\nAvailable GP: "+GpFormatter.exact(available)+"\nRemaining: "+GpFormatter.exact(Math.subtractExact(available,price))+"\n\n"+explanation;
	}
}
