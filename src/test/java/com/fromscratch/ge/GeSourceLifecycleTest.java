package com.fromscratch.ge;

import java.util.*;
import net.runelite.api.*;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

/** Login/hop packets, late arrays and mutable API fields at the actual source boundary. */
public class GeSourceLifecycleTest
{
	private static class Packet implements GrandExchangeOffer
	{
		GrandExchangeOfferState state=GrandExchangeOfferState.BUYING;int item=ItemID.WOODEN_SHIELD,total=10,filled;long price=100,gp;
		public int getItemId(){return item;}public int getTotalQuantity(){return total;}public int getQuantitySold(){return filled;}public long getPrice(){return price;}public long getSpent(){return gp;}public GrandExchangeOfferState getState(){return state;}
	}
	private static class Access implements RuneLiteGeSource.Access
	{
		boolean logged=true;GrandExchangeOffer[] offers;
		public boolean loggedIn(){return logged;}public GrandExchangeOffer[] offers(){return offers;}public String itemName(int id){return "Wooden shield";}
	}
	private static Packet empty(){Packet p=new Packet();p.state=GrandExchangeOfferState.EMPTY;p.item=-1;p.total=0;p.price=0;return p;}
	@Test public void initialLoggedInEmptyEventsAreIgnored(){Access a=new Access();RuneLiteGeSource s=new RuneLiteGeSource(a);for(int slot=0;slot<8;slot++){s.changed(slot,empty());}assertTrue(s.drain().isEmpty());}
	@Test public void nonLoggedInClearCannotDeleteAnOffer(){Access a=new Access();RuneLiteGeSource s=new RuneLiteGeSource(a);s.changed(0,new Packet());s.drain();a.logged=false;s.changed(0,empty());a.logged=true;assertTrue(s.drain().isEmpty());}
	@Test public void lateCurrentOfferArraySeedsOnlyNonemptySlots(){Access a=new Access();RuneLiteGeSource s=new RuneLiteGeSource(a);assertTrue(s.drain().isEmpty());a.offers=new GrandExchangeOffer[]{empty(),new Packet(),empty(),empty(),empty(),empty(),empty(),empty()};List<GeObservation> rows=s.drain();assertEquals(1,rows.size());assertEquals(1,rows.get(0).getSlot());assertTrue(rows.get(0).isInitial());assertTrue(s.drain().isEmpty());}
	@Test public void confirmedSameSessionClearIsDeliveredAsUnknownCollection(){Access a=new Access();RuneLiteGeSource s=new RuneLiteGeSource(a);s.changed(0,new Packet());s.drain();s.changed(0,empty());GeObservation clear=s.drain().get(0);assertFalse(clear.isInitial());assertEquals(GrandExchangeOfferState.EMPTY,clear.getState());}
	@Test public void reconnectStartsNewObservationSession(){Access a=new Access();RuneLiteGeSource s=new RuneLiteGeSource(a);s.changed(0,new Packet());UUID session=s.drain().get(0).getSession();s.resetSession();s.changed(0,empty());assertTrue(s.drain().isEmpty());s.changed(0,new Packet());assertNotEquals(session,s.drain().get(0).getSession());}
	@Test public void missingOrMalformedOfferIsUnknownAndNeverEmpty(){Access a=new Access();RuneLiteGeSource s=new RuneLiteGeSource(a);s.changed(0,null);Packet p=new Packet();p.filled=11;p.gp=1100;s.changed(0,p);assertTrue(s.drain().isEmpty());assertTrue(s.takeObservationGap());assertFalse(s.takeObservationGap());}
	@Test public void invalidSlotAndNegativeItemAreRejected(){Access a=new Access();RuneLiteGeSource s=new RuneLiteGeSource(a);s.changed(-1,new Packet());s.changed(8,new Packet());Packet p=new Packet();p.item=-1;s.changed(0,p);assertTrue(s.drain().isEmpty());assertTrue(s.takeObservationGap());}
	@Test public void sourcePreservesBeyondMaxCashLongs(){Access a=new Access();RuneLiteGeSource s=new RuneLiteGeSource(a);Packet p=new Packet();p.price=3_000_000_000L;p.total=1;p.filled=1;p.gp=2_900_000_000L;s.changed(0,p);GeObservation o=s.drain().get(0);assertEquals(3_000_000_000L,o.getLimitPrice());assertEquals(2_900_000_000L,o.getReportedGp());}
}
