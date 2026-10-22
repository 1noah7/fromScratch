package com.fromscratch.ge;

import java.util.*;
import lombok.Value;
import net.runelite.api.GrandExchangeOfferState;

/** Read-only bounded journal page, separate from bank movement history. */
public final class GeHistory
{
	private GeHistory(){}
	/** Query-only state, never a new save format. Carries deduplication across finalized pages. */
	@Value public static class Cursor
	{
		UUID revision;
		Set<UUID> seen;
		List<GeTrade> pending;
		public Cursor(UUID revision,Set<UUID> seen,List<GeTrade> pending)
		{this.revision=revision;this.seen=Collections.unmodifiableSet(new HashSet<>(seen));this.pending=Collections.unmodifiableList(new ArrayList<>(pending));}
	}
	@Value public static class Page
	{
		List<GeTrade> rows;
		Cursor next;
		String message;
		boolean scanning;
		public Page(List<GeTrade> rows,Cursor next,String message){this(rows,next,message,false);}
		public Page(List<GeTrade> rows,Cursor next,String message,boolean scanning){this.rows=Collections.unmodifiableList(new ArrayList<>(rows));this.next=next;this.message=message;this.scanning=scanning;}
	}
	/** Derive a completed offer from its durable cumulative observation, never sum/replay fill deltas. */
	public static GeTrade finalized(GeUpdate update)
	{
		GeObservation offer=update.getObservation();
		if(offer==null||offer.getState()==GrandExchangeOfferState.EMPTY||offer.getFilled()==0){return null;}
		boolean partial=offer.getFilled()<offer.getTotal();
		if(partial&&offer.getState()!=GrandExchangeOfferState.CANCELLED_BUY&&offer.getState()!=GrandExchangeOfferState.CANCELLED_SELL){return null;}
		for(GeTrade evidence:update.getTrades())
		{
			if(evidence.getEvidence()==GeTrade.Evidence.UNCERTAIN||evidence.getItemId()!=offer.getItemId()||evidence.isBuy()!=offer.buy()){continue;}
			Long tax=offer.buy()||evidence.getOrigin()==GeOffer.Origin.PREEXISTING?null:GeTax.estimate(offer.getItemId(),offer.getFilled(),offer.getReportedGp());
			return new GeTrade(GeEngine.id("finalized:"+evidence.getGeneration()),evidence.getGeneration(),offer.getSlot(),offer.getItemId(),offer.getName(),offer.buy(),offer.getFilled(),offer.getReportedGp(),tax,
				tax==null?"Not observed":GeTax.method(offer.getItemId(),offer.getFilled(),offer.getReportedGp()),(offer.buy()?"Bought":"Sold")+(partial?" (Partial)":""),evidence.getOrigin(),evidence.getEvidence(),offer.getAt());
		}
		return null;
	}
}
