package com.fromscratch.observation;

import com.fromscratch.model.ContainerSnapshot;
import com.fromscratch.model.SnapshotLocation;
import com.fromscratch.tracking.Holdings;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;

/**
 * Session-bound, tick-coalesced observations. Unknown reads retain reliable history;
 * an invalidated location cannot be compared until a new valid observation arrives.
 */
public final class ItemObservationCollector
{
	private final BankSnapshotReader.Access access;
	private final Clock clock;
	private final Map<SnapshotLocation, ContainerSnapshot> observed = new EnumMap<>(SnapshotLocation.class);
	private final Map<SnapshotLocation, Instant> times = new EnumMap<>(SnapshotLocation.class);
	private final Map<Integer, ItemObservation.Action> actions = new HashMap<>();
	private final Map<Integer, Long> actionTicks = new HashMap<>();
	private final Map<Integer, Long> actionVersions = new HashMap<>();
	private UUID sessionId = UUID.randomUUID();
	private long tick;
	private long changedAt;
	private long bankOpenedAt;
	private boolean uncertainBank;
	private boolean bankOpen;
	private long version;
	private long uncertainAt;
	private long bankActivityAt;
	private long acknowledged;
	private long unsupportedAt;
	private Holdings acknowledgedHoldings;
	private final Map<Integer, Long> ordinaryReceipts = new HashMap<>();
	private final Map<Integer, Long> receiptVersions = new HashMap<>();

