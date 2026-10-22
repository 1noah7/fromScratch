package com.fromscratch.ge;

import com.fromscratch.bank.*;
import java.awt.*;
import java.util.*;
import javax.swing.*;

/** Asynchronous, read-only GE history; no native exchange interfaces or game actions are touched. */
public final class GeHistoryDialog
{
	private GeHistoryDialog(){}
	public static void show(Component owner,BankActions actions,boolean buy,BankIcons icons)
	{
		GeHistoryView view=new GeHistoryView(icons);JLabel status=new JLabel("Loading…");JPanel content=new JPanel(new BorderLayout(6,8));content.add(view,BorderLayout.CENTER);content.add(status,BorderLayout.SOUTH);
		JButton previous=new JButton("Previous"),next=new JButton("Next"),close=new JButton("Close");JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT));buttons.add(previous);buttons.add(next);buttons.add(close);
		JDialog dialog=BankDialogs.dialog(owner,buy?"Grand Exchange Purchases":"Grand Exchange Sales",content,buttons,new Dimension(900,540));
		java.util.List<GeHistory.Cursor> cursors=new ArrayList<>();cursors.add(null);int[] index={0};GeHistory.Cursor[] following={null};Runnable[] load=new Runnable[1];
		load[0]=()->{previous.setEnabled(false);next.setEnabled(false);status.setText("Loading…");actions.exchangeHistory(buy,cursors.get(index[0]),page->SwingUtilities.invokeLater(()->{
			if(!dialog.isDisplayable()){return;}view.page(page.getRows());following[0]=page.getNext();previous.setEnabled(index[0]>0);next.setEnabled(following[0]!=null);status.setText(page.getMessage().isEmpty()?"Page "+(index[0]+1)+" • up to 25 records • search/filter apply to this page":page.getMessage());
		}));};
		previous.addActionListener(e->{index[0]--;load[0].run();});next.addActionListener(e->{if(cursors.size()<=index[0]+1){cursors.add(following[0]);}index[0]++;load[0].run();});close.addActionListener(e->dialog.dispose());dialog.getRootPane().setDefaultButton(close);load[0].run();dialog.setVisible(true);
	}
}
