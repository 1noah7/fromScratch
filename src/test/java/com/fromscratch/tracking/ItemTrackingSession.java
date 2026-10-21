package com.fromscratch.tracking;

import com.fromscratch.model.RunRecord;
import com.fromscratch.model.VirtualItemOwnership;
import com.fromscratch.observation.ItemObservation;
import com.fromscratch.ui.ItemReview;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntUnaryOperator;
import lombok.Getter;

/** Client-confined host-session gate. Merely observing a restored run never commits a reconciliation. */
public final class ItemTrackingSession
{
	private final ItemAccountingEngine engine = new ItemAccountingEngine();
	private final IntUnaryOperator noteBase;
	@Getter private RunRecord run;
	@Getter private boolean preparing = true;
	@Getter private ItemReview review;
	@Getter private boolean reviewRequested;
	private UUID reviewSession;
	private boolean cancellableReview;
	@Getter private AccountingPlan lastPlan;
	@Getter private String message = "Checking saved item holdings when your bank is available.";
	public ItemTrackingSession(RunRecord run, IntUnaryOperator noteBase, boolean freshActivation)
	{
		this.run = run; this.noteBase = noteBase;
	}
	public void requestReview()
	{
		cancellableReview = !preparing;
		preparing = true; reviewRequested = true; review = null; message = "Review your item changes in Account Tools.";
	}
	public void cancelReview(UUID token)
	{
		if (review != null && !review.getToken().equals(token)) { return; }
		if (cancellableReview) { preparing = false; reviewRequested = false; message = "Item tracking active."; }
		review = null; cancellableReview = false;
	}
	public void changed() { review = null; }
	/** Return only automatic candidates; review proposals remain inert until confirm(). */
	public AccountingPlan observe(ItemObservation observation)
	{
		lastPlan = null;
		if (observation == null || observation.getHoldings() == null || !observation.isStable())
		{
			review = null;
			if (preparing && observation != null)
			{
				message = observation.isBankOpen() && !observation.getProgress().ready(com.fromscratch.model.SnapshotLocation.BANK)
					? "Keep the normal bank open while its contents load."
					: !observation.isStable() ? "Let item movements finish, then review the settled comparison."
					: "Complete the remaining observation steps. Empty containers are valid.";
			}
			else if (observation != null && observation.getHoldings() == null)
			{
				message = com.fromscratch.ui.PreparationChecklist.instruction(observation.getProgress()) + " Previously saved ownership is preserved.";
			}
			return null;
		}
		if (!preparing) { message = "Item tracking active."; }
		if (preparing)
		{
			if (!observation.isBankOpen()) { review = null; return null; }
			if (review != null && observation.getSessionId().equals(reviewSession) && review.getCurrent().equals(observation.getHoldings())) { return null; }
			AccountingPlan plan = engine.plan(run, observation, noteBase, true, false, Collections.emptyMap(), "");
			lastPlan = plan;
			boolean transfersOnly = plan.getTransactions().stream().allMatch(entry -> entry.getCategory() == ItemTransaction.Category.TRANSFER
				|| entry.getLegacyWithdrawalChange() != 0 || entry.getItemId() == -1);
			if (!reviewRequested && !observation.isUncertainBankChange() && (transfersOnly
				|| run.getItemProgress().getPhysical().equals(observation.getHoldings()) && plan.getOwnership().equals(run.getOwnership())))
			{ return plan; } // unchanged sessions can safely reopen tracking without an approval or speculative credit
			review = new ItemReview(UUID.randomUUID(), run.getItemProgress().getPhysical(), observation.getHoldings(),
				run.getOwnership(), run.getItemProgress().getLegacyDeficits(), plan);
			reviewSession = observation.getSessionId();
			reviewRequested = true;
			message = plan.getExplanation(); return null;
		}
		if (run.getItemProgress().getPhysical().equals(observation.getHoldings()) && !observation.isUncertainBankChange()) { return null; }
		AccountingPlan plan = engine.plan(run, observation, noteBase, false, false, Collections.emptyMap(), "");
		lastPlan = plan;
		if (plan.isReviewRequired()) { preparing = true; reviewRequested = true; review = null; message = plan.getExplanation(); return null; }
		return plan;
	}
	public AccountingPlan confirm(UUID token, ItemObservation observation, Map<Integer, VirtualItemOwnership> corrections,
		String reason, boolean permanentlyRemoved)
	{
		return confirm(token, observation, corrections, reason, permanentlyRemoved, null);
	}
	public AccountingPlan confirm(UUID token, ItemObservation observation, ItemReviewEdits edits)
	{
		return confirm(token, observation, edits.getOwnership(), edits.getReason(), false, edits);
	}
	private AccountingPlan confirm(UUID token, ItemObservation observation, Map<Integer, VirtualItemOwnership> corrections,
		String reason, boolean permanentlyRemoved, ItemReviewEdits edits)
	{
		if (!preparing || review == null || !review.getToken().equals(token) || observation == null || !observation.isStable()
			|| !observation.getSessionId().equals(reviewSession) || !observation.isBankOpen() || observation.getHoldings() == null || !review.getCurrent().equals(observation.getHoldings()))
		{ review = null; throw new IllegalStateException("The observed items changed. Review a fresh comparison before confirming."); }
		return engine.plan(run, observation, noteBase, true, permanentlyRemoved, corrections, reason, edits);
	}
	public void committed(RunRecord saved)
	{
		if (!run.getState().getRunId().equals(saved.getState().getRunId())) { throw new IllegalArgumentException("Run changed during item commit"); }
		run = saved; preparing = false; reviewRequested = false; cancellableReview = false; review = null; message = "Item tracking active.";
	}
}
