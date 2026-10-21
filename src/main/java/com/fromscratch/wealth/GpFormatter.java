package com.fromscratch.wealth;

import com.fromscratch.overlay.VirtualQuantityFormatter;

/** Compact GP shares the integer stack policy; grouped exact GP remains available in tooltips. */
public final class GpFormatter
{
	private GpFormatter() { }
	public static String compact(long gp)
	{
		return VirtualQuantityFormatter.signed(gp) + " GP";
	}
	public static String exact(long gp) { return String.format(java.util.Locale.ROOT,"%,d GP",gp); }
}
