package com.fromscratch.ui;

import java.util.UUID;

/** UI requests only local plugin operations; implementations never perform player actions. */
public interface SetupActions
{
	SetupActions NONE = new SetupActions()
	{
		public void begin() { }
		public void confirm(UUID reviewToken) { }
		public void cancel() { }
		public void reset(UUID runId) { }
		public void retry() { }
	};

	void begin();
	void confirm(UUID reviewToken);
	void cancel();
	void reset(UUID runId);
	void retry();
	default void reviewItems() { }
	default void confirmItems(UUID token, java.util.Map<Integer, com.fromscratch.model.VirtualItemOwnership> corrections, String reason, boolean permanentlyRemoved) { }
	default void confirmUpdate(UUID token, com.fromscratch.tracking.ItemReviewEdits edits)
	{ confirmItems(token, edits.getOwnership(), edits.getReason(), false); }
	default void cancelUpdate(UUID token) { }
}
