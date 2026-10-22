package com.fromscratch.buyback;

import com.fromscratch.bank.*;
import java.time.Instant;
import java.util.*;
import net.runelite.api.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;

/** Fail closed unless a current-session server container agrees with the visible native bank-side view. */
public final class LootingBagReader
{
	public interface Access { boolean live(); boolean visible(); Item[] contents(); Item[] displayed(); String name(int id); default boolean emptyMessage(){return false;} }
	private final Access access;
	public LootingBagReader(Client client)
	{
		this(new Access(){public boolean live(){return client.isClientThread()&&client.getGameState()==GameState.LOGGED_IN&&client.getLocalPlayer()!=null;}
			public boolean visible(){Widget w=client.getWidget(InterfaceID.Bankside.LOOTINGBAG_CONTAINER);return w!=null&&!w.isHidden();}
			public Item[] contents(){ItemContainer c=client.getItemContainer(InventoryID.LOOTING_BAG);return c==null?null:c.getItems();}
			public Item[] displayed(){Widget w=client.getWidget(InterfaceID.Bankside.LOOTINGBAG_ITEMS);if(w==null||w.isHidden()){return null;}Widget[] children=w.getDynamicChildren();if(children==null){children=w.getStaticChildren();}if(children==null){return null;}Item[] items=new Item[children.length];for(int i=0;i<items.length;i++){items[i]=children[i]==null?new Item(-1,0):new Item(children[i].getItemId(),children[i].getItemQuantity());}return items;}
			public boolean emptyMessage(){Widget w=client.getWidget(InterfaceID.Bankside.LOOTINGBAG_ITEMS);if(w==null||w.isHidden()){return false;}Widget[] children=w.getDynamicChildren();if(children==null){children=w.getStaticChildren();}if(children==null){return false;}for(Widget child:children){if(child!=null&&child.getText()!=null&&"The bag is empty.".equals(net.runelite.client.util.Text.removeTags(child.getText()).trim())){return true;}}return false;}
			public String name(int id){return client.getItemDefinition(id).getName();}});
	}
	public LootingBagReader(Access access){this.access=access;}
	public BagObservation read(BankObservation bank,UUID serverSession)
	{
		try
		{
			if(bank==null||!bank.isReady()||!bank.getSession().equals(serverSession)||!access.live()){return BagObservation.unknown();}
			long count=0;int bag=-1;for(int id:BuybackCatalog.variants("looting_bag_family").keySet()){long n=bank.getBank().quantity(id);count=Math.addExact(count,n);if(n>0){bag=id;}}
			if(count!=1){return BagObservation.unknown();}
			// A packet received for a banked bag is fresh without an inventory-side panel.
			Item[] contents=access.contents();if(contents==null||contents.length>28){return BagObservation.unknown();}
			return observation(bag,aggregate(contents));
		}
		catch(RuntimeException ex){return BagObservation.unknown();}
	}
	/** A native View request identifies the bag. Its inspection may precede returning the bag to the bank. */
	public BagObservation inspect(BankObservation bank,int requestedId,boolean currentPacket)
	{
		try
		{
			if(bank==null||!access.live()||!access.visible()||!BuybackCatalog.variants("looting_bag_family").containsKey(requestedId)){return BagObservation.unknown();}
			Item[] contents=access.contents(),display=access.displayed();if(display==null){return BagObservation.unknown();}
			Map<Integer,Long> shown=aggregate(display);
			// Native empty text is explicit evidence, never an inference from a missing container.
			if(contents==null||!currentPacket){return shown.isEmpty()&&access.emptyMessage()?observation(requestedId,shown):BagObservation.unknown();}
			if(contents.length>28||!shown.equals(aggregate(contents))){return BagObservation.unknown();}
			return observation(requestedId,shown);
		}
		catch(RuntimeException ex){return BagObservation.unknown();}
	}
	private BagObservation observation(int id,Map<Integer,Long> quantities)
	{Map<Integer,String> names=new TreeMap<>();quantities.keySet().forEach(item->names.put(item,access.name(item)));return new BagObservation(id,quantities,names,Instant.now());}
	private static Map<Integer,Long> aggregate(Item[] items)
	{
		Map<Integer,Long> q=new TreeMap<>();for(Item item:items){if(item==null){throw new IllegalArgumentException("Incomplete contents");}if(item.getId()<0){continue;}if(item.getQuantity()<=0){throw new IllegalArgumentException("Incomplete quantity");}q.merge(item.getId(),(long)item.getQuantity(),Math::addExact);}return BagObservation.quantities(q);
	}
}
