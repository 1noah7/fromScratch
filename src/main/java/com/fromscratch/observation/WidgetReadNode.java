package com.fromscratch.observation;

import net.runelite.api.widgets.Widget;

/** Read-only adapter; all calls are confined to the client thread. */
final class WidgetReadNode implements EmptyContainerVerifier.Node
{
	private final Widget widget;
	private WidgetReadNode(Widget widget) { this.widget = widget; }
	static WidgetReadNode of(Widget widget) { return widget == null ? null : new WidgetReadNode(widget); }
	public boolean hidden() { return widget.isHidden(); }
	public boolean selfHidden() { return widget.isSelfHidden(); }
	public int type() { return widget.getType(); }
	public int itemId() { return widget.getItemId(); }
	public int quantity() { return widget.getItemQuantity(); }
	public int spriteId() { return widget.getSpriteId(); }
	public WidgetReadNode child(int index) { return of(widget.getChild(index)); }
}
