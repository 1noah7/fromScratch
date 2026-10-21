package com.fromscratch.wealth;

import com.fromscratch.bank.BankRun;
import java.time.Instant;
import java.util.*;
import java.math.BigInteger;
import lombok.Value;

/** Derived worker-calculated current market subtotal, never a persisted or speculative bank operation. */
@Value
public class BankValuation
{
	UUID revision;
	Long subtotalGp;
	int missingTypes;
	int staleTypes;
	Instant oldestReceivedAt;
	int pricedTypes;
	public static BankValuation calculate(BankRun run,Map<Integer,PriceQuote> prices)
	{
		BigInteger total=BigInteger.ZERO;int missing=0,stale=0,priced=0;Instant oldest=null;
		for(Map.Entry<Integer,Long> entry:run.getBalances().entrySet())
		{
			if(entry.getValue()==0){continue;}PriceQuote quote=prices.get(entry.getKey());
			if(entry.getKey()==net.runelite.api.gameval.ItemID.COINS){quote=PriceQuote.coins(Instant.EPOCH);}
			if(quote==null||quote.getUnitGp()==null){missing++;continue;}
			priced++;
			if(quote.isStale()){stale++;}if(quote.getSource()!=PriceQuote.Source.COINS&&(oldest==null||quote.getReceivedAt().isBefore(oldest))){oldest=quote.getReceivedAt();}
			total=total.add(BigInteger.valueOf(entry.getValue()).multiply(BigInteger.valueOf(quote.getUnitGp())));
		}
		Long value;try{value=total.longValueExact();}catch(ArithmeticException ex){value=null;}
		return new BankValuation(run.getRevision(),value,missing,stale,oldest,priced);
	}
	public String label() { return subtotalGp==null?"Out of range":pricedTypes==0&&missingTypes>0?"Unavailable":GpFormatter.compact(subtotalGp)+(missingTypes>0?"*":staleTypes>0?"~":""); }
	public String explanation()
	{
		return (subtotalGp==null?"Valuation exceeds signed long range.":pricedTypes==0&&missingTypes>0?"No verified current price records available.":GpFormatter.exact(subtotalGp)+"; "+(missingTypes>0?"priced subtotal":"signed bank market estimate"))+". "+missingTypes+" unpriced types; "+staleTypes+" stale types. RuneLite GE guide; oldest received: "+(oldestReceivedAt==null?"N/A":oldestReceivedAt)+". Coins included once. Not a spendable wallet.";
	}
}
