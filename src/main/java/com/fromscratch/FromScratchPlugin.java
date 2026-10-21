package com.fromscratch;

import com.fromscratch.bank.BankController;
import com.fromscratch.bank.RuneLiteBankSource;
import com.fromscratch.overlay.BankOverlayRegistration;
import com.fromscratch.overlay.BankOverlayState;
import com.fromscratch.bank.BankSidebar;
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
	private BankSidebar sidebar;

	@Inject
	private FromScratchConfig config;

	@Inject
	private BankController setupController;

	@Inject
	private RuneLiteBankSource observer;

	@Inject
	private ClientThread clientThread;
	@Inject private BankOverlayRegistration bankOverlay;
	@Inject private BankOverlayState bankOverlayState;
	@Inject private com.fromscratch.wealth.PriceService prices;

	@Override
	protected void startUp()
	{
		bankOverlayState.configure(config);
		observer.configure(config.developmentItemTrace());
		bankOverlay.start();
		prices.start();
		// Queue UI work without blocking the client thread. Lifecycle operations
		// share the EDT queue, so rapid enable/disable sequences retain their order.
		clientThread.invokeLater(observer::resetSession);
		setupController.start(this::getPluginDirectory);
		SwingUtilities.invokeLater(() ->
		{
			sidebar.bind(setupController);
			sidebar.show();
		});
		log.debug("From Scratch started");
	}

	@Override
	protected void shutDown()
	{
		bankOverlay.stop();
		setupController.stop();
		prices.stop();
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
		GameState state = event.getGameState();
		// Region loading and a normal world hop do not establish a new host login.
		// The renderer already hides itself outside LOGGED_IN. Clearing a valid
		// projection here could strand it when unchanged state is restored.
		if (state != GameState.LOGGED_IN && state != GameState.LOADING && state != GameState.HOPPING)
		{
			bankOverlayState.clear();
			observer.resetSession();
			setupController.tick();
		}
		else if (state == GameState.LOGGED_IN) { setupController.tick(); }
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
		if (id == InventoryID.BANK)
		{
			observer.changed();
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.BANKMAIN || event.getGroupId() == InterfaceID.BANKPIN_KEYPAD
			|| event.getGroupId() == InterfaceID.BANKPIN_SETTINGS)
		{
			observer.changed();
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		if (event.getGroupId() == InterfaceID.BANKMAIN)
		{
			observer.changed();
		}
		else if (event.getGroupId() == InterfaceID.BANKPIN_KEYPAD || event.getGroupId() == InterfaceID.BANKPIN_SETTINGS)
		{
			observer.changed();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (FromScratchConfig.GROUP.equals(event.getGroup()))
		{
			bankOverlayState.configure(config);
			observer.configure(config.developmentItemTrace());
		}
	}

	@Provides
	FromScratchConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FromScratchConfig.class);
	}
}
