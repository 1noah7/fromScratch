package com.fromscratch.observation;

import java.util.HashMap;
import java.util.Map;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.WidgetType;

/**
 * Positive evidence for null personal containers, never a null-to-empty default.
 * The normal inventory builds all 28 children, including hidden BLANKOBJECT slots.
 * wear_initslot builds an item child (1) and empty-slot icon (2); wear_updateslot
 * toggles them in opposite directions. Every logical equipment slot must agree.
 * Two different game ticks with a complete visible view guard transient UI builds.
 * This verifier never reads bank item widgets or derives populated quantities.
 */
public final class EmptyContainerVerifier
{
	public enum Result { UNKNOWN, NON_EMPTY, SETTLING, EMPTY }
	private static final int INVENTORY_SLOTS = 28;
	// ARMS, HAIR and JAW in EquipmentInventorySlot are appearance positions, not item slots.
	private static final int[] EQUIPMENT_COMPONENTS = {
		InterfaceID.Wornitems.SLOT0, InterfaceID.Wornitems.SLOT1, InterfaceID.Wornitems.SLOT2,
		InterfaceID.Wornitems.SLOT3, InterfaceID.Wornitems.SLOT4, InterfaceID.Wornitems.SLOT5,
		InterfaceID.Wornitems.SLOT7, InterfaceID.Wornitems.SLOT9, InterfaceID.Wornitems.SLOT10,
		InterfaceID.Wornitems.SLOT12, InterfaceID.Wornitems.SLOT13
	};
	private final Access access;
	private final Map<Integer, Candidate> candidates = new HashMap<>();
	private final Map<Integer, Result> results = new HashMap<>();
	private long tick;

	public EmptyContainerVerifier(Access access) { this.access = access; }
	public void gameTick() { tick++; }
	public void reset() { candidates.clear(); results.clear(); }
	public void invalidate(int id) { candidates.remove(id); results.remove(id); }

	public Result verify(int id)
	{
		int view = -1;
		Result evidence = Result.UNKNOWN;
		if (id == InventoryID.INV)
		{
			if (open(InterfaceID.INVENTORY, InterfaceID.Inventory.ITEMS)) { view = InterfaceID.Inventory.ITEMS; }
			else if (open(InterfaceID.BANKSIDE, InterfaceID.Bankside.ITEMS)) { view = InterfaceID.Bankside.ITEMS; }
			if (view != -1) { evidence = inventory(access.widget(view)); }
		}
		else if (id == InventoryID.WORN && open(InterfaceID.WORNITEMS, InterfaceID.Wornitems.UNIVERSE))
		{
			view = InterfaceID.Wornitems.UNIVERSE;
			evidence = equipment();
		}
		Result result = evidence;
		if (evidence == Result.EMPTY)
		{
			Candidate prior = candidates.get(id);
			if (prior == null || prior.view != view)
			{
				candidates.put(id, new Candidate(view, tick));
				result = Result.SETTLING;
			}
			else if (tick <= prior.tick) { result = Result.SETTLING; }
		}
		else { candidates.remove(id); }
		results.put(id, result);
		return result;
	}

	private boolean open(int group, int component)
	{
		Node root = access.widget(component);
		return access.isInterfaceOpen(group) && root != null && !root.hidden();
	}

	private Result inventory(Node root)
	{
		boolean complete = true;
		for (int index = 0; index < INVENTORY_SLOTS; index++)
		{
			Node slot = root.child(index);
			if (slot == null || slot.type() != WidgetType.GRAPHIC) { complete = false; continue; }
			if (!slot.selfHidden() && slot.itemId() >= 0 && slot.itemId() != ItemID.BLANKOBJECT)
			{
				return Result.NON_EMPTY;
			}
			// A default itemId=-1 widget is not proof that inventory_drawitem ran.
			if (!slot.selfHidden() || slot.itemId() != ItemID.BLANKOBJECT || slot.quantity() != 1)
			{
				complete = false;
			}
		}
		return complete ? Result.EMPTY : Result.UNKNOWN;
	}

	private Result equipment()
	{
		boolean complete = true;
		for (int component : EQUIPMENT_COMPONENTS)
		{
			Node parent = access.widget(component);
			if (parent == null || parent.hidden()) { complete = false; continue; }
			Node item = parent.child(1);
			Node emptyIcon = parent.child(2);
			if (item != null && item.type() == WidgetType.GRAPHIC && !item.selfHidden() && item.itemId() >= 0)
			{
				return Result.NON_EMPTY;
			}
			if (item == null || emptyIcon == null || item.type() != WidgetType.GRAPHIC
				|| emptyIcon.type() != WidgetType.GRAPHIC) { complete = false; continue; }
			// Item ID can retain the previous item after unequipping. The empty
			// branch explicitly hides that child and shows the initialized icon.
			if (!item.selfHidden() || emptyIcon.selfHidden() || emptyIcon.spriteId() < 0)
			{
				complete = false;
			}
		}
		return complete ? Result.EMPTY : Result.UNKNOWN;
	}

	public String diagnostics()
	{
		return "emptyVerification[INV=" + results.getOrDefault(InventoryID.INV, Result.UNKNOWN)
			+ ",WORN=" + results.getOrDefault(InventoryID.WORN, Result.UNKNOWN) + "]";
	}

	/** Small supported widget-read boundary, also usable without a game client in tests. */
	public interface Access
	{
		boolean isInterfaceOpen(int group);
		Node widget(int component);
	}

	public interface Node
	{
		boolean hidden();
		boolean selfHidden();
		int type();
		int itemId();
		int quantity();
		int spriteId();
		Node child(int index);
	}

	private static final class Candidate
	{
		private final int view;
		private final long tick;
		private Candidate(int view, long tick) { this.view = view; this.tick = tick; }
	}
}
