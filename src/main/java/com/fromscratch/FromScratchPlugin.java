package com.fromscratch;

import com.fromscratch.controller.SetupController;
import com.fromscratch.observation.RuneLiteSnapshotObserver;
import com.fromscratch.overlay.BankOverlayRegistration;
import com.fromscratch.overlay.BankOverlayState;
import com.fromscratch.ui.FromScratchSidebar;
import com.google.inject.Provides;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

/**
 * Wires lifecycle and supported observations to local snapshot setup. It never
 * sends game actions; storage and UI remain outside the client observation layer.
 */
@Slf4j
@PluginDescriptor(
	name = "From Scratch",
	internalName = "from_scratch",
	configName = FromScratchConfig.GROUP,
	description = "Start fresh with a separate virtual economy and progression",
	tags = {"bank", "progression", "challenge", "virtual", "ironman", "wealth"}
)
public class FromScratchPlugin extends Plugin
{
	@Inject
	private FromScratchSidebar sidebar;

	@Inject
	private FromScratchConfig config;

	@Inject
	private SetupController setupController;

	@Inject
	private RuneLiteSnapshotObserver observer;

	@Inject
	private ClientThread clientThread;
	@Inject private BankOverlayRegistration bankOverlay;
	@Inject private BankOverlayState bankOverlayState;

	@Override
	protected void startUp()
	{
		bankOverlayState.configure(config);
		bankOverlay.start();
		// Queue UI work without blocking the client thread. Lifecycle operations
		// share the EDT queue, so rapid enable/disable sequences retain their order.
		clientThread.invokeLater(observer::resetSession);
		setupController.start(this::getPluginDirectory);
		SwingUtilities.invokeLater(() ->
		{
			sidebar.bind(setupController);
			sidebar.show(config.showFoundationHelp());
		});
		log.debug("From Scratch started");
	}

	@Override
	protected void shutDown()
	{
		bankOverlay.stop();
		setupController.stop();
		clientThread.invokeLater(observer::resetSession);
		SwingUtilities.invokeLater(sidebar::hide);
		log.debug("From Scratch stopped");
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		observer.gameTick();
		setupController.tick();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() != GameState.LOGGED_IN)
		{
			bankOverlayState.clear();
			observer.resetSession();
			setupController.tick();
		}
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		// Rendering revalidates character identity each frame. A same-character
		// configuration-profile change must not strand its valid projection.
		clientThread.invokeLater(setupController::tick);
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		int id = event.getContainerId();
		if (id == InventoryID.BANK || id == InventoryID.INV || id == InventoryID.WORN)
		{
			observer.containerChanged(id, event.getItemContainer());
			setupController.observationChanged();
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		personalInterfaceChanged(event.getGroupId());
		if (event.getGroupId() == InterfaceID.BANKMAIN) { observer.bankOpened(); }
		if (event.getGroupId() == InterfaceID.BANKMAIN || event.getGroupId() == InterfaceID.BANKPIN_KEYPAD
			|| event.getGroupId() == InterfaceID.BANKPIN_SETTINGS)
		{
			setupController.observationChanged();
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		personalInterfaceChanged(event.getGroupId());
		if (event.getGroupId() == InterfaceID.BANKMAIN)
		{
			observer.bankClosed();
			setupController.observationChanged();
		}
		else if (event.getGroupId() == InterfaceID.BANKPIN_KEYPAD || event.getGroupId() == InterfaceID.BANKPIN_SETTINGS)
		{
			setupController.observationChanged();
		}
	}

	private void personalInterfaceChanged(int group)
	{
		if (group == InterfaceID.INVENTORY || group == InterfaceID.WORNITEMS || group == InterfaceID.BANKSIDE)
		{
			observer.personalInterfaceChanged();
			setupController.observationChanged();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (FromScratchConfig.GROUP.equals(event.getGroup()))
		{
			bankOverlayState.configure(config);
			SwingUtilities.invokeLater(() -> sidebar.refresh(config.showFoundationHelp()));
		}
	}

	@Provides
	FromScratchConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FromScratchConfig.class);
	}
}
