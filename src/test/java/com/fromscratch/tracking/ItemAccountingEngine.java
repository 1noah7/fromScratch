package com.fromscratch.tracking;

import com.fromscratch.model.RunRecord;
import com.fromscratch.model.SnapshotLocation;
import com.fromscratch.model.VirtualItemOwnership;
import com.fromscratch.model.VirtualLocation;
import com.fromscratch.model.VirtualOwnership;
import com.fromscratch.observation.ItemObservation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.IntUnaryOperator;
import net.runelite.api.gameval.ItemID;

/**
 * Pure, exact-ID item reducer. Match transfer legs before considering earnings.
 * Virtual-first allocation is deterministic; uncertain destruction/transformation
 * requires an explicit review, never a guessed automatic acquisition.
 */
public final class ItemAccountingEngine
{
	public AccountingPlan plan(RunRecord run, ItemObservation observation, IntUnaryOperator noteBase,
		boolean reconciliation, boolean permanentlyRemoved, Map<Integer, VirtualItemOwnership> corrections, String reason)
	{
		return plan(run, observation, noteBase, reconciliation, permanentlyRemoved, corrections, reason, null);
	}
	public AccountingPlan plan(RunRecord run, ItemObservation observation, IntUnaryOperator noteBase,
		boolean reconciliation, boolean permanentlyRemoved, Map<Integer, VirtualItemOwnership> corrections, String reason, ItemReviewEdits edits)
	{
		if (observation == null || !observation.isStable() || observation.getHoldings() == null)
		{ throw new IllegalStateException("Wait for complete, settled inventory, equipment and bank observations."); }
		if (reason != null && reason.length() > 300) { throw new IllegalArgumentException("Keep the correction reason within 300 characters."); }
		ItemProgress old = run.getItemProgress();
		if (!old.isEstablished() && !reconciliation) { throw new IllegalStateException("Confirm the physical baseline before crediting items."); }
		if (old.isEstablished() && !reconciliation && !old.getSessionId().equals(observation.getSessionId()))
		{ throw new IllegalStateException("Confirm this host session's reconciliation before tracking items."); }
		if (old.isEstablished() && old.getSessionId().equals(observation.getSessionId()) && observation.getTick() <= old.getTick())
		{ throw new IllegalStateException("This observation was already processed or arrived out of order."); }
		return new Reduction(run, observation, noteBase, reconciliation, permanentlyRemoved, corrections, reason, edits).reduce();
	}

	private static final class Reduction
	{
		private final RunRecord run;
		private final ItemProgress old;
		private final ItemObservation observation;
		private final Holdings before;
		private final Holdings after;
		private final IntUnaryOperator noteBase;
		private final boolean reconciliation;
		private final boolean permanentlyRemoved;
		private final Map<Integer, VirtualItemOwnership> corrections;
		private final String correctionReason;
		private final ItemReviewEdits edits;
		private final Map<Integer, long[]> virtual = new TreeMap<>();
		private final Map<Integer, long[]> deltas = new TreeMap<>();
		private final Map<Integer, Long> unallocated = new HashMap<>();
		private final Map<Integer, ReturnBalance> returns;
		private final Map<Integer, Long> legacyDeficits;
		private final Set<Integer> unlocked;
		private final Set<Integer> manuallyAcquired = new HashSet<>();
		private final List<ItemTransaction> entries = new ArrayList<>();
		private final List<ItemTransaction> acquisitions;
		private boolean review;
		private boolean mixed;
		private String explanation = "Observed ordinary item changes.";

