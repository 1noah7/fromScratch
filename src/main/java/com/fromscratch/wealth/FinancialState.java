package com.fromscratch.wealth;

import java.time.Instant;
import java.util.*;
import lombok.Value;

/** Bounded checkpoint totals and per-ID high-water marks; full event evidence remains in linked journals. */
@Value
public class FinancialState
{
	Instant measuredFrom;
	boolean importedBaseline;
	Map<Integer,Long> highWater;
	long gainedGp;
	Map<Integer,Long> unpricedGains;
	long expenseGp;
	long expenseRecords;
	long eventCount;
	String importArchive;
	public FinancialState(Instant measuredFrom,boolean importedBaseline,Map<Integer,Long> highWater,long gainedGp,Map<Integer,Long> unpricedGains,long expenseGp,long expenseRecords,long eventCount)
	{this(measuredFrom,importedBaseline,highWater,gainedGp,unpricedGains,expenseGp,expenseRecords,eventCount,null);}
	public FinancialState(Instant measuredFrom,boolean importedBaseline,Map<Integer,Long> highWater,long gainedGp,Map<Integer,Long> unpricedGains,long expenseGp,long expenseRecords,long eventCount,String importArchive)
	{
		if(measuredFrom==null||gainedGp<0||expenseGp<0||expenseRecords<0||eventCount<expenseRecords){throw new IllegalArgumentException("Invalid financial checkpoint");}
		this.measuredFrom=measuredFrom;this.importedBaseline=importedBaseline;this.highWater=positive(highWater);this.gainedGp=gainedGp;this.unpricedGains=positive(unpricedGains);this.expenseGp=expenseGp;this.expenseRecords=expenseRecords;this.eventCount=eventCount;
		if((expenseRecords==0)!=(expenseGp==0)){throw new IllegalArgumentException("Expense total lacks matching records");}
		this.unpricedGains.forEach((id,n)->{if(n>this.highWater.getOrDefault(id,0L)){throw new IllegalArgumentException("Unknown gains exceed recognized high-water");}});
		if(importArchive!=null&&(!importedBaseline||!importArchive.matches("bank-migration-[a-f0-9-]{36}"))){throw new IllegalArgumentException("Invalid financial archive reference");}this.importArchive=importArchive;
	}
	public FinancialState archived(String archive){return new FinancialState(measuredFrom,importedBaseline,highWater,gainedGp,unpricedGains,expenseGp,expenseRecords,eventCount,archive);}
	private static Map<Integer,Long> positive(Map<Integer,Long> values)
	{
		Map<Integer,Long> result=new TreeMap<>();Objects.requireNonNull(values).forEach((id,n)->{if(id==null||id<0||n==null||n<0){throw new IllegalArgumentException("Invalid financial quantity");}if(n>0){result.put(id,n);}});return Collections.unmodifiableMap(result);
	}
	public static FinancialState baseline(Map<Integer,Long> balances,Instant at,boolean imported)
	{
		Map<Integer,Long> h=new TreeMap<>();balances.forEach((id,n)->{if(n>0){h.put(id,n);}});return new FinancialState(at,imported,h,0,Collections.emptyMap(),0,0,0);
	}
}
