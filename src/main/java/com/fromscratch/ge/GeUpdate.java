package com.fromscratch.ge;

import java.util.*;
import lombok.Value;

/** Immutable evidence embedded in the same forced bank journal as its resulting GE checkpoint. */
@Value
public class GeUpdate
{
	GeObservation observation;
	List<GeTrade> trades;
	List<GeMatch> matches;
	Map<Integer,Long> gainExclusions;
	public GeUpdate(GeObservation observation,List<GeTrade> trades,List<GeMatch> matches,Map<Integer,Long> exclusions)
	{
		if(trades.size()>2||matches.size()>256){throw new IllegalArgumentException("Oversized GE evidence");}
		this.observation=observation;this.trades=Collections.unmodifiableList(new ArrayList<>(trades));this.matches=Collections.unmodifiableList(new ArrayList<>(matches));
		Map<Integer,Long> copy=new TreeMap<>();exclusions.forEach((id,n)->{if(id==null||id<0||n==null||n<=0){throw new IllegalArgumentException("Invalid gain exclusion");}copy.put(id,n);});gainExclusions=Collections.unmodifiableMap(copy);
	}
	public static GeUpdate empty(){return new GeUpdate(null,Collections.emptyList(),Collections.emptyList(),Collections.emptyMap());}
	public boolean emptyEvidence(){return observation==null&&trades.isEmpty()&&matches.isEmpty()&&gainExclusions.isEmpty();}
}
