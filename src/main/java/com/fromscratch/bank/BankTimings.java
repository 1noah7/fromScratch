package com.fromscratch.bank;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/** Opt-in monotonic, sampled phase timings. No item contents, identities or file paths are recorded. */
@Singleton @Slf4j
public final class BankTimings
{
	public enum Phase { EVENT, CAPTURE, AGGREGATE, DIFFERENCE, PREPARED, WRITE_START, WRITE_END, PUBLISHED, OVERLAY, SWING }
	private volatile boolean enabled;
	private long lastSample;
	private Trace current;
	public synchronized void configure(boolean enabled) { this.enabled = enabled; if (!enabled) { current = null; } }
	public synchronized void event()
	{
		if (!enabled || !log.isDebugEnabled()) { current = null; return; }
		long now = System.nanoTime();
		if (current != null && !current.reached(Phase.PUBLISHED)) { return; }
		current = lastSample != 0 && now - lastSample < 5_000_000_000L ? null : new Trace(System::nanoTime, message -> log.debug("From Scratch timing: {}", message));
		if (current != null) { lastSample = now; }
	}
	public synchronized Trace current() { return current; }
	public static void mark(Trace trace, Phase phase) { if (trace != null) { trace.mark(phase); } }
	public static final class Trace
	{
		private final LongSupplier clock;
		private final Consumer<String> output;
		private final long[] times = new long[Phase.values().length];
		private boolean complete;
		Trace(LongSupplier clock, Consumer<String> output) { this.clock = clock; this.output = output; Arrays.fill(times, -1); mark(Phase.EVENT); }
		public synchronized boolean reached(Phase phase) { return times[phase.ordinal()] >= 0; }
		public synchronized void mark(Phase phase)
		{
			if (times[phase.ordinal()] >= 0) { return; }
			times[phase.ordinal()] = clock.getAsLong();
			if (phase == Phase.PUBLISHED) { output.accept(summary("committed")); }
			if (!complete && reached(Phase.PUBLISHED) && reached(Phase.OVERLAY) && reached(Phase.SWING)) { complete = true; output.accept(summary("presented")); }
		}
		public synchronized String summary(String label)
		{
			StringBuilder text = new StringBuilder(label).append(" elapsed_ms[");
			for (Phase phase : Phase.values()) { long n = times[phase.ordinal()]; text.append(phase).append('=').append(n < 0 ? "pending" : String.format(java.util.Locale.ROOT, "%.3f", (n - times[0]) / 1_000_000.0)).append(' '); }
			return text.append(']').toString();
		}
		public synchronized void failed() { if (!complete) { complete = true; output.accept(summary("save_failed")); } }
	}
}
