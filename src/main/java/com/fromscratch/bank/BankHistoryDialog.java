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
		DefaultTableModel model = new DefaultTableModel(new String[]{"Time", "Item", "Change"}, 0)
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
		JButton previous = new JButton("Previous"), next = new JButton("Next"), close = new JButton("Close"); JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT)); buttons.add(previous); buttons.add(next); buttons.add(close);
		JDialog dialog = BankDialogs.dialog(owner, manual ? "Manual Corrections" : "Automatic Changes", content, buttons, new Dimension(620, 440));
		java.util.List<BankHistory.Cursor> cursors = new ArrayList<>(); cursors.add(null); int[] index = {0}; BankHistory.Cursor[] following = {null}; Runnable[] load = new Runnable[1];
		load[0] = () ->
		{
			previous.setEnabled(false); next.setEnabled(false); status.setText("Loading…");
			actions.history(manual, cursors.get(index[0]), page -> SwingUtilities.invokeLater(() ->
			{
				if (!dialog.isDisplayable()) { return; } model.setRowCount(0);
				for (BankHistory.Row row : page.getRows()) { model.addRow(new Object[]{DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault()).format(row.getAt()), row.getName(), row.getChange()}); }
				following[0] = page.getNext(); previous.setEnabled(index[0] > 0); next.setEnabled(following[0] != null); status.setText(page.getMessage().isEmpty() ? "Page " + (index[0] + 1) + " • up to 25 changes" : page.getMessage());
			}));
		};
		previous.addActionListener(e -> { index[0]--; load[0].run(); }); next.addActionListener(e -> { if (cursors.size() <= index[0] + 1) { cursors.add(following[0]); } index[0]++; load[0].run(); }); close.addActionListener(e -> dialog.dispose()); dialog.getRootPane().setDefaultButton(close);
		load[0].run(); dialog.setVisible(true);
	}
}
