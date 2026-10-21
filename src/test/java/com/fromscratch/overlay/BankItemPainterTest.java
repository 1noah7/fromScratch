package com.fromscratch.overlay;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import com.fromscratch.model.VirtualOwnershipTest;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.ui.FontManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/** Real Java2D painting against synthetic pixels; no game client, item sprite or account. */
public class BankItemPainterTest
{
	private BufferedImage image;
	private Graphics2D graphics;
	private final Rectangle item = new Rectangle(20, 20, 36, 32);
	@Before public void setUp()
	{
		image = new BufferedImage(160, 160, BufferedImage.TYPE_INT_ARGB);
		graphics = image.createGraphics();
		graphics.setColor(Color.WHITE); graphics.fillRect(0, 0, 160, 160);
	}
	@After public void tearDown() { graphics.dispose(); }
	private void paint(long quantity, boolean numbers, boolean dim, Color color)
	{
		BankItemPainter.paint(graphics, item, quantity, VirtualQuantityFormatter.format(quantity), new BankOverlayState.Settings(numbers, dim, color));
	}
	@Test public void zeroQuantityHasExactCompactLabel() { assertEquals("0", VirtualQuantityFormatter.format(0)); }
	@Test public void compactLabelsCoverThousandsMillionsAndBillions()
	{
		assertEquals("999", VirtualQuantityFormatter.format(999));
		assertEquals("1.25K", VirtualQuantityFormatter.format(1250));
		assertEquals("1.25M", VirtualQuantityFormatter.format(1250000));
		assertEquals("1.2B", VirtualQuantityFormatter.format(1200000000));
	}
	@Test public void largeLongLabelsKeepCorrectMagnitudeWithoutOverflow()
	{
		assertEquals("9.22E", VirtualQuantityFormatter.format(Long.MAX_VALUE));
		assertEquals("999K", VirtualQuantityFormatter.format(999999));
	}
	@Test(expected = IllegalArgumentException.class) public void negativeQuantityCannotBeFormatted() { VirtualQuantityFormatter.format(-1); }
	@Test public void textIsRightAlignedToCurrentItemBounds()
	{
		graphics.setFont(FontManager.getRunescapeSmallFont());
		assertEquals(item.x + item.width - 2, BankItemPainter.textX(item, graphics.getFontMetrics(), "75") + graphics.getFontMetrics().stringWidth("75"));
		assertTrue(BankItemPainter.textY(item, graphics.getFontMetrics()) > item.y + 20);
	}
	@Test public void scrollingAndSearchRepositioningUseActualBounds()
	{
		graphics.setFont(FontManager.getRunescapeSmallFont());
		Rectangle moved = new Rectangle(item.x + 35, item.y + 70, item.width, item.height);
		assertEquals(35, BankItemPainter.textX(moved, graphics.getFontMetrics(), "0") - BankItemPainter.textX(item, graphics.getFontMetrics(), "0"));
		assertEquals(70, BankItemPainter.textY(moved, graphics.getFontMetrics()) - BankItemPainter.textY(item, graphics.getFontMetrics()));
	}
	@Test public void ordinaryQuantityGlyphsRemainReadableThroughTint()
	{
		// Synthetic ordinary labels use the same small font and standard quantity colors.
		// The overlay may fade their colors, but must retain glyph shape and contrast.
		Color background = new Color(64, 56, 48);
		for (Color color : new Color[]{Color.YELLOW, Color.GREEN, Color.WHITE})
		{
			graphics.setColor(background); graphics.fillRect(item.x, item.y, item.width, item.height);
			graphics.setFont(FontManager.getRunescapeSmallFont());
			graphics.setColor(Color.BLACK); graphics.drawString("125K", item.x + 1, item.y + 11);
			graphics.setColor(color); graphics.drawString("125K", item.x, item.y + 10);
			int[] original = image.getRGB(item.x, item.y, item.width, 12, null, 0, item.width);
			paint(0, true, true, BankOverlayState.DEFAULT_COLOR);
			int glyphPixels = 0;
			for (int y = 0; y < 12; y++)
			{
				for (int x = 0; x < item.width; x++)
				{
					if (original[y * item.width + x] != color.getRGB()) { continue; }
					Color faded = new Color(image.getRGB(item.x + x, item.y + y));
					assertTrue("Quantity glyph contrast must remain readable", faded.getGreen() > 160);
					glyphPixels++;
				}
			}
			assertTrue("The fixture must contain ordinary quantity glyphs", glyphPixels > 0);
		}
	}
	@Test public void fullItemReceivesUniformTintWithoutUpperLowerSplit()
	{
		paint(0, false, true, BankOverlayState.DEFAULT_COLOR);
		int tinted = image.getRGB(item.x, item.y);
		assertNotEquals(Color.WHITE.getRGB(), tinted);
		for (int y = item.y; y < item.y + item.height; y++)
		{
			for (int x = item.x; x < item.x + item.width; x++) { assertEquals(tinted, image.getRGB(x, y)); }
		}
	}
	@Test public void neutralTintMutesItemsAndPreservesQuantityContrast()
	{
		graphics.setColor(Color.BLACK); graphics.fillRect(item.x, item.y + 16, item.width, 16);
		paint(0, false, true, BankOverlayState.DEFAULT_COLOR);
		Color light = new Color(image.getRGB(item.x, item.y));
		Color dark = new Color(image.getRGB(item.x, item.y + 16));
		assertEquals(light.getRed(), light.getGreen()); assertEquals(light.getGreen(), light.getBlue());
		assertEquals(dark.getRed(), dark.getGreen()); assertEquals(dark.getGreen(), dark.getBlue());
		assertTrue("Light glyphs must remain recognizable", light.getRed() > 160);
		assertTrue("Stronger tint must visibly mute highlights", light.getRed() < 190);
		assertTrue("Dark outlines must remain dark", dark.getRed() < 50);
		assertTrue("Retain at least half of the original contrast", light.getRed() - dark.getRed() >= 128);
	}
	@Test public void tallerItemIsTintedFromItsFirstRowThroughItsLastRow()
	{
		Rectangle tall = new Rectangle(60, 10, 36, 64);
		BankItemPainter.paint(graphics, tall, 0, "0", new BankOverlayState.Settings(false, true, Color.MAGENTA));
		assertNotEquals(Color.WHITE.getRGB(), image.getRGB(tall.x, tall.y));
		assertEquals(image.getRGB(tall.x, tall.y), image.getRGB(tall.x, tall.y + tall.height - 1));
		assertEquals(Color.WHITE.getRGB(), image.getRGB(tall.x, tall.y - 1));
	}
	@Test public void darkerTintKeepsBankBackgroundCloseInsteadOfCreatingABrightGrayTile()
	{
		Color bankBackground = new Color(64, 56, 48);
		graphics.setColor(bankBackground); graphics.fillRect(item.x, item.y, item.width, item.height);
		paint(0, false, true, BankOverlayState.DEFAULT_COLOR);
		Color tinted = new Color(image.getRGB(item.x, item.y));
		assertTrue(Math.abs(tinted.getRed() - bankBackground.getRed()) <= 16);
		assertTrue(Math.abs(tinted.getGreen() - bankBackground.getGreen()) <= 16);
		assertTrue(Math.abs(tinted.getBlue() - bankBackground.getBlue()) <= 16);
	}
	@Test public void tintTracksMovedTabSearchAndDragBoundsWithoutPaintingAnOldPosition()
	{
		Rectangle moved = new Rectangle(95, 75, item.width, item.height);
		BankItemPainter.paint(graphics, moved, 0, "0", new BankOverlayState.Settings(false, true, Color.MAGENTA));
		assertEquals(Color.WHITE.getRGB(), image.getRGB(item.x, item.y));
		assertNotEquals(Color.WHITE.getRGB(), image.getRGB(moved.x, moved.y));
		assertEquals(image.getRGB(moved.x, moved.y), image.getRGB(moved.x, moved.y + moved.height - 1));
		assertEquals(Color.WHITE.getRGB(), image.getRGB(moved.x - 1, moved.y));
		assertEquals(Color.WHITE.getRGB(), image.getRGB(moved.x + moved.width, moved.y));
	}
	@Test public void scrollingClipsWholeItemTintAtBothViewportEdges()
	{
		Rectangle viewport = new Rectangle(20, 30, 36, 10); graphics.setClip(viewport);
		paint(0, false, true, Color.MAGENTA);
		assertNotEquals(Color.WHITE.getRGB(), image.getRGB(item.x, viewport.y));
		assertEquals(image.getRGB(item.x, viewport.y), image.getRGB(item.x, viewport.y + viewport.height - 1));
		assertEquals(Color.WHITE.getRGB(), image.getRGB(item.x, viewport.y - 1));
		assertEquals(Color.WHITE.getRGB(), image.getRGB(item.x, viewport.y + viewport.height));
		assertEquals(viewport, graphics.getClipBounds());
	}
	@Test public void purpleCounterIsDrawnAfterTintWithoutBeingFaded()
	{
		paint(0, true, true, BankOverlayState.DEFAULT_COLOR);
		int purplePixels = 0;
		for (int y = item.y + 18; y < item.y + item.height; y++)
		{
			for (int x = item.x + 20; x < item.x + item.width; x++)
			{
				if (image.getRGB(x, y) == BankOverlayState.DEFAULT_COLOR.getRGB()) { purplePixels++; }
			}
		}
		assertTrue(purplePixels > 0);
	}
	@Test public void disablingLockEffectPreservesTheWholeItemIncludingItsTop()
	{
		paint(0, false, false, BankOverlayState.DEFAULT_COLOR);
		for (int y = item.y; y < item.y + item.height; y++)
		{
			for (int x = item.x; x < item.x + item.width; x++) { assertEquals(Color.WHITE.getRGB(), image.getRGB(x, y)); }
		}
	}
	@Test public void positiveOwnershipPreservesEverySpritePixel()
	{
		paint(75, false, true, BankOverlayState.DEFAULT_COLOR);
		for (int y = item.y; y < item.y + item.height; y++)
		{
			for (int x = item.x; x < item.x + item.width; x++) { assertEquals(Color.WHITE.getRGB(), image.getRGB(x, y)); }
		}
	}
	@Test public void fractionalScaleKeepsTintAlignedWithFullItemBounds()
	{
		graphics.scale(1.5, 1.5); paint(0, false, true, Color.MAGENTA);
		assertNotEquals(Color.WHITE.getRGB(), image.getRGB(31, 31));
		assertEquals(image.getRGB(31, 31), image.getRGB(31, 76));
		assertEquals(Color.WHITE.getRGB(), image.getRGB(29, 31));
		assertEquals(Color.WHITE.getRGB(), image.getRGB(31, 79));
	}
	@Test public void saturatedSpritePixelsAreFadedEquallyAtTheTopAndBottom()
	{
		graphics.setColor(Color.RED); graphics.fillRect(item.x + 8, item.y, 12, item.height);
		paint(0, false, true, BankOverlayState.DEFAULT_COLOR);
		Color top = new Color(image.getRGB(item.x + 10, item.y));
		assertEquals(top.getRGB(), image.getRGB(item.x + 10, item.y + item.height - 1));
		assertTrue("The top must also lose saturation", top.getGreen() > 0 && top.getBlue() > 0);
		assertTrue(top.getRed() - top.getGreen() < 190);
	}
	@Test public void zeroOwnedSpriteAreaIsDimmed()
	{
		paint(0, false, true, BankOverlayState.DEFAULT_COLOR);
		assertNotEquals(Color.WHITE.getRGB(), image.getRGB(item.x + 1, item.y + 15));
	}
	@Test public void positiveBankOwnershipLeavesSpriteAreaNormal()
	{
		paint(1, false, true, BankOverlayState.DEFAULT_COLOR);
		assertEquals(Color.WHITE.getRGB(), image.getRGB(item.x + 1, item.y + 15));
	}
	@Test public void customQuantityColorIsActuallyDrawnAtBottomRight()
	{
		paint(0, true, false, Color.MAGENTA);
		int pixels = 0;
		for (int y = item.y + 18; y < item.y + item.height; y++)
		{
			for (int x = item.x + 20; x < item.x + item.width; x++) { if (image.getRGB(x, y) == Color.MAGENTA.getRGB()) { pixels++; } }
		}
		assertTrue(pixels > 0);
	}
	@Test public void parentViewportClipIsPreserved()
	{
		Rectangle clip = new Rectangle(20, 20, 20, 25); graphics.setClip(clip);
		paint(0, true, true, Color.MAGENTA);
		assertEquals(clip, graphics.getClipBounds());
		assertEquals(Color.WHITE.getRGB(), image.getRGB(41, 35));
		assertEquals(Color.WHITE.getRGB(), image.getRGB(25, 46));
	}
	@Test public void completelyOffscreenItemPaintsNothing()
	{
		graphics.setClip(0, 0, 10, 10); paint(0, true, true, Color.MAGENTA);
		assertEquals(Color.WHITE.getRGB(), image.getRGB(25, 40));
	}
	@Test public void graphicsFontPaintAndClipAreRestored()
	{
		Font font = graphics.getFont(); graphics.setColor(Color.GREEN); graphics.setClip(0, 0, 100, 100);
		paint(0, true, true, Color.MAGENTA);
		assertEquals(font, graphics.getFont()); assertEquals(Color.GREEN, graphics.getColor());
		assertEquals(new Rectangle(0, 0, 100, 100), graphics.getClipBounds());
	}
	@Test public void uiScaleUsesTheGraphicsTransformOnce()
	{
		graphics.scale(2, 2); paint(0, false, true, Color.MAGENTA);
		assertNotEquals(Color.WHITE.getRGB(), image.getRGB(45, 75));
		assertEquals(Color.WHITE.getRGB(), image.getRGB(25, 40));
	}
	@Test public void bothDisabledFeaturesLeavePixelsUnchanged()
	{
		paint(0, false, false, Color.MAGENTA);
		assertEquals(Color.WHITE.getRGB(), image.getRGB(25, 40));
	}
	@Test public void onlyNormalBankItemComponentsAreEligible()
	{
		assertTrue(BankItemPainter.eligible(InterfaceID.Bankmain.ITEMS, ItemID.SHARK, 1));
		for (int id : new int[]{InterfaceID.Inventory.ITEMS, InterfaceID.Bankside.ITEMS, InterfaceID.SharedBank.ITEMS, InterfaceID.Wornitems.SLOT0})
		{
			assertFalse(BankItemPainter.eligible(id, ItemID.SHARK, 1));
		}
	}
	@Test public void emptySlotsAndFillersAreExcludedButPlaceholdersAreEligible()
	{
		assertFalse(BankItemPainter.eligible(InterfaceID.Bankmain.ITEMS, -1, 0));
		assertFalse(BankItemPainter.eligible(InterfaceID.Bankmain.ITEMS, ItemID.BANK_FILLER, 1));
		assertFalse(BankItemPainter.eligible(InterfaceID.Bankmain.ITEMS, ItemID.BLANKOBJECT, 1));
		assertTrue(BankItemPainter.eligible(InterfaceID.Bankmain.ITEMS, ItemID.SHARK, 0));
	}
	@Test public void placeholdersDisplayZeroEvenWithVirtualCopiesElsewhere()
	{
		assertEquals(0, BankItemPainter.quantity(VirtualOwnershipTest.fixture(), ItemID.SHARK, true));
		assertEquals(75, BankItemPainter.quantity(VirtualOwnershipTest.fixture(), ItemID.SHARK, false));
	}
}
