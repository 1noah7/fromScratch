package com.fromscratch.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

/** Player-only local decisions. Cancel never submits quantities; Apply revalidates on the client thread. */
final class ItemReviewDialog
{
	private ItemReviewDialog() { }
	static void show(Component parent, SetupView view, SetupActions actions)
	{
		ItemReview review = view.getItemReview(); if (review == null) { return; }
		ItemReviewTable model = new ItemReviewTable(review, view.getItemNames());
		JTable table = new JTable(model)
		{
			@Override public javax.swing.table.TableCellEditor getCellEditor(int row, int column)
			{
				return convertColumnIndexToModel(column) == 3
					? new DefaultCellEditor(new JComboBox<>(model.choices(convertRowIndexToModel(row)))) : super.getCellEditor(row, column);
			}
		};
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		for (int i = 0; i < model.getColumnCount(); i++) { table.getColumnModel().getColumn(i).setPreferredWidth(i == 0 ? 180 : i == 4 ? 330 : 145); }
		table.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(new JComboBox<>(ItemReviewTable.Decision.values())));
		TableRowSorter<ItemReviewTable> sorter = new TableRowSorter<>(model); table.setRowSorter(sorter);
		sorter.setSortKeys(java.util.Collections.singletonList(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
		List<TableColumn> advancedColumns = new ArrayList<>();
		while (table.getColumnCount() > 5) { TableColumn column = table.getColumnModel().getColumn(5); advancedColumns.add(column); table.removeColumn(column); }
		JTextField search = new JTextField(); search.setToolTipText("Search item names");
		JComboBox<ItemReviewTable.Filter> filter = new JComboBox<>(ItemReviewTable.Filter.values());
		JCheckBox unchanged = new JCheckBox("Show unchanged items"); JCheckBox advanced = new JCheckBox("Advanced quantities");
		JLabel empty = new JLabel(); empty.setForeground(FromScratchPanel.PURPLE);
		Runnable refresh = () ->
		{
			if (table.isEditing()) { table.getCellEditor().stopCellEditing(); }
			model.filter((ItemReviewTable.Filter) filter.getSelectedItem(), search.getText(), unchanged.isSelected());
			empty.setText(!model.hasChanges() ? "No item changes detected." : model.getRowCount() == 0 ? "No changes match this filter." : model.getRowCount() + " items shown.");
		};
		search.getDocument().addDocumentListener(new DocumentListener()
		{
			public void insertUpdate(DocumentEvent e) { refresh.run(); }
			public void removeUpdate(DocumentEvent e) { refresh.run(); }
			public void changedUpdate(DocumentEvent e) { refresh.run(); }
		});
		filter.addActionListener(e -> refresh.run()); unchanged.addActionListener(e -> refresh.run());
		advanced.addActionListener(e ->
		{
			if (table.isEditing()) { table.getCellEditor().stopCellEditing(); }
			for (TableColumn column : advancedColumns) { if (advanced.isSelected()) { table.addColumn(column); } else { table.removeColumn(column); } }
		});
		JPanel filters = new JPanel(new java.awt.GridLayout(0, 2, 6, 4));
		filters.add(new JLabel("Search items")); filters.add(search); filters.add(filter); filters.add(unchanged); filters.add(advanced); filters.add(empty);
		JTextArea help = new JTextArea("Played on mobile or another computer? Update your From Scratch progress when you return to this PC.\n"
			+ "Only changed items are shown. Newly Earned uses the net gain, or one copy for an unchanged item; use Advanced quantities for a different amount.\n"
			+ "Gameplay completed entirely away from this PC may not be reconstructable. Only final holdings can be compared.");
		help.setEditable(false); help.setLineWrap(true); help.setWrapStyleWord(true); help.setOpaque(false);
		JPanel top = new JPanel(new BorderLayout(4, 8)); top.add(help, BorderLayout.NORTH); top.add(filters, BorderLayout.SOUTH);
		JPanel panel = new JPanel(new BorderLayout(8, 8)); panel.add(top, BorderLayout.NORTH);
		JScrollPane scroll = new JScrollPane(table); scroll.setPreferredSize(new Dimension(980, 300)); panel.add(scroll, BorderLayout.CENTER);
		JComboBox<ItemReviewTable.Decision> bulk = new JComboBox<>(ItemReviewTable.Decision.values());
		JButton selected = new JButton("Apply to selected items");
		selected.addActionListener(e ->
		{
			if (table.isEditing()) { table.getCellEditor().stopCellEditing(); }
			int[] rows = Arrays.stream(table.getSelectedRows()).map(table::convertRowIndexToModel).toArray();
			if (rows.length == 0) { JOptionPane.showMessageDialog(parent, "Select one or more items first."); return; }
			try { model.decide(rows, (ItemReviewTable.Decision) bulk.getSelectedItem()); refresh.run(); }
			catch (IllegalArgumentException ex) { JOptionPane.showMessageDialog(parent, ex.getMessage(), "Choose applicable items", JOptionPane.WARNING_MESSAGE); }
		});
		JTextField reason = new JTextField(); reason.setToolTipText("Optional explanation; selected decisions are recorded in item history");
		JPanel bottom = new JPanel(new java.awt.GridLayout(0, 2, 6, 4));
		bottom.add(bulk); bottom.add(selected); bottom.add(new JLabel("Explanation (optional)")); bottom.add(reason); panel.add(bottom, BorderLayout.SOUTH);
		refresh.run();
		while (JOptionPane.showOptionDialog(parent, panel, "Update Progress", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
			null, new String[]{"Review and apply", "Cancel"}, "Review and apply") == JOptionPane.OK_OPTION)
		{
			try
			{
				if (table.isEditing() && !table.getCellEditor().stopCellEditing()) { continue; }
				if (!model.hasChanges()) { JOptionPane.showMessageDialog(parent, "No item changes detected."); continue; }
				com.fromscratch.tracking.ItemReviewEdits edits = model.edits(reason.getText());
				JTextArea summary = new JTextArea(model.details()); summary.setEditable(false);
				JScrollPane preview = new JScrollPane(summary); preview.setPreferredSize(new Dimension(850, 260));
				if (JOptionPane.showConfirmDialog(parent, preview, "Apply these item changes?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) { continue; }
				actions.confirmUpdate(review.getToken(), edits); return;
			}
			catch (IllegalArgumentException | ArithmeticException ex) { JOptionPane.showMessageDialog(parent, ex.getMessage(), "Check item decisions", JOptionPane.WARNING_MESSAGE); }
		}
		actions.cancelUpdate(review.getToken());
	}
}
