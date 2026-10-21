package com.fromscratch.tracking;

import com.fromscratch.FromScratchConfig;
import com.fromscratch.model.RunRecord;
import com.fromscratch.model.SnapshotLocation;
import com.fromscratch.model.VirtualLocation;
import com.fromscratch.observation.ItemObservation;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/** Opt-in single-item DEBUG trace. Never logs a whole snapshot, character, path or account identifier. */
@Singleton
@Slf4j
public final class ItemTrackingDiagnostics
{
	private static final long INTERVAL_NANOS = 2_000_000_000L;
	private volatile boolean enabled;
	private volatile int itemId;
	private final Map<String, Long> last = new HashMap<>();
	public synchronized void configure(FromScratchConfig config)
	{
		enabled = config.developmentItemTrace(); itemId = config.developmentTraceItemId(); last.clear();
	}
	public void observe(RunRecord run, ItemObservation observation, boolean gate)
	{
		if (!enabled || !log.isDebugEnabled()) { return; }
		StringBuilder after = new StringBuilder();
		for (SnapshotLocation location : SnapshotLocation.values())
		{
			com.fromscratch.model.ContainerSnapshot container = observation.getProgress().getContainers().get(location);
			after.append(location).append('=').append(container == null ? "unknown" : container.quantity(itemId)).append(' ');
		}
		write("OBSERVE", "before=" + quantities(run.getItemProgress().getPhysical()) + " after=" + after
			+ " gate=" + gate + " stable=" + observation.isStable() + " bankOpen=" + observation.isBankOpen()
			+ " bankContext=" + observation.isBankInteraction() + " receipt=" + observation.getOrdinaryReceipts().getOrDefault(itemId, 0L));
	}
	private String quantities(Holdings holdings)
	{
		StringBuilder text = new StringBuilder();
		for (SnapshotLocation location : SnapshotLocation.values()) { text.append(location).append('=').append(holdings.at(itemId, location)).append(' '); }
		return text.toString();
	}
	public void plan(AccountingPlan plan, boolean pending)
	{
		if (!enabled || !log.isDebugEnabled()) { return; }
		StringBuilder classifications = new StringBuilder();
		for (ItemTransaction entry : plan.getTransactions())
		{ if (entry.getItemId() == itemId) { classifications.append(entry.getCategory()).append(' '); } }
		write("PLAN", "class=" + classifications + " pendingReview=" + pending + " reason=" + plan.getExplanation()
			+ " earnedBank=" + plan.getOwnership().at(itemId, VirtualLocation.BANK) + " deficit=" + plan.getProgress().getLegacyDeficits().quantity(itemId));
	}
	public void pending(boolean pending, String reason) { if (enabled) { write("REVIEW", "pending=" + pending + " reason=" + reason); } }
	public void committed(RunRecord run)
	{
		if (!enabled || !log.isDebugEnabled()) { return; }
		long bank = run.getOwnership().at(itemId, VirtualLocation.BANK);
		write("COMMIT", "success=true earnedBank=" + bank + " deficit=" + run.getItemProgress().getLegacyDeficits().quantity(itemId)
			+ " display=" + run.getItemProgress().getLegacyDeficits().display(itemId, bank));
	}
	public void failed(String reason) { if (enabled) { write("COMMIT", "success=false reason=" + reason); } }
	public void rendered(int id, long earnedBank, long balance)
	{
		if (enabled && id == itemId) { write("RENDER", "earnedBank=" + earnedBank + " display=" + balance); }
	}
	private synchronized void write(String stage, String message)
	{
		if (!enabled || !log.isDebugEnabled()) { return; }
		long now = System.nanoTime(); Long previous = last.get(stage);
		if (previous != null && now - previous < INTERVAL_NANOS) { return; }
		last.put(stage, now);
		log.debug("From Scratch item trace: item={} stage={} {}", itemId, stage, message);
	}
}
