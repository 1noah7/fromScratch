package com.fromscratch.overlay;

/** Compact floored labels, including large longs that exceed OSRS physical stack limits. */
public final class VirtualQuantityFormatter
{
	private static final long[] UNITS = {1, 1_000L, 1_000_000L, 1_000_000_000L,
		1_000_000_000_000L, 1_000_000_000_000_000L, 1_000_000_000_000_000_000L};
	private static final String[] SUFFIXES = {"", "K", "M", "B", "T", "Q", "E"};
	private VirtualQuantityFormatter() { }

	public static String format(long quantity)
	{
		if (quantity < 0) { throw new IllegalArgumentException("Virtual quantities cannot be negative"); }
		int unit = UNITS.length - 1;
		while (unit > 0 && quantity < UNITS[unit]) { unit--; }
		if (unit == 0) { return Long.toString(quantity); }
		long whole = quantity / UNITS[unit];
		long hundredths = (quantity % UNITS[unit]) / (UNITS[unit] / 100);
		String fraction = "";
		if (whole < 100 && hundredths >= 10) { fraction = "." + hundredths / 10; }
		if (whole < 10 && hundredths % 10 != 0)
		{
			fraction = "." + (hundredths < 10 ? "0" : "") + hundredths;
		}
		return whole + fraction + SUFFIXES[unit];
	}
}
