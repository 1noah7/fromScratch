package com.fromscratch.persistence;

import com.fromscratch.model.RunRecord;
import com.fromscratch.model.SnapshotLocation;
import com.fromscratch.model.VirtualLocation;
import com.fromscratch.tracking.ItemLedgerBatch;
import com.fromscratch.tracking.ItemTransaction;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import net.runelite.client.util.Filepath;

/** Historical M4 journal compatibility. The live bank repository only verifies/archives these segments. */
final class ItemJournalStore
{
	private final RunRecordCodec codec;
	ItemJournalStore(RunRecordCodec codec) { this.codec = codec; }
	void append(Filepath directory, RunRecord before, RunRecord after, java.util.List<ItemTransaction> entries) throws IOException
	{
		validateDeltas(before, after, entries);
		ItemLedgerBatch batch = new ItemLedgerBatch(after.getRevision(), before.getItemProgress().getLedgerHead(), before.getRevision(),
			after.getState().getCharacterKey(), after.getState().getRunId(), after.getItemProgress().getSessionId(),
			after.getItemProgress().getSequence(), after.getItemProgress().getObservedAt(), SaveIntegrity.sha256(codec.encode(after)), entries);
		if (batch.getSequence() != Math.incrementExact(before.getItemProgress().getSequence())) { throw new SaveException("Ledger sequence changed; reload before retrying."); }
		Filepath file = path(directory, batch.getRunId(), batch.getId());
		if (!file.exists()) { BankStorageFiles.writeNew(file, codec.journal.encode(batch)); }
		if (!read(file).equals(batch)) { throw new SaveException("Ledger batch conflicts with existing recovery evidence. Files were preserved."); }
	}
	void verifyHead(Filepath directory, RunRecord run) throws IOException
	{
		verifyHead(directory, run, RunRecordCodec.FORMAT_VERSION);
	}
	private void verifyHead(Filepath directory, RunRecord run, int format) throws IOException
	{
		if (run.getItemProgress() == null || run.getItemProgress().getLedgerHead() == null) { return; }
		ItemLedgerBatch batch = read(path(directory, run.getState().getRunId(), run.getItemProgress().getLedgerHead()));
		if (!batch.getId().equals(run.getRevision()) || batch.getSequence() != run.getItemProgress().getSequence()
			|| !batch.getCharacter().equals(run.getState().getCharacterKey()) || !batch.getRunId().equals(run.getState().getRunId())
			|| !batch.getSessionId().equals(run.getItemProgress().getSessionId()) || !batch.getTime().equals(run.getItemProgress().getObservedAt())
			|| !batch.getResultSha256().equals(SaveIntegrity.sha256(codec.encodeVersion(run, format))))
		{ throw new SaveException("Item ledger and checkpoint disagree. Files were preserved; reload is blocked."); }
	}
	void verifyChain(Filepath directory, RunRecord run) throws IOException
	{
		verifyChain(directory, run, RunRecordCodec.FORMAT_VERSION);
	}
	void verifyChain(Filepath directory, RunRecord run, int format) throws IOException
	{
		verifyHead(directory, run, format);
		if (run.getItemProgress() == null) { return; }
		UUID head = run.getItemProgress().getLedgerHead(); long sequence = run.getItemProgress().getSequence();
		Set<UUID> transactions = new HashSet<>();
		while (head != null)
		{
			ItemLedgerBatch batch = read(path(directory, run.getState().getRunId(), head));
			if (batch.getSequence() != sequence || !head.equals(batch.getId()) || !batch.getCharacter().equals(run.getState().getCharacterKey())
				|| !batch.getRunId().equals(run.getState().getRunId()) || (sequence > 1 && !batch.getPreviousRevision().equals(batch.getPreviousHead())))
			{ throw new SaveException("Item journal chain is incomplete or belongs to another run. Files were preserved."); }
			for (ItemTransaction entry : batch.getEntries()) { if (!transactions.add(entry.getId())) { throw new SaveException("Duplicate transaction in item journal; files were preserved."); } }
			head = batch.getPreviousHead(); sequence--;
		}
		if (sequence != 0) { throw new SaveException("Item journal chain is incomplete; files were preserved."); }
		for (ItemTransaction entry : run.getItemProgress().getRecent()) { if (!transactions.contains(entry.getId())) { throw new SaveException("Recent history has no journal entry."); } }
		for (ItemTransaction entry : run.getItemProgress().getAcquisitions()) { if (!transactions.contains(entry.getId())) { throw new SaveException("Acquisition history has no journal entry."); } }
	}
	private ItemLedgerBatch read(Filepath file) throws IOException
	{
		if (!file.exists()) { throw new SaveException("An item journal segment is missing. Files were preserved; restore its matching checkpoint and ledger."); }
		return codec.journal.decode(BankStorageFiles.readText(file));
	}
	private static Filepath path(Filepath directory, UUID run, UUID id) { return directory.joinSegment("ledger-" + run + "-" + id + ".json"); }
	private static void validateDeltas(RunRecord before, RunRecord after, java.util.List<ItemTransaction> entries) throws SaveException
	{
		Map<Integer, Long> totals = new HashMap<>(before.getOwnership().getTotals());
		Map<Integer, Long> withdrawals = new HashMap<>(before.getItemProgress().getLegacyDeficits().getItems());
		Map<Integer, Map<SnapshotLocation, Long>> locations = new HashMap<>();
		before.getOwnership().getItems().forEach((id, item) ->
		{
			Map<SnapshotLocation, Long> counts = new java.util.EnumMap<>(SnapshotLocation.class);
			for (SnapshotLocation location : SnapshotLocation.values()) { counts.put(location, item.at(VirtualLocation.valueOf(location.name()))); }
			locations.put(id, counts);
		});
		for (ItemTransaction entry : entries)
		{
			if (entry.getItemId() < 0) { continue; }
			totals.merge(entry.getItemId(), entry.getVirtualChange(), Math::addExact);
			withdrawals.merge(entry.getItemId(), entry.getLegacyWithdrawalChange(), Math::addExact);
			Map<SnapshotLocation, Long> counts = locations.computeIfAbsent(entry.getItemId(), ignored -> new java.util.EnumMap<>(SnapshotLocation.class));
			entry.getLocationChanges().forEach((location, delta) -> counts.merge(location, delta, Math::addExact));
		}
		Set<Integer> ids = new HashSet<>(totals.keySet()); ids.addAll(after.getOwnership().getItems().keySet());
		ids.addAll(withdrawals.keySet()); ids.addAll(after.getItemProgress().getLegacyDeficits().getItems().keySet());
		for (int id : ids)
		{
			if (totals.getOrDefault(id, 0L) != after.getOwnership().total(id)) { throw new SaveException("Transaction totals do not match the item checkpoint."); }
			if (withdrawals.getOrDefault(id, 0L) != after.getItemProgress().getLegacyDeficits().quantity(id)) { throw new SaveException("Legacy withdrawal deltas do not match the item checkpoint."); }
			for (SnapshotLocation location : SnapshotLocation.values())
			{
				long expected = locations.getOrDefault(id, java.util.Collections.emptyMap()).getOrDefault(location, 0L);
				if (expected != after.getOwnership().at(id, VirtualLocation.valueOf(location.name()))) { throw new SaveException("Transaction locations do not match the item checkpoint."); }
			}
		}
	}
}
