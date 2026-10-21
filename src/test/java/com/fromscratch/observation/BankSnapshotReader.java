package com.fromscratch.observation;

import com.fromscratch.model.ContainerSnapshot;
import com.fromscratch.model.LegacySnapshot;
import com.fromscratch.model.SnapshotLocation;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;

/**
 * Client-thread setup collector. Personal containers may be observed before the
 * bank opens. They are usable for at most one minute in this uninterrupted setup
 * session, unless a relevant container event invalidates them earlier. The bank
 * itself must always be read from the mounted normal bank at capture/confirmation.
 */
public final class BankSnapshotReader
{
	static final Duration MAX_OBSERVATION_AGE = Duration.ofSeconds(60);
	private final Access access;
	private final Clock clock;
	private final EnumMap<SnapshotLocation, Observation> observations = new EnumMap<>(SnapshotLocation.class);
	private final EnumMap<SnapshotLocation, Integer> sizes = new EnumMap<>(SnapshotLocation.class);
	private boolean awaitingTick = true;
	private boolean mounted;
	private boolean visible;
	private boolean pin;
	private String predicate = "NOT_OBSERVED";
	private ContainerSnapshot lastBank;

	public BankSnapshotReader(Access access) { this(access, Clock.systemUTC()); }
	BankSnapshotReader(Access access, Clock clock) { this.access = access; this.clock = clock; }

	/** Logout, identity change, cancellation and a new Begin discard only ephemeral observations. */
	public void reset()
	{
		observations.clear();
		sizes.clear();
		lastBank = null;
		predicate = "NOT_OBSERVED";
		interfaceChanged();
	}

	public void interfaceChanged()
	{
		awaitingTick = true;
		observations.remove(SnapshotLocation.BANK);
	}
	public void gameTick() { awaitingTick = false; }

	/**
	 * Invalidate a changed location before reading its event payload. An event may
	 * carry valid personal data even if a subsequent getter is unavailable. Wait
	 * for a tick before combining related transfer updates into a preview.
	 */
	public void containerChanged(int inventoryId, List<ObservedItem> items)
	{
		SnapshotLocation location = location(inventoryId);
		if (location == null) { return; }
		awaitingTick = true;
		observations.remove(location);
		if (location != SnapshotLocation.BANK) { observe(location, items, clock.instant()); }
		else if (items != null)
		{
			try { bankChanged(SnapshotAssembler.container(items)); }
			catch (IllegalStateException | IllegalArgumentException | ArithmeticException ex) { invalidatePersonal(); }
		}
		else if (lastBank != null) { invalidatePersonal(); }
	}

	public CaptureProgress progress()
	{
		EnumMap<SnapshotLocation, ContainerSnapshot> valid = new EnumMap<>(SnapshotLocation.class);
		Instant now = clock.instant();
		for (SnapshotLocation location : SnapshotLocation.values())
		{
			Observation observation = observations.get(location);
			if (observation != null && fresh(observation, now)
				&& (location != SnapshotLocation.BANK || !awaitingTick)) { valid.put(location, observation.contents); }
		}
		return new CaptureProgress(valid);
	}

	public LegacySnapshot capture()
	{
		Instant now = clock.instant();
		mounted = access.isInterfaceOpen(InterfaceID.BANKMAIN);
		visible = access.isVisible(InterfaceID.Bankmain.ITEMS_CONTAINER);
		pin = access.isInterfaceOpen(InterfaceID.BANKPIN_KEYPAD) || access.isInterfaceOpen(InterfaceID.BANKPIN_SETTINGS);
		String bankProblem = bankProblem();
		observations.remove(SnapshotLocation.BANK); // never reuse a closed bank or a failed read
		if (bankProblem == null)
		{
			read(SnapshotLocation.BANK, InventoryID.BANK, now);
			Observation bank = observations.get(SnapshotLocation.BANK);
			if (bank != null) { bankChanged(bank.contents); }
		}
		else { sizes.remove(SnapshotLocation.BANK); }

		// Observe both even when the bank or another personal container is missing.
		read(SnapshotLocation.INVENTORY, InventoryID.INV, now);
		read(SnapshotLocation.EQUIPMENT, InventoryID.WORN, now);
		EnumMap<SnapshotLocation, ContainerSnapshot> valid = new EnumMap<>(SnapshotLocation.class);
		for (SnapshotLocation location : SnapshotLocation.values())
		{
			Observation observation = observations.get(location);
			if (observation != null && fresh(observation, now)) { valid.put(location, observation.contents); }
		}
		CaptureProgress progress = new CaptureProgress(valid);
		if (bankProblem != null) { throw new IllegalStateException(bankProblem); }
		for (SnapshotLocation location : SnapshotLocation.values())
		{
			if (!progress.ready(location))
			{
				predicate = location + "_UNAVAILABLE_OR_STALE";
				String name = location == SnapshotLocation.BANK ? "Bank" : location == SnapshotLocation.INVENTORY ? "Inventory" : "Equipment";
				throw new IllegalStateException(name + " container data is unavailable, incomplete or stale. "
					+ (location == SnapshotLocation.BANK ? "Keep the normal bank open or close and reopen it."
						: "Close the bank and open your normal " + name.toLowerCase(java.util.Locale.ROOT)
							+ " interface, then reopen the bank within 60 seconds without moving items."));
			}
		}
		predicate = "READY";
		return new LegacySnapshot(now, valid);
	}

