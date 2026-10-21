package com.fromscratch.ge;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import lombok.Value;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.gameval.ItemID;

/** Pure cumulative-offer reducer. Attribution never changes physical or signed bank quantities. */
public final class GeEngine
{
	public static final Duration MATCH_WINDOW=Duration.ofHours(24);
	private GeEngine(){}
	@Value public static class Result { GeState state; GeUpdate evidence; }
	public static UUID id(String text){return UUID.nameUUIDFromBytes(text.getBytes(StandardCharsets.UTF_8));}
	public static Result observe(GeState before,GeObservation now,UUID run)
	{
		GeOffer previous=before.getOffers().get(now.getSlot());
		if(now.getState()==GrandExchangeOfferState.EMPTY)
		{
			// An initial empty can be a login clear. No cancellation, fill or collection is inferred.
			if(now.isInitial()||previous==null||previous.isClosed()||!now.getSession().equals(previous.getLast().getSession())){return unchanged(before);}
			Map<Integer,GeOffer> slots=new TreeMap<>(before.getOffers());slots.put(now.getSlot(),new GeOffer(previous.getGeneration(),previous.getLast(),previous.getOrigin(),true,previous.isUncertain()));
			GeTrade status=trade(previous.getGeneration(),now,previous.getLast(),previous.getOrigin(),0,0,GeTrade.Evidence.STATUS,"Slot cleared; collection destination unknown.");
			return result(before,slots,before.getCredits(),before.getMovements(),Collections.singletonList(status),Collections.emptyList(),now);
		}
		boolean matching=previous!=null&&!previous.isClosed()&&previous.getLast().sameOffer(now);
		if(matching&&previous.getLast().sameAmounts(now))
		{
			if(previous.getLast().getSession().equals(now.getSession())){return unchanged(before);}
			// Refresh connection context without replaying fills. Otherwise a later genuine
			// clear in this session would be mistaken for an initial login clear forever.
			Map<Integer,GeOffer> slots=new TreeMap<>(before.getOffers());slots.put(now.getSlot(),new GeOffer(previous.getGeneration(),now,previous.getOrigin(),false,previous.isUncertain()||now.isInitial()));
			return result(before,slots,before.getCredits(),before.getMovements(),Collections.emptyList(),Collections.emptyList(),now);
		}
		if(matching&&previous.getLast().terminal()&&!now.terminal()&&now.getFilled()>=previous.getLast().getFilled())
		{return unchanged(before); /* Delayed in-progress packets cannot reopen a terminal generation or recreate refunds. */}
		if(matching&&(now.getFilled()<previous.getLast().getFilled()||now.getReportedGp()<previous.getLast().getReportedGp()))
		{
			if(!now.isInitial())
			{
				if(previous.isUncertain()){return unchanged(before);}
				Map<Integer,GeOffer> slots=new TreeMap<>(before.getOffers());slots.put(now.getSlot(),new GeOffer(previous.getGeneration(),previous.getLast(),previous.getOrigin(),false,true));
				GeTrade issue=trade(previous.getGeneration(),now,previous.getLast(),previous.getOrigin(),0,0,GeTrade.Evidence.UNCERTAIN,"Out-of-order offer data ignored; earlier evidence preserved.");
				return result(before,slots,before.getCredits(),before.getMovements(),Collections.singletonList(issue),Collections.emptyList(),now);
			}
			matching=false; // Offline regression can be identical slot reuse, never a negative fill.
		}
		UUID generation=matching?previous.getGeneration():id(run+":"+now.getEventId()+":"+now.getSlot());
		boolean observedPlacement=previous!=null&&previous.isClosed()&&!now.isInitial()
			&&(!previous.getLast().sameOffer(now)||!now.terminal()&&now.getFilled()==0);
		GeOffer.Origin origin=matching?previous.getOrigin():now.isBeforeStart()?GeOffer.Origin.PREEXISTING:
			observedPlacement?GeOffer.Origin.OBSERVED_NEW:GeOffer.Origin.UNKNOWN;
		long quantity=matching?Math.subtractExact(now.getFilled(),previous.getLast().getFilled()):now.getFilled();
		long gp=matching?Math.subtractExact(now.getReportedGp(),previous.getLast().getReportedGp()):now.getReportedGp();
		GeTrade.Evidence kind=quantity==0?GeTrade.Evidence.STATUS:!matching&&origin!=GeOffer.Origin.OBSERVED_NEW?GeTrade.Evidence.EXISTING_SNAPSHOT:
			now.isInitial()?GeTrade.Evidence.COMPATIBLE_NET_FILL:GeTrade.Evidence.FILL;
		if((quantity==0)!=(gp==0)){kind=GeTrade.Evidence.UNCERTAIN;quantity=0;gp=0;}
		String description=kind==GeTrade.Evidence.EXISTING_SNAPSHOT?"Existing cumulative snapshot; earlier executions/timing unknown.":
			kind==GeTrade.Evidence.COMPATIBLE_NET_FILL?"Compatible net fill since last observation; offline slot reuse cannot be excluded.":now.getState().name().replace('_',' ');
		GeTrade row=trade(generation,now,now,origin,quantity,gp,kind,description);
		Map<Integer,GeOffer> slots=new TreeMap<>(before.getOffers());slots.put(now.getSlot(),new GeOffer(generation,now,origin,false,now.isInitial()||kind==GeTrade.Evidence.UNCERTAIN));
		List<GeCredit> credits=liveCredits(before,now.getAt());List<GeMovement> movements=liveMovements(before,now.getAt());List<GeMatch> matches=new ArrayList<>();
		if(quantity>0)
		{
			addCredit(credits,row,now.buy()?now.getItemId():ItemID.COINS,now.buy()?quantity:gp,now.buy()?GeCredit.Kind.PURCHASE:GeCredit.Kind.SALE_PROCEEDS);
			// A compatible earlier withdrawal is an association, not proof that those copies were sold.
			matchWithdrawal(movements,matches,row,now.buy()?ItemID.COINS:now.getItemId(),now.buy()?gp:quantity);
		}
		boolean firstTerminal=now.terminal()&&(!matching||!previous.getLast().terminal());
		if(firstTerminal)
		{
			if(now.buy())
			{
				try{long refund=Math.subtractExact(Math.multiplyExact(now.getTotal(),now.getLimitPrice()),now.getReportedGp());if(refund>0){addCredit(credits,row,ItemID.COINS,refund,GeCredit.Kind.BUY_REFUND);}}
				catch(ArithmeticException ex){/* No representable refund budget: retain the reported trade, never invent one. */}
			}
			else if(now.getState()==GrandExchangeOfferState.CANCELLED_SELL&&now.getTotal()>now.getFilled())
			{addCredit(credits,row,now.getItemId(),now.getTotal()-now.getFilled(),GeCredit.Kind.UNSOLD_RETURN);}
		}
		return result(before,slots,credits,movements,Collections.singletonList(row),matches,now);
	}
	private static GeTrade trade(UUID generation,GeObservation event,GeObservation values,GeOffer.Origin origin,long quantity,long gp,GeTrade.Evidence kind,String status)
	{
		Long tax=!values.buy()&&quantity>0&&(kind==GeTrade.Evidence.FILL||kind==GeTrade.Evidence.COMPATIBLE_NET_FILL)&&origin!=GeOffer.Origin.PREEXISTING?GeTax.estimate(values.getItemId(),quantity,gp):null;
		return new GeTrade(id(generation+":"+event.getEventId()+":"+kind),generation,event.getSlot(),values.getItemId(),values.getName(),values.buy(),quantity,gp,tax,
			tax==null?"Not observed":GeTax.method(values.getItemId(),quantity,gp),status,origin,kind,event.getAt());
	}
	/** Consume compatible positive movements once. The financial engine caps exclusions at its new high-water portion. */
	public static Result bank(GeState before,UUID bankEvent,Map<Integer,Long> delta,Instant at)
	{
		List<GeCredit> credits=liveCredits(before,at);List<GeMovement> movements=liveMovements(before,at);List<GeMatch> matches=new ArrayList<>();Map<Integer,Long> excluded=new TreeMap<>();
		for(Map.Entry<Integer,Long> entry:delta.entrySet())
		{
			int item=entry.getKey();long remaining=entry.getValue();
			if(remaining<0){movements.add(new GeMovement(bankEvent,item,Math.negateExact(remaining),at));continue;}
			for(ListIterator<GeCredit> it=credits.listIterator();remaining>0&&it.hasNext();)
			{
				GeCredit credit=it.next();long factor=credit.getItemId()==ItemID.COINS&&item==ItemID.PLATINUM?1000:1;
				if(credit.getItemId()!=item&&factor==1){continue;}long used=Math.min(credit.getRemaining()/factor,remaining);if(used==0){continue;}remaining-=used;long gpUsed=Math.multiplyExact(used,factor);
				matches.add(new GeMatch(credit.getTradeId(),bankEvent,item,used,true,credit.getOrigin()));excluded.merge(item,used,Math::addExact);
				if(gpUsed==credit.getRemaining()){it.remove();}else{it.set(credit.remaining(credit.getRemaining()-gpUsed));}
			}
		}
		trim(movements,128);
		GeState state=new GeState(before.getOffers(),credits,movements,before.getRecent(),before.getPurchases(),before.getSales(),before.getEstimatedTaxGp(),before.getTaxRecords(),before.getArchive());
		return new Result(state,new GeUpdate(null,Collections.emptyList(),matches,excluded));
	}
	private static void matchWithdrawal(List<GeMovement> movements,List<GeMatch> matches,GeTrade trade,int item,long quantity)
	{
		for(ListIterator<GeMovement> it=movements.listIterator();quantity>0&&it.hasNext();)
		{
			GeMovement movement=it.next();if(movement.getItemId()!=item){continue;}long used=Math.min(quantity,movement.getRemaining());quantity-=used;
			matches.add(new GeMatch(trade.getId(),movement.getBankEvent(),item,used,false,trade.getOrigin()));
			if(used==movement.getRemaining()){it.remove();}else{it.set(new GeMovement(movement.getBankEvent(),item,movement.getRemaining()-used,movement.getAt()));}
		}
	}
	private static void addCredit(List<GeCredit> credits,GeTrade trade,int item,long quantity,GeCredit.Kind kind)
	{if(quantity>0){credits.add(new GeCredit(id(trade.getId()+":"+kind),trade.getId(),item,quantity,kind,trade.getAt(),trade.getOrigin()));trim(credits,256);}}
	private static Result result(GeState before,Map<Integer,GeOffer> offers,List<GeCredit> credits,List<GeMovement> movements,List<GeTrade> rows,List<GeMatch> matches,GeObservation observation)
	{
		List<GeTrade> recent=new ArrayList<>(rows);recent.addAll(before.getRecent());trimTail(recent,30);long purchases=before.getPurchases(),sales=before.getSales(),tax=before.getEstimatedTaxGp(),taxRecords=before.getTaxRecords();
		for(GeTrade row:rows){if(row.execution()){if(row.isBuy()){purchases=Math.incrementExact(purchases);}else{sales=Math.incrementExact(sales);}}if(row.getEstimatedTax()!=null&&row.getEstimatedTax()>0){tax=Math.addExact(tax,row.getEstimatedTax());taxRecords=Math.incrementExact(taxRecords);}}
		return new Result(new GeState(offers,credits,movements,recent,purchases,sales,tax,taxRecords,before.getArchive()),new GeUpdate(observation,rows,matches,Collections.emptyMap()));
	}
	private static Result unchanged(GeState state){return new Result(state,GeUpdate.empty());}
	private static List<GeCredit> liveCredits(GeState state,Instant at){List<GeCredit> result=new ArrayList<>();for(GeCredit credit:state.getCredits()){if(!credit.getAt().isAfter(at)&&Duration.between(credit.getAt(),at).compareTo(MATCH_WINDOW)<=0){result.add(credit);}}return result;}
	private static List<GeMovement> liveMovements(GeState state,Instant at){List<GeMovement> result=new ArrayList<>();for(GeMovement movement:state.getMovements()){if(!movement.getAt().isAfter(at)&&Duration.between(movement.getAt(),at).compareTo(MATCH_WINDOW)<=0){result.add(movement);}}return result;}
	private static <T> void trim(List<T> list,int max){while(list.size()>max){list.remove(0);}}
	private static <T> void trimTail(List<T> list,int max){while(list.size()>max){list.remove(list.size()-1);}}
}
