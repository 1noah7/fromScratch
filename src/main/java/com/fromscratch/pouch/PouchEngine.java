package com.fromscratch.pouch;

import java.util.*;

/** Source-limited reservation; never subtracts pre-existing bank ownership or changes physical P. */
public final class PouchEngine
{
	private PouchEngine(){}
	/** Convert only overlapping historical reservations to receipt debits; other known old arrivals stay protected. */
	public static PouchState purchased(PouchState old,Map<Integer,Long> contents)
	{
		Map<Integer,Long> retired=new TreeMap<>(old.getRetired()),remaining=new TreeMap<>(old.getRemaining());
		contents.forEach((id,n)->{long converted=Math.min(n,remaining.getOrDefault(id,0L));if(converted>0){remaining.put(id,remaining.get(id)-converted);retired.merge(id,converted,Math::addExact);}});remaining.values().removeIf(n->n==0);
		return new PouchState(remaining.isEmpty()?PouchState.Phase.READY:PouchState.Phase.PREPARED,old.getPreparation(),old.getRecorded(),remaining,old.getHeld(),retired,old.getArchive());
	}
	public static Map<Integer,Long> transfers(PouchState state,Map<Integer,Long> delta)
	{
		// The player verifies the source on finishing. Reserve only *new* bank increases,
		// up to the recorded budget; never debit a pre-existing earned stack.
		Map<Integer,Long> held=new TreeMap<>();if(state.getPhase()!=PouchState.Phase.PREPARED){return held;}
		state.getRemaining().forEach((id,n)->{long q=Math.min(n,Math.max(0,delta.getOrDefault(id,0L)));if(q>0){held.put(id,q);}});return held;
	}
	public static PouchState bank(PouchState old,Map<Integer,Long> delta)
	{
		Map<Integer,Long> match=transfers(old,delta);if(match.isEmpty()){return old;}
		Map<Integer,Long> pending=new TreeMap<>(old.getRemaining()),held=new TreeMap<>(old.getHeld());match.forEach((id,n)->{pending.put(id,pending.get(id)-n);held.put(id,Math.addExact(held.getOrDefault(id,0L),n));});
		return new PouchState(old.getPhase(),old.getPreparation(),old.getRecorded(),pending,held,old.getRetired(),old.getArchive());
	}
	public static PouchState apply(PouchState old,PouchAction action,UUID id)
	{
		if(old.getPhase()==PouchState.Phase.READY){throw new IllegalArgumentException("Pouch preparation is already complete.");}
		if(action.getKind()==PouchAction.Kind.REGISTER)
		{
			if(old.getPhase()!=PouchState.Phase.UNREVIEWED){throw new IllegalArgumentException("A pouch preparation is already recorded. Review it before continuing.");}
			return new PouchState(PouchState.Phase.PREPARED,id,action.getQuantities(),action.getQuantities(),Collections.emptyMap(),Collections.emptyMap(),old.getArchive());
		}
		Map<Integer,Long> pending=new TreeMap<>(old.getRemaining()),held=new TreeMap<>(old.getHeld()),retired=new TreeMap<>(old.getRetired());
		if(action.getKind()==PouchAction.Kind.RESTORE_EARNED)
		{
			// Releasing unrelated earnings replenishes the finite plan: the actual old
			// pouch contents still need to arrive or be explicitly retired.
			action.getQuantities().forEach((r,n)->{if(n>held.getOrDefault(r,0L)){throw new IllegalArgumentException("Only held pouch deposits can be restored.");}held.put(r,held.get(r)-n);pending.put(r,Math.addExact(pending.getOrDefault(r,0L),n));});
			return new PouchState(old.getPhase(),old.getPreparation(),old.getRecorded(),pending,held,retired,old.getArchive());
		}
		if(!pending.isEmpty()&&!action.isRetireMissing()){throw new IllegalArgumentException("Some recorded runes have not reached the bank. Bank them, or confirm they never arrived.");}
		pending.forEach((r,n)->retired.put(r,Math.addExact(retired.getOrDefault(r,0L),n)));
		return new PouchState(PouchState.Phase.READY,old.getPreparation(),old.getRecorded(),Collections.emptyMap(),held,retired,old.getArchive());
	}
}
