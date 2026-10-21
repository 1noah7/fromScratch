package com.fromscratch.bank;

import java.awt.*;
import javax.swing.*;
import net.runelite.client.ui.FontManager;

/** Fixed-height two-column statistic card; no inner scrolling and no per-event component reconstruction. */
public final class BankTile extends JPanel
{
	private final JLabel value = new JLabel("Coming soon", SwingConstants.CENTER);
	private final JLabel detail = new JLabel("",SwingConstants.CENTER);
	private final JLabel icon = new JLabel();
	private final JLabel label;
	public BankTile(String title, String wrappedTitle, int itemId, BankIcons icons)
	{
		setLayout(new BorderLayout(0, 3)); setBackground(new Color(43, 39, 33));
		setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(75, 62, 85)), BorderFactory.createEmptyBorder(6, 3, 6, 3)));
		setPreferredSize(new Dimension(100, 112)); setMinimumSize(new Dimension(92, 112));
		icon.setHorizontalAlignment(SwingConstants.CENTER); icons.apply(icon, itemId); add(icon, BorderLayout.NORTH);
		label = new JLabel("<html><center>" + wrappedTitle + "</center></html>", SwingConstants.CENTER);
		label.setFont(FontManager.getRunescapeSmallFont()); label.setForeground(new Color(216, 205, 181)); label.setToolTipText(title); add(label, BorderLayout.CENTER);
		value.setFont(FontManager.getRunescapeFont()); value.setForeground(new Color(0xFFD54F)); value.setToolTipText(title + ": Coming soon");
		JPanel numbers=new JPanel(new BorderLayout(0,1));numbers.setOpaque(false);numbers.add(value,BorderLayout.NORTH);
		// RuneLite bitmap fonts render HTML <small> with clipped descenders; use a separate standard-font caption.
		detail.setFont(new Font(Font.DIALOG,Font.PLAIN,10));detail.setForeground(new Color(216,205,181));detail.setVisible(false);numbers.add(detail,BorderLayout.SOUTH);add(numbers,BorderLayout.SOUTH);
		getAccessibleContext().setAccessibleName(title);
	}
	public void value(String text, String explanation) { value(text,explanation,null); }
	public void value(String text,String explanation,String caption)
	{
		if(!text.equals(value.getText())){value.setText(text);}value.setToolTipText(explanation);detail.setToolTipText(explanation);label.setToolTipText(explanation);icon.setToolTipText(explanation);setToolTipText(explanation);
		detail.setText(caption==null?"":caption);detail.setVisible(caption!=null);
	}
	/** Small explicit-accounting action; attach once because Swing mouse events do not bubble from labels. */
	public void onActivate(Runnable action)
	{
		setFocusable(true);setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		java.awt.event.MouseAdapter mouse=new java.awt.event.MouseAdapter(){@Override public void mouseClicked(java.awt.event.MouseEvent e){if(SwingUtilities.isLeftMouseButton(e)&&e.getClickCount()==1){action.run();}}};
		addActivation(this,mouse);
		getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("SPACE"),"record");getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ENTER"),"record");getActionMap().put("record",new AbstractAction(){public void actionPerformed(java.awt.event.ActionEvent e){action.run();}});
	}
	private static void addActivation(Container root,java.awt.event.MouseListener mouse)
	{
		root.addMouseListener(mouse);root.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));for(Component c:root.getComponents()){if(c instanceof Container){addActivation((Container)c,mouse);}else{c.addMouseListener(mouse);c.setCursor(root.getCursor());}}
	}
}
