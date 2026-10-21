package com.fromscratch.ui;

import com.fromscratch.FromScratchPlugin;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

/** Owns exactly one navigation registration while enabled. All state is EDT-confined. */
@Singleton
public final class FromScratchSidebar
{
	private final Consumer<NavigationButton> addNavigation;
	private final Consumer<NavigationButton> removeNavigation;
	private NavigationButton navigation;
	private FromScratchPanel panel;
	private SetupView view = SetupView.signedOut();
	private SetupActions actions = SetupActions.NONE;

	public void bind(SetupActions actions)
	{
		requireEdt();
		this.actions = actions;
		if (panel != null) { panel.render(view, actions); }
	}

	/** Client callbacks deliver immutable data; Swing never reads game state directly. */
	public void update(SetupView updated)
	{
		SwingUtilities.invokeLater(() ->
		{
			view = updated;
			if (panel != null) { panel.render(view, actions); }
		});
	}

	@Inject
	public FromScratchSidebar(ClientToolbar toolbar)
	{
		this(toolbar::addNavigation, toolbar::removeNavigation);
	}

	// The narrow toolbar boundary permits lifecycle tests without a game client.
	FromScratchSidebar(Consumer<NavigationButton> addNavigation, Consumer<NavigationButton> removeNavigation)
	{
		this.addNavigation = addNavigation;
		this.removeNavigation = removeNavigation;
	}

	public void show(boolean showHelp)
	{
		requireEdt();
		if (navigation != null)
		{
			refresh(showHelp);
			return;
		}
		panel = new FromScratchPanel(showHelp);
		panel.render(view, actions);
		navigation = NavigationButton.builder()
			.tooltip("From Scratch")
			.icon(ImageUtil.loadImageResource(FromScratchPlugin.class, "icon.png"))
			.priority(6)
			.panel(panel)
			.build();
		addNavigation.accept(navigation);
	}

	public void refresh(boolean showHelp)
	{
		requireEdt();
		if (panel != null)
		{
			panel.setFoundationHelpVisible(showHelp);
		}
	}

	public void hide()
	{
		requireEdt();
		view = SetupView.signedOut();
		if (navigation != null)
		{
			removeNavigation.accept(navigation);
			navigation = null;
			panel = null;
		}
	}

	private static void requireEdt()
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			throw new IllegalStateException("Sidebar operations require the EDT");
		}
	}
}
