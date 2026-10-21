package com.fromscratch.tracking;

import com.fromscratch.model.VirtualOwnership;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Value;

/** Pure candidate; a review-required plan has no authority until explicitly confirmed and durably saved. */
@Value
public class AccountingPlan
{
	VirtualOwnership ownership;
	ItemProgress progress;
	List<ItemTransaction> transactions;
	boolean reviewRequired;
	String explanation;
	public AccountingPlan(VirtualOwnership ownership, ItemProgress progress, List<ItemTransaction> transactions, boolean reviewRequired, String explanation)
	{
		progress.validateOwnership(ownership);
		this.ownership = ownership; this.progress = progress;
		this.transactions = Collections.unmodifiableList(new ArrayList<>(transactions));
		this.reviewRequired = reviewRequired; this.explanation = explanation;
	}
}