	private String bankProblem()
	{
		if (access.isInterfaceOpen(InterfaceID.BANKPIN_KEYPAD))
		{
			predicate = "BANK_PIN";
			return "Enter your Bank PIN manually, then return to the normal bank. Capture will retry automatically.";
		}
		if (access.isInterfaceOpen(InterfaceID.BANKPIN_SETTINGS))
		{
			predicate = "BANK_PIN_SETTINGS";
			return "Close Bank PIN settings and return to the normal bank. Capture will retry automatically.";
		}
		if (!mounted)
		{
			predicate = "BANK_NOT_MOUNTED";
			return "Open the normal bank to continue. Inventory and equipment observations remain valid for up to 60 seconds.";
		}
		if (!visible)
		{
			predicate = "BANK_ITEMS_HIDDEN";
			return "The bank item interface is not ready. Return to the normal bank items view; capture will retry automatically.";
		}
		if (awaitingTick)
		{
			predicate = "SETTLING";
			return "Container updates or the bank interface are settling. Keep the bank open; capture will retry on the next game tick.";
		}
		return null;
	}

	private void read(SnapshotLocation location, int id, Instant now)
	{
		try { observe(location, access.items(id), now); }
		catch (IllegalStateException | IllegalArgumentException | ArithmeticException ex)
		{
			observations.remove(location);
			sizes.put(location, -2);
		}
	}

	private void observe(SnapshotLocation location, List<ObservedItem> items, Instant now)
	{
		sizes.put(location, items == null ? -1 : items.size());
		if (items == null) { return; } // unknown does not mean empty, nor refresh cache age
		try { observations.put(location, new Observation(SnapshotAssembler.container(items), now)); }
		catch (IllegalStateException | IllegalArgumentException | ArithmeticException ex)
		{
			observations.remove(location);
			sizes.put(location, -2);
		}
	}

	private void bankChanged(ContainerSnapshot bank)
	{
		// A changed bank with an unavailable personal container could otherwise
		// combine opposite sides of a transfer. Require new personal observations.
		if (lastBank != null && !lastBank.equals(bank)) { invalidatePersonal(); }
		lastBank = bank;
	}

	private void invalidatePersonal()
	{
		observations.remove(SnapshotLocation.INVENTORY);
		observations.remove(SnapshotLocation.EQUIPMENT);
	}

	private static boolean fresh(Observation observation, Instant now)
	{
		Duration age = Duration.between(observation.time, now);
		return !age.isNegative() && age.compareTo(MAX_OBSERVATION_AGE) <= 0;
	}

	/** Only availability, sizes and ages: never item IDs, quantities or character identity. */
	public String diagnostics()
	{
		return "bankMounted=" + mounted + " bankVisible=" + visible + " pin=" + pin
			+ " settling=" + awaitingTick + " predicate=" + predicate
			+ " bank=" + diagnostic(SnapshotLocation.BANK)
			+ " inventory=" + diagnostic(SnapshotLocation.INVENTORY)
			+ " equipment=" + diagnostic(SnapshotLocation.EQUIPMENT);
	}

	private String diagnostic(SnapshotLocation location)
	{
		Integer size = sizes.get(location);
		Observation cached = observations.get(location);
		return "{liveSize=" + (size == null ? "notRead" : size) + ",observed=" + (cached != null)
			+ ",ageMs=" + (cached == null ? "unknown" : Duration.between(cached.time, clock.instant()).toMillis()) + "}";
	}

	private static SnapshotLocation location(int id)
	{
		return id == InventoryID.BANK ? SnapshotLocation.BANK : id == InventoryID.INV ? SnapshotLocation.INVENTORY
			: id == InventoryID.WORN ? SnapshotLocation.EQUIPMENT : null;
	}

	private static final class Observation
	{
		private final ContainerSnapshot contents;
		private final Instant time;
		private Observation(ContainerSnapshot contents, Instant time) { this.contents = contents; this.time = time; }
	}

	/** Client-thread reads in production; synthetic complete observations in tests. */
	public interface Access
	{
		boolean isInterfaceOpen(int interfaceId);
		boolean isVisible(int componentId);
		/**
		 * Complete copied data, including an observed empty list; null means unknown.
		 * The production adapter may independently verify a null personal container
		 * from its complete rendered empty-slot layout. BANK has no such fallback.
		 */
		List<ObservedItem> items(int inventoryId);
	}
}
