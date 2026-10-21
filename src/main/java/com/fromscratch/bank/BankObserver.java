package com.fromscratch.bank;

import com.fromscratch.model.ContainerSnapshot;
import java.time.Instant;
import java.util.UUID;

/** Capture once after the next game-tick boundary; container events invalidate cached observations. */
public final class BankObserver
{
	public interface Access { boolean normalBankOpen(); ContainerSnapshot readBank(); default BankTimings.Trace trace() { return null; } }
	private final Access access;
	private UUID session = UUID.randomUUID();
	private long tick;
	private long changedAt = -1;
	private ContainerSnapshot candidate;
	private BankObservation cached;
	public BankObserver(Access access) { this.access = access; }
	public void reset() { session = UUID.randomUUID(); tick = 0; changedAt = -1; candidate = null; cached = null; }
	public void tick() { tick++; }
	public void changed() { candidate = null; cached = null; changedAt = tick; }
	public BankObservation observe()
	{
		if (!access.normalBankOpen()) { changed(); return waiting("Open your bank. Unlock the bank PIN first if prompted."); }
		if (tick <= changedAt) { return waiting("Reading your bank…"); }
		if (cached != null) { return cached; }
		ContainerSnapshot now;
		try { now = access.readBank(); }
		catch (RuntimeException ex) { changed(); return waiting("Bank data is incomplete. Close and reopen the bank to retry."); }
		if (now == null) { changed(); return waiting("Waiting for complete bank data. If this persists, close and reopen the bank."); }
		candidate = now;
		cached = new BankObservation(session, tick, Instant.now(), candidate, "Bank ready.", access.trace());
		return cached;
	}
	private BankObservation waiting(String text) { return new BankObservation(session, tick, Instant.now(), null, text); }
}
