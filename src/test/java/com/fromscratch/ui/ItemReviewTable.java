package com.fromscratch.ui;

import com.fromscratch.model.SnapshotLocation;
import com.fromscratch.model.VirtualItemOwnership;
import com.fromscratch.model.VirtualLocation;
import com.fromscratch.tracking.ItemReviewEdits;
import com.fromscratch.tracking.ItemTransaction;
import java.util.*;
import javax.swing.table.AbstractTableModel;
import net.runelite.api.gameval.ItemID;

/** Difference-focused local decisions. Exact IDs identify rows; names are presentation only. */
public final class ItemReviewTable extends AbstractTableModel
{
	public enum Filter
	{
		ALL("All Changes"), GAINS("Gains"), LOSSES("Losses"), REVIEW("Needs Review");
		private final String label;
		Filter(String label) { this.label = label; }
		@Override public String toString() { return label; }
	}
	public enum Decision
	{
		DETECTED("Use detected change"), EARNED("Newly Earned"), LEGACY("Existing Legacy"), RETURNED("Returned Item"),
		LOST("Lost / Consumed"), RESOLVE_DEFICIT("Resolve legacy withdrawal"), CUSTOM("Custom quantities");
		private final String label;
		Decision(String label) { this.label = label; }
		@Override public String toString() { return label; }
	}
	private static final String[] COLUMNS = {"Item", "Change", "Category", "Decision", "Result", "Earned in bank", "Earned in inventory", "Earned worn", "Earned elsewhere", "Legacy withdrawn"};
	private final ItemReview review;
	private final List<Row> all = new ArrayList<>();
	private final List<Row> visible = new ArrayList<>();
	public ItemReviewTable(ItemReview review, Map<Integer, String> names)
	{
		this.review = review;
		Set<Integer> ids = new TreeSet<>(review.getBefore().ids()); ids.addAll(review.getCurrent().ids());
		ids.addAll(review.getPrevious().getItems().keySet()); ids.addAll(review.getProposed().getItems().keySet());
		ids.addAll(review.getPreviousDeficits().getItems().keySet()); ids.addAll(review.getProposedDeficits().getItems().keySet()); ids.remove(ItemID.COINS);
		for (int id : ids) { all.add(new Row(id, names.getOrDefault(id, "Unknown item #" + id))); }
		filter(Filter.ALL, "", false);
	}
	public int getRowCount() { return visible.size(); }
	public int getColumnCount() { return COLUMNS.length; }
	public String getColumnName(int column) { return COLUMNS[column]; }
	@Override public Class<?> getColumnClass(int column) { return column == 1 ? Long.class : column == 3 ? Decision.class : Object.class; }
	public Object getValueAt(int row, int column)
	{
		Row item = visible.get(row);
		switch (column)
		{
			case 0: return item.name;
			case 1: return item.delta;
			case 2: return !item.changed ? "Unchanged" : item.delta > 0 ? "Gained — confirm source" : item.delta < 0 ? "Missing — review loss" : item.needsReview ? "Review ownership" : "Moved between containers";
			case 3: return item.decision;
			case 4: return "Bank " + item.quantities[0] + "; inventory " + item.quantities[1] + "; worn " + item.quantities[2] + "; legacy withdrawn " + item.quantities[4];
			default: return item.quantities[column - 5];
		}
	}
	public boolean isCellEditable(int row, int column) { return column == 3 || column >= 5; }
	@Override public void setValueAt(Object value, int row, int column)
	{
		Row item = visible.get(row);
		if (column == 3) { item.decide((Decision) value); }
		else if (column >= 5) { item.quantities[column - 5] = value; item.decision = Decision.CUSTOM; }
		fireTableRowsUpdated(row, row);
	}
	public int itemId(int row) { return visible.get(row).id; }
	public void filter(Filter filter, String search, boolean showUnchanged)
	{
		String query = search.toLowerCase(Locale.ROOT).strip(); visible.clear();
		for (Row row : all)
		{
			if (!showUnchanged && !row.changed || !row.name.toLowerCase(Locale.ROOT).contains(query)) { continue; }
			if (filter == Filter.GAINS && row.delta <= 0 || filter == Filter.LOSSES && row.delta >= 0
				|| filter == Filter.REVIEW && (!row.needsReview || row.decision != Decision.DETECTED)) { continue; }
			visible.add(row);
		}
		fireTableDataChanged();
	}
	/** Model indices must be converted from the JTable's current sorted view by the dialog. */
	public void decide(int[] rows, Decision decision)
	{
		for (int row : rows)
		{
			if (!Arrays.asList(choices(row)).contains(decision)) { throw new IllegalArgumentException(decision + " is not applicable to " + visible.get(row).name); }
		}
		for (int row : rows) { visible.get(row).decide(decision); } fireTableDataChanged();
	}
	public Decision[] choices(int row)
	{
		Row item = visible.get(row); List<Decision> choices = new ArrayList<>(); choices.add(Decision.DETECTED);
		if (review.getCurrent().total(item.id) > 0) { choices.add(Decision.EARNED); choices.add(Decision.LEGACY); choices.add(Decision.RETURNED); }
		if (item.delta < 0) { choices.add(Decision.LOST); }
		if (review.getProposedDeficits().quantity(item.id) > 0) { choices.add(Decision.RESOLVE_DEFICIT); }
		choices.add(Decision.CUSTOM); return choices.toArray(new Decision[0]);
	}
	public boolean hasChanges() { return all.stream().anyMatch(row -> row.changed || row.decision != Decision.DETECTED); }
	public Map<Integer, VirtualItemOwnership> corrections() { return edits("Reviewed item changes.").getOwnership(); }
	public ItemReviewEdits edits(String reason)
	{
		Map<Integer, VirtualItemOwnership> changes = new HashMap<>(); Map<Integer, Long> deficits = new HashMap<>();
		Set<Integer> returned = new HashSet<>(); Set<Integer> permanent = new HashSet<>();
		Map<Integer, String> decisions = new HashMap<>();
		for (Row row : all)
		{
			VirtualItemOwnership item = row.ownership();
			if (!item.equals(review.getProposed().item(row.id))) { changes.put(row.id, item); }
			long deficit = number(row.quantities[4], row.name);
			if (deficit != review.getProposedDeficits().quantity(row.id)) { deficits.put(row.id, deficit); }
			if (row.decision == Decision.RETURNED) { returned.add(row.id); }
			if (row.decision == Decision.LOST) { permanent.add(row.id); }
			if (row.decision != Decision.DETECTED) { decisions.put(row.id, row.decision.toString()); }
		}
		return new ItemReviewEdits(changes, deficits, returned, permanent, reason.isBlank() ? "Reviewed item decisions in Update Progress." : reason, decisions);
	}
	public String summary()
	{
		int changed = 0; int decided = 0; long units = 0;
		for (Row row : all)
		{
			if (row.changed || row.decision != Decision.DETECTED) { changed++; }
			if (row.decision != Decision.DETECTED) { decided++; }
			units = Math.addExact(units, Math.subtractExact(row.ownership().getTotal(), review.getPrevious().total(row.id)));
		}
		return changed + " changed item types; " + decided + " explicit decisions. Net earned copies: " + (units > 0 ? "+" : "") + units
			+ ". Only these confirmed observations and decisions will be applied.";
	}
	public String details()
	{
		StringBuilder text = new StringBuilder(summary()).append("\n\n");
		for (Row row : all)
		{
			if (!row.changed && row.decision == Decision.DETECTED) { continue; }
			text.append(row.name).append(" — ").append(row.decision).append("; physical change ")
				.append(row.delta).append("; earned copies ").append(review.getPrevious().total(row.id)).append(" → ")
				.append(row.ownership().getTotal()).append("; legacy withdrawn ").append(review.getPreviousDeficits().quantity(row.id))
				.append(" → ").append(row.quantities[4]).append('\n');
		}
		return text.toString();
	}
	private static long number(Object value, String name)
	{
		String text = String.valueOf(value);
		if (!text.matches("0|[1-9][0-9]*")) { throw new IllegalArgumentException("Enter whole, nonnegative quantities for " + name); }
		try { return Long.parseLong(text); } catch (NumberFormatException ex) { throw new IllegalArgumentException("Quantity is too large for " + name); }
	}
	private final class Row
	{
		final int id; final String name; final long delta; final boolean changed; final boolean needsReview;
		Decision decision = Decision.DETECTED; Object[] quantities = new Object[5];
		Row(int id, String name)
		{
			this.id = id; this.name = name; delta = review.getCurrent().total(id) - review.getBefore().total(id);
			boolean physical = false;
			for (SnapshotLocation location : SnapshotLocation.values()) { physical |= review.getCurrent().at(id, location) != review.getBefore().at(id, location); }
			changed = physical || !review.getPrevious().item(id).equals(review.getProposed().item(id))
				|| review.getPreviousDeficits().quantity(id) != review.getProposedDeficits().quantity(id);
			needsReview = delta != 0 || review.getPrevious().total(id) != review.getProposed().total(id)
				|| review.getTransactions().stream().anyMatch(entry -> entry.getItemId() == id && entry.getCategory() == ItemTransaction.Category.RECONCILIATION_ADJUSTMENT
				&& entry.getLegacyWithdrawalChange() == 0 && entry.getQuantity() > 0 && entry.getVirtualChange() == 0);
			set(review.getProposed().item(id)); quantities[4] = review.getProposedDeficits().quantity(id);
		}
		void set(VirtualItemOwnership item)
		{
			quantities[0] = item.at(VirtualLocation.BANK); quantities[1] = item.at(VirtualLocation.INVENTORY);
			quantities[2] = item.at(VirtualLocation.EQUIPMENT); quantities[3] = item.unallocated();
		}
		void decide(Decision next)
		{
			decision = next; quantities[4] = review.getProposedDeficits().quantity(id); set(review.getProposed().item(id));
			if (next == Decision.RESOLVE_DEFICIT) { quantities[4] = 0L; }
			long elsewhere = review.getProposed().item(id).unallocated();
			long previousHere = Math.min(review.getCurrent().total(id), Math.max(0, review.getPrevious().total(id) - elsewhere));
			if (next == Decision.LEGACY) { allocate(Math.addExact(elsewhere, previousHere)); }
			if (next == Decision.EARNED)
			{
				long extra = Math.min(delta > 0 ? delta : 1, review.getCurrent().total(id) - previousHere);
				allocate(Math.addExact(elsewhere, Math.addExact(previousHere, extra)));
			}
			if (next == Decision.RETURNED)
			{
				long acquired = review.getTransactions().stream().filter(entry -> entry.getItemId() == id && entry.getCategory() == ItemTransaction.Category.ACQUISITION)
					.mapToLong(ItemTransaction::getVirtualChange).reduce(0, Math::addExact);
				long returningHere = Math.min(review.getCurrent().total(id), Math.max(0, review.getProposed().total(id) - acquired - elsewhere));
				allocate(Math.addExact(elsewhere, returningHere));
				long bankReturn = Math.max(0, review.getCurrent().at(id, SnapshotLocation.BANK) - review.getBefore().at(id, SnapshotLocation.BANK));
				long earnedReturn = Math.min(bankReturn, Math.max(0, number(quantities[0], name) - review.getPrevious().at(id, VirtualLocation.BANK)));
				quantities[4] = Math.min(review.getProposedDeficits().quantity(id), Math.max(0, review.getPreviousDeficits().quantity(id) - (bankReturn - earnedReturn)));
			}
		}
		private void allocate(long total)
		{
			long left = total - Math.min(total, review.getProposed().item(id).unallocated());
			Map<VirtualLocation, Long> locations = new EnumMap<>(VirtualLocation.class);
			for (SnapshotLocation location : SnapshotLocation.values())
			{
				VirtualLocation virtual = VirtualLocation.valueOf(location.name());
				long value = Math.min(left, Math.min(review.getProposed().at(id, virtual), review.getCurrent().at(id, location)));
				locations.put(virtual, value); left -= value;
			}
			for (SnapshotLocation location : SnapshotLocation.values())
			{
				VirtualLocation virtual = VirtualLocation.valueOf(location.name()); long value = locations.get(virtual);
				long increase = Math.max(0, review.getCurrent().at(id, location) - review.getBefore().at(id, location));
				long add = Math.min(left, Math.min(increase, review.getCurrent().at(id, location) - value));
				locations.put(virtual, value + add); left -= add;
			}
			for (SnapshotLocation location : SnapshotLocation.values())
			{
				VirtualLocation virtual = VirtualLocation.valueOf(location.name()); long value = locations.get(virtual);
				long add = Math.min(left, review.getCurrent().at(id, location) - value); locations.put(virtual, value + add); left -= add;
			}
			set(new VirtualItemOwnership(total, locations));
		}
		VirtualItemOwnership ownership()
		{
			Map<VirtualLocation, Long> locations = new EnumMap<>(VirtualLocation.class); long total = 0;
			for (int index = 0; index < 4; index++)
			{
				long q = number(quantities[index], name);
				try { total = Math.addExact(total, q); } catch (ArithmeticException ex) { throw new IllegalArgumentException("Quantity is too large for " + name); }
				if (index < 3)
				{
					SnapshotLocation location = SnapshotLocation.values()[index];
					if (q > review.getCurrent().at(id, location)) { throw new IllegalArgumentException("Earned quantity exceeds observed " + location.name().toLowerCase(Locale.ROOT) + " for " + name); }
					locations.put(VirtualLocation.valueOf(location.name()), q);
				}
				else if (q > review.getPrevious().total(id)) { throw new IllegalArgumentException("Elsewhere can preserve only previously earned copies."); }
			}
			return new VirtualItemOwnership(total, locations);
		}
	}
}
