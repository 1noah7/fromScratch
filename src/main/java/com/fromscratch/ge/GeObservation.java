package com.fromscratch.ge;

import java.time.Instant;
import java.util.*;
import lombok.Value;
import net.runelite.api.GrandExchangeOfferState;

/** Client-thread value copy: never retain the client's mutable offer object across threads. */
@Value
public class GeObservation
{
	UUID eventId;
	UUID session;
	Instant at;
	int slot;
	int itemId;
	String name;
	GrandExchangeOfferState state;
	long total;
	long filled;
	long limitPrice;
	long reportedGp;
	boolean initial;
	boolean beforeStart;
	public GeObservation(UUID eventId, UUID session, Instant at, int slot, int itemId, String name,
		GrandExchangeOfferState state, long total, long filled, long limitPrice, long reportedGp, boolean initial)
	{this(eventId,session,at,slot,itemId,name,state,total,filled,limitPrice,reportedGp,initial,false);}
	public GeObservation(UUID eventId, UUID session, Instant at, int slot, int itemId, String name,
		GrandExchangeOfferState state, long total, long filled, long limitPrice, long reportedGp, boolean initial,boolean beforeStart)
	{
		if(eventId==null||session==null||at==null||slot<0||slot>=8||state==null||itemId<0||name==null||name.length()>200
			||total<0||filled<0||filled>total||limitPrice<0||reportedGp<0||((filled==0)!=(reportedGp==0))) { throw new IllegalArgumentException("Invalid GE observation"); }
		if(state!=GrandExchangeOfferState.EMPTY&&(total==0||limitPrice==0)){throw new IllegalArgumentException("Incomplete GE offer");}
		this.eventId=eventId;this.session=session;this.at=at;this.slot=slot;this.itemId=itemId;this.name=name;this.state=state;
		this.total=total;this.filled=filled;this.limitPrice=limitPrice;this.reportedGp=reportedGp;this.initial=initial;this.beforeStart=beforeStart;
	}
	/** Lifecycle evidence avoids guessing provenance from coarse or adjusted wall-clock timestamps. */
	public GeObservation beforeStart(){return new GeObservation(eventId,session,at,slot,itemId,name,state,total,filled,limitPrice,reportedGp,initial,true);}
	public boolean buy(){return state==GrandExchangeOfferState.BUYING||state==GrandExchangeOfferState.BOUGHT||state==GrandExchangeOfferState.CANCELLED_BUY;}
	public boolean terminal(){return state==GrandExchangeOfferState.BOUGHT||state==GrandExchangeOfferState.SOLD||state==GrandExchangeOfferState.CANCELLED_BUY||state==GrandExchangeOfferState.CANCELLED_SELL;}
	public boolean sameOffer(GeObservation other){return itemId==other.itemId&&buy()==other.buy()&&total==other.total&&limitPrice==other.limitPrice;}
	public boolean sameAmounts(GeObservation other){return sameOffer(other)&&state==other.state&&filled==other.filled&&reportedGp==other.reportedGp;}
}
