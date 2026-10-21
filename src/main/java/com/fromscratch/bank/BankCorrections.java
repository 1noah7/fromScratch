package com.fromscratch.bank;

import java.util.*;
import java.util.stream.Collectors;

/** Search remains available with no pending differences, including absent negative-balance items. */
public final class BankCorrections
{
	private BankCorrections() { }
	public static List<Integer> search(BankView view, String text)
	{
		String query = text.trim().toLowerCase(Locale.ROOT);
		return view.getNames().keySet().stream().filter(id -> view.name(id).toLowerCase(Locale.ROOT).contains(query) || Integer.toString(id).equals(query))
			.sorted(Comparator.comparing((Integer id) -> view.name(id), String.CASE_INSENSITIVE_ORDER).thenComparingInt(Integer::intValue)).collect(Collectors.toList());
	}
	public static long desired(BankRun run, int id, String input, boolean adjustment)
	{
		long value = Long.parseLong(input.trim()); long desired = adjustment ? Math.addExact(run.balance(id), value) : value;
		if (desired == Long.MIN_VALUE || desired > run.physical(id)) { throw new IllegalArgumentException("Positive progress cannot exceed the physical bank quantity."); }
		Math.subtractExact(desired, run.balance(id)); return desired;
	}
}
