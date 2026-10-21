package com.fromscratch.observation;

import com.fromscratch.model.LegacySnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.WidgetNode;
import net.runelite.api.HashTable;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneScapeProfileType;

/** Client-thread-only observation adapter. It never sends game actions or reads special storage. */
@Singleton
@Slf4j
public final class RuneLiteSnapshotObserver implements SnapshotSource
{
	private final Client client;
	private final ConfigManager configManager;
	private final BankSnapshotReader bankReader;
	private final EmptyContainerVerifier emptyVerifier;
	private long verifiedHash;
	private RuneScapeProfileType verifiedType;
	private String verifiedProfile;
	private String verifiedCharacter;
	private boolean initializing;
	private final DiagnosticRateLimit diagnosticRateLimit = new DiagnosticRateLimit();

	@Inject
	public RuneLiteSnapshotObserver(Client client, ConfigManager configManager)
	{
		this.client = client;
		this.configManager = configManager;
		emptyVerifier = new EmptyContainerVerifier(new EmptyContainerVerifier.Access()
		{
			public boolean isInterfaceOpen(int id) { return interfaceOpen(id); }
			public EmptyContainerVerifier.Node widget(int id) { return WidgetReadNode.of(client.getWidget(id)); }
		});
		PersonalContainerReader containerReader = new PersonalContainerReader(
			id -> copy(id, id == InventoryID.BANK), emptyVerifier);
		bankReader = new BankSnapshotReader(new BankSnapshotReader.Access()
		{
			public boolean isInterfaceOpen(int interfaceId)
			{
				// getWidget alone can return a cached definition after closure. The
				// component table contains mounted interfaces, including an already
				// open bank whose WidgetLoaded event preceded plugin enablement.
				return interfaceOpen(interfaceId);
			}

			public boolean isVisible(int componentId)
			{
				Widget widget = client.getWidget(componentId);
				return widget != null && !widget.isHidden();
			}

			public List<ObservedItem> items(int inventoryId)
			{
				return containerReader.items(inventoryId);
			}
		});
	}

	public void resetSession()
	{
		endInitialization();
		verifiedCharacter = null;
	}

	public void beginInitialization()
	{
		emptyVerifier.reset();
		bankReader.reset();
		initializing = true;
	}

	public void endInitialization()
	{
		emptyVerifier.reset();
		initializing = false;
		bankReader.reset();
	}

	public CaptureProgress progress() { return bankReader.progress(); }

	public void containerChanged(int id, ItemContainer container)
	{
		if (!initializing) { return; }
		emptyVerifier.reset();
		List<ObservedItem> items = null;
		try { items = copy(container, id == InventoryID.BANK); }
		catch (IllegalStateException ex) { /* A malformed payload must still invalidate the old observation. */ }
		bankReader.containerChanged(id, items);
	}

	public void bankOpened() { bankReader.interfaceChanged(); }
	public void bankClosed() { bankReader.interfaceChanged(); }
	public void gameTick() { emptyVerifier.gameTick(); bankReader.gameTick(); }

	/** UI loading/closure invalidates verification candidates, not earlier valid setup observations. */
	public void personalInterfaceChanged() { emptyVerifier.reset(); }

	public String characterKey()
	{
		if (!client.isClientThread()) { throw new IllegalStateException("Snapshot observations require the client thread"); }
		if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null) { return null; }
		long hash = client.getAccountHash();
		RuneScapeProfileType type = RuneScapeProfileType.getCurrent(client);
		String profile = configManager.getRSProfileKey();
		if (verifiedCharacter != null && hash == verifiedHash && type == verifiedType && Objects.equals(profile, verifiedProfile))
		{
			return verifiedCharacter;
		}
		String key = CharacterIdentity.verify(true, hash, type, profile, configManager.getRSProfiles()).orElse(null);
		if (!Objects.equals(key, verifiedCharacter)) { endInitialization(); }
		verifiedHash = hash;
		verifiedType = type;
		verifiedProfile = profile;
		verifiedCharacter = key;
		return key;
	}

	public LegacySnapshot capture(String expectedCharacter)
	{
		if (expectedCharacter == null || !expectedCharacter.equals(characterKey()))
		{
			throw new IllegalStateException("Log in and wait for a verified character identity before initializing");
		}
		try { return bankReader.capture(); }
		finally
		{
			if (log.isDebugEnabled() && diagnosticRateLimit.allow(System.nanoTime()))
			{
				log.debug("From Scratch capture: loggedIn={} initializing={} liveSizes[BANK={},INV={},WORN={}] {} {}",
					client.getGameState() == GameState.LOGGED_IN, initializing,
					liveSize(InventoryID.BANK), liveSize(InventoryID.INV), liveSize(InventoryID.WORN), bankReader.diagnostics(), emptyVerifier.diagnostics());
			}
		}
	}

	private boolean interfaceOpen(int id)
	{
		HashTable<WidgetNode> components = client.getComponentTable();
		if (components == null) { return false; }
		for (WidgetNode node : components) { if (node.getId() == id) { return true; } }
		return false;
	}

	private int liveSize(int id)
	{
		ItemContainer container = client.getItemContainer(id);
		return container == null ? -1 : container.getItems() == null ? -2 : container.getItems().length;
	}

	private List<ObservedItem> copy(int containerId, boolean bank)
	{
		return copy(client.getItemContainer(containerId), bank);
	}

	private List<ObservedItem> copy(ItemContainer container, boolean bank)
	{
		if (container == null || container.getItems() == null) { return null; }
		List<ObservedItem> result = new ArrayList<>();
		for (Item item : container.getItems())
		{
			if (item == null) { throw new IllegalStateException("A container is incomplete; reopen the bank and retry"); }
			int id = item.getId();
			boolean placeholder = id >= 0 && item.getQuantity() > 0
				&& ((bank && id == ItemID.BANK_FILLER) || client.getItemDefinition(id).getPlaceholderTemplateId() != -1);
			result.add(new ObservedItem(id, item.getQuantity(), placeholder));
		}
		return result;
	}
}
