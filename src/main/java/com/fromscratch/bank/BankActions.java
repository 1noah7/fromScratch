package com.fromscratch.bank;

import java.util.UUID;

/** User intent only; no operation operates the game UI. */
public interface BankActions
{
	void startRun(boolean attested);
	void anchor(UUID revision, com.fromscratch.model.ContainerSnapshot reviewedBank, boolean approveReductions);
	void correct(UUID revision, int itemId, long desired);
	void reset(UUID revision);
	void retry();
	default void recordExpense(UUID revision,long gp,String reason) { throw new UnsupportedOperationException("Expense recording is unavailable"); }
	default void history(boolean manual, BankHistory.Cursor cursor, java.util.function.Consumer<BankHistory.Page> callback)
	{ callback.accept(new BankHistory.Page(java.util.Collections.emptyList(), null, "No bank changes recorded.")); }
}
