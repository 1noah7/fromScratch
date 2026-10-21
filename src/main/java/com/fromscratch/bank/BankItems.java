package com.fromscratch.bank;

import com.fromscratch.model.ContainerSnapshot;
import java.util.*;
import java.util.function.IntPredicate;
import net.runelite.api.Item;
import net.runelite.api.gameval.ItemID;

/** Exact-ID full API container aggregation, shared by the live adapter and realistic event fixtures. */
public final class BankItems
{
	private BankItems() { }
	public static ContainerSnapshot aggregate(Item[] items, IntPredicate placeholder)
	{
		if (items == null) { return null; }
		Map<Integer, Long> quantities = new TreeMap<>(); int slots = 0;
		for (Item item : items)
		{
			if (item == null || item.getQuantity() < 0) { throw new IllegalArgumentException("Incomplete bank slot"); }
			int id = item.getId(); int n = item.getQuantity();
			if (id < 0 || n == 0 || id == ItemID.BANK_FILLER || id == ItemID.BLANKOBJECT || placeholder.test(id)) { continue; }
			quantities.merge(id, (long) n, Math::addExact); slots++;
		}
		return new ContainerSnapshot(quantities, slots);
	}
}
