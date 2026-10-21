package com.fromscratch.bank;

import com.fromscratch.model.ContainerSnapshot;
import java.time.Instant;
import java.util.UUID;
import lombok.Value;

/** Only a non-null bank is a complete, stable, currently mounted observation. */
@Value
public class BankObservation
{
	UUID session;
	long tick;
	Instant at;
	ContainerSnapshot bank;
	String instruction;
	BankTimings.Trace trace;
	public BankObservation(UUID session, long tick, Instant at, ContainerSnapshot bank, String instruction)
	{ this(session, tick, at, bank, instruction, null); }
	public BankObservation(UUID session, long tick, Instant at, ContainerSnapshot bank, String instruction, BankTimings.Trace trace)
	{ this.session = session; this.tick = tick; this.at = at; this.bank = bank; this.instruction = instruction; this.trace = trace; }
	public boolean isReady() { return bank != null; }
}
