package com.fromscratch.bank;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;

/** Explicit primary-action paint keeps enabled/disabled and interaction states distinct under the dark theme. */
final class BankPrimaryButton extends JButton
{
	static final Color NORMAL = new Color(38, 112, 44);
	static final Color HOVER = new Color(46, 135, 56);
	static final Color PRESSED = new Color(29, 87, 35);
	static final Color DISABLED = new Color(48, 53, 49);
	static final Color DISABLED_TEXT = new Color(174, 185, 176);
	BankPrimaryButton(String text)
	{
		super(text); setUI(new BasicButtonUI()
		{
			@Override protected void paintText(Graphics graphics, AbstractButton button, Rectangle area, String label)
			{
				graphics.setColor(button.isEnabled()?Color.WHITE:DISABLED_TEXT);
				javax.swing.plaf.basic.BasicGraphicsUtils.drawStringUnderlineCharAt(graphics,label,button.getDisplayedMnemonicIndex(),area.x+getTextShiftOffset(),area.y+graphics.getFontMetrics().getAscent()+getTextShiftOffset());
			}
		}); setBackground(NORMAL); setForeground(Color.WHITE);
		setFont(new Font(Font.DIALOG, Font.BOLD, 12)); setContentAreaFilled(false); setOpaque(false); setRolloverEnabled(true);
		setHorizontalAlignment(CENTER); setVerticalAlignment(CENTER);
		setEnabled(false);
		setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(88, 154, 92)), BorderFactory.createEmptyBorder(12, 12, 12, 12)));
	}
	@Override public Dimension getPreferredSize()
	{
		Dimension preferred = super.getPreferredSize(); return new Dimension(Math.max(186, preferred.width), Math.max(44, preferred.height));
	}
	@Override protected void paintComponent(Graphics graphics)
	{
		graphics.setColor(!isEnabled() ? DISABLED : getModel().isPressed() ? PRESSED : getModel().isRollover() ? HOVER : NORMAL);
		graphics.fillRect(0, 0, getWidth(), getHeight()); super.paintComponent(graphics);
	}
}
