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
	@Inject public RuneLiteBankSource(Client client, ConfigManager config, BankTimings timings)
	{
		this.client = client; this.config = config; this.timings = timings;
		observer = new BankObserver(new BankObserver.Access()
		{
			public boolean normalBankOpen() { return mounted(InterfaceID.BANKMAIN) && visible(InterfaceID.Bankmain.ITEMS_CONTAINER) && !mounted(InterfaceID.BANKPIN_KEYPAD) && !mounted(InterfaceID.BANKPIN_SETTINGS); }
			public ContainerSnapshot readBank() { return read(); }
			public BankTimings.Trace trace() { return timings.current(); }
		});
	}
	public void resetSession() { observer.reset(); character = null; }
	public void gameTick() { observer.tick(); }
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
