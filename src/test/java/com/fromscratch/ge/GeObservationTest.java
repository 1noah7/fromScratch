package com.fromscratch.ge;

import com.fromscratch.SnapshotFixtures;
import java.util.*;
import net.runelite.api.*;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

/** Real RuneLite offer interfaces with mutable synthetic packets; no account or client needed. */
public class GeObservationTest
{
	static final UUID SESSION=UUID.randomUUID(),RUN=UUID.randomUUID();
	static GeObservation observation(int slot,GrandExchangeOfferState state,long filled,long gp)
	{return new GeObservation(UUID.randomUUID(),SESSION,SnapshotFixtures.TIME.plusSeconds(1),slot,ItemID.WOODEN_SHIELD,"Wooden shield",state,10,filled,100,gp,false);}
	static GeEngine.Result apply(GeState state,GeObservation value){return GeEngine.observe(state,value,RUN);}
	@Test public void firstOfferEstablishesAnUncertainBaseline(){GeEngine.Result r=apply(GeState.empty(),observation(0,GrandExchangeOfferState.BUYING,0,0));assertEquals(GeOffer.Origin.UNKNOWN,r.getState().getOffers().get(0).getOrigin());assertEquals(0,r.getState().getPurchases());}
	@Test public void partialBuyCountsOnlyIncrementalFills(){GeState s=apply(GeState.empty(),observation(0,GrandExchangeOfferState.BUYING,3,285)).getState();GeEngine.Result r=apply(s,observation(0,GrandExchangeOfferState.BUYING,7,660));assertEquals(4,r.getEvidence().getTrades().get(0).getQuantity());assertEquals(375,r.getEvidence().getTrades().get(0).getReportedGp());}
	@Test public void repeatedIdenticalPacketsAreIdempotent(){GeObservation o=observation(0,GrandExchangeOfferState.SELLING,2,200);GeState s=apply(GeState.empty(),o).getState();assertSame(s,apply(s,o).getState());}
	@Test public void partialCancelDoesNotReplayExecutedQuantity(){GeState s=apply(GeState.empty(),observation(0,GrandExchangeOfferState.BUYING,3,285)).getState();GeEngine.Result r=apply(s,observation(0,GrandExchangeOfferState.CANCELLED_BUY,3,285));assertEquals(0,r.getEvidence().getTrades().get(0).getQuantity());assertTrue(r.getState().getCredits().stream().anyMatch(c->c.getKind()==GeCredit.Kind.BUY_REFUND&&c.getRemaining()==715));}
	@Test public void independentSlotsHaveIndependentGenerations(){GeState s=apply(GeState.empty(),observation(0,GrandExchangeOfferState.BUYING,0,0)).getState();s=apply(s,observation(1,GrandExchangeOfferState.BUYING,0,0)).getState();assertEquals(2,s.getOffers().size());assertNotEquals(s.getOffers().get(0).getGeneration(),s.getOffers().get(1).getGeneration());}
	@Test public void initialEmptyNeverClosesSavedOffer(){GeState s=apply(GeState.empty(),observation(0,GrandExchangeOfferState.BUYING,3,285)).getState();GeObservation clear=new GeObservation(UUID.randomUUID(),UUID.randomUUID(),SnapshotFixtures.TIME,0,0,"",GrandExchangeOfferState.EMPTY,0,0,0,0,true);assertSame(s,apply(s,clear).getState());}
	@Test public void observedClearThenReuseCreatesNewGeneration(){GeState s=apply(GeState.empty(),observation(0,GrandExchangeOfferState.BOUGHT,10,950)).getState();UUID generation=s.getOffers().get(0).getGeneration();s=apply(s,new GeObservation(UUID.randomUUID(),SESSION,SnapshotFixtures.TIME,0,0,"",GrandExchangeOfferState.EMPTY,0,0,0,0,false)).getState();s=apply(s,observation(0,GrandExchangeOfferState.BUYING,0,0)).getState();assertNotEquals(generation,s.getOffers().get(0).getGeneration());assertEquals(GeOffer.Origin.OBSERVED_NEW,s.getOffers().get(0).getOrigin());}
	@Test public void outOfOrderPacketDoesNotInventNegativeFill(){GeState s=apply(GeState.empty(),observation(0,GrandExchangeOfferState.SELLING,7,700)).getState();GeEngine.Result r=apply(s,observation(0,GrandExchangeOfferState.SELLING,3,300));assertEquals(7,r.getState().getOffers().get(0).getLast().getFilled());assertEquals(GeTrade.Evidence.UNCERTAIN,r.getEvidence().getTrades().get(0).getEvidence());}
	@Test public void taxUsesPerItemCapAndNoBuyerCharge(){assertEquals(10_000_000,GeTax.estimate(ItemID.WOODEN_SHIELD,2,2_000_000_000));assertEquals(0,GeTax.estimate(ItemID.MINDRUNE,100,100000));assertEquals(0,GeTax.estimate(ItemID.WOODEN_SHIELD,10,490));}
	@Test public void runtimeSourceCopiesValuesAndIgnoresLoginClears()
	{
		class Packet implements GrandExchangeOffer {long gp;public int getQuantitySold(){return 0;}public int getItemId(){return ItemID.WOODEN_SHIELD;}public int getTotalQuantity(){return 10;}public long getPrice(){return 100;}public long getSpent(){return gp;}public GrandExchangeOfferState getState(){return GrandExchangeOfferState.BUYING;}}
		Packet packet=new Packet();RuneLiteGeSource source=new RuneLiteGeSource(new RuneLiteGeSource.Access(){public boolean loggedIn(){return true;}public GrandExchangeOffer[] offers(){return null;}public String itemName(int id){return "Wooden shield";}});
		source.changed(0,packet);java.util.List<GeObservation> copy=source.drain();packet.gp=999;assertEquals(0,copy.get(0).getReportedGp());assertTrue(source.drain().isEmpty());
	}
	@Test public void terminalPacketReplayAfterClearDoesNotInventAConfirmedNewExecution()
	{
		GeState s=apply(GeState.empty(),observation(0,GrandExchangeOfferState.BOUGHT,10,950)).getState();
		s=apply(s,new GeObservation(UUID.randomUUID(),SESSION,SnapshotFixtures.TIME,0,0,"",GrandExchangeOfferState.EMPTY,0,0,0,0,false)).getState();
		GeEngine.Result r=apply(s,observation(0,GrandExchangeOfferState.BOUGHT,10,950));assertEquals(GeOffer.Origin.UNKNOWN,r.getState().getOffers().get(0).getOrigin());assertEquals(GeTrade.Evidence.EXISTING_SNAPSHOT,r.getEvidence().getTrades().get(0).getEvidence());assertEquals(0,r.getState().getPurchases());
	}
	@Test public void delayedInProgressPacketCannotReopenTerminalGenerationOrRecreateRefunds()
	{
		GeState s=apply(GeState.empty(),observation(0,GrandExchangeOfferState.BOUGHT,10,950)).getState();assertSame(s,apply(s,observation(0,GrandExchangeOfferState.BUYING,10,950)).getState());
	}
	@Test public void publishedToolAndBondExemptionsUseVerifiedItemConstants()
	{for(int id:new int[]{ItemID.OSRS_BOND,ItemID.SPADE,ItemID.POH_SAW,ItemID.WATERING_CAN_0,ItemID.GLASSBLOWINGPIPE,ItemID.MINDRUNE}){assertEquals(0,GeTax.estimate(id,10,100000));assertTrue(GeTax.method(id,10,100000).contains("exemption"));}}
	@Test public void preStartLifecycleEvidencePreservesTheOriginalTimestamp()
	{GeObservation o=observation(0,GrandExchangeOfferState.BOUGHT,10,950).beforeStart();GeEngine.Result r=GeEngine.observe(GeState.empty(),o,RUN);assertEquals(GeOffer.Origin.PREEXISTING,r.getState().getOffers().get(0).getOrigin());assertEquals(o.getAt(),r.getState().getOffers().get(0).getLast().getAt());}
	@Test public void unchangedResumeRefreshesSessionSoLaterClearAndReuseAreRecognized()
	{
		GeState s=apply(GeState.empty(),observation(0,GrandExchangeOfferState.BUYING,3,285)).getState();UUID generation=s.getOffers().get(0).getGeneration(),newSession=UUID.randomUUID();
		GeObservation resume=new GeObservation(UUID.randomUUID(),newSession,SnapshotFixtures.TIME,0,ItemID.WOODEN_SHIELD,"Wooden shield",GrandExchangeOfferState.BUYING,10,3,100,285,true);
		s=apply(s,resume).getState();assertEquals(generation,s.getOffers().get(0).getGeneration());assertEquals(newSession,s.getOffers().get(0).getLast().getSession());assertEquals(0,s.getPurchases());
		s=apply(s,new GeObservation(UUID.randomUUID(),newSession,SnapshotFixtures.TIME,0,0,"",GrandExchangeOfferState.EMPTY,0,0,0,0,false)).getState();assertTrue(s.getOffers().get(0).isClosed());
		s=apply(s,new GeObservation(UUID.randomUUID(),newSession,SnapshotFixtures.TIME,0,ItemID.WOODEN_SHIELD,"Wooden shield",GrandExchangeOfferState.BUYING,10,0,100,0,false)).getState();assertNotEquals(generation,s.getOffers().get(0).getGeneration());
	}
}
