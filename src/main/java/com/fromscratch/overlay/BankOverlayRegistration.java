package com.fromscratch.overlay;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.ui.overlay.OverlayManager;

/** Exactly one overlay registration per enabled lifecycle, with immediate render-state cleanup. */
@Singleton
public final class BankOverlayRegistration
{
	private final Runnable add;
	private final Runnable remove;
	private final BankOverlayState state;
	private boolean registered;

	@Inject
	public BankOverlayRegistration(OverlayManager manager, VirtualBankOverlay overlay, BankOverlayState state)
	{
		this(() -> manager.add(overlay), () -> { manager.remove(overlay); overlay.clearCache(); }, state);
	}
	BankOverlayRegistration(Runnable add, Runnable remove, BankOverlayState state)
	{
		this.add = add; this.remove = remove; this.state = state;
	}
	public synchronized void start()
	{
		if (!registered) { state.clear(); add.run(); registered = true; }
		state.enable();
	}
	public synchronized void stop()
	{
		state.disable();
		if (registered) { remove.run(); registered = false; }
	}
}
