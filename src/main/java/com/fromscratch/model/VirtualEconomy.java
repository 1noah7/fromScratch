package com.fromscratch.model;

import java.util.Map;

/**
 * Currency/wealth remain placeholders until M5. Item totals delegate to the
 * separate ownership model; physical legacy coins never enter these balances.
 */
public final class VirtualEconomy
{
	private final VirtualOwnership ownership;
	private final long unlocked;
	public VirtualEconomy() { this(VirtualOwnership.empty()); }
	public VirtualEconomy(VirtualOwnership ownership) { this(ownership, 0); }
	public VirtualEconomy(VirtualOwnership ownership, long unlocked) { this.ownership = ownership; this.unlocked = unlocked; }
	public long getSpendableGp() { return 0L; }
	public long getNetWorth() { return 0L; }
	public long getTotalValueGained() { return 0L; }
	public long getItemsUnlocked() { return unlocked; }
	public Map<Integer, Long> getItems() { return ownership.getTotals(); }
}
