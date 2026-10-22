package com.fromscratch.preview;

import com.fromscratch.bank.*;
import com.fromscratch.buyback.*;
import com.fromscratch.ge.*;
import com.fromscratch.model.ContainerSnapshot;
import com.fromscratch.playtime.PlaytimeTracker;
import com.fromscratch.wealth.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.ui.laf.RuneLiteLAF;
import net.runelite.client.util.AsyncBufferedImage;

/** Owner-side screenshots of unchanged production components. Synthetic data only; never launches RuneScape. */
public final class InterfacePreviews
{
	private static final Instant NOW = Instant.parse("2026-10-22T18:30:00Z");
	private static final UUID SESSION = UUID.fromString("00000000-0000-0000-0000-000000000010");
	private static final String CHARACTER = String.join("", Collections.nCopies(64, "a"));
	private static Path work, output;
	private static final Map<Integer, String> NAMES = new TreeMap<>();
	private static final int[] DASHBOARD_ICONS = {ItemID.PLATINUM, ItemID.CASKET, ItemID.COINS, ItemID.CRYSTAL_KEY,
		ItemID.GIANT_STOPWATCH, ItemID.ASHES, ItemID.TZHAAR_CAPE_FIRE, ItemID.BH_RUNE_POUCH};

	public static void main(String[] args) throws Exception
	{
		work = Paths.get(args[1]); output = Paths.get(args[2]); Files.createDirectories(work);
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		Set<Integer> ids = new TreeSet<>(); for (int id : DASHBOARD_ICONS) ids.add(id);
		for (BuybackEntry entry : BuybackCatalog.entries()) { ids.add(entry.getIcon()); NAMES.put(entry.getIcon(), entry.getName()); }
		ids.addAll(Arrays.asList(ItemID.NATURERUNE, ItemID.LAWRUNE, ItemID.DRAGON_BONES, ItemID.COINS));
		NAMES.put(ItemID.NATURERUNE, "Nature rune"); NAMES.put(ItemID.LAWRUNE, "Law rune");
		NAMES.put(ItemID.DRAGON_BONES, "Dragon bones"); NAMES.put(ItemID.COINS, "Coins");
		if (args[0].equals("icons"))
		{
			List<String> lines = new ArrayList<>(); for (int id : ids) lines.add(Integer.toString(id));
			Files.write(work.resolve("item-ids.txt"), lines, StandardCharsets.UTF_8); return;
		}
		Map<Integer, AsyncBufferedImage> images = new HashMap<>();
		for (int id : ids)
		{
			BufferedImage source = ImageIO.read(work.resolve("icons/" + id + ".png").toFile());
			if (source == null) throw new IOException("Invalid official sprite: " + id);
			AsyncBufferedImage image = new AsyncBufferedImage(null, source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
			Graphics2D graphics = image.createGraphics(); graphics.drawImage(source, 0, 0, null); graphics.dispose(); image.loaded(); images.put(id, image);
		}
		Files.createDirectories(output);
		SwingUtilities.invokeAndWait(() -> render(new BankIcons(images::get)));
		System.out.println("Production-interface previews rendered with sample data; no game/profile access.");
	}

	private static void render(BankIcons icons)
	{
		RuneLiteLAF.setup();
		Map<Integer, Long> legacy = new TreeMap<>();
		for (BuybackEntry entry : BuybackCatalog.entries())
		{
			if (entry.getId().equals("looting_bag_family") || entry.getId().equals("ancient_sceptre")) continue;
			entry.getRoles().forEach((role, level) -> BuybackCatalog.variants(role).entrySet().stream()
				.filter(e -> e.getValue().equals(level)).findFirst().ifPresent(e -> legacy.put(e.getKey(), 1L)));
		}
		legacy.put(ItemID.COINS, 2_000_000L); legacy.put(ItemID.NATURERUNE, 500L);
		BankRun initial = BankEngine.start(CHARACTER, null, snapshot(legacy), true, SESSION, NOW.minusSeconds(172800));
		Map<Integer, Long> physical = new TreeMap<>(legacy);
		physical.put(ItemID.COINS, 6_800_000L); physical.put(ItemID.NATURERUNE, 920L);
		physical.put(ItemID.LAWRUNE, 199L); physical.put(ItemID.DRAGON_BONES, 125L); physical.put(ItemID.LOOTING_BAG, 1L);
		Map<Integer, PriceQuote> prices = new HashMap<>();
		prices.put(ItemID.NATURERUNE, quote(ItemID.NATURERUNE, 108)); prices.put(ItemID.LAWRUNE, quote(ItemID.LAWRUNE, 117));
		prices.put(ItemID.DRAGON_BONES, quote(ItemID.DRAGON_BONES, 2460)); prices.put(ItemID.LOOTING_BAG, PriceQuote.unknown(ItemID.LOOTING_BAG));
		BankRun earned = BankEngine.observe(initial, snapshot(physical), SESSION, NOW.minusSeconds(3600), null, prices);
		BankRun run = BankEngine.buyback(earned, "fire_cape", UUID.fromString("00000000-0000-0000-0000-000000000011"), SESSION, NOW.minusSeconds(1800));
		BankValuation valuation = BankValuation.calculate(run, prices).withPeak(5_176_143L);
		BankView view = new BankView(run, new BankObservation(SESSION, 10, NOW, snapshot(physical), ""), NAMES, false, false, "", null, valuation);
		BankPanel dashboard = new BankPanel(icons); dashboard.render(view, null);
		dashboard.playtime(new PlaytimeTracker.View(CHARACTER, run.getRunId(), 48_000, null));
		paint(dashboard, 260, dashboard.getPreferredSize().height, "dashboard.png");
		Component statistics = named(dashboard, "statisticTiles");
		int overviewHeight = SwingUtilities.convertPoint(statistics, 0, statistics.getHeight(), dashboard).y + 6;
		try
		{
			BufferedImage full = ImageIO.read(output.resolve("dashboard.png").toFile());
			ImageIO.write(full.getSubimage(0, 0, full.getWidth(), overviewHeight * 2), "png", output.resolve("dashboard-overview.png").toFile());
		}
		catch (IOException ex) { throw new IllegalStateException(ex); }
		BankPanel setup = new BankPanel(icons); setup.render(new BankView(null, new BankObservation(SESSION, 1, NOW, snapshot(legacy), ""), NAMES, false, false, ""), null);
		paint(setup, 260, setup.getPreferredSize().height, "setup.png");
		BuybackView shop = new BuybackView(view, icons, entry -> { });
		paint(shop, 920, 720, "legacy-buybacks.png");
		shop.filter("", "Utility & Storage"); paint(shop, 920, 365, "legacy-buybacks-utility.png");
		JPanel owner = new JPanel(); owner.setSize(1180, 460);
		BankActions actions = actions();
		GeHistoryDialog.show(owner, actions, true, icons);
		GeHistoryDialog.show(owner, actions, false, icons);
		BankHistoryDialog.show(owner, actions, false);
		ItemHistoryDialog.show(owner, actions, ItemID.NATURERUNE, "Nature rune");
	}

	private static BankActions actions()
	{
		return new BankActions()
		{
			public void startRun(boolean attested) { } public void anchor(UUID r, ContainerSnapshot b, boolean a) { }
			public void correct(UUID r, int id, long n) { } public void reset(UUID r) { } public void retry() { }
			@Override public void exchangeHistory(boolean buy, GeHistory.Cursor cursor, java.util.function.Consumer<GeHistory.Page> callback)
			{
				List<GeTrade> rows = new ArrayList<>();
				rows.add(trade(buy, ItemID.LAWRUNE, "Law rune", 199, 23_283, false, 0));
				rows.add(trade(buy, ItemID.NATURERUNE, "Nature rune", 420, 45_360, true, 1));
				rows.add(trade(buy, ItemID.DRAGON_BONES, "Dragon bones", 125, 307_500, false, 2));
				rows.add(trade(buy, ItemID.LAWRUNE, "Law rune", 1500, 175_500, false, 3));
				rows.add(trade(buy, ItemID.NATURERUNE, "Nature rune", 1800, 194_400, false, 4));
				callback.accept(new GeHistory.Page(rows, null, ""));
				SwingUtilities.invokeLater(() -> captureDialog(buy ? "ge-purchases.png" : "ge-sales.png", 1180, 420));
			}
			@Override public void history(boolean manual, BankHistory.Cursor cursor, java.util.function.Consumer<BankHistory.Page> callback)
			{
				List<BankHistory.Row> rows = Arrays.asList(
					new BankHistory.Row(NOW.minusSeconds(90), ItemID.NATURERUNE, "Nature rune", 120),
					new BankHistory.Row(NOW.minusSeconds(330), ItemID.DRAGON_BONES, "Dragon bones", 45),
					new BankHistory.Row(NOW.minusSeconds(420), ItemID.LAWRUNE, "Law rune", -12),
					new BankHistory.Row(NOW.minusSeconds(600), ItemID.COINS, "Coins", 32_500),
					new BankHistory.Row(NOW.minusSeconds(900), ItemID.NATURERUNE, "Nature rune", -500, "Automatic — Legacy Rune Pouch", SESSION));
				callback.accept(new BankHistory.Page(rows, null, ""));
				SwingUtilities.invokeLater(() -> captureDialog("bank-history.png", 980, 400));
			}
			@Override public void itemHistory(int id, BankHistory.Cursor cursor, java.util.function.Consumer<ItemHistory.Page> callback)
			{
				callback.accept(new ItemHistory.Page(Arrays.asList(
					new ItemHistory.Row(NOW.minusSeconds(90), "Bank deposit", 120, "Physical bank change; progress change: 120. Not proof of income or spending."),
					new ItemHistory.Row(NOW.minusSeconds(300), "Bank withdrawal", -20, "Physical bank change; progress change: -20. Not proof of income or spending."),
					new ItemHistory.Row(NOW.minusSeconds(900), "Legacy Rune Pouch offset", -500, "Signed bank balance adjustment")), null, ""));
				SwingUtilities.invokeLater(() -> captureDialog("item-history.png", 1100, 375));
			}
		};
	}

	private static GeTrade trade(boolean buy, int id, String name, long quantity, long gp, boolean partial, int slot)
	{
		return new GeTrade(UUID.randomUUID(), UUID.randomUUID(), slot, id, name, buy, quantity, gp,
			buy ? null : gp / 50, buy ? "Not applicable" : "Estimated 2% average price",
			(buy ? "Bought" : "Sold") + (partial ? " (Partial)" : ""), GeOffer.Origin.OBSERVED_NEW,
			GeTrade.Evidence.FILL, NOW.minusSeconds(slot * 900L));
	}
	private static ContainerSnapshot snapshot(Map<Integer, Long> values) { return new ContainerSnapshot(values, values.size()); }
	private static PriceQuote quote(int id, long gp) { return new PriceQuote(id, gp, NOW, PriceQuote.Source.RUNELITE_GE_GUIDE, false); }
	private static void captureDialog(String file, int width, int height)
	{
		for (Window window : Window.getWindows())
		{
			if (!(window instanceof JDialog) || !window.isVisible()) continue;
			JDialog dialog = (JDialog) window;
			try
			{
				dialog.setSize(width, height);
				if (file.equals("item-history.png"))
				{
					// The real dialog permits ordinary column resizing; widen Details for this capture.
					JTable table = table(dialog); int[] widths = {175, 225, 80, 550};
					for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
				}
				layout(dialog); paint((JComponent) dialog.getContentPane(), width, height - dialog.getInsets().top - dialog.getInsets().bottom, file);
			}
			finally { dialog.dispose(); }
			return;
		}
		throw new IllegalStateException("Sample dialog did not become visible: " + file);
	}
	private static void paint(JComponent component, int width, int height, String file)
	{
		component.setSize(width, height); layout(component);
		BufferedImage image = new BufferedImage(width * 2, height * 2, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics(); graphics.scale(2, 2); component.printAll(graphics); graphics.dispose();
		try { if (!ImageIO.write(image, "png", output.resolve(file).toFile())) throw new IOException("PNG writer unavailable"); }
		catch (IOException ex) { throw new IllegalStateException("Could not capture " + file, ex); }
	}
	private static void layout(Container root)
	{
		root.doLayout(); for (Component child : root.getComponents()) if (child instanceof Container) layout((Container) child);
	}
	private static Component named(Container root, String name)
	{
		for (Component child : root.getComponents())
		{
			if (name.equals(child.getName())) return child;
			if (child instanceof Container) { Component found = named((Container) child, name); if (found != null) return found; }
		}
		return null;
	}
	private static JTable table(Container root)
	{
		for (Component child : root.getComponents())
		{
			if (child instanceof JTable) return (JTable) child;
			if (child instanceof Container) { JTable found = table((Container) child); if (found != null) return found; }
		}
		return null;
	}
}
