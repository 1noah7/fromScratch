package com.fromscratch.bank;

import java.awt.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Read-only item evidence, selected from existing Bank History. Bounded asynchronous pages. */
public final class ItemHistoryDialog
{
	private ItemHistoryDialog() { }
	public static void show(Component owner,BankActions actions,int itemId,String name)
	{
		DefaultTableModel model=new DefaultTableModel(new String[]{"Time","Action","Quantity","Details"},0)
		{ @Override public boolean isCellEditable(int row,int col) { return false; } };
		JTable table=new JTable(model);table.setAutoCreateRowSorter(true);table.setFillsViewportHeight(true);
		JPanel content=new JPanel(new BorderLayout(6,6));content.add(new JScrollPane(table),BorderLayout.CENTER);
		JLabel status=new JLabel("Loading…");content.add(status,BorderLayout.SOUTH);
		JButton previous=new JButton("Previous"),next=new JButton("Next"),close=new JButton("Close");
		JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT));buttons.add(previous);buttons.add(next);buttons.add(close);
		JDialog dialog=BankDialogs.dialog(owner,name+" — Item History",content,buttons,new Dimension(760,420));
		java.util.List<BankHistory.Cursor> cursors=new ArrayList<>();cursors.add(null);int[] index={0};BankHistory.Cursor[] following={null};Runnable[] load=new Runnable[1];
		load[0]=()->
		{
			previous.setEnabled(false);next.setEnabled(false);
			actions.itemHistory(itemId,cursors.get(index[0]),page->SwingUtilities.invokeLater(()->
			{
				if(!dialog.isDisplayable()) { return; }model.setRowCount(0);
				for(ItemHistory.Row row:page.getRows()) { model.addRow(new Object[]{DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault()).format(row.getAt()),row.getAction(),(row.getQuantity()>0?"+":"")+row.getQuantity(),row.getDetails()}); }
				following[0]=page.getNext();previous.setEnabled(index[0]>0);next.setEnabled(following[0]!=null);status.setText(page.getMessage().isEmpty()?"Page "+(index[0]+1)+" • recorded evidence only":page.getMessage());
			}));
		};
		previous.addActionListener(e->{index[0]--;load[0].run();});next.addActionListener(e->{if(cursors.size()<=index[0]+1) { cursors.add(following[0]); }index[0]++;load[0].run();});
		close.addActionListener(e->dialog.dispose());dialog.getRootPane().setDefaultButton(close);load[0].run();dialog.setVisible(true);
	}
}
