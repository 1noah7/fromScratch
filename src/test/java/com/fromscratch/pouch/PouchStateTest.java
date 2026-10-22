package com.fromscratch.pouch;

import java.util.*;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

/** Persistence-boundary invariants and deliberate player confirmations. */
public class PouchStateTest
{
	private static void rejected(Runnable r){try{r.run();fail("Expected rejection");}catch(IllegalArgumentException|ArithmeticException expected){}}
	@Test public void emptyCannotBeConfirmedImplicitly(){rejected(()->new PouchAction(PouchAction.Kind.FINISH,Collections.emptyMap(),false,false,"Not verified."));}
	@Test public void arbitraryItemsCannotEnterLegacyRunePlan(){rejected(()->new PouchAction(PouchAction.Kind.REGISTER,Collections.singletonMap(ItemID.WOODEN_SHIELD,5L),false,false,"Wrong type."));}
	@Test public void negativeOrUnbalancedStateIsRejected(){rejected(()->new PouchState(PouchState.Phase.PREPARED,UUID.randomUUID(),Collections.singletonMap(ItemID.NATURERUNE,500L),Collections.emptyMap(),Collections.emptyMap(),Collections.emptyMap(),null));rejected(()->PouchState.quantities(Collections.singletonMap(ItemID.NATURERUNE,-1L)));}
	@Test public void recordedBudgetCannotOverflow(){rejected(()->new PouchState(PouchState.Phase.PREPARED,UUID.randomUUID(),Collections.singletonMap(ItemID.NATURERUNE,Long.MAX_VALUE),Collections.singletonMap(ItemID.NATURERUNE,Long.MAX_VALUE),Collections.singletonMap(ItemID.NATURERUNE,1L),Collections.emptyMap(),null));}
	@Test public void observedZeroOrNegativeDeltasDoNotConsumePlan(){PouchState state=PouchEngine.apply(PouchState.unreviewed(),new PouchAction(PouchAction.Kind.REGISTER,Collections.singletonMap(ItemID.NATURERUNE,500L),false,false,"Verified contents."),UUID.randomUUID());assertSame(state,PouchEngine.bank(state,Collections.singletonMap(ItemID.NATURERUNE,-500L)));assertSame(state,PouchEngine.bank(state,Collections.emptyMap()));assertEquals(Long.valueOf(500),state.getRemaining().get(ItemID.NATURERUNE));}
	@Test public void completedPlanCannotBeRegisteredOrChargedAgain(){PouchState ready=PouchEngine.apply(PouchState.unreviewed(),new PouchAction(PouchAction.Kind.FINISH,Collections.emptyMap(),true,false,"Verified empty."),UUID.randomUUID());rejected(()->PouchEngine.apply(ready,new PouchAction(PouchAction.Kind.REGISTER,Collections.singletonMap(ItemID.NATURERUNE,500L),false,false,"Duplicate."),UUID.randomUUID()));}
}
