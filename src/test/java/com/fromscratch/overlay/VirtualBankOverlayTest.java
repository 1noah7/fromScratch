package com.fromscratch.overlay;

import com.fromscratch.FromScratchConfig;
import com.fromscratch.SnapshotFixtures;
import com.fromscratch.model.VirtualOwnershipTest;
import com.fromscratch.ui.SetupView;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.gameval.ItemID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/** Exercise the real overlay pass with synthetic widget observations, not just its final painter. */
public class VirtualBankOverlayTest
{
	private final View view = new View();
	private final BankOverlayState state = new BankOverlayState();
	private final VirtualBankOverlay overlay = new VirtualBankOverlay(view, state);
	private final BufferedImage image = new BufferedImage(180, 180, BufferedImage.TYPE_INT_ARGB);
	private Graphics2D graphics;

	@Before public void setUp()
	{
		graphics = image.createGraphics();
		clearFrame();
		state.enable(); com.fromscratch.overlay.LegacyOverlayFixture.update(state, active());
	}
	@After public void tearDown() { graphics.dispose(); }

	private static SetupView active()
	{
		return new SetupView(SetupView.Mode.ACTIVE, "Active",
			SnapshotFixtures.active(SnapshotFixtures.CHARACTER_A, 100).withOwnership(VirtualOwnershipTest.fixture()), null, null);
	}
	private void clearFrame()
	{
		graphics.setClip(null); graphics.setColor(Color.WHITE); graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
	}
	private void locked(int y) { view.add(ItemID.COINS, new Rectangle(30, y, 36, 32)); }
	private void assertTinted(int x, int y) { assertNotEquals(Color.WHITE.getRGB(), image.getRGB(x, y)); }
	private void assertUntouched(int x, int y) { assertEquals(Color.WHITE.getRGB(), image.getRGB(x, y)); }

