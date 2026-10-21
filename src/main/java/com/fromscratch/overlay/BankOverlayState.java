package com.fromscratch.overlay;

import com.fromscratch.FromScratchConfig;
import com.fromscratch.model.RunRecord;
import com.fromscratch.model.RunStatus;
import com.fromscratch.model.VirtualLocation;
import com.fromscratch.model.VirtualOwnership;
import com.fromscratch.ui.SetupView;
import java.awt.Color;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Singleton;
import lombok.Getter;

/** Immutable render projections. No game containers, disk access or accounting is used by painters. */
@Singleton
public final class BankOverlayState
{
	public static final Color DEFAULT_COLOR = new Color(0xB450FF);
	@Getter private volatile boolean enabled;
	@Getter private volatile Projection projection;
	@Getter private volatile Settings settings = new Settings(true, true, DEFAULT_COLOR);

	public void enable() { enabled = true; }
	public void disable() { enabled = false; projection = null; }
	public void clear() { projection = null; }
	public void configure(FromScratchConfig config)
	{
		settings = new Settings(config.bankVirtualQuantities(), config.lockedBankItems(), config.virtualQuantityColor());
	}

	public void update(SetupView view)
	{
		RunRecord run = view.getSaved();
		if (!enabled || view.getMode() != SetupView.Mode.ACTIVE || run == null || run.getState().getStatus() != RunStatus.ACTIVE)
		{
			projection = null;
			return;
		}
		projection = new Projection(run);
	}

	@Getter
	public static final class Projection
	{
		private final String characterKey;
		private final VirtualOwnership ownership;
		private final Map<Integer, String> labels;
		private Projection(RunRecord run)
		{
			characterKey = run.getState().getCharacterKey();
			ownership = run.getOwnership();
			Map<Integer, String> labels = new HashMap<>();
			ownership.getItems().forEach((id, item) ->
			{
				long bank = item.at(VirtualLocation.BANK);
				if (bank > 0) { labels.put(id, VirtualQuantityFormatter.format(bank)); }
			});
			this.labels = Collections.unmodifiableMap(labels);
		}
		public String label(int id) { return labels.getOrDefault(id, "0"); }
	}

	@Getter
	public static final class Settings
	{
		private final boolean quantities;
		private final boolean dimLocked;
		private final Color color;
		public Settings(boolean quantities, boolean dimLocked, Color color)
		{
			this.quantities = quantities;
			this.dimLocked = dimLocked;
			this.color = color == null ? DEFAULT_COLOR : new Color(color.getRGB());
		}
	}
}
