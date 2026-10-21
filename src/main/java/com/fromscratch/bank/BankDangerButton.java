package com.fromscratch.bank;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;

/** Explicit painting keeps the destructive action red even under RuneLite's dark look and feel. */
final class BankDangerButton extends JButton
{
	static final Color NORMAL = new Color(155, 43, 43);
	static final Color HOVER = new Color(184, 53, 53);
	static final Color PRESSED = new Color(119, 30, 30);
	BankDangerButton(String text)
	{
		super(text); setUI(new BasicButtonUI()); setBackground(NORMAL); setForeground(Color.WHITE);
		setContentAreaFilled(false); setOpaque(false); setRolloverEnabled(true);
		setHorizontalAlignment(CENTER); setVerticalAlignment(CENTER);
		setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(225, 104, 104)), BorderFactory.createEmptyBorder(9, 12, 9, 12)));
	}
	@Override protected void paintComponent(Graphics graphics)
	{
		graphics.setColor(!isEnabled() ? new Color(82, 48, 48) : getModel().isPressed() ? PRESSED : getModel().isRollover() ? HOVER : NORMAL);
		graphics.fillRect(0, 0, getWidth(), getHeight()); super.paintComponent(graphics);
	}
}
