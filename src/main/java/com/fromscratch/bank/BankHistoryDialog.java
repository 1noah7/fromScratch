package com.fromscratch.bank;

import java.awt.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

/** Only 25 named rows per read; filters/sorting operate on the displayed page. No accounting mutations. */
public final class BankHistoryDialog
{
	private BankHistoryDialog() { }
	public static void show(Component owner, BankActions actions, boolean manual)
	{
		DefaultTableModel model = new DefaultTableModel(new String[]{"Time", "Item", "Change", "Source", "Purchase"}, 0)
		{
			@Override public boolean isCellEditable(int r, int c) { return false; }
			@Override public Class<?> getColumnClass(int c) { return c == 2 ? Long.class : String.class; }
		};
		JTable table = new JTable(model); table.setFillsViewportHeight(true); TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model); table.setRowSorter(sorter);
		table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() { @Override protected void setValue(Object value) { long n = ((Number) value).longValue(); super.setValue((n > 0 ? "+" : "") + n); } });
		JTextField search = new JTextField(16); JComboBox<String> filter = new JComboBox<>(new String[]{"All changes", "Gains", "Losses"});
		JPanel controls = new JPanel(new BorderLayout(8, 0)); controls.add(search, BorderLayout.CENTER); controls.add(filter, BorderLayout.EAST); search.setToolTipText("Search items on this page");
		Runnable filtering = () -> sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() { @Override public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) { long n = ((Number) entry.getValue(2)).longValue(); return entry.getStringValue(1).toLowerCase(Locale.ROOT).contains(search.getText().trim().toLowerCase(Locale.ROOT)) && (filter.getSelectedIndex() == 0 || (filter.getSelectedIndex() == 1 ? n > 0 : n < 0)); } });
		search.getDocument().addDocumentListener(BankDialogs.changes(filtering)); filter.addActionListener(e -> filtering.run());
		JPanel content = new JPanel(new BorderLayout(6, 8)); content.add(controls, BorderLayout.NORTH); content.add(new JScrollPane(table), BorderLayout.CENTER); JLabel status = new JLabel("Loading…"); content.add(status, BorderLayout.SOUTH);
		JButton itemHistory=new JButton("Item History");itemHistory.setToolTipText("Select a row, or enter an item name in Search, then view its full recorded history.");
		java.util.List<BankHistory.Row> displayed=new ArrayList<>();
		itemHistory.addActionListener(e->
		{
			int selected=table.getSelectedRow();
			if(selected>=0) { BankHistory.Row row=displayed.get(table.convertRowIndexToModel(selected));ItemHistoryDialog.show(owner,actions,row.getItemId(),row.getName());return; }
			String query=search.getText().trim().toLowerCase(Locale.ROOT);
			actions.historyItems(items->SwingUtilities.invokeLater(()->
			{
				java.util.List<Map.Entry<Integer,String>> matches=new ArrayList<>();
				items.entrySet().stream().filter(entry->entry.getValue()!=null&&entry.getValue().toLowerCase(Locale.ROOT).contains(query)).sorted(Map.Entry.comparingByValue()).limit(100).forEach(matches::add);
				if(matches.isEmpty()) { JOptionPane.showMessageDialog(owner,"No known items match. Search a bank-history page or try another name.","Item History",JOptionPane.INFORMATION_MESSAGE);return; }
				// Labels contain names only. IDs remain internal so same-name variants are never merged.
				String[] labels=new String[matches.size()];for(int i=0;i<labels.length;i++) { labels[i]=matches.get(i).getValue()+" ("+(i+1)+")"; }
				Object choice=labels.length==1?labels[0]:JOptionPane.showInputDialog(owner,"Choose an item (first 100 matches)","Item History",JOptionPane.PLAIN_MESSAGE,null,labels,labels[0]);
				if(choice!=null) { int index=Arrays.asList(labels).indexOf(choice);Map.Entry<Integer,String> item=matches.get(index);ItemHistoryDialog.show(owner,actions,item.getKey(),item.getValue()); }
			}));
		});
		JButton previous = new JButton("Previous"), next = new JButton("Next"), close = new JButton("Close"); JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT)); buttons.add(itemHistory);buttons.add(previous); buttons.add(next); buttons.add(close);
		JDialog dialog = BankDialogs.dialog(owner, manual ? "Manual Corrections" : "Automatic Changes", content, buttons, new Dimension(620, 440));
		java.util.List<BankHistory.Cursor> cursors = new ArrayList<>(); cursors.add(null); int[] index = {0}; BankHistory.Cursor[] following = {null}; Runnable[] load = new Runnable[1];
		load[0] = () ->
		{
			previous.setEnabled(false); next.setEnabled(false); status.setText("Loading…");
			actions.history(manual, cursors.get(index[0]), page -> SwingUtilities.invokeLater(() ->
			{
				if (!dialog.isDisplayable()) { return; } model.setRowCount(0);displayed.clear();displayed.addAll(page.getRows());
				for (BankHistory.Row row : page.getRows()) { model.addRow(new Object[]{DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault()).format(row.getAt()), row.getName(), row.getChange(),row.getCategory(),row.getPurchase()==null?"":row.getCategory().contains("Looting Bag")?"Looting Bag":"Rune Pouch / Divine Rune Pouch"}); }
				following[0] = page.getNext(); previous.setEnabled(index[0] > 0); next.setEnabled(following[0] != null); status.setText(page.getMessage().isEmpty() ? "Page " + (index[0] + 1) + " • up to 25 changes" : page.getMessage());
			}));
		};
		previous.addActionListener(e -> { index[0]--; load[0].run(); }); next.addActionListener(e -> { if (cursors.size() <= index[0] + 1) { cursors.add(following[0]); } index[0]++; load[0].run(); }); close.addActionListener(e -> dialog.dispose()); dialog.getRootPane().setDefaultButton(close);
		load[0].run(); dialog.setVisible(true);
	}
}
