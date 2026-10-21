package com.fromscratch.ui;

import com.fromscratch.model.VirtualOwnership;
import com.fromscratch.tracking.Holdings;
import java.util.UUID;
import lombok.Value;
import com.fromscratch.tracking.AccountingPlan;
import com.fromscratch.tracking.ItemTransaction;
import com.fromscratch.tracking.LegacyBankDeficits;
import java.util.List;
import java.util.Collections;

/** Immutable proposal presented on Swing; its token expires when observed holdings change. */
@Value
public class ItemReview
{
	UUID token;
	Holdings before;
	Holdings current;
	VirtualOwnership proposed;
	VirtualOwnership previous;
	String explanation;
	List<ItemTransaction> transactions;
	LegacyBankDeficits previousDeficits;
	LegacyBankDeficits proposedDeficits;
	public ItemReview(UUID token, Holdings before, Holdings current, VirtualOwnership proposed, VirtualOwnership previous, String explanation)
	{
		this(token, before, current, proposed, previous, explanation, Collections.emptyList(), LegacyBankDeficits.empty(), LegacyBankDeficits.empty());
	}
	public ItemReview(UUID token, Holdings before, Holdings current, VirtualOwnership previous,
		LegacyBankDeficits previousDeficits, AccountingPlan plan)
	{
		this(token, before, current, plan.getOwnership(), previous, plan.getExplanation(), plan.getTransactions(), previousDeficits, plan.getProgress().getLegacyDeficits());
	}
	private ItemReview(UUID token, Holdings before, Holdings current, VirtualOwnership proposed, VirtualOwnership previous,
		String explanation, List<ItemTransaction> transactions, LegacyBankDeficits previousDeficits, LegacyBankDeficits proposedDeficits)
	{
		this.token = token; this.before = before; this.current = current; this.proposed = proposed; this.previous = previous;
		this.explanation = explanation; this.transactions = Collections.unmodifiableList(new java.util.ArrayList<>(transactions));
		this.previousDeficits = previousDeficits; this.proposedDeficits = proposedDeficits;
	}
}
