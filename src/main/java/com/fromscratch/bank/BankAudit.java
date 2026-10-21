package com.fromscratch.bank;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Value;

/** One durable bank operation with optional high-water valuation/explicit-expense evidence, never inferred acquisition provenance. */
@Value
public class BankAudit
{
	public enum Kind { START, AUTOMATIC, SESSION_CHECK, CORRECTION, MIGRATION, ANCHOR, RESET, FINANCIAL_BASELINE, EXPENSE, EXCHANGE_BASELINE, EXCHANGE }
	UUID id;
	UUID session;
	Instant at;
	Kind kind;
	Map<Integer, Long> physicalChanges;
	Map<Integer, Long> balanceChanges;
	String reason;
	java.util.List<com.fromscratch.wealth.ValuationEvent> valuations;
	com.fromscratch.ge.GeUpdate exchange;
	public BankAudit(UUID id, UUID session, Instant at, Kind kind, Map<Integer, Long> physicalChanges,
		Map<Integer, Long> balanceChanges, String reason)
	{ this(id,session,at,kind,physicalChanges,balanceChanges,reason,java.util.Collections.emptyList()); }
	public BankAudit(UUID id, UUID session, Instant at, Kind kind, Map<Integer, Long> physicalChanges,
		Map<Integer, Long> balanceChanges, String reason, java.util.List<com.fromscratch.wealth.ValuationEvent> valuations)
	{this(id,session,at,kind,physicalChanges,balanceChanges,reason,valuations,com.fromscratch.ge.GeUpdate.empty());}
	public BankAudit(UUID id, UUID session, Instant at, Kind kind, Map<Integer, Long> physicalChanges,
		Map<Integer, Long> balanceChanges, String reason, java.util.List<com.fromscratch.wealth.ValuationEvent> valuations, com.fromscratch.ge.GeUpdate exchange)
	{
		this.id = java.util.Objects.requireNonNull(id);
		this.session = java.util.Objects.requireNonNull(session);
		this.at = java.util.Objects.requireNonNull(at);
		this.kind = java.util.Objects.requireNonNull(kind);
		this.physicalChanges = BankRun.quantities(physicalChanges, false);
		this.balanceChanges = BankRun.quantities(balanceChanges, false);
		if (reason == null || reason.length() > 500) { throw new IllegalArgumentException("Invalid bank change description"); }
		this.reason = reason;
		this.valuations=java.util.Collections.unmodifiableList(new java.util.ArrayList<>(valuations));
		this.exchange=java.util.Objects.requireNonNull(exchange);
	}
}
