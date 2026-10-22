package com.fromscratch.buyback;

import com.fromscratch.bank.BankRun;
import java.util.*;
import net.runelite.api.gameval.ItemID;

/** Pure eligibility and physical-copy allocation. Never changes P/B or invents out-of-bank holdings. */
public final class BuybackEngine
{
	private BuybackEngine(){}
	public static long available(BankRun run){return Math.subtractExact(run.balance(ItemID.COINS),run.getBuybacks().getCommittedGp());}
	public static String status(BankRun run,BuybackEntry entry)
	{
		if(run==null||!run.isActive()||!run.isAnchored()){return "Start a run first";}
		Map<String,Integer> owned=run.getBuybacks().getEntitlements();
		if(entry.getRoles().entrySet().stream().allMatch(e->owned.getOrDefault(e.getKey(),0)>=e.getValue())){return "Already unlocked";}
		for(Map.Entry<String,Integer> role:entry.getRoles().entrySet())
		{
			long physical=0,raw=0,qualifying=0;
			for(Map.Entry<Integer,Integer> variant:BuybackCatalog.variants(role.getKey()).entrySet())
			{
				int id=variant.getKey();physical=Math.addExact(physical,run.physical(id));raw=Math.addExact(raw,run.balance(id));
				if(variant.getValue()==role.getValue()){qualifying=Math.addExact(qualifying,run.physical(id));}
			}
			// The difference is still locked legacy stock. A naturally positive copy cannot qualify alone.
			if(qualifying==0){return entry.getRoles().size()>1?"Missing set pieces":"Not in bank";}
			if(physical<=Math.max(0,raw)){return entry.getRoles().size()>1?"Missing set pieces":"Already unlocked";}
			if(owned.containsKey(role.getKey())&&raw<run.getBuybacks().getBankFloors().get(role.getKey())){return "Return unlocked copy";}
		}
		return available(run)<BuybackCatalog.cost(entry,owned)?"Not enough GP":"Available";
	}
	public static BuybackState purchase(BankRun run,BuybackPurchase purchase)
	{
		BuybackEntry entry=BuybackCatalog.entry(purchase.getCatalogId());String status=status(run,entry);
		if(entry.getId().equals("rune_pouch_family")&&purchase.getPouchItemId()<0){throw new IllegalArgumentException("Rune Pouch data is not available yet. Keep the bank open, wait a game tick and retry.");}
		if(!"Available".equals(status)){throw new IllegalArgumentException(status+". Reopen Legacy Buybacks after banking the required items/GP.");}
		if(purchase.getGp()!=BuybackCatalog.cost(entry,run.getBuybacks().getEntitlements())){throw new IllegalArgumentException("Buyback price changed. Review the purchase again.");}
		if(!purchase.getBankFloors().equals(purchaseFloors(run,entry))){throw new IllegalArgumentException("Paid-copy bank observation changed");}
		return run.getBuybacks().add(purchase);
	}
	/** A first payment anchors its one copy at the current bank, even after earlier legacy withdrawals. */
	public static Map<String,Long> purchaseFloors(BankRun run,BuybackEntry entry)
	{
		Map<String,Long> floors=new TreeMap<>();for(String role:entry.getRoles().keySet()){floors.put(role,run.getBuybacks().getBankFloors().getOrDefault(role,Math.min(0,raw(run.getBalances(),role))));}return floors;
	}
	private static long raw(Map<Integer,Long> balances,String role){long n=0;for(int id:BuybackCatalog.variants(role).keySet()){n=Math.addExact(n,balances.getOrDefault(id,0L));}return n;}
	/** Return movements raise a nonpositive anchor, capped at zero. They never add paid copies. */
	public static BuybackState progress(BuybackState state,Map<Integer,Long> balances)
	{
		if(state.getEntitlements().isEmpty()){return state;}
		Map<String,Long> floors=new TreeMap<>(state.getBankFloors());floors.replaceAll((role,floor)->Math.max(floor,Math.min(0,raw(balances,role))));
		return floors.equals(state.getBankFloors())?state:new BuybackState(state.getCatalogVersion(),state.getEntitlements(),floors,state.getCommittedGp(),state.getPurchases(),state.getArchive(),state.getPouch());
	}
	/**
	 * Raw bank progress keeps its original equation. One paid role adds a single offset across
	 * its genuine variants. The paid bank floor excludes withdrawals preceding the purchase;
	 * later reductions consume virtual units first. Returns raise that floor, capped at zero,
	 * without multiplying the allowance. A missing physical form grants no copy.
	 * Higher Void tiers require their upgrade. No inventory or transformation event is inferred.
	 */
	public static Map<Integer,Long> balances(BankRun run)
	{
		if(run.getBuybacks().getEntitlements().isEmpty()){return run.getBalances();}
		Map<Integer,Long> result=new TreeMap<>(run.getBalances());
		for(Map.Entry<String,Integer> entitlement:run.getBuybacks().getEntitlements().entrySet())
		{
			Map<Integer,Integer> variants=BuybackCatalog.variants(entitlement.getKey());long raw=0;
			for(int id:variants.keySet()){raw=Math.addExact(raw,run.balance(id));result.remove(id);}
			long floor=run.getBuybacks().getBankFloors().get(entitlement.getKey());
			long allowance=raw>=floor?BuybackState.COPIES_PER_ROLE:Math.addExact(Math.subtractExact(raw,floor),BuybackState.COPIES_PER_ROLE);
			if(allowance<=0){if(allowance!=0){int id=variants.keySet().stream().filter(i->run.physical(i)>0).findFirst().orElseGet(()->variants.keySet().stream().filter(i->run.balance(i)<0).findFirst().orElse(variants.keySet().iterator().next()));result.put(id,allowance);}continue;}
			long natural=Math.max(0,raw),paid=allowance;
			List<Integer> ids=new ArrayList<>(variants.keySet());ids.sort(Comparator.<Integer>comparingLong(run::balance).reversed().thenComparingInt(i->i));
			for(int id:ids){long n=Math.min(run.physical(id),natural);if(n>0){result.put(id,n);natural-=n;}}
			ids.sort(Comparator.<Integer>comparingInt(variants::get).reversed().thenComparingInt(i->i));
			for(int id:ids){if(variants.get(id)>entitlement.getValue()){continue;}long n=Math.min(Math.subtractExact(run.physical(id),result.getOrDefault(id,0L)),paid);if(n>0){result.put(id,Math.addExact(result.getOrDefault(id,0L),n));paid-=n;}}
		}
		// The coin counter shows unspent challenge GP; the saved raw coin cursor is untouched.
		long coins=available(run);if(coins==0){result.remove(ItemID.COINS);}else{result.put(ItemID.COINS,coins);}
		return Collections.unmodifiableMap(result);
	}
	/** Preserve the bank-positive metric and count each durable paid slot once, even while withdrawn. */
	public static long unlocked(BankRun run)
	{
		Set<Integer> covered=new HashSet<>();for(String role:run.getBuybacks().getEntitlements().keySet()){covered.addAll(BuybackCatalog.variants(role).keySet());}
		return Math.addExact(run.getBuybacks().getEntitlements().size(),balances(run).entrySet().stream().filter(e->e.getValue()>0&&!covered.contains(e.getKey())).count());
	}
}
