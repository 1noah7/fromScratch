package com.fromscratch.bank;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

/** Content-sized readable dialogs shared by setup, corrections, history and the typed reset safeguard. */
public final class BankDialogs
{
	private BankDialogs() { }
	public static JTextArea text(String value, int columns)
	{
		JTextArea text = new JTextArea(value, 0, columns)
		{
			@Override public Dimension getPreferredSize()
			{
				Dimension preferred = super.getPreferredSize();
				// The text UI recomputes wrapped height from the actual width after resize.
				if (getWidth() > 0 && getUI() != null) { preferred.height = getUI().getPreferredSize(this).height; }
				return preferred;
			}
		}; text.setLineWrap(true); text.setWrapStyleWord(true); text.setEditable(false);
		text.setOpaque(false); text.setFont(new Font(Font.DIALOG, Font.PLAIN, 12)); text.setFocusable(false); text.setBorder(BorderFactory.createEmptyBorder(4, 2, 8, 2));
		// Give wrapping a real width before pack(); zero-row text inside a scroll pane collapsed to one line.
		int width = text.getFontMetrics(text.getFont()).charWidth('m') * columns; text.setSize(width, Short.MAX_VALUE);
		return text;
	}
	public static JDialog dialog(Component owner, String title, JComponent content, JPanel buttons, Dimension preferred)
	{
		JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(owner), title, Dialog.ModalityType.APPLICATION_MODAL);
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); dialog.setResizable(true);
		JPanel body = new JPanel(new BorderLayout(8, 12)); body.setBorder(BorderFactory.createEmptyBorder(14, 14, 12, 14));
		readable(content); readable(buttons);
		body.add(content, BorderLayout.CENTER); body.add(buttons, BorderLayout.SOUTH); dialog.setContentPane(body);
		dialog.pack(); Dimension wanted = preferred == null ? dialog.getSize() : preferred;
		Rectangle screen = owner.getGraphicsConfiguration() == null ? GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds() : owner.getGraphicsConfiguration().getBounds();
		Insets insets = owner.getGraphicsConfiguration() == null ? new Insets(0, 0, 0, 0) : Toolkit.getDefaultToolkit().getScreenInsets(owner.getGraphicsConfiguration());
		Dimension maximum = new Dimension(Math.max(200, screen.width - insets.left - insets.right - 32), Math.max(150, screen.height - insets.top - insets.bottom - 32));
		if (wanted.height > maximum.height)
		{
			// Only genuinely long explanations get a scrollbar. Action buttons always stay outside it.
			body.remove(content); JScrollPane scroll = new JScrollPane(content); scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER); body.add(scroll, BorderLayout.CENTER);
		}
		dialog.setSize(fit(wanted, maximum)); dialog.setMinimumSize(fit(new Dimension(360, preferred == null ? 170 : 320), maximum)); dialog.setLocationRelativeTo(owner);
		dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
		return dialog;
	}
	static Dimension fit(Dimension wanted, Dimension maximum) { return new Dimension(Math.min(maximum.width, Math.max(360, wanted.width)), Math.min(maximum.height, Math.max(170, wanted.height))); }
	private static void readable(Component component) { component.setFont(new Font(Font.DIALOG, Font.PLAIN, 12)); if (component instanceof Container) { for (Component child : ((Container) component).getComponents()) { readable(child); } } }
	public static boolean confirm(Component owner, String title, String description, String action, boolean typedReset)
	{
		JPanel content = new JPanel(new BorderLayout(0, 8)); JTextArea text = text(description, 42); content.add(text, BorderLayout.NORTH);
		JTextField typed = new JTextField();
		if (typedReset) { JPanel entry = new JPanel(new BorderLayout(0, 4)); entry.add(new JLabel("Type RESET to confirm"), BorderLayout.NORTH); entry.add(typed, BorderLayout.CENTER); content.add(entry, BorderLayout.CENTER); }
		JButton approve = new JButton(action), cancel = new JButton("Cancel"); JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT)); buttons.add(cancel); buttons.add(approve);
		JDialog dialog = dialog(owner, title, content, buttons, null); boolean[] accepted = {false};
		approve.setEnabled(!typedReset); if (typedReset) { typed.getDocument().addDocumentListener(changes(() -> approve.setEnabled(resetConfirmed(typed.getText())))); }
		if (typedReset) { approve.setBackground(new Color(109, 43, 43)); approve.setForeground(Color.WHITE); }
		approve.addActionListener(e -> { if (!accepted[0] && approve.isEnabled()) { accepted[0] = true; approve.setEnabled(false); dialog.dispose(); } }); cancel.addActionListener(e -> dialog.dispose());
		dialog.getRootPane().setDefaultButton(approve); if (typedReset) { SwingUtilities.invokeLater(typed::requestFocusInWindow); }
		dialog.setVisible(true); return accepted[0];
	}
	public static boolean resetConfirmed(String text) { return "RESET".equals(text); }
	public static DocumentListener changes(Runnable run) { return new DocumentListener() { public void insertUpdate(DocumentEvent e) { run.run(); } public void removeUpdate(DocumentEvent e) { run.run(); } public void changedUpdate(DocumentEvent e) { run.run(); } }; }
}