		Reduction(RunRecord run, ItemObservation observation, IntUnaryOperator noteBase, boolean reconciliation,
			boolean permanentlyRemoved, Map<Integer, VirtualItemOwnership> corrections, String reason, ItemReviewEdits edits)
		{
			this.run = run; old = run.getItemProgress(); this.observation = observation;
			before = old.getPhysical(); after = observation.getHoldings(); this.noteBase = noteBase;
			this.reconciliation = reconciliation; this.permanentlyRemoved = permanentlyRemoved;
			this.corrections = corrections; correctionReason = reason;
			this.edits = edits;
			returns = new HashMap<>(old.getReturns()); unlocked = new HashSet<>(old.getUnlocked());
			legacyDeficits = new HashMap<>(old.getLegacyDeficits().getItems());
			acquisitions = new ArrayList<>(old.getAcquisitions());
			review = reconciliation || observation.isUncertainBankChange();
		}
		AccountingPlan reduce()
		{
			Set<Integer> ids = new TreeSet<>(before.ids()); ids.addAll(after.ids()); ids.addAll(run.getOwnership().getItems().keySet());
			for (int id : ids)
			{
				long[] allocated = new long[SnapshotLocation.values().length];
				long[] delta = new long[allocated.length];
				VirtualItemOwnership owned = run.getOwnership().item(id);
				Map<SnapshotLocation, Long> repaired = new EnumMap<>(SnapshotLocation.class);
				long unknown = Math.addExact(owned.unallocated(), owned.at(VirtualLocation.GRAND_EXCHANGE));
				for (SnapshotLocation location : SnapshotLocation.values())
				{
					int l = location.ordinal(); long v = owned.at(VirtualLocation.valueOf(location.name()));
					allocated[l] = Math.min(v, before.at(id, location));
					unknown = Math.addExact(unknown, v - allocated[l]);
					if (v != allocated[l]) { repaired.put(location, allocated[l] - v); }
					delta[l] = Math.subtractExact(after.at(id, location), before.at(id, location));
				}
				if (unknown != owned.unallocated()) { review = true; explanation = "Some saved ownership needs a location review; it has been preserved as elsewhere."; }
				if (!repaired.isEmpty())
				{
					entry(id, 0, 0, repaired, null, null, ItemTransaction.Category.RECONCILIATION_ADJUSTMENT,
						ItemTransaction.Confidence.NET_RECONCILED, "Preserve unbacked saved allocation as elsewhere until reviewed.");
				}
				virtual.put(id, allocated); deltas.put(id, delta); unallocated.put(id, unknown);
				if (id == ItemID.COINS)
				{
					// Preserve any imported M3 currency-item ownership without implementing
					// a cash engine. Unsupported allocations move to existing unallocated
					// ownership rather than exceeding a physical location or being erased.
					Map<SnapshotLocation, Long> changes = new EnumMap<>(SnapshotLocation.class);
					for (SnapshotLocation location : SnapshotLocation.values())
					{
						long excess = Math.max(0, allocated[location.ordinal()] - after.at(id, location));
						if (excess != 0)
						{
							allocated[location.ordinal()] -= excess; unknown = Math.addExact(unknown, excess); changes.put(location, -excess);
						}
					}
					unallocated.put(id, unknown);
					if (!changes.isEmpty()) { review = true; entry(id, 0, 0, changes, null, null, ItemTransaction.Category.RECONCILIATION_ADJUSTMENT,
						ItemTransaction.Confidence.NET_RECONCILED, "Imported currency-item ownership preserved as elsewhere; currency accounting is deferred."); }
				}
			}
			// First pair same-ID movements, then explicitly linked notes. No market,
			// charge/dose or crafting equivalence is inferred from similar names.
			for (int id : ids) { if (id != ItemID.COINS) { match(id, id); } }
			Map<Integer, List<Integer>> positiveByBase = new HashMap<>();
			for (int id : ids)
			{
				if (id != ItemID.COINS && sum(deltas.get(id)) > 0)
				{ positiveByBase.computeIfAbsent(noteBase.applyAsInt(id), ignored -> new ArrayList<>()).add(id); }
			}
			for (int source : ids)
			{
				if (source == ItemID.COINS || sum(deltas.get(source)) >= 0) { continue; }
				for (int destination : positiveByBase.getOrDefault(noteBase.applyAsInt(source), Collections.emptyList()))
				{
					if (source != destination && destination != ItemID.COINS && sum(deltas.get(destination)) > 0
						) { match(source, destination); }
				}
			}
			boolean loss = false; boolean gain = false;
			for (int id : ids)
			{
				if (id == ItemID.COINS) { continue; }
				for (long delta : deltas.get(id)) { loss |= delta < 0; gain |= delta > 0; }
			}
			boolean unexplainedBankGain = false;
			for (int id : ids)
			{
				boolean pickup = observation.getActions().get(id) == ItemObservation.Action.ACQUIRE && !observation.isUnsupportedAction();
				if (id != ItemID.COINS && !pickup && sum(deltas.get(id)) > observation.getOrdinaryReceipts().getOrDefault(id, 0L))
				{ unexplainedBankGain = true; }
			}
			if (!reconciliation && unexplainedBankGain && observation.isBankInteraction())
			{
				review = true; explanation = "A banking increase has no matching transfer yet. Review the complete bank before accepting new ownership.";
			}
			mixed = gain && (loss || before.total(ItemID.COINS) > after.total(ItemID.COINS) || observation.isUnsupportedAction());
			if (mixed)
			{
				review = true;
				explanation = "Different items or coins changed together. New outputs stay locked until you review their ownership.";
			}
			for (int id : ids)
			{
				if (id == ItemID.COINS) { continue; } // observe physical coins; financial accounting is explicitly deferred
				for (SnapshotLocation location : SnapshotLocation.values())
				{
					long delta = deltas.get(id)[location.ordinal()];
					if (delta < 0) { remove(id, location, -delta); }
					else if (delta > 0) { gain(id, location, delta); }
				}
			}
			Map<Integer, VirtualItemOwnership> items = new HashMap<>();
			virtual.forEach((id, allocations) -> items.put(id, ownership(allocations, unallocated.getOrDefault(id, 0L))));
			for (Map.Entry<Integer, VirtualItemOwnership> correction : corrections.entrySet())
			{
				int id = correction.getKey(); VirtualItemOwnership proposed = correction.getValue();
				if (id == ItemID.COINS || id < 0 || proposed == null) { throw new IllegalArgumentException("Currency is not editable in item reconciliation"); }
				VirtualItemOwnership previous = items.getOrDefault(id, new VirtualItemOwnership(0, Collections.emptyMap()));
				if (proposed.equals(previous)) { continue; }
				if (correctionReason == null || correctionReason.isBlank()) { throw new IllegalArgumentException("Explain manual ownership corrections before confirming"); }
				if (proposed.unallocated() > run.getOwnership().total(id)) { throw new IllegalArgumentException("Elsewhere can only preserve previously virtual copies"); }
				Map<SnapshotLocation, Long> changes = new EnumMap<>(SnapshotLocation.class);
				for (SnapshotLocation location : SnapshotLocation.values())
				{
					changes.put(location, proposed.at(VirtualLocation.valueOf(location.name())) - previous.at(VirtualLocation.valueOf(location.name())));
				}
				long change = Math.subtractExact(proposed.getTotal(), previous.getTotal());
				if (change < 0) { change = Math.addExact(change, rejectProposedAcquisitions(id, -change, changes)); }
				// Preserving a copy that the proposed reduction removed is a location
				// correction, not another lifetime acquisition of that same copy.
				long retained = change > 0 ? Math.min(change, Math.max(0, run.getOwnership().total(id) - previous.getTotal())) : 0;
				if (retained > 0)
				{
					Map<SnapshotLocation, Long> retainedLocations = new EnumMap<>(SnapshotLocation.class);
					long remaining = retained;
					for (SnapshotLocation location : SnapshotLocation.values())
					{
						long amount = Math.min(remaining, Math.max(0, changes.getOrDefault(location, 0L)));
						if (amount != 0) { retainedLocations.put(location, amount); changes.merge(location, -amount, Math::addExact); remaining -= amount; }
					}
					entry(id, retained, retained, retainedLocations, null, null, ItemTransaction.Category.RECONCILIATION_ADJUSTMENT,
						ItemTransaction.Confidence.MANUAL, "Preserve previously owned copies: " + correctionReason);
					change -= retained;
				}
				if (change != 0 || retained == 0 || changes.values().stream().anyMatch(delta -> delta != 0))
				{
					ItemTransaction entry = entry(id, Math.abs(change), change, changes, null, null,
						ItemTransaction.Category.RECONCILIATION_ADJUSTMENT, ItemTransaction.Confidence.MANUAL, correctionReason);
					if (change > 0 && (edits == null || !edits.getReturned().contains(id))) { manuallyAcquired.add(id); prepend(acquisitions, entry); }
				}
				items.put(id, proposed);
			}
			if (edits != null)
			{
				edits.getDeficitOverrides().forEach((id, quantity) ->
				{
					long existing = legacyDeficits.getOrDefault(noteBase.applyAsInt(id), 0L);
					changeDeficit(id, Math.subtractExact(quantity, existing), true);
				});
				edits.getDecisions().forEach((id, decision) -> entry(id, 0, 0, Collections.emptyMap(), null, null,
					ItemTransaction.Category.RECONCILIATION_ADJUSTMENT, ItemTransaction.Confidence.MANUAL,
					"Update Progress decision: " + decision + ". " + correctionReason));
			}
			// Rebuild only this batch's lifetime contributions. Rejecting an uncommitted
			// proposed gain must not leave a false acquisition/unlock in the ledger or UI.
			unlocked.clear(); unlocked.addAll(old.getUnlocked());
			for (ItemTransaction entry : entries)
			{
				if (entry.getVirtualChange() > 0 && entry.getCategory() == ItemTransaction.Category.ACQUISITION)
				{ unlocked.add(noteBase.applyAsInt(entry.getItemId())); }
			}
			for (int id : manuallyAcquired) { unlocked.add(noteBase.applyAsInt(id)); }
			if (entries.isEmpty())
			{
				entry(-1, 0, 0, Collections.emptyMap(), null, null, ItemTransaction.Category.RECONCILIATION_ADJUSTMENT,
					reconciliation ? ItemTransaction.Confidence.NET_RECONCILED : ItemTransaction.Confidence.OBSERVED,
					reconciliation ? "Host session baseline confirmed; no item ownership change." : "Physical baseline advanced; currency accounting is deferred.");
			}
			List<ItemTransaction> recent = new ArrayList<>(old.getRecent());
			for (ItemTransaction entry : entries) { prepend(recent, entry); }
			ItemProgress progress = new ItemProgress(after, returns, unlocked, recent, acquisitions, observation.getSessionId(),
				observation.getTick(), observation.getTime(), UUID.randomUUID(), Math.incrementExact(old.getSequence()), true,
				new LegacyBankDeficits(legacyDeficits));
			return new AccountingPlan(new VirtualOwnership(items), progress, entries, review, explanation);
		}
		private void match(int sourceId, int destinationId)
		{
			for (SnapshotLocation source : SnapshotLocation.values())
			{
				for (SnapshotLocation destination : SnapshotLocation.values())
				{
					long[] from = deltas.get(sourceId); long[] to = deltas.get(destinationId);
					if (from[source.ordinal()] >= 0 || to[destination.ordinal()] <= 0) { continue; }
					long quantity = Math.min(-from[source.ordinal()], to[destination.ordinal()]);
					long v = Math.min(quantity, virtual.get(sourceId)[source.ordinal()]);
					if (source == SnapshotLocation.BANK && destination != SnapshotLocation.BANK)
					{ changeDeficit(sourceId, quantity - v, false); }
					else if (destination == SnapshotLocation.BANK && source != SnapshotLocation.BANK)
					{ changeDeficit(destinationId, -(quantity - v), false); }
					virtual.get(sourceId)[source.ordinal()] -= v;
					virtual.get(destinationId)[destination.ordinal()] = Math.addExact(virtual.get(destinationId)[destination.ordinal()], v);
					from[source.ordinal()] += quantity; to[destination.ordinal()] -= quantity;
					Map<SnapshotLocation, Long> changes = new EnumMap<>(SnapshotLocation.class);
					changes.merge(source, -v, Math::addExact);
					if (sourceId == destinationId) { changes.merge(destination, v, Math::addExact); }
					entry(sourceId, quantity, sourceId == destinationId ? 0 : -v, changes, source, destination,
						ItemTransaction.Category.TRANSFER, confidence(), sourceId == destinationId ? "Virtual-first container transfer." : "Explicit noted/unnoted conversion; paired with destination entry.");
					if (sourceId != destinationId)
					{
						entry(destinationId, quantity, v, Collections.singletonMap(destination, v), source, destination,
							ItemTransaction.Category.TRANSFER, confidence(), "Explicit noted/unnoted conversion; paired with source entry.");
					}
				}
			}
		}
		private void remove(int id, SnapshotLocation location, long quantity)
		{
			long v = Math.min(quantity, virtual.get(id)[location.ordinal()]);
			virtual.get(id)[location.ordinal()] -= v;
			if (location == SnapshotLocation.BANK) { changeDeficit(id, quantity - v, false); }
			ItemObservation.Action action = observation.getActions().get(id);
			boolean consumed = !reconciliation && action == ItemObservation.Action.CONSUME && quantity == 1;
			boolean dropped = !reconciliation && action == ItemObservation.Action.DROP;
			if (!consumed && !dropped) { review = true; if (!mixed) { explanation = "An item left the observed containers. Review whether it was consumed, lost or moved elsewhere."; } }
			boolean permanent = permanentlyRemoved || edits != null && edits.getPermanentlyRemoved().contains(id);
			if (!consumed && (!permanent || dropped))
			{
				ReturnBalance pool = returns.getOrDefault(id, new ReturnBalance(0, 0));
				returns.put(id, new ReturnBalance(Math.addExact(pool.getLegacy(), quantity - v), Math.addExact(pool.getVirtual(), v)));
			}
			entry(id, quantity, -v, Collections.singletonMap(location, -v), location, null,
				consumed ? ItemTransaction.Category.CONSUMPTION : ItemTransaction.Category.LOSS, confidence(),
				consumed ? "Observed one-item consumption after an Eat action." : dropped ? "Observed drop; preserve return identity." : "Confirmed net reduction; no unobserved transaction history inferred.");
		}
		private void gain(int id, SnapshotLocation location, long quantity)
		{
			ReturnBalance pool = returns.getOrDefault(id, new ReturnBalance(0, 0));
			// Identical dropped copies cannot be identified. Legacy-first returns are
			// conservative and prevent using a legacy pickup to unlock a virtual copy.
			long legacyReturn = Math.min(quantity, pool.getLegacy()); quantity -= legacyReturn;
			if (location == SnapshotLocation.BANK && legacyReturn > 0) { changeDeficit(id, -legacyReturn, false); }
			long virtualReturn = Math.min(quantity, pool.getVirtual()); quantity -= virtualReturn;
			returns.put(id, new ReturnBalance(pool.getLegacy() - legacyReturn, pool.getVirtual() - virtualReturn));
			long elsewhere = unallocated.getOrDefault(id, 0L);
			long preservedReturn = Math.min(virtualReturn, elsewhere); elsewhere -= preservedReturn;
			long elsewhereReturn = Math.min(quantity, elsewhere); quantity -= elsewhereReturn; elsewhere -= elsewhereReturn;
			unallocated.put(id, elsewhere);
			long locatedReturn = Math.addExact(virtualReturn, elsewhereReturn);
			virtual.get(id)[location.ordinal()] = Math.addExact(virtual.get(id)[location.ordinal()], locatedReturn);
			if (preservedReturn != 0 || elsewhereReturn != 0)
			{
				review = true;
				explanation = "Previously owned items may have returned from elsewhere. Review their location; no new acquisition was credited.";
			}
			if (legacyReturn != 0 || locatedReturn != 0)
			{
				entry(id, Math.addExact(legacyReturn, locatedReturn), virtualReturn - preservedReturn, Collections.singletonMap(location, locatedReturn),
					null, location, ItemTransaction.Category.RECONCILIATION_ADJUSTMENT, confidence(), "Return of previously removed copies; no new acquisition.");
			}
			if (quantity == 0) { return; }
			if (mixed)
			{
				entry(id, quantity, 0, Collections.emptyMap(), null, location, ItemTransaction.Category.RECONCILIATION_ADJUSTMENT,
					confidence(), "Ambiguous new output remains locked unless explicitly corrected during review.");
				return;
			}
			virtual.get(id)[location.ordinal()] = Math.addExact(virtual.get(id)[location.ordinal()], quantity);
			unlocked.add(noteBase.applyAsInt(id));
			ItemTransaction acquired = entry(id, quantity, quantity, Collections.singletonMap(location, quantity), null, location,
				ItemTransaction.Category.ACQUISITION, confidence(), reconciliation ? "Confirmed net increase since the last host checkpoint." : "New holdings after ordinary transfers and known returns were excluded.");
			prepend(acquisitions, acquired);
		}
		private ItemTransaction.Confidence confidence() { return reconciliation ? ItemTransaction.Confidence.NET_RECONCILED : ItemTransaction.Confidence.OBSERVED; }
		private void changeDeficit(int id, long change, boolean manual)
		{
			int identity = noteBase.applyAsInt(id);
			long before = legacyDeficits.getOrDefault(identity, 0L);
			long next = change >= 0 ? Math.addExact(before, change) : before - Math.min(before, -change);
			if (next == before) { return; }
			if (next == 0) { legacyDeficits.remove(identity); } else { legacyDeficits.put(identity, next); }
			entries.add(new ItemTransaction(UUID.randomUUID(), observation.getTime(), identity, Math.abs(next - before), 0,
				Collections.emptyMap(), null, null, ItemTransaction.Category.RECONCILIATION_ADJUSTMENT,
				manual ? ItemTransaction.Confidence.MANUAL : confidence(),
				manual ? "Reviewed outstanding legacy withdrawal correction: " + correctionReason
					: "Outstanding legacy bank withdrawals changed; earned ownership is unchanged.", next - before));
		}
		private long rejectProposedAcquisitions(int id, long reduction, Map<SnapshotLocation, Long> correctionChanges)
		{
			long rejected = 0; int count = entries.size();
			for (int index = 0; index < count && rejected < reduction; index++)
			{
				ItemTransaction original = entries.get(index);
				if (original.getItemId() != id || original.getCategory() != ItemTransaction.Category.ACQUISITION) { continue; }
				long amount = Math.min(original.getVirtualChange(), reduction - rejected); rejected = Math.addExact(rejected, amount);
				correctionChanges.merge(original.getDestination(), amount, Math::addExact);
				acquisitions.removeIf(entry -> entry.getId().equals(original.getId()));
				long accepted = original.getVirtualChange() - amount;
				ItemTransaction replacement = new ItemTransaction(original.getId(), original.getTime(), id, accepted == 0 ? amount : accepted,
					accepted, accepted == 0 ? Collections.emptyMap() : Collections.singletonMap(original.getDestination(), accepted), null,
					original.getDestination(), accepted == 0 ? ItemTransaction.Category.RECONCILIATION_ADJUSTMENT : ItemTransaction.Category.ACQUISITION,
					ItemTransaction.Confidence.MANUAL, accepted == 0 ? "Proposed new holdings confirmed as legacy: " + correctionReason : "Manually confirmed portion of the observed gain: " + correctionReason);
				entries.set(index, replacement);
				if (accepted > 0)
				{
					prepend(acquisitions, replacement);
					entry(id, amount, 0, Collections.emptyMap(), null, original.getDestination(), ItemTransaction.Category.RECONCILIATION_ADJUSTMENT,
						ItemTransaction.Confidence.MANUAL, "Remaining proposed holdings confirmed as legacy.");
				}
			}
			return rejected;
		}
		private ItemTransaction entry(int id, long quantity, long virtualChange, Map<SnapshotLocation, Long> changes,
			SnapshotLocation source, SnapshotLocation destination, ItemTransaction.Category category, ItemTransaction.Confidence confidence, String reason)
		{
			ItemTransaction entry = new ItemTransaction(UUID.randomUUID(), observation.getTime(), id, quantity, virtualChange,
				changes, source, destination, category, confidence, reason);
			entries.add(entry); return entry;
		}
		private static VirtualItemOwnership ownership(long[] allocated, long unknown)
		{
			Map<VirtualLocation, Long> locations = new EnumMap<>(VirtualLocation.class); long total = unknown;
			for (SnapshotLocation location : SnapshotLocation.values())
			{
				long v = allocated[location.ordinal()]; total = Math.addExact(total, v); locations.put(VirtualLocation.valueOf(location.name()), v);
			}
			return new VirtualItemOwnership(total, locations);
		}
		private static long sum(long[] quantities) { long sum = 0; for (long q : quantities) { sum = Math.addExact(sum, q); } return sum; }
		private static void prepend(List<ItemTransaction> recent, ItemTransaction entry)
		{
			recent.add(0, entry); if (recent.size() > ItemProgress.RECENT_LIMIT) { recent.remove(recent.size() - 1); }
		}
	}
}
