package com.fromscratch.bank;

import com.fromscratch.model.ContainerSnapshot;
import java.time.Instant;
import java.util.*;
import lombok.Value;

/** Bank checkpoint with fixed historical financial totals. Initial bank and imported legacy evidence never change. */
@Value
public class BankRun
{
	String character;
	UUID runId;
	UUID previousRunId;
	Instant startedAt;
	UUID revision;
	long sequence;
	ContainerSnapshot initialBank;
	Map<Integer, Long> physical;
	Map<Integer, Long> balances;
	boolean anchored;
	String legacyDocument;
	String migrationArchive;
	List<BankAudit> recent;
	BankAudit hostSummary;
	com.fromscratch.wealth.FinancialState financial;
	com.fromscratch.ge.GeState exchange;
	public BankRun(String character, UUID runId, UUID previousRunId, Instant startedAt, UUID revision, long sequence,
		ContainerSnapshot initialBank, Map<Integer, Long> physical, Map<Integer, Long> balances, boolean anchored,
		String legacyDocument, String migrationArchive, List<BankAudit> recent, BankAudit hostSummary)
	{ this(character,runId,previousRunId,startedAt,revision,sequence,initialBank,physical,balances,anchored,legacyDocument,migrationArchive,recent,hostSummary,
		com.fromscratch.wealth.FinancialState.baseline(balances,startedAt==null?recent.get(0).getAt():startedAt,false)); }
	public BankRun(String character, UUID runId, UUID previousRunId, Instant startedAt, UUID revision, long sequence,
		ContainerSnapshot initialBank, Map<Integer, Long> physical, Map<Integer, Long> balances, boolean anchored,
		String legacyDocument, String migrationArchive, List<BankAudit> recent, BankAudit hostSummary, com.fromscratch.wealth.FinancialState financial)
	{this(character,runId,previousRunId,startedAt,revision,sequence,initialBank,physical,balances,anchored,legacyDocument,migrationArchive,recent,hostSummary,financial,com.fromscratch.ge.GeState.empty());}
	public BankRun(String character, UUID runId, UUID previousRunId, Instant startedAt, UUID revision, long sequence,
		ContainerSnapshot initialBank, Map<Integer, Long> physical, Map<Integer, Long> balances, boolean anchored,
		String legacyDocument, String migrationArchive, List<BankAudit> recent, BankAudit hostSummary, com.fromscratch.wealth.FinancialState financial, com.fromscratch.ge.GeState exchange)
	{
		if (character == null || !character.matches("[a-f0-9]{64}")) { throw new IllegalArgumentException("Unverified character identity"); }
		if (sequence < 1 || recent == null || recent.isEmpty() || recent.size() > 30) { throw new IllegalArgumentException("Incomplete bank history"); }
		this.character = character; this.runId = runId; this.previousRunId = previousRunId; this.startedAt = startedAt;
		this.revision = Objects.requireNonNull(revision); this.sequence = sequence; this.initialBank = initialBank;
		this.physical = quantities(physical, true); this.balances = quantities(balances, false); this.anchored = anchored;
		this.legacyDocument = legacyDocument; this.migrationArchive = migrationArchive;
		this.recent = Collections.unmodifiableList(new ArrayList<>(recent)); this.hostSummary = hostSummary;
		this.financial=Objects.requireNonNull(financial);
		this.exchange=Objects.requireNonNull(exchange);
		this.balances.forEach((id,n)->{if(n>financial.getHighWater().getOrDefault(id,0L)){throw new IllegalArgumentException("Financial high-water below bank progress");}});
		if (!revision.equals(recent.get(0).getId())) { throw new IllegalArgumentException("Bank history head mismatch"); }
		if (migrationArchive != null && !migrationArchive.matches("bank-migration-[a-f0-9-]{36}")) { throw new IllegalArgumentException("Invalid archive reference"); }
		if ((legacyDocument == null) != (migrationArchive == null)) { throw new IllegalArgumentException("Incomplete migration evidence"); }
		if (runId == null)
		{
			if(!exchange.equals(com.fromscratch.ge.GeState.empty())){throw new IllegalArgumentException("Reset state retains GE progress");}
			if (startedAt != null || initialBank != null || !physical.isEmpty() || !balances.isEmpty() || anchored) { throw new IllegalArgumentException("Invalid reset state"); }
			if(!financial.getHighWater().isEmpty()||financial.getEventCount()!=0||financial.getGainedGp()!=0||financial.getExpenseGp()!=0||!financial.getUnpricedGains().isEmpty()){throw new IllegalArgumentException("Reset state retains active financial progress");}
		}
		else if (startedAt == null || initialBank == null) { throw new IllegalArgumentException("Incomplete active bank run"); }
		if (anchored) { this.balances.forEach((id, n) -> { if (n > this.physical.getOrDefault(id, 0L)) { throw new IllegalArgumentException("Bank progress exceeds physical bank quantity"); } }); }
	}
	public boolean isActive() { return runId != null; }
	public long balance(int id) { return balances.getOrDefault(id, 0L); }
	public long physical(int id) { return physical.getOrDefault(id, 0L); }
	public long unlocked() { return balances.values().stream().filter(n -> n > 0).count(); }
	/** Sparse zero records are omitted, not interpreted as corruption. MIN_VALUE is excluded for safe signed display/deltas. */
	static Map<Integer, Long> quantities(Map<Integer, Long> values, boolean positive)
	{
		TreeMap<Integer, Long> result = new TreeMap<>();
		Objects.requireNonNull(values).forEach((id, n) ->
		{
			if (id == null || id < 0 || n == null || n == Long.MIN_VALUE || (positive && n < 0)) { throw new IllegalArgumentException("Invalid bank quantity"); }
			if (n != 0) { result.put(id, n); }
		});
		return Collections.unmodifiableMap(result);
	}
}
