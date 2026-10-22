package com.fromscratch.bank;

import java.awt.Component;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.*;
import net.runelite.client.util.Filepath;

/** Local progress-file tools only. Chooser on EDT; validation/archives/imports on the controller's worker. */
final class ProgressDialogs
{
	private ProgressDialogs() { }
	static JPopupMenu menu(Component owner,Supplier<BankActions> actions,Supplier<BankView> view)
	{
		JPopupMenu menu=new JPopupMenu();JMenuItem export=new JMenuItem("Export Progress"),restore=new JMenuItem("Import Progress");menu.add(export);menu.add(restore);
		export.addActionListener(e->
		{
			BankActions selected=actions.get();if(selected==null){return;}
			List<Filepath> files=new Filepath.Chooser().setIsSave().setAcceptsFiles().setDialogTitle("Export Progress").addExtensionFilter("From Scratch Progress","fsp").setDefaultExtension("fsp").setFileName("from-scratch-progress-"+DateTimeFormatter.ofPattern("uuuuMMdd-HHmmss").format(java.time.LocalDateTime.now())+".fsp").showDialog(owner);
			Filepath file=selection(files);if(file==null){return;}selected.exportProgress(file,text->message(owner,text));
		});
		restore.addActionListener(e->
		{
			BankActions selected=actions.get();if(selected==null){return;}
			List<Filepath> files=new Filepath.Chooser().setIsOpen().setAcceptsFiles().setDialogTitle("Import Progress").addExtensionFilter("From Scratch Progress","fsp").showDialog(owner);
			Filepath file=selection(files);if(file==null){return;}
			selected.previewImport(file,candidate->confirm(owner,selected,candidate),text->message(owner,text));
		});
		if(view.get()!=null&&view.get().isError())
		{
			JMenuItem recovery=new JMenuItem("Recover Previous Save");menu.add(recovery);recovery.addActionListener(e->{BankActions selected=actions.get();if(selected!=null){selected.previewRecovery(candidate->confirm(owner,selected,candidate),text->message(owner,text));}});
		}
		return menu;
	}
	static Filepath selection(List<Filepath> files){return files==null||files.isEmpty()?null:files.get(0);}
	private static void confirm(Component owner,BankActions actions,com.fromscratch.persistence.ProgressBackup.Candidate candidate)
	{
		SwingUtilities.invokeLater(()->
		{
			BankRun run=candidate.getRun();String started=DateTimeFormatter.ofPattern("d MMM uuuu, HH:mm").withZone(ZoneId.systemDefault()).format(run.getStartedAt());
			String updated=DateTimeFormatter.ofPattern("d MMM uuuu, HH:mm").withZone(ZoneId.systemDefault()).format(run.getRecent().get(0).getAt());
			boolean confirmed=BankDialogs.confirm(owner,"Import Progress?","Restore the run started "+started+"?\nLast recorded update: "+updated+".\n\nThis replaces this character's local progression and saved Time Played. Any newer changes will be lost from the active run. Existing files will be backed up. Histories will not be merged. Close other RuneLite clients before continuing.","Import Progress",false);
			if(confirmed){actions.importProgress(candidate,text->message(owner,text));}else{actions.cancelImport(candidate);}
		});
	}
	private static void message(Component owner,String text)
	{
		SwingUtilities.invokeLater(()->
		{
			JButton close=new JButton("Close");JPanel buttons=new JPanel();buttons.add(close);JDialog dialog=BankDialogs.dialog(owner,"From Scratch",BankDialogs.text(text,40),buttons,null);close.addActionListener(e->dialog.dispose());dialog.getRootPane().setDefaultButton(close);dialog.setVisible(true);
		});
	}
}
