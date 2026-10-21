package com.fromscratch.ge;

import java.util.*;
import lombok.Value;

/** Constant-bounded current GE state; complete immutable history is paged from the existing journal. */
@Value
public class GeState
{
	Map<Integer,GeOffer> offers;
	List<GeCredit> credits;
	List<GeMovement> movements;
	List<GeTrade> recent;
	long purchases;
	long sales;
	long estimatedTaxGp;
	long taxRecords;
	String archive;
	public GeState(Map<Integer,GeOffer> offers,List<GeCredit> credits,List<GeMovement> movements,List<GeTrade> recent,long purchases,long sales,long taxGp,long taxRecords,String archive)
	{
		if(offers.size()>8||credits.size()>256||movements.size()>128||recent.size()>30||purchases<0||sales<0||taxGp<0||taxRecords<0||taxRecords>sales||(taxRecords==0)!=(taxGp==0)
			||archive!=null&&!archive.matches("bank-migration-[a-f0-9-]{36}")){throw new IllegalArgumentException("Invalid GE checkpoint");}
		Map<Integer,GeOffer> copy=new TreeMap<>();offers.forEach((slot,offer)->{if(slot==null||offer==null||slot!=offer.getLast().getSlot()){throw new IllegalArgumentException("GE slot mismatch");}copy.put(slot,offer);});
		Set<UUID> ids=new HashSet<>();for(GeOffer offer:offers.values()){if(!ids.add(offer.getGeneration())){throw new IllegalArgumentException("Duplicate offer generation across slots");}}
		ids.clear();for(GeCredit c:credits){if(!ids.add(c.getId())){throw new IllegalArgumentException("Duplicate GE credit");}}
		ids.clear();for(GeTrade t:recent){if(!ids.add(t.getId())){throw new IllegalArgumentException("Duplicate GE trade");}}
		this.offers=Collections.unmodifiableMap(copy);this.credits=Collections.unmodifiableList(new ArrayList<>(credits));this.movements=Collections.unmodifiableList(new ArrayList<>(movements));this.recent=Collections.unmodifiableList(new ArrayList<>(recent));
		this.purchases=purchases;this.sales=sales;estimatedTaxGp=taxGp;this.taxRecords=taxRecords;this.archive=archive;
	}
	public static GeState empty(){return empty(null);}
	public static GeState empty(String archive){return new GeState(Collections.emptyMap(),Collections.emptyList(),Collections.emptyList(),Collections.emptyList(),0,0,0,0,archive);}
}
