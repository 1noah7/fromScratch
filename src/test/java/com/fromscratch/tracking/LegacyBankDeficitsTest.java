package com.fromscratch.tracking;

import com.fromscratch.model.*;
import com.fromscratch.overlay.VirtualQuantityFormatter;
import java.util.*;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.fromscratch.tracking.TrackingFixtures.*;

public class LegacyBankDeficitsTest
{
	@Test public void legacyDeficitDoesNotUseOrPermitNegativeOwnership()
	{
		RunRecord old = run(100, 0, 0, 0, 0, 0); AccountingPlan next = plan(old, holdings(99, 1, 0), 2);
		assertEquals(-1, next.getProgress().getLegacyDeficits().display(ItemID.SHARK, 0));
		assertEquals(0, next.getOwnership().total(ItemID.SHARK)); assertEquals(0, old.getItemProgress().getLegacyDeficits().quantity(ItemID.SHARK));
	}
	@Test public void earnedCopiesDoNotRepayOutstandingLegacyWithdrawals()
	{
		RunRecord old = run(100, 0, 0, 0, 0, 0); RunRecord withdrawn = old.withItems(plan(old, holdings(98, 2, 0), 2));
		RunRecord earned = withdrawn.withItems(plan(withdrawn, holdings(98, 5, 0), 3));
		AccountingPlan depositEarned = plan(earned, holdings(101, 2, 0), 4);
		assertEquals(3, depositEarned.getOwnership().at(ItemID.SHARK, VirtualLocation.BANK));
		assertEquals(2, depositEarned.getProgress().getLegacyDeficits().quantity(ItemID.SHARK));
		assertEquals(1, depositEarned.getProgress().getLegacyDeficits().display(ItemID.SHARK, 3));
		assertEquals(1, depositEarned.getProgress().getAcquisitions().size());
	}
	@Test public void legacyReturnDoesNotAlterLifetimeAcquisitions()
	{
		RunRecord old = run(100, 0, 0, 0, 0, 0); RunRecord withdrawn = old.withItems(plan(old, holdings(98, 2, 0), 2));
		RunRecord earned = withdrawn.withItems(plan(withdrawn, holdings(98, 5, 0), 3));
		RunRecord deposited = earned.withItems(plan(earned, holdings(101, 2, 0), 4));
		AccountingPlan returned = plan(deposited, holdings(103, 0, 0), 5);
		assertEquals(0, returned.getProgress().getLegacyDeficits().quantity(ItemID.SHARK)); assertEquals(3, returned.getOwnership().total(ItemID.SHARK));
		assertEquals(deposited.getItemProgress().getUnlocked(), returned.getProgress().getUnlocked());
		assertEquals(deposited.getItemProgress().getAcquisitions(), returned.getProgress().getAcquisitions());
	}
	@Test public void normalSessionLocationOnlyChangesAutomaticallyReopenTracking()
	{
		ItemTrackingSession session = new ItemTrackingSession(run(100, 0, 0, 0, 0, 0), id -> id, false);
		AccountingPlan automatic = session.observe(observation(holdings(99, 1, 0), 2));
		assertNotNull(automatic); assertNull(session.getReview()); assertEquals(0, automatic.getOwnership().total(ItemID.SHARK));
		session.committed(session.getRun().withItems(automatic)); assertFalse(session.isPreparing());
	}
	@Test public void explicitNotedLegacyWithdrawalAndReturnUseOneDeficitIdentity()
	{
		int noted = ItemID.SHARK + 1; RunRecord run = run(100, 0, 0, 0, 0, 0);
		AccountingPlan withdrawal = ENGINE.plan(run, observation(holdings(items(ItemID.SHARK, 98), items(noted, 2), Collections.emptyMap()), 2),
			id -> id == noted ? ItemID.SHARK : id, false, false, Collections.emptyMap(), "");
		assertEquals(2, withdrawal.getProgress().getLegacyDeficits().quantity(ItemID.SHARK));
		AccountingPlan returned = ENGINE.plan(run.withItems(withdrawal), observation(holdings(100, 0, 0), 3),
			id -> id == noted ? ItemID.SHARK : id, false, false, Collections.emptyMap(), "");
		assertEquals(0, returned.getProgress().getLegacyDeficits().quantity(ItemID.SHARK)); assertEquals(0, returned.getOwnership().total(ItemID.SHARK));
	}
	@Test public void checkedExtremeSignedDisplayHasAMinusWithoutOverflow()
	{
		LegacyBankDeficits deficits = new LegacyBankDeficits(Collections.singletonMap(ItemID.SHARK, Long.MAX_VALUE));
		assertEquals(-Long.MAX_VALUE, deficits.display(ItemID.SHARK, 0));
		assertEquals("-1", VirtualQuantityFormatter.signed(-1)); assertEquals("-2500", VirtualQuantityFormatter.signed(-2500));
		assertEquals("-9223372036854M", VirtualQuantityFormatter.signed(-Long.MAX_VALUE));
	}
	@Test(expected = IllegalArgumentException.class) public void negativeDeficitsAreRejected() { new LegacyBankDeficits(Collections.singletonMap(ItemID.SHARK, -1L)); }
	@Test(expected = ArithmeticException.class) public void accumulatingBeyondLongRangeCannotWrap()
	{
		RunRecord run = run(100, 0, 0, 0, 0, 0); ItemProgress p = run.getItemProgress();
		ItemProgress max = new ItemProgress(p.getPhysical(), p.getReturns(), p.getUnlocked(), p.getRecent(), p.getAcquisitions(), p.getSessionId(), p.getTick(),
			p.getObservedAt(), p.getLedgerHead(), p.getSequence(), true, new LegacyBankDeficits(Collections.singletonMap(ItemID.SHARK, Long.MAX_VALUE)));
		RunRecord fixture = new RunRecord(run.getState(), run.getRevision(), null, run.getSnapshot(), run.getOwnership(), max);
		plan(fixture, holdings(99, 1, 0), 2);
	}
}
