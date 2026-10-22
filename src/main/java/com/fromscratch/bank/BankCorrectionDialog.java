package com.fromscratch.bank;

import java.awt.*;
import javax.swing.*;

/** Direct, revision-bound corrections. Opening, searching and cancelling never mutate progression. */
public final class BankCorrectionDialog
{
	public static final String PURPOSE="Use this only if From Scratch recorded an incorrect bank quantity.";
	private BankCorrectionDialog() { }
	public static void show(Component owner, BankView captured, BankActions actions)
	{
		JTextField search = new JTextField(20); DefaultListModel<Integer> rows = new DefaultListModel<>(); JList<Integer> list = new JList<>(rows);
		list.setCellRenderer(new DefaultListCellRenderer() { @Override public Component getListCellRendererComponent(JList<?> l, Object value, int index, boolean selected, boolean focus) { return super.getListCellRendererComponent(l, captured.name((Integer) value) + " (#" + value + ")", index, selected, focus); } });
		JLabel physical = new JLabel("Select an item"), balance = new JLabel(" "), preview = new JLabel(" "); JTextField input = new JTextField("0", 14); JCheckBox adjustment = new JCheckBox("Apply signed adjustment");
		JPanel selection = new JPanel(new BorderLayout(0, 5)); selection.add(BankDialogs.text(PURPOSE,42),BorderLayout.NORTH);JPanel searchRow=new JPanel(new BorderLayout(0,5));searchRow.add(new JLabel("Search item by name"),BorderLayout.NORTH);searchRow.add(search,BorderLayout.CENTER);selection.add(searchRow,BorderLayout.CENTER);
		JPanel fields = new JPanel(new GridLayout(0, 1, 0, 5)); fields.add(physical); fields.add(balance); fields.add(new JLabel("Desired signed bank balance")); fields.add(input); fields.add(adjustment); fields.add(preview);
		if(!captured.getRun().getBuybacks().getPurchases().isEmpty()){fields.add(BankDialogs.text("Edits bank progress before buybacks. Paid unlocks and previous spending are kept.",42));}
		if (!captured.ready()) { fields.add(new JLabel("Open your bank before saving a correction.")); }
		JPanel content = new JPanel(new BorderLayout(0, 10)); content.add(selection, BorderLayout.NORTH); content.add(new JScrollPane(list), BorderLayout.CENTER); content.add(fields, BorderLayout.SOUTH);
		JButton save = new JButton("Save Correction"), cancel = new JButton("Cancel"); save.setEnabled(false); JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT)); buttons.add(cancel); buttons.add(save);
		JDialog dialog = BankDialogs.dialog(owner, "Manual Bank Corrections", content, buttons, new Dimension(540, 560));
		Runnable update = () ->
		{
			Integer id = list.getSelectedValue(); save.setEnabled(false); if (id == null) { return; }
			physical.setText((captured.ready() ? "Physical bank quantity: " : "Last observed bank quantity: ") + captured.getRun().physical(id)); balance.setText("Current bank balance: " + captured.getRun().balance(id));
			try { long desired = BankCorrections.desired(captured.getRun(), id, input.getText(), adjustment.isSelected()); preview.setText("Preview: " + captured.getRun().balance(id) + " → " + desired); save.setEnabled(captured.ready()); }
			catch (RuntimeException ex) { preview.setText("Enter a valid signed count within the bank limit."); }
		};
		Runnable filter = () -> { Integer selected = list.getSelectedValue(); rows.clear(); BankCorrections.search(captured, search.getText()).forEach(rows::addElement); if (selected != null) { list.setSelectedValue(selected, true); } };
		search.getDocument().addDocumentListener(BankDialogs.changes(filter)); input.getDocument().addDocumentListener(BankDialogs.changes(update)); list.addListSelectionListener(e -> update.run()); adjustment.addActionListener(e -> update.run()); filter.run();
		save.addActionListener(e ->
		{
			Integer id = list.getSelectedValue(); if (id == null || !save.isEnabled()) { return; }
			long desired = BankCorrections.desired(captured.getRun(), id, input.getText(), adjustment.isSelected());
			if (BankDialogs.confirm(dialog, "Confirm Bank Correction", captured.name(id) + ": " + captured.getRun().balance(id) + " → " + desired + ". This change will be recorded in Bank History.", "Save Correction", false))
			{ save.setEnabled(false); actions.correct(captured.getRun().getRevision(), id, desired); dialog.dispose(); }
		});
		cancel.addActionListener(e -> dialog.dispose()); dialog.getRootPane().setDefaultButton(save); dialog.setVisible(true);
	}
}
