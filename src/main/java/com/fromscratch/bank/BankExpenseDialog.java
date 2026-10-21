package com.fromscratch.bank;

import com.fromscratch.wealth.GpFormatter;
import java.awt.*;
import javax.swing.*;

/** Explicit statistical expense entry. No inference, wallet deduction or game action is performed. */
final class BankExpenseDialog
{
	private BankExpenseDialog(){}
	static void show(Component owner,BankView captured,BankActions actions)
	{
		JPanel content=new JPanel(new BorderLayout(0,8));content.add(BankDialogs.text("Record only a loss or expense you can verify. Ordinary bank withdrawals are not spending. This records a statistic; Bank GP and item counts stay unchanged.",42),BorderLayout.NORTH);
		JPanel fields=new JPanel(new GridLayout(0,1,0,5));JTextField amount=new JTextField(),reason=new JTextField();fields.add(new JLabel("GP amount (positive integer)"));fields.add(amount);fields.add(new JLabel("What was lost or paid?"));fields.add(reason);content.add(fields,BorderLayout.CENTER);
		StringBuilder recent=new StringBuilder("Recent recorded expenses:\n");int shown=0;
		for(BankAudit audit:captured.getRun().getRecent()){for(com.fromscratch.wealth.ValuationEvent event:audit.getValuations()){if(event.getKind()==com.fromscratch.wealth.ValuationEvent.Kind.EXPENSE&&shown++<5){recent.append(audit.getAt()).append(" | ").append(GpFormatter.exact(event.knownGp())).append(" | ").append(event.getReason()).append('\n');}}}
		if(shown==0){recent.append("None recorded.");}content.add(BankDialogs.text(recent.toString(),42),BorderLayout.SOUTH);
		JButton save=new JButton("Review Expense"),cancel=new JButton("Cancel");save.setEnabled(false);JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT));buttons.add(cancel);buttons.add(save);JDialog dialog=BankDialogs.dialog(owner,"Record Loss / Expense",content,buttons,null);
		Runnable validate=()->{try{save.setEnabled(Long.parseLong(amount.getText().trim())>0&&!reason.getText().trim().isEmpty()&&reason.getText().trim().length()<=240);}catch(NumberFormatException ex){save.setEnabled(false);}};
		amount.getDocument().addDocumentListener(BankDialogs.changes(validate));reason.getDocument().addDocumentListener(BankDialogs.changes(validate));
		save.addActionListener(e->{if(!save.isEnabled()){return;}long gp=Long.parseLong(amount.getText().trim());if(BankDialogs.confirm(dialog,"Record Loss / Expense?",GpFormatter.exact(gp)+": "+reason.getText().trim()+". This does not deduct bank coins.","Record Expense",false)){save.setEnabled(false);actions.recordExpense(captured.getRun().getRevision(),gp,reason.getText().trim());dialog.dispose();}});
		cancel.addActionListener(e->dialog.dispose());dialog.getRootPane().setDefaultButton(save);dialog.setVisible(true);
	}
}
