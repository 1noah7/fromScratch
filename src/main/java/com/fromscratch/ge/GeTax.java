package com.fromscratch.ge;

import net.runelite.api.gameval.ItemID;
import java.util.*;

/** Explicit 2% average-price estimate, not observed tax or a second debit from bank coins. */
public final class GeTax
{
	public static final long PER_ITEM_CAP=5_000_000;
	private static final Set<Integer> EXEMPT=Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
		ItemID.CHISEL,ItemID.DIBBER,ItemID.GARDENING_TROWEL,ItemID.GLASSBLOWINGPIPE,ItemID.HAMMER,
		ItemID.NEEDLE,ItemID.PESTLE_AND_MORTAR,ItemID.RAKE,ItemID.SECATEURS,ItemID.SHEARS,ItemID.SPADE,
		ItemID.WATERING_CAN_0,ItemID.POH_SAW,ItemID.OSRS_BOND,ItemID.MINDRUNE)));
	private GeTax(){}
	public static long estimate(int itemId,long quantity,long reportedGp)
	{
		if(quantity<=0||reportedGp<0){throw new IllegalArgumentException("Invalid tax inputs");}
		// Explicit items from Jagex's December 2021 / May 2025 announcements. Broad new
		// food/ammo categories are not guessed; other amounts remain labelled estimates.
		if(EXEMPT.contains(itemId)){return 0;}
		return Math.multiplyExact(quantity,Math.min(PER_ITEM_CAP,(reportedGp/quantity)/50));
	}
	public static String method(int itemId,long quantity,long gp)
	{
		return EXEMPT.contains(itemId)?"Published Jagex item exemption":gp/quantity<50?"Average below 50 GP: estimated zero":"Estimated 2% of average, floored per item, capped 5M each; exemptions/rate unverified";
	}
}
