package com.fromscratch.bank;

import com.fromscratch.model.ContainerSnapshot;
import com.fromscratch.observation.CharacterIdentity;
import java.util.*;
import javax.inject.*;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.gameval.*;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.*;

/** BANK quantity authority with on-request banked pouch varbits; no inventory/equipment reads. */
@Singleton @Slf4j
public final class RuneLiteBankSource implements BankSource
{
	private final Client client;
	private final ConfigManager config;
	private final BankObserver observer;
	private final BankTimings timings;
	private String character;
	private long verifiedHash;
	private RuneScapeProfileType verifiedType;
	private String verifiedProfile;
	private volatile boolean diagnostics;
	private long lastDiagnostic;
	private UUID bagSession;
	private String bagCharacter,inspectionCharacter;
	private int requestedBag=-1,requestedBagTick,bagPacketTick;
	private UUID inspectionSession;
	private com.fromscratch.buyback.BagObservation inspectedBag;
	@Inject public RuneLiteBankSource(Client client, ConfigManager config, BankTimings timings)
	{
		this.client = client; this.config = config; this.timings = timings;
		observer = new BankObserver(new BankObserver.Access()
		{
			public boolean normalBankOpen() { return bankOpen(); }
			public ContainerSnapshot readBank() { return read(); }
			public BankTimings.Trace trace() { return timings.current(); }
		});
	}
	public void resetSession() { observer.reset(); character = null; bagUnavailable(); }
	/** A server contents packet must belong to this bank session, never a retained old container. */
	public void bagChanged(){bagPacketTick=client.getTickCount();inspectedBag=null;bagCharacter=bankOpen()?characterKey():null;bagSession=bagCharacter==null?null:observer.observe().getSession();}
	public void bagUnavailable(){bagSession=null;bagCharacter=null;inspectedBag=null;requestedBag=-1;inspectionCharacter=null;inspectionSession=null;}
	/** Explicit native View/Check only. No inventory container is read or tracked. */
	public void bagClicked(net.runelite.api.events.MenuOptionClicked event)
	{
		String option=event.getMenuOption();if((event.getMenuAction()!=MenuAction.CC_OP&&event.getMenuAction()!=MenuAction.CC_OP_LOW_PRIORITY)||!("View".equals(option)||"Check".equals(option))||!bankOpen()){return;}
		int id=event.getMenuEntry().getItemId();
		if(!com.fromscratch.buyback.BuybackCatalog.variants("looting_bag_family").containsKey(id))
		{Widget parent=client.getWidget(event.getParam1());Widget item=parent==null||event.getParam0()<0?null:parent.getChild(event.getParam0());if(item!=null){id=item.getItemId();}}
		if(com.fromscratch.buyback.BuybackCatalog.variants("looting_bag_family").containsKey(id))
		{requestedBag=id;requestedBagTick=client.getTickCount();inspectionCharacter=characterKey();inspectionSession=observer.observe().getSession();inspectedBag=null;}
		else{requestedBag=-1;}
	}
	/** Some empty checks return a native message instead of opening a contents panel. */
	public void bagMessage(net.runelite.api.events.ChatMessage event)
	{
		if(requestedBag<0||event.getType()!=ChatMessageType.GAMEMESSAGE||!bankOpen()||client.getTickCount()>requestedBagTick+3||!Objects.equals(inspectionCharacter,characterKey())){return;}
		String text=net.runelite.client.util.Text.removeTags(event.getMessage());
		if("The bag is empty.".equals(text)||"Your looting bag is empty.".equals(text))
		{inspectedBag=new com.fromscratch.buyback.BagObservation(requestedBag,Collections.emptyMap(),Collections.emptyMap(),java.time.Instant.now());requestedBag=-1;}
	}
	private void inspectBag()
	{
		if(requestedBag<0||client.getTickCount()<=requestedBagTick||!bankOpen()||!Objects.equals(inspectionCharacter,characterKey())){return;}
		com.fromscratch.buyback.BagObservation value=new com.fromscratch.buyback.LootingBagReader(client).inspect(observer.observe(),requestedBag,bagPacketTick>=requestedBagTick&&Objects.equals(bagCharacter,inspectionCharacter)&&Objects.equals(bagSession,inspectionSession));
		if(value.fresh(java.time.Instant.now())){inspectedBag=value;requestedBag=-1;}
		else if(client.getTickCount()>requestedBagTick+3){requestedBag=-1;}
	}
	public com.fromscratch.buyback.BagObservation bag()
	{
		BankObservation bank=observer.observe();String key=characterKey();com.fromscratch.buyback.LootingBagReader reader=new com.fromscratch.buyback.LootingBagReader(client);
		com.fromscratch.buyback.BagObservation packet=reader.read(bank,Objects.equals(key,bagCharacter)?bagSession:null);if(packet.fresh(java.time.Instant.now())){return packet;}
		inspectBag();
		if(inspectedBag!=null&&Objects.equals(key,inspectionCharacter)&&bank.isReady()&&bank.getSession().equals(inspectionSession)&&inspectedBag.fresh(java.time.Instant.now()))
		{long count=0;int form=-1;for(int id:com.fromscratch.buyback.BuybackCatalog.variants("looting_bag_family").keySet()){long n=bank.getBank().quantity(id);count=Math.addExact(count,n);if(n>0){form=id;}}if(count==1){return new com.fromscratch.buyback.BagObservation(form,inspectedBag.getQuantities(),inspectedBag.getNames(),inspectedBag.getAt());}}
		return com.fromscratch.buyback.BagObservation.unknown();
	}
	/** Interface lifetime is distinct from a container awaiting its next complete tick observation. */
	public boolean bankOpen() { return mounted(InterfaceID.BANKMAIN) && visible(InterfaceID.Bankmain.ITEMS_CONTAINER) && !mounted(InterfaceID.BANKPIN_KEYPAD) && !mounted(InterfaceID.BANKPIN_SETTINGS); }
	public void gameTick() { observer.tick();if(!bankOpen()){bagUnavailable();}else if(requestedBag>=0){inspectBag();} }
	public void changed() { timings.event(); observer.changed(); }
	public void configure(boolean enabled) { diagnostics = enabled; timings.configure(enabled); }
	public String characterKey()
	{
		if (!client.isClientThread()) { throw new IllegalStateException("Bank observations require the client thread"); }
		boolean loggedIn = client.getGameState() == GameState.LOGGED_IN && client.getLocalPlayer() != null;
		long hash = loggedIn ? client.getAccountHash() : 0;
		RuneScapeProfileType type = loggedIn ? RuneScapeProfileType.getCurrent(client) : null;
		String profile = loggedIn ? config.getRSProfileKey() : null;
		// Rendering revalidates cheap identity fields, not profile lists or SHA hashes on each frame.
		if (character != null && loggedIn && hash == verifiedHash && type == verifiedType && Objects.equals(profile, verifiedProfile)) { return character; }
		String key = loggedIn ? CharacterIdentity.verify(true, hash, type, profile, config.getRSProfiles()).orElse(null) : null;
		if (!Objects.equals(key, character)) { observer.reset(); character = key; }
		verifiedHash = hash; verifiedType = type; verifiedProfile = profile;
		return key;
	}
	public BankObservation observe()
	{
		BankObservation result = observer.observe();
		long now = System.nanoTime();
		if (diagnostics && log.isDebugEnabled() && (lastDiagnostic == 0 || now - lastDiagnostic >= 5_000_000_000L))
		{ lastDiagnostic = now; log.debug("From Scratch bank: mounted={} ready={} types={} tick={} predicate={}", mounted(InterfaceID.BANKMAIN), result.isReady(), result.isReady() ? result.getBank().getQuantities().size() : -1, result.getTick(), result.getInstruction()); }
		return result;
	}
	public String itemName(int id) { try { return client.getItemDefinition(id).getName(); } catch (RuntimeException ex) { return "Item #" + id; } }
	public int priceId(int id)
	{
		ItemComposition item=client.getItemDefinition(id);
		return item.getNote()!=-1?item.getLinkedNoteId():id;
	}
	public boolean buybackDefinition(int id)
	{
		try
		{
			ItemComposition item=client.getItemDefinition(id);
			return item!=null&&!item.isTradeable()&&!item.isGeTradeable()&&item.getNote()==-1&&item.getPlaceholderTemplateId()==-1;
		}
		catch(RuntimeException ex){return false;}
	}
	public com.fromscratch.pouch.PouchObservation pouch(){return new com.fromscratch.pouch.RuneLitePouchReader(client).banked(observer.observe());}
	private boolean mounted(int group)
	{
		HashTable<WidgetNode> components = client.getComponentTable(); if (components == null) { return false; }
		for (WidgetNode node : components) { if (node.getId() == group) { return true; } } return false;
	}
	private boolean visible(int id) { Widget w = client.getWidget(id); return w != null && !w.isHidden(); }
	private ContainerSnapshot read()
	{
		ItemContainer container = client.getItemContainer(InventoryID.BANK);
		Item[] items = container == null ? null : container.getItems();
		if (items != null) { BankTimings.mark(timings.current(), BankTimings.Phase.CAPTURE); }
		ContainerSnapshot result = BankItems.aggregate(items, id -> client.getItemDefinition(id).getPlaceholderTemplateId() != -1);
		if (result != null) { BankTimings.mark(timings.current(), BankTimings.Phase.AGGREGATE); } return result;
	}
}
