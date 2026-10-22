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
	default void buyback(UUID revision,String catalogId,UUID purchaseId){throw new UnsupportedOperationException("Buybacks unavailable");}
	default void pouchPreview(java.util.function.Consumer<com.fromscratch.pouch.PouchObservation> callback){callback.accept(com.fromscratch.pouch.PouchObservation.unknown());}
	default void buyback(UUID revision,String catalogId,UUID purchaseId,com.fromscratch.pouch.PouchObservation contents,java.util.function.Consumer<com.fromscratch.buyback.BuybackPurchase> committed){buyback(revision,catalogId,purchaseId);}
	default void recordExpense(UUID revision,long gp,String reason) { throw new UnsupportedOperationException("Expense recording is unavailable"); }
	default void exchangeHistory(boolean buy,BankHistory.Cursor cursor,java.util.function.Consumer<com.fromscratch.ge.GeHistory.Page> callback)
	{callback.accept(new com.fromscratch.ge.GeHistory.Page(java.util.Collections.emptyList(),null,"No GE history recorded."));}
	default void history(boolean manual, BankHistory.Cursor cursor, java.util.function.Consumer<BankHistory.Page> callback)
	{ callback.accept(new BankHistory.Page(java.util.Collections.emptyList(), null, "No bank changes recorded.")); }
}