	@Test public void firstFullyVisibleRowIsShaded()
	{
		locked(40); overlay.render(graphics);
		assertTinted(30, 40); assertTinted(30, 71); assertUntouched(30, 39);
	}
	@Test public void partiallyVisibleTopRowIsShadedEvenThoughItsOriginIsOutsideViewport()
	{
		locked(25);
		assertFalse(view.viewport.contains(30, 25));
		assertTrue(view.viewport.intersects(view.items.get(0).bounds));
		overlay.render(graphics);
		assertTinted(30, 40); assertTinted(30, 56); assertUntouched(30, 39);
	}
	@Test public void partiallyVisibleBottomRowIsShaded()
	{
		locked(105); overlay.render(graphics);
		assertTinted(30, 105); assertTinted(30, 119); assertUntouched(30, 120);
	}
	@Test public void topBottomAndMiddleRowsHaveIdenticalTreatment()
	{
		locked(25); locked(65); locked(105); overlay.render(graphics);
		assertTinted(30, 40);
		assertEquals(image.getRGB(30, 40), image.getRGB(30, 65));
		assertEquals(image.getRGB(30, 40), image.getRGB(30, 119));
	}
	@Test public void nothingPaintsOutsideContentViewportIncludingTabsAndSearchArea()
	{
		// Include fully outside, edge-touching, partial-left/right and oversize observations.
		locked(1); locked(8); locked(25); locked(105); locked(120);
		view.add(ItemID.COINS, new Rectangle(5, 65, 36, 32));
		view.add(ItemID.COINS, new Rectangle(130, 65, 36, 32));
		overlay.render(graphics);
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				if (!view.viewport.contains(x, y)) { assertUntouched(x, y); }
			}
		}
	}
	@Test public void partiallyVisibleBottomRowDoesNotMovePurpleLabelIntoVisibleFragment()
	{
		locked(105); overlay.render(graphics);
		assertEquals(0, purplePixels());
		// The same item's normal label becomes visible when its real bounds move up.
		clearFrame(); view.items.clear(); locked(70); overlay.render(graphics);
		assertTrue(purplePixels() > 0);
	}
	@Test public void topFragmentKeepsItsPurpleCounterAtTheOriginalItemCoordinates()
	{
		locked(25); overlay.render(graphics);
		BufferedImage expected = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D expectedGraphics = expected.createGraphics();
		try
		{
			expectedGraphics.setColor(Color.WHITE); expectedGraphics.fillRect(0, 0, image.getWidth(), image.getHeight());
			expectedGraphics.setClip(view.viewport);
			BankItemPainter.paint(expectedGraphics, new Rectangle(30, 25, 36, 32), 0, "0", state.getSettings());
			assertTrue(purplePixels() > 0);
			for (int y = 40; y < 57; y++)
			{
				for (int x = 30; x < 66; x++) { assertEquals(expected.getRGB(x, y), image.getRGB(x, y)); }
			}
		}
		finally { expectedGraphics.dispose(); }
	}
	@Test public void resizedViewportImmediatelyChangesClipping()
	{
		locked(105); overlay.render(graphics); assertTinted(30, 110);
		clearFrame(); view.viewport = new Rectangle(20, 40, 120, 60); overlay.render(graphics);
		assertUntouched(30, 110);
		clearFrame(); view.viewport = new Rectangle(20, 40, 120, 110); overlay.render(graphics);
		assertTinted(30, 136);
	}
	@Test public void repeatedScrollingUsesCurrentBoundsAndNeverLeaksOutsideViewport()
	{
		BankOverlayState.Projection original = state.getProjection();
		for (int offset = -36; offset <= 120; offset += 3)
		{
			clearFrame(); view.items.clear(); locked(offset); overlay.render(graphics);
			for (int y = 0; y < image.getHeight(); y++)
			{
				boolean visible = y >= offset && y < offset + 32 && y >= 40 && y < 120;
				assertEquals(visible, image.getRGB(30, y) != Color.WHITE.getRGB());
			}
		}
		assertSame(original, state.getProjection());
	}
	@Test public void tabSearchPositionsAndEmptyResultsUseCurrentWidgets()
	{
		locked(40); overlay.render(graphics); assertTinted(30, 40);
		clearFrame(); view.items.clear(); view.add(ItemID.COINS, new Rectangle(100, 80, 36, 32)); overlay.render(graphics);
		assertUntouched(30, 40); assertTinted(100, 80);
		clearFrame(); view.items.clear(); overlay.render(graphics);
		assertUntouched(30, 40); assertUntouched(100, 80);
	}
	@Test public void bankClosureDoesNotVisitOrShadeWidgets()
	{
		locked(25); view.visible = false; overlay.render(graphics);
		assertEquals(0, view.visits); assertUntouched(30, 40);
	}
	@Test public void disableAndReenableDoNotAccumulateShading()
	{
		locked(25); overlay.render(graphics); int once = image.getRGB(30, 40);
		clearFrame(); state.disable(); overlay.render(graphics); assertUntouched(30, 40);
		state.enable(); com.fromscratch.overlay.LegacyOverlayFixture.update(state, active()); overlay.render(graphics);
		assertEquals(once, image.getRGB(30, 40));
	}
	@Test public void positiveBankOwnershipKeepsItsPartiallyVisibleSpriteNormal()
	{
		view.add(ItemID.SHARK, new Rectangle(30, 25, 36, 32)); overlay.render(graphics);
		assertUntouched(30, 40);
		assertTrue(purplePixels() > 0);
	}
	@Test public void invalidViewportDoesNotVisitOrPaintItems()
	{
		locked(25); view.viewport = null; overlay.render(graphics);
		view.viewport = new Rectangle(20, 40, 0, 80); overlay.render(graphics);
		assertEquals(0, view.visits); assertUntouched(30, 40);
	}
	@Test public void existingNonRectangularGraphicsClipIsIntersectedAndRestored()
	{
		locked(25); Shape clip = new Ellipse2D.Double(20, 40, 60, 40); graphics.setClip(clip);
		// Java2D normalizes the requested ellipse into its own clip path. Compare the
		// actual saved graphics state, rather than a second conversion of the input shape.
		Shape original = graphics.getClip();
		overlay.render(graphics);
		Area difference = new Area(original); difference.exclusiveOr(new Area(graphics.getClip()));
		assertTrue(difference.isEmpty());
		assertUntouched(30, 40); assertTinted(45, 50);
	}
	@Test public void offscreenWidgetsDoNotTriggerDefinitionsAndVisibleDefinitionsRemainCached()
	{
		view.add(ItemID.SHARK, new Rectangle(30, -40, 36, 32)); locked(25);
		for (int i = 0; i < 5; i++) { clearFrame(); overlay.render(graphics); }
		assertEquals(1, view.definitionReads);
		overlay.clearCache(); clearFrame(); overlay.render(graphics);
		assertEquals(2, view.definitionReads);
	}

	private int purplePixels()
	{
		int count = 0;
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++) { if (image.getRGB(x, y) == BankOverlayState.DEFAULT_COLOR.getRGB()) { count++; } }
		}
		return count;
	}

	/** Represents current widget bounds; deliberately includes origins omitted by RuneLite's callback list. */
	private static final class View implements VirtualBankOverlay.Context
	{
		private Rectangle viewport = new Rectangle(20, 40, 120, 80);
		private boolean visible = true;
		private int visits;
		private int definitionReads;
		private final List<Item> items = new ArrayList<>();
		public String characterKey() { return SnapshotFixtures.CHARACTER_A; }
		public boolean bankVisible() { return visible; }
		public boolean placeholder(int itemId) { definitionReads++; return false; }
		public Rectangle bankViewport() { return viewport; }
		public void forEachItem(Rectangle clip, VirtualBankOverlay.ItemConsumer consumer)
		{
			visits++;
			for (Item item : items) { consumer.accept(item.id, 1, item.bounds); }
		}
		private void add(int id, Rectangle bounds) { items.add(new Item(id, bounds)); }
	}
	private static final class Item
	{
		private final int id;
		private final Rectangle bounds;
		private Item(int id, Rectangle bounds) { this.id = id; this.bounds = bounds; }
	}
}
