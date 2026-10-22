package com.fromscratch.pouch;

import java.time.Instant;
import java.util.*;
import net.runelite.api.*;
import net.runelite.api.gameval.VarbitID;

/** On-request account varbits, as used by core 1.13.3 Rune Pouch; no inventory/UI-open prerequisite. */
public final class RuneLitePouchReader
{
	public static final int[] TYPES={VarbitID.RUNE_POUCH_TYPE_1,VarbitID.RUNE_POUCH_TYPE_2,VarbitID.RUNE_POUCH_TYPE_3,VarbitID.RUNE_POUCH_TYPE_4,VarbitID.RUNE_POUCH_TYPE_5,VarbitID.RUNE_POUCH_TYPE_6};
	public static final int[] QUANTITIES={VarbitID.RUNE_POUCH_QUANTITY_1,VarbitID.RUNE_POUCH_QUANTITY_2,VarbitID.RUNE_POUCH_QUANTITY_3,VarbitID.RUNE_POUCH_QUANTITY_4,VarbitID.RUNE_POUCH_QUANTITY_5,VarbitID.RUNE_POUCH_QUANTITY_6};
	public interface Access { boolean live(); int varbit(int id); int rune(int type); default int serverVarbit(int id){return varbit(id);} }
	private final Access access;
	public RuneLitePouchReader(Client client){this(new Access(){public boolean live(){return client.isClientThread()&&client.getGameState()==GameState.LOGGED_IN&&client.getLocalPlayer()!=null&&client.getEnum(EnumID.RUNEPOUCH_RUNE)!=null;}public int varbit(int id){return client.getVarbitValue(id);}public int serverVarbit(int id){return client.getServerVarbitValue(id);}public int rune(int type){return client.getEnum(EnumID.RUNEPOUCH_RUNE).getIntValue(type);}});}
	public RuneLitePouchReader(Access access){this.access=access;}
	public PouchObservation read()
	{
		try
		{
			if(!access.live()){return PouchObservation.unknown();}Map<Integer,Long> runes=new TreeMap<>();
			for(int slot=0;slot<TYPES.length;slot++)
			{
				int quantity=access.varbit(QUANTITIES[slot]),type=access.varbit(TYPES[slot]);
				// Server values are account state, independent of pouch location. Reject a
				// client-side prediction still awaiting its server update, not an old UI timestamp.
				if(quantity!=access.serverVarbit(QUANTITIES[slot])||type!=access.serverVarbit(TYPES[slot])){return PouchObservation.unknown("Rune Pouch contents are updating. Wait a game tick with the bank open and retry.");}
				if(quantity<0||type<0){return PouchObservation.unknown("Rune Pouch data is incomplete. Wait a game tick and retry.");}if(quantity==0){continue;}
				int rune=type==0?-1:access.rune(type);if(!PouchRunes.ids().containsKey(rune)){return PouchObservation.unknown();}runes.merge(rune,(long)quantity,Math::addExact);
			}
			return new PouchObservation(true,runes,Instant.now());
		}
		catch(RuntimeException ex){return PouchObservation.unknown();}
	}
	/** BankObserver supplies a complete current bank; read current account values afresh on every request. */
	public PouchObservation banked(com.fromscratch.bank.BankObservation bank)
	{
		// Do not age-gate the cached bank observation: a bank already open at plugin
		// startup may legitimately have tick 0. BankObserver handles open/change settling.
		if(bank==null||!bank.isReady()){return PouchObservation.unknown("Bank data is still loading. Keep the bank open, wait a game tick and retry.");}
		long count=0;int pouch=-1;
		for(int id:com.fromscratch.buyback.BuybackCatalog.variants("rune_pouch_family").keySet()){long n=bank.getBank().quantity(id);count=Math.addExact(count,n);if(n>0){pouch=id;}}
		if(count!=1){return PouchObservation.unknown(count==0?"No eligible Rune Pouch is in your bank.":"Multiple pouches are banked; their contents cannot be attributed safely.");}
		PouchObservation current=read();return current.isAvailable()?new PouchObservation(true,current.getQuantities(),current.getAt(),pouch):current;
	}
}
