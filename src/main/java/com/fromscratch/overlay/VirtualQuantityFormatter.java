package com.fromscratch.overlay;

/** Traditional OSRS stack labels. Quotients truncate; no floating point or negation of MIN_VALUE. */
public final class VirtualQuantityFormatter
{
	private VirtualQuantityFormatter() { }
	public static String format(long quantity)
	{
		if (quantity < 0) { throw new IllegalArgumentException("Physical quantity cannot be negative"); }
		return signed(quantity);
	}
	public static String signed(long quantity)
	{
		if (quantity > -10_000 && quantity < 10_000) { return Long.toString(quantity); }
		if (quantity > -10_000_000 && quantity < 10_000_000) { return quantity / 1_000 + "K"; }
		return quantity / 1_000_000 + "M";
	}
}
