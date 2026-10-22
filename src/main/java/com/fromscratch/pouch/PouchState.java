package com.fromscratch.pouch;

import java.util.*;
import lombok.Value;

/** One run-scoped preparation. Unarrived runes are a pending plan, never an item debt. */
@Value
public class PouchState
{
	public enum Phase { UNREVIEWED, PREPARED, READY }
	Phase phase;
	UUID preparation;
	Map<Integer,Long> recorded,remaining,held,retired;
	String archive;
	public PouchState(Phase phase,UUID preparation,Map<Integer,Long> recorded,Map<Integer,Long> remaining,Map<Integer,Long> held,Map<Integer,Long> retired,String archive)
	{
		this.phase=Objects.requireNonNull(phase);this.preparation=preparation;
		this.recorded=quantities(recorded);this.remaining=quantities(remaining);this.held=quantities(held);this.retired=quantities(retired);this.archive=archive;
		if(archive!=null&&!archive.matches("bank-migration-[a-f0-9-]{36}")){throw new IllegalArgumentException("Invalid pouch archive");}
		Set<Integer> ids=new HashSet<>(this.recorded.keySet());ids.addAll(this.remaining.keySet());ids.addAll(this.held.keySet());ids.addAll(this.retired.keySet());
		for(int id:ids){if(this.recorded.getOrDefault(id,0L)!=Math.addExact(Math.addExact(this.remaining.getOrDefault(id,0L),this.held.getOrDefault(id,0L)),this.retired.getOrDefault(id,0L))){throw new IllegalArgumentException("Pouch preparation quantities do not balance");}}
		if(phase==Phase.UNREVIEWED&&(!ids.isEmpty()||preparation!=null)||phase==Phase.PREPARED&&preparation==null||!ids.isEmpty()&&preparation==null||phase==Phase.READY&&!this.remaining.isEmpty()){throw new IllegalArgumentException("Incomplete pouch preparation");}
	}
	public static PouchState unreviewed(){return unreviewed(null);}
	public static PouchState unreviewed(String archive){return new PouchState(Phase.UNREVIEWED,null,Collections.emptyMap(),Collections.emptyMap(),Collections.emptyMap(),Collections.emptyMap(),archive);}
	public static Map<Integer,Long> quantities(Map<Integer,Long> source)
	{
		Map<Integer,Long> out=new TreeMap<>();source.forEach((id,n)->{if(!PouchRunes.ids().containsKey(id)||n==null||n<0){throw new IllegalArgumentException("Invalid pouch rune quantity");}if(n>0){out.put(id,n);}});return Collections.unmodifiableMap(out);
	}
}
