package com.fromscratch.ui;

import java.time.Duration;
import java.time.Instant;

/** Elapsed run time, including time away; this is not a claimed play-time counter. */
public final class RunDuration
{
	private RunDuration() { }
	public static long minutes(Instant start, Instant now) { return start == null ? 0 : Math.max(0, Duration.between(start, now).toMinutes()); }
	public static String format(long minutes)
	{
		long hours = minutes / 60;
		return (hours >= 24 ? (hours / 24) + "d " : "") + (hours % 24) + "h " + String.format(java.util.Locale.ROOT, "%02d", minutes % 60) + "m";
	}
}
