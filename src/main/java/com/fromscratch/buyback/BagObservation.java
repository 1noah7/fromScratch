package com.fromscratch.buyback;

import java.time.Instant;
import java.util.*;
import lombok.Value;

/** One on-request native bank-side contents observation, not ongoing personal ownership. */
@Value
public class BagObservation
{
	public static final String INSTRUCTION="No current Looting Bag contents were received. With your bank open, withdraw the bag and choose View, then return the bag and retry. Keep the bank open; do not empty its contents before buying back.";
	int itemId; Map<Integer,Long> quantities; Map<Integer,String> names; Instant at;
	public BagObservation(int itemId,Map<Integer,Long> quantities,Map<Integer,String> names,Instant at)
	{
		if(itemId!=-1&&!BuybackCatalog.variants("looting_bag_family").containsKey(itemId)){throw new IllegalArgumentException("Invalid bag form");}
		this.itemId=itemId;this.quantities=quantities(quantities);this.names=Collections.unmodifiableMap(new TreeMap<>(names));this.at=Objects.requireNonNull(at);
		if(itemId==-1&&!this.quantities.isEmpty()){throw new IllegalArgumentException("Unknown bag contents");}
	}
	public boolean fresh(Instant now){return itemId>0&&!at.isAfter(now)&&now.isBefore(at.plusSeconds(120));}
	public static BagObservation unknown(){return new BagObservation(-1,Collections.emptyMap(),Collections.emptyMap(),Instant.now());}
	public static Map<Integer,Long> quantities(Map<Integer,Long> values)
	{
		if(values.size()>28){throw new IllegalArgumentException("Bag capacity exceeded");}
		Map<Integer,Long> copy=new TreeMap<>();values.forEach((id,n)->{if(id<0||n==null||n<=0||n>Integer.MAX_VALUE){throw new IllegalArgumentException("Invalid bag quantity");}copy.put(id,n);});return Collections.unmodifiableMap(copy);
	}
}