	public ItemObservationCollector(BankSnapshotReader.Access access) { this(access, Clock.systemUTC()); }
	public ItemObservationCollector(BankSnapshotReader.Access access, Clock clock) { this.access = access; this.clock = clock; }
	public void reset()
	{
		observed.clear(); times.clear(); actions.clear(); actionTicks.clear(); actionVersions.clear();
		sessionId = UUID.randomUUID(); tick = 0; changedAt = 0; bankOpenedAt = 0; uncertainBank = false; bankOpen = false;
		version = 0; uncertainAt = 0; bankActivityAt = 0; acknowledged = 0; unsupportedAt = 0;
		acknowledgedHoldings = null; ordinaryReceipts.clear(); receiptVersions.clear();
	}
	public void gameTick() { tick = Math.incrementExact(tick); }
	/** Only the freshly validated, explicitly confirmed initial snapshot may seed a new session. */
	public void seedConfirmed(com.fromscratch.model.LegacySnapshot snapshot)
	{
		for (SnapshotLocation location : SnapshotLocation.values())
		{ observed.put(location, snapshot.at(location)); times.put(location, snapshot.getCapturedAt()); }
		changedAt = tick; version++;
	}
	public void bankInterfaceChanged() { bankOpenedAt = tick; changedAt = tick; bankActivityAt = ++version; }
	public void action(int id, ItemObservation.Action action)
	{
		if (id >= 0) { actions.put(id, action); actionTicks.put(id, tick); actionVersions.put(id, ++version); }
	}
	public void unsupportedAction() { unsupportedAt = ++version; changedAt = tick; }
	public void acknowledge(long throughVersion)
	{
		actionVersions.entrySet().removeIf(entry ->
		{
			if (entry.getValue() <= throughVersion) { actions.remove(entry.getKey()); actionTicks.remove(entry.getKey()); return true; } return false;
		});
		if (uncertainAt <= throughVersion) { uncertainBank = false; }
		acknowledged = Math.max(acknowledged, throughVersion);
		receiptVersions.entrySet().removeIf(entry ->
		{
			if (entry.getValue() <= throughVersion) { ordinaryReceipts.remove(entry.getKey()); return true; } return false;
		});
	}
	/** Bind acquisition evidence to the exact committed observation, even if newer events already arrived. */
	public void acknowledge(ItemObservation observation)
	{
		if (!sessionId.equals(observation.getSessionId())) { return; }
		acknowledgedHoldings = observation.getHoldings();
		acknowledge(observation.getEventVersion());
	}
	public void changed(int id, List<ObservedItem> items)
	{
		SnapshotLocation location = location(id);
		if (location == null) { return; }
		changedAt = tick;
		version++;
		observed.remove(location); times.remove(location);
		if (location == SnapshotLocation.BANK && !openBank()) { uncertainBank = true; uncertainAt = version; }
		// A complete bank event while the normal bank is open is authoritative even
		// if the player closes it before the next tick. Closed-bank payloads cannot
		// re-establish authority (e.g. specialized storage or an unexplained update).
		if (location != SnapshotLocation.BANK || openBank()) { accept(location, items); }
		rememberOrdinaryReceipts();
	}
	public ItemObservation poll(boolean preparing)
	{
		bankOpen = openBank();
		if (bankOpen && tick > bankOpenedAt) { read(SnapshotLocation.BANK, InventoryID.BANK); }
		read(SnapshotLocation.INVENTORY, InventoryID.INV);
		read(SnapshotLocation.EQUIPMENT, InventoryID.WORN);
		rememberOrdinaryReceipts();
		Map<SnapshotLocation, ContainerSnapshot> valid = new EnumMap<>(SnapshotLocation.class);
		observed.forEach((location, contents) ->
		{
			Duration age = Duration.between(times.get(location), clock.instant());
			if (!preparing || (!age.isNegative() && age.compareTo(Duration.ofSeconds(60)) <= 0)) { valid.put(location, contents); }
		});
		if (preparing && !bankOpen) { valid.remove(SnapshotLocation.BANK); }
		CaptureProgress progress = new CaptureProgress(valid);
		Holdings holdings = valid.size() == SnapshotLocation.values().length ? Holdings.fromContainers(valid) : null;
		Map<Integer, ItemObservation.Action> recent = new HashMap<>();
		actions.forEach((id, action) -> { if (tick - actionTicks.get(id) <= 4) { recent.put(id, action); } });
		return new ItemObservation(sessionId, tick, clock.instant(), progress, holdings, tick > changedAt + 1,
			bankOpen, uncertainBank, recent, version, bankOpen || bankActivityAt > acknowledged, unsupportedAt > acknowledged, ordinaryReceipts);
	}
	private void rememberOrdinaryReceipts()
	{
		if (acknowledgedHoldings == null || openBank() || uncertainBank || unsupportedAt > acknowledged
			|| bankActivityAt > acknowledged || observed.size() != SnapshotLocation.values().length) { return; }
		// Only personal holdings can receive ordinary new items while the bank is
		// closed. Avoid copying/scanning a large cached bank again for this hint.
		java.util.Set<Integer> candidates = new java.util.HashSet<>(observed.get(SnapshotLocation.INVENTORY).getQuantities().keySet());
		candidates.addAll(observed.get(SnapshotLocation.EQUIPMENT).getQuantities().keySet());
		for (int id : candidates)
		{
			long quantity = 0;
			for (SnapshotLocation location : SnapshotLocation.values()) { quantity = Math.addExact(quantity, observed.get(location).quantity(id)); }
			long gain = quantity - acknowledgedHoldings.total(id);
			if (gain > ordinaryReceipts.getOrDefault(id, 0L))
			{ ordinaryReceipts.put(id, gain); receiptVersions.put(id, version); }
		}
	}
	private void read(SnapshotLocation location, int id)
	{
		try
		{
			List<ObservedItem> items = access.items(id);
			if (items == null)
			{
				if (location == SnapshotLocation.BANK && bankOpen) { observed.remove(location); times.remove(location); }
				return;
			}
			accept(location, items);
		}
		catch (IllegalStateException | IllegalArgumentException | ArithmeticException ex) { observed.remove(location); times.remove(location); }
	}
	private void accept(SnapshotLocation location, List<ObservedItem> items)
	{
		if (items == null) { return; }
		try
		{
			ContainerSnapshot next = SnapshotAssembler.container(items);
			ContainerSnapshot previous = observed.put(location, next);
			if (previous != null && !previous.getQuantities().equals(next.getQuantities())) { changedAt = tick; version++; }
			times.put(location, clock.instant());
		}
		catch (IllegalArgumentException | IllegalStateException | ArithmeticException ex) { observed.remove(location); times.remove(location); }
	}
	private boolean openBank()
	{
		return access.isInterfaceOpen(InterfaceID.BANKMAIN) && access.isVisible(InterfaceID.Bankmain.ITEMS_CONTAINER)
			&& !access.isInterfaceOpen(InterfaceID.BANKPIN_KEYPAD) && !access.isInterfaceOpen(InterfaceID.BANKPIN_SETTINGS);
	}
	private static SnapshotLocation location(int id)
	{
		return id == InventoryID.INV ? SnapshotLocation.INVENTORY : id == InventoryID.WORN ? SnapshotLocation.EQUIPMENT
			: id == InventoryID.BANK ? SnapshotLocation.BANK : null;
	}
	/** Availability/age only: never item IDs, quantities, character keys or account hashes. */
	public String diagnostics()
	{
		StringBuilder result = new StringBuilder();
		for (SnapshotLocation location : SnapshotLocation.values())
		{
			ContainerSnapshot contents = observed.get(location); Instant time = times.get(location);
			result.append(location).append('=')
				.append(contents == null ? "UNKNOWN" : contents.getQuantities().isEmpty() ? "EMPTY" : "POPULATED")
				.append(" ageMs=").append(time == null ? -1 : Duration.between(time, clock.instant()).toMillis()).append(' ');
		}
		return result.toString();
	}
}
