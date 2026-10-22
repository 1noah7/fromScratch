package com.fromscratch.bank;

import com.fromscratch.model.ContainerSnapshot;
import java.time.Instant;
import java.util.*;
import com.fromscratch.wealth.*;

/** Pure checked bank arithmetic and quoted high-water valuation; no acquisition provenance or personal-container inference. */
public final class BankEngine
{
	private BankEngine() { }
	public static BankRun start(String character, UUID previous, ContainerSnapshot bank, boolean attested, UUID session, Instant at)
	{
		if (!attested) { throw new IllegalArgumentException("Confirm that inventory and equipment have been banked first."); }
		BankAudit audit = audit(session, at, BankAudit.Kind.START, Collections.emptyMap(), Collections.emptyMap(), "Bank-only run started; initial progress is zero.");
		return new BankRun(character, UUID.randomUUID(), previous, at, audit.getId(), 1, bank, bank.getQuantities(), Collections.emptyMap(), true, null, null, Collections.singletonList(audit), null);
	}
	public static BankRun observe(BankRun old, ContainerSnapshot bank, UUID session, Instant at)
	{ return observe(old, bank, session, at, null); }
	public static BankRun observe(BankRun old, ContainerSnapshot bank, UUID session, Instant at, BankTimings.Trace trace)
	{ return observe(old,bank,session,at,trace,Collections.emptyMap()); }
	public static BankRun observe(BankRun old, ContainerSnapshot bank, UUID session, Instant at, BankTimings.Trace trace, Map<Integer,PriceQuote> prices)
	{
		requireAnchored(old);
		Map<Integer, Long> delta = differences(old.getPhysical(), bank.getQuantities());
		BankTimings.mark(trace, BankTimings.Phase.DIFFERENCE);
		boolean first = old.getHostSummary() == null || !old.getHostSummary().getSession().equals(session);
		if (delta.isEmpty() && !first) { return old; }
		Map<Integer, Long> balances = new TreeMap<>(old.getBalances());
		Map<Integer,Long> held=com.fromscratch.pouch.PouchEngine.transfers(old.getBuybacks().getPouch(),delta);
		// P always advances by the full observed delta. Only B/financial attribution
		// exclude the explicit one-time legacy-source reservation, in this same commit.
		Map<Integer,Long> progress=new TreeMap<>(delta);held.forEach((id,n)->progress.put(id,Math.subtractExact(progress.get(id),n)));progress.values().removeIf(n->n==0);
		progress.forEach((id, n) -> balances.put(id, Math.addExact(old.balance(id), n)));
		BankAudit change = audit(session, at, delta.isEmpty() ? BankAudit.Kind.SESSION_CHECK : BankAudit.Kind.AUTOMATIC, delta, progress,
			first ? "Bank changes detected on return to this PC." : "Bank quantities updated.");
		if(!delta.isEmpty())
		{
			com.fromscratch.ge.GeUpdate attribution=com.fromscratch.ge.GeEngine.bank(old.getExchange(),change.getId(),progress,at).getEvidence();
			change=new BankAudit(change.getId(),session,at,change.getKind(),delta,progress,change.getReason(),FinancialEngine.recognize(old.getFinancial(),balances,change.getId(),prices,at,attribution.getGainExclusions()),attribution,null,null,held);
		}
		BankRun next = next(old, bank.getQuantities(), balances, true, change, first ? change : old.getHostSummary());
		BankTimings.mark(trace, BankTimings.Phase.PREPARED); return next;
	}
	public static BankRun correct(BankRun old, int id, long desired, UUID session, Instant at)
	{
		requireAnchored(old);
		if (id < 0 || desired == Long.MIN_VALUE || desired > old.physical(id)) { throw new IllegalArgumentException("A positive balance cannot exceed the physical bank quantity."); }
		long difference = Math.subtractExact(desired, old.balance(id));
		if (difference == 0) { return old; }
		Map<Integer, Long> balances = new TreeMap<>(old.getBalances()); balances.put(id, desired);
		BankAudit change = audit(session, at, BankAudit.Kind.CORRECTION, Collections.emptyMap(), Collections.singletonMap(id, difference), "Player confirmed bank-count correction.");
		return next(old, old.getPhysical(), balances, true, change, old.getHostSummary());
	}
	/** Existing balances are kept; excess positive balances require explicit approval, never an implicit clamp. */
	public static BankRun anchor(BankRun old, ContainerSnapshot bank, boolean approveExcessReduction, UUID session, Instant at)
	{
		if (!old.isActive() || old.isAnchored()) { throw new IllegalArgumentException("No migration anchor is pending."); }
		Map<Integer, Long> balances = new TreeMap<>(old.getBalances());
		balances.replaceAll((id, n) ->
		{
			long physical = bank.getQuantities().getOrDefault(id, 0L);
			if (n > physical && !approveExcessReduction) { throw new IllegalArgumentException("Some saved positive balances exceed this bank. Bank those items first, or explicitly approve the listed reductions."); }
			return Math.min(n, physical);
		});
		BankAudit change = audit(session, at, BankAudit.Kind.ANCHOR, Collections.emptyMap(), differences(old.getBalances(), balances), "Player confirmed one-time bank-only migration anchor; earlier changes were not replayed.");
		return next(old, bank.getQuantities(), balances, true, change, change);
	}
	public static BankRun reset(BankRun old, UUID session, Instant at)
	{
		BankAudit change = audit(session, at, BankAudit.Kind.RESET, Collections.emptyMap(), Collections.emptyMap(), "Player confirmed reset after verified archive.");
		return new BankRun(old.getCharacter(), null, old.getRunId(), null, change.getId(), Math.incrementExact(old.getSequence()), null,
			Collections.emptyMap(), Collections.emptyMap(), false, old.getLegacyDocument(), old.getMigrationArchive(), Collections.singletonList(change), null);
	}
	static BankRun next(BankRun old, Map<Integer, Long> physical, Map<Integer, Long> balances, boolean anchored, BankAudit change, BankAudit summary)
	{
		com.fromscratch.ge.GeState exchange=old.getExchange();
		if(change.getKind()==BankAudit.Kind.EXCHANGE){exchange=com.fromscratch.ge.GeEngine.observe(exchange,change.getExchange().getObservation(),old.getRunId()).getState();}
		else if(change.getKind()==BankAudit.Kind.AUTOMATIC){exchange=com.fromscratch.ge.GeEngine.bank(exchange,change.getId(),change.getBalanceChanges(),change.getAt()).getState();}
		com.fromscratch.pouch.PouchState pouch=old.getBuybacks().getPouch();
		if(change.getKind()==BankAudit.Kind.AUTOMATIC){pouch=com.fromscratch.pouch.PouchEngine.bank(pouch,change.getPhysicalChanges());}
		else if(change.getKind()==BankAudit.Kind.POUCH){pouch=com.fromscratch.pouch.PouchEngine.apply(pouch,change.getPouch(),change.getId());}
		else if(change.getBuyback()!=null&&change.getBuyback().getPouchItemId()>0){pouch=com.fromscratch.pouch.PouchEngine.purchased(pouch,change.getBuyback().getLegacyRunes());}
		List<BankAudit> recent = new ArrayList<>();
		for(BankAudit previous:old.getRecent())
		{
			// Full quotes stay in that operation's immutable journal/checkpoint. The rolling current view
			// needs only older movement summaries; avoid rewriting a 1,200-item quote batch 30 times.
			recent.add(summary(previous));
		}
		recent.add(0, change); if (recent.size() > 30) { recent.remove(30); }
		return new BankRun(old.getCharacter(), old.getRunId(), old.getPreviousRunId(), old.getStartedAt(), change.getId(), Math.incrementExact(old.getSequence()), old.getInitialBank(), physical, balances, anchored,
			old.getLegacyDocument(), old.getMigrationArchive(), recent, summary(summary),FinancialEngine.advance(old.getFinancial(),balances,change),exchange,
			(change.getBuyback()==null?com.fromscratch.buyback.BuybackEngine.progress(old.getBuybacks(),balances):com.fromscratch.buyback.BuybackEngine.purchase(old,change.getBuyback())).withPouch(pouch));
	}
	private static BankAudit summary(BankAudit audit)
	{
		return audit!=null&&audit.getKind()==BankAudit.Kind.AUTOMATIC&&!audit.getValuations().isEmpty()
			?new BankAudit(audit.getId(),audit.getSession(),audit.getAt(),audit.getKind(),audit.getPhysicalChanges(),audit.getBalanceChanges(),audit.getReason(),Collections.emptyList(),audit.getExchange(),null,null,audit.getPouchTransfers()):audit;
	}
	/** GE changes are observational audit operations; P/B and the immutable initial bank stay unchanged. */
	public static BankRun exchange(BankRun old,com.fromscratch.ge.GeObservation observation)
	{
		if(!old.isActive()){return old;}
		com.fromscratch.ge.GeEngine.Result result=com.fromscratch.ge.GeEngine.observe(old.getExchange(),observation,old.getRunId());
		if(result.getEvidence().emptyEvidence()){return old;}
		BankAudit audit=new BankAudit(com.fromscratch.ge.GeEngine.id(old.getRunId()+":"+observation.getEventId()),observation.getSession(),observation.getAt(),BankAudit.Kind.EXCHANGE,Collections.emptyMap(),Collections.emptyMap(),"Grand Exchange observation recorded.",Collections.emptyList(),result.getEvidence());
		return next(old,old.getPhysical(),old.getBalances(),old.isAnchored(),audit,old.getHostSummary());
	}
	/** One paid entitlement operation; raw bank observations and historical gain quotes are unchanged. */
	public static BankRun buyback(BankRun old,String catalogId,UUID purchaseId,UUID session,Instant at)
	{return buyback(old,catalogId,purchaseId,session,at,com.fromscratch.pouch.PouchObservation.unknown());}
	public static BankRun buyback(BankRun old,String catalogId,UUID purchaseId,UUID session,Instant at,com.fromscratch.pouch.PouchObservation contents)
	{
		requireAnchored(old);
		com.fromscratch.buyback.BuybackEntry entry=com.fromscratch.buyback.BuybackCatalog.entry(catalogId);
		boolean pouch=catalogId.equals("rune_pouch_family");if(pouch&&(!contents.fresh(at)||old.physical(contents.getPouchId())!=1||com.fromscratch.buyback.BuybackCatalog.variants(catalogId).keySet().stream().mapToLong(old::physical).reduce(0L,Math::addExact)!=1)){throw new IllegalArgumentException("Rune Pouch data is not available yet. Keep the bank open, wait a game tick and retry.");}
		Map<Integer,Long> adjustments=new TreeMap<>(),balances=new TreeMap<>(old.getBalances());if(pouch){contents.getQuantities().forEach((id,n)->{adjustments.put(id,Math.negateExact(n));balances.put(id,Math.subtractExact(old.balance(id),n));});}
		com.fromscratch.buyback.BuybackPurchase purchase=new com.fromscratch.buyback.BuybackPurchase(purchaseId,catalogId,com.fromscratch.buyback.BuybackCatalog.cost(entry,old.getBuybacks().getEntitlements()),at,com.fromscratch.buyback.BuybackEngine.purchaseFloors(old,entry),pouch?contents.getPouchId():-1,pouch?contents.getQuantities():Collections.emptyMap());
		BankAudit change=new BankAudit(purchaseId,session,at,BankAudit.Kind.BUYBACK,Collections.emptyMap(),adjustments,"Player confirmed legacy buyback: "+entry.getName(),Collections.emptyList(),com.fromscratch.ge.GeUpdate.empty(),purchase);
		return next(old,old.getPhysical(),balances,true,change,old.getHostSummary());
	}
	/** Scoped explicit setup/review; restoring a misidentified deposit recognizes only current high-water progress. */
	public static BankRun pouch(BankRun old,com.fromscratch.pouch.PouchAction action,UUID session,Instant at,Map<Integer,PriceQuote> prices)
	{
		requireAnchored(old);boolean present=com.fromscratch.buyback.BuybackCatalog.variants("rune_pouch_family").keySet().stream().anyMatch(id->old.physical(id)>0);
		if(!present){throw new IllegalArgumentException("Bank your Rune Pouch or Divine Rune Pouch before reviewing its contents.");}
		Map<Integer,Long> balances=new TreeMap<>(old.getBalances());UUID id=UUID.randomUUID();List<ValuationEvent> values=Collections.emptyList();
		if(action.getKind()==com.fromscratch.pouch.PouchAction.Kind.RESTORE_EARNED){action.getQuantities().forEach((r,n)->balances.put(r,Math.addExact(old.balance(r),n)));values=FinancialEngine.recognize(old.getFinancial(),balances,id,prices,at);}
		BankAudit change=new BankAudit(id,session,at,BankAudit.Kind.POUCH,Collections.emptyMap(),differences(old.getBalances(),balances),"Player confirmed legacy pouch transfer review: "+action.getReason(),values,com.fromscratch.ge.GeUpdate.empty(),null,action,Collections.emptyMap());
		return next(old,old.getPhysical(),balances,true,change,old.getHostSummary());
	}
	/** A recorded statistical expense never changes bank quantities or the bank coin balance. */
	public static BankRun expense(BankRun old,long gp,String reason,UUID session,Instant at)
	{
		requireAnchored(old);if(gp<=0){throw new IllegalArgumentException("Enter a positive loss/expense amount.");}
		UUID id=UUID.randomUUID();ValuationEvent event=new ValuationEvent(ValuationEvent.identity(id,ValuationEvent.Kind.EXPENSE,net.runelite.api.gameval.ItemID.COINS),ValuationEvent.Kind.EXPENSE,net.runelite.api.gameval.ItemID.COINS,gp,PriceQuote.coins(at),reason);
		BankAudit change=new BankAudit(id,session,at,BankAudit.Kind.EXPENSE,Collections.emptyMap(),Collections.emptyMap(),"Player confirmed recorded loss/expense: "+reason,Collections.singletonList(event));
		return next(old,old.getPhysical(),old.getBalances(),true,change,old.getHostSummary());
	}
	public static Map<Integer, Long> differences(Map<Integer, Long> before, Map<Integer, Long> after)
	{
		Map<Integer, Long> delta = new TreeMap<>(); Set<Integer> ids = new TreeSet<>(before.keySet()); ids.addAll(after.keySet());
		for (int id : ids) { long n = Math.subtractExact(after.getOrDefault(id, 0L), before.getOrDefault(id, 0L)); if (n != 0) { delta.put(id, n); } }
		return delta;
	}
	private static BankAudit audit(UUID session, Instant at, BankAudit.Kind kind, Map<Integer, Long> physical, Map<Integer, Long> balance, String reason)
	{ return new BankAudit(UUID.randomUUID(), session, at, kind, physical, balance, reason); }
	private static void requireAnchored(BankRun run) { if (!run.isActive() || !run.isAnchored()) { throw new IllegalArgumentException("Confirm the one-time bank migration first."); } }
}
