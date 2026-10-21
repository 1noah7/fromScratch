package com.fromscratch.observation;

import java.util.concurrent.TimeUnit;

/** Monotonic, client-thread-only throttle shared by success and failure diagnostics. */
final class DiagnosticRateLimit
{
	private static final long INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(10);
	private boolean logged;
	private long last;

	boolean allow(long now)
	{
		if (logged && now - last < INTERVAL_NANOS) { return false; }
		logged = true;
		last = now;
		return true;
	}
}
