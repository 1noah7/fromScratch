package com.fromscratch.wealth;

import com.fromscratch.bank.BankAudit;
import java.time.Instant;
import java.util.*;
import net.runelite.api.gameval.ItemID;

/** Conservative bank high-water recognition. Never treats withdrawals, corrections or price refresh as income. */
public final class FinancialEngine
{
	private FinancialEngine() { }
	public static List<ValuationEvent> recognize(FinancialState before,Map<Integer,Long> balances,UUID audit,Map<Integer,PriceQuote> prices,Instant at)
	{
		List<ValuationEvent> events=new ArrayList<>();
		gains(before,balances).forEach((id,q)->events.add(new ValuationEvent(ValuationEvent.identity(audit,ValuationEvent.Kind.GAIN,id),ValuationEvent.Kind.GAIN,id,q,
			id==ItemID.COINS?PriceQuote.coins(at):prices.getOrDefault(id,PriceQuote.unknown(id)),"New bank progression high-water mark.")));
		return events;
	}
	private static Map<Integer,Long> gains(FinancialState before,Map<Integer,Long> balances)
	{
		Map<Integer,Long> result=new TreeMap<>();balances.forEach((id,n)->{long high=before.getHighWater().getOrDefault(id,0L);if(n>high){result.put(id,Math.subtractExact(n,high));}});return result;
	}
	/** Also called at the persistence boundary: invalid/missing/duplicate event evidence rejects the entire commit. */
	public static FinancialState advance(FinancialState before,Map<Integer,Long> balances,BankAudit audit)
	{
		Map<Integer,Long> required=audit.getKind()==BankAudit.Kind.AUTOMATIC?gains(before,balances):Collections.emptyMap();
		Map<Integer,Long> actual=new TreeMap<>(),unknown=new TreeMap<>(before.getUnpricedGains()),high=new TreeMap<>(before.getHighWater());
		long gained=before.getGainedGp(),expense=before.getExpenseGp(),records=before.getExpenseRecords(),count=before.getEventCount();Set<UUID> ids=new HashSet<>();
		for(ValuationEvent e:audit.getValuations())
		{
			if(!ids.add(e.getId())||!e.getId().equals(ValuationEvent.identity(audit.getId(),e.getKind(),e.getItemId()))){throw new IllegalArgumentException("Duplicate or mismatched financial event");}
			if(e.getKind()==ValuationEvent.Kind.GAIN)
			{
				if(actual.put(e.getItemId(),e.getQuantity())!=null){throw new IllegalArgumentException("Duplicate gain");}
				if(e.getQuote().verifiedNow()){gained=Math.addExact(gained,e.knownGp());}else{unknown.put(e.getItemId(),Math.addExact(unknown.getOrDefault(e.getItemId(),0L),e.getQuantity()));}
			}
			else
			{
				if(audit.getKind()!=BankAudit.Kind.EXPENSE||e.getItemId()!=ItemID.COINS||e.getQuote().getSource()!=PriceQuote.Source.COINS||!e.getQuote().verifiedNow()||e.getQuote().getUnitGp()!=1L){throw new IllegalArgumentException("Unverified expense");}
				expense=Math.addExact(expense,e.knownGp());records=Math.incrementExact(records);
			}
			count=Math.incrementExact(count);
		}
		if(!required.equals(actual)||(audit.getKind()==BankAudit.Kind.EXPENSE&&audit.getValuations().size()!=1)){throw new IllegalArgumentException("Financial evidence does not match bank progression");}
		// Corrections can raise the comparison ceiling, but never earn value or lower it for later replay.
		balances.forEach((id,n)->{if(n>high.getOrDefault(id,0L)){high.put(id,n);}});
		return new FinancialState(before.getMeasuredFrom(),before.isImportedBaseline(),high,gained,unknown,expense,records,count,before.getImportArchive());
	}
}
