package com.fromscratch.pouch;

import java.time.Instant;
import java.util.*;
import lombok.Value;

/** Current pouch values associated with one banked form; unavailable is distinct from verified empty. */
@Value
public class PouchObservation
{
	boolean available;
	Map<Integer,Long> quantities;
	Instant at;
	int pouchId;
	String problem;
	public PouchObservation(boolean available,Map<Integer,Long> quantities,Instant at){this(available,quantities,at,-1);}
	public PouchObservation(boolean available,Map<Integer,Long> quantities,Instant at,int pouchId){this(available,quantities,at,pouchId,"Rune Pouch data is not available yet. Keep the bank open, wait a game tick and retry.");}
	private PouchObservation(boolean available,Map<Integer,Long> quantities,Instant at,int pouchId,String problem){this.available=available;this.quantities=PouchState.quantities(quantities);this.at=Objects.requireNonNull(at);this.pouchId=pouchId;this.problem=Objects.requireNonNull(problem);if(pouchId!=-1&&!com.fromscratch.buyback.BuybackCatalog.variants("rune_pouch_family").containsKey(pouchId)){throw new IllegalArgumentException("Invalid pouch form");}}
	public boolean verified(){return available&&pouchId>0;}
	public boolean fresh(Instant now){return verified()&&!at.isAfter(now)&&now.isBefore(at.plusSeconds(120));}
	public static PouchObservation unknown(){return new PouchObservation(false,Collections.emptyMap(),Instant.now());}
	public static PouchObservation unknown(String problem){return new PouchObservation(false,Collections.emptyMap(),Instant.now(),-1,problem);}
}
