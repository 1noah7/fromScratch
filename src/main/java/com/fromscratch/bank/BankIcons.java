package com.fromscratch.bank;

import com.fromscratch.FromScratchPlugin;
import java.awt.Dimension;
import java.util.*;
import java.util.function.IntFunction;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;
import net.runelite.api.gameval.ItemID;

/** EDT-owned, cached genuine item sprites. Sprite completion repaints through RuneLite's supported wrapper. */
@Singleton
public final class BankIcons
{
	// Verified against the resolved RuneLite 1.13.3 gameval names and item descriptions.
	public static final int AUTOMATIC_CHANGES = ItemID.LEAGUE_BANKERS_NOTE;
	public static final int MANUAL_CORRECTIONS = ItemID.HORROR_DIARY2;
	public static final int BANK_CORRECTIONS = ItemID.PURPLE_PARTYHAT;
	private final IntFunction<AsyncBufferedImage> images;
	private final Map<Integer, AsyncBufferedImage> cached = new HashMap<>();
	private final Icon fallback = new ImageIcon(ImageUtil.loadImageResource(FromScratchPlugin.class, "icon.png"));
	@Inject public BankIcons(ItemManager items) { this(items::getImage); }
	public BankIcons(IntFunction<AsyncBufferedImage> images) { this.images = images; }
	public void apply(JLabel label, int itemId)
	{
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setPreferredSize(new Dimension(36, 32));
		label.putClientProperty("fromscratch.itemId", itemId);
		AsyncBufferedImage image;
		if (cached.containsKey(itemId)) { image = cached.get(itemId); }
		else { try { image = images.apply(itemId); } catch (RuntimeException ex) { image = null; } cached.put(itemId, image); }
		if (image == null) { label.setIcon(fallback); }
		else { image.addTo(label); }
	}
}
