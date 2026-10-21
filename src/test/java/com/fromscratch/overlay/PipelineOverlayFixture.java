package com.fromscratch.overlay;

import com.fromscratch.SnapshotFixtures;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/** Test-only bridge to the actual accepted overlay pass, not a second renderer implementation. */
public final class PipelineOverlayFixture
{
	private PipelineOverlayFixture() { }
	public static BufferedImage render(BankOverlayState state, int itemId, boolean present)
	{ return render(state, itemId, 1, present, itemId); }

	/** The linked ID models the normal variant returned by a verified placeholder definition. */
	public static BufferedImage render(BankOverlayState state, int itemId, int physicalQuantity, boolean present, int balanceItemId)
	{
		VirtualBankOverlay overlay = new VirtualBankOverlay(new VirtualBankOverlay.Context()
		{
			public String characterKey() { return SnapshotFixtures.CHARACTER_A; }
			public boolean bankVisible() { return true; }
			public boolean placeholder(int id) { return itemId != balanceItemId; }
			public int balanceItemId(int id) { return balanceItemId; }
			public Rectangle bankViewport() { return new Rectangle(0, 0, 100, 100); }
			public void forEachItem(Rectangle viewport, VirtualBankOverlay.ItemConsumer consumer)
			{ if (present) { consumer.accept(itemId, physicalQuantity, new Rectangle(10, 10, 36, 32)); } }
		}, state);
		BufferedImage result = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = result.createGraphics(); graphics.setColor(Color.RED); graphics.fillRect(0, 0, 100, 100);
		overlay.render(graphics); graphics.dispose(); return result;
	}
}
