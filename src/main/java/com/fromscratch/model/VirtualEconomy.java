package com.fromscratch.model;

import java.util.Collections;
import java.util.Map;

/**
 * The only supported economy in M2 is empty. Physical legacy coins never enter
 * these balances. Future nonzero accounting requires an explicit schema migration.
 */
public final class VirtualEconomy
{
	public long getSpendableGp() { return 0L; }
	public long getNetWorth() { return 0L; }
	public long getTotalValueGained() { return 0L; }
	public long getItemsUnlocked() { return 0L; }
	public Map<Integer, Long> getItems() { return Collections.emptyMap(); }
}
