package com.fromscratch.observation;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.model.InitializationSession;
import com.fromscratch.model.LegacySnapshot;
import com.fromscratch.model.RunRecord;
import com.fromscratch.model.SnapshotLocation;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.WidgetType;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/** Actual verification -> collection -> preview path, with read-only synthetic widget/container data. */
public class PersonalContainerReaderTest
{
	private final Set<Integer> mounted = new HashSet<>();
	private final Map<Integer, Node> widgets = new HashMap<>();
	private final Map<Integer, List<ObservedItem>> containers = new HashMap<>();
	private EmptyContainerVerifier verifier;
	private PersonalContainerReader personal;
	private BankSnapshotReader bank;

	@Before
	public void setUp()
	{
		verifier = new EmptyContainerVerifier(new EmptyContainerVerifier.Access()
		{
			public boolean isInterfaceOpen(int id) { return mounted.contains(id); }
			public EmptyContainerVerifier.Node widget(int id) { return widgets.get(id); }
		});
		personal = new PersonalContainerReader(containers::get, verifier);
		bank = new BankSnapshotReader(new BankSnapshotReader.Access()
		{
			public boolean isInterfaceOpen(int id) { return mounted.contains(id); }
			public boolean isVisible(int id) { return widgets.containsKey(id) && !widgets.get(id).hidden; }
			public List<ObservedItem> items(int id) { return personal.items(id); }
		});
		containers.put(InventoryID.BANK, Collections.singletonList(new ObservedItem(ItemID.COINS, 100, false)));
	}

	@Test
	public void observedEmptyInventoryAndEmptyEquipmentAreAccepted()
	{
		assertCombination(false, false, false);
	}

	@Test
	public void observedPopulatedInventoryAndEmptyEquipmentAreAccepted()
	{
		assertCombination(true, false, false);
	}

	@Test
	public void observedEmptyInventoryAndPopulatedEquipmentAreAccepted()
	{
		assertCombination(false, true, false);
	}

	@Test
	public void observedPopulatedInventoryAndPopulatedEquipmentAreAccepted()
	{
		assertCombination(true, true, false);
	}

	@Test
	public void nullEmptyInventoryAndNullEmptyEquipmentAreIndependentlyVerified()
	{
		assertCombination(false, false, true);
	}

	@Test
	public void populatedInventoryAndVerifiedNullEmptyEquipmentAreAccepted()
	{
		assertCombination(true, false, true);
	}

	@Test
	public void verifiedNullEmptyInventoryAndPopulatedEquipmentAreAccepted()
	{
		assertCombination(false, true, true);
	}

	@Test
	public void populatedContainersRequireNoEmptyInterfaceVerification()
	{
		assertCombination(true, true, true);
	}

	private LegacySnapshot assertCombination(boolean inventoryPopulated, boolean equipmentPopulated, boolean nullWhenEmpty)
	{
		containers.put(InventoryID.INV, contents(inventoryPopulated, nullWhenEmpty, ItemID.SHARK));
		containers.put(InventoryID.WORN, contents(equipmentPopulated, nullWhenEmpty, ItemID.ABYSSAL_WHIP));
		if (nullWhenEmpty)
		{
			// Observe actual tabs in sequence, never assume both personal tabs are visible together.
			showInventory();
			pollBeforeBank();
			pollBeforeBank();
			mounted.clear();
			showEquipment();
			verifier.reset(); // normal interface lifecycle invalidates incomplete verification candidates
			pollBeforeBank();
			pollBeforeBank();
		}
		openBank();
		tick();
		LegacySnapshot result = bank.capture();
		assertEquals(inventoryPopulated ? 1 : 0, result.at(SnapshotLocation.INVENTORY).getOccupiedSlots());
		assertEquals(equipmentPopulated ? 1 : 0, result.at(SnapshotLocation.EQUIPMENT).getOccupiedSlots());
		assertEquals(100, result.at(SnapshotLocation.BANK).quantity(ItemID.COINS));
		return result;
	}

	@Test
	public void observedEmptySlotSentinelsAreValidWithoutWidgets()
	{
		containers.put(InventoryID.INV, Collections.nCopies(28, new ObservedItem(-1, 0, false)));
		containers.put(InventoryID.WORN, Collections.nCopies(14, new ObservedItem(-1, 0, false)));
		openBank();
		tick();
		assertEquals(0, bank.capture().at(SnapshotLocation.INVENTORY).getTotalQuantity());
		assertEquals(0, bank.capture().at(SnapshotLocation.EQUIPMENT).getTotalQuantity());
	}

	@Test
	public void nullInventoryAndEquipmentWithNoIndependentEvidenceRemainUnknown()
	{
		tick();
		assertNull(personal.items(InventoryID.INV));
		assertNull(personal.items(InventoryID.WORN));
		openBank();
		tick();
		try { bank.capture(); fail("Unknown is not empty"); }
		catch (IllegalStateException expected) { assertTrue(expected.getMessage().contains("Inventory")); }
	}

	@Test
	public void nullBankIsNeverVerifiedFromPersonalInterfaces()
	{
		assertCombination(false, false, true);
		containers.put(InventoryID.BANK, null);
		assertNull(personal.items(InventoryID.BANK));
		try { bank.capture(); fail("A complete bank is mandatory"); }
		catch (IllegalStateException expected) { assertTrue(expected.getMessage().contains("Bank")); }
	}

	@Test
	public void inventoryProofRequiresTwoDifferentGameTicks()
	{
		showInventory();
		assertSettling(InventoryID.INV);
		assertSettling(InventoryID.INV); // repeated reads or a confirmation click cannot advance time
		tick();
		assertTrue(personal.items(InventoryID.INV).isEmpty());
	}

	@Test
	public void bankSideInventoryCanVerifyAllTwentyEightEmptySlots()
	{
		showInventory();
		Node inventory = widgets.remove(InterfaceID.Inventory.ITEMS);
		mounted.clear();
		mounted.add(InterfaceID.BANKSIDE);
		widgets.put(InterfaceID.Bankside.ITEMS, inventory);
		assertSettling(InventoryID.INV);
		tick();
		assertTrue(personal.items(InventoryID.INV).isEmpty());
	}

	@Test
	public void incompleteInventoryGridDoesNotProveEmptiness()
	{
		showInventory();
		widgets.get(InterfaceID.Inventory.ITEMS).children.remove(27);
		assertUnknown(InventoryID.INV);
	}

	@Test
	public void defaultUnbuiltInventoryWidgetsDoNotProveEmptiness()
	{
		showInventory();
		widgets.get(InterfaceID.Inventory.ITEMS).children.get(0).id = -1;
		assertUnknown(InventoryID.INV);
	}

	@Test
	public void hiddenInventoryRootDoesNotProveEmptiness()
	{
		showInventory();
		widgets.get(InterfaceID.Inventory.ITEMS).hidden = true;
		assertUnknown(InventoryID.INV);
	}

	@Test
	public void cachedWidgetsWithoutMountedInterfaceDoNotProveEmptiness()
	{
		showInventory();
		mounted.clear();
		assertUnknown(InventoryID.INV);
	}

	@Test
	public void populatedInventoryWidgetCannotReplaceAMissingContainer()
	{
		showInventory();
		Node slot = widgets.get(InterfaceID.Inventory.ITEMS).children.get(27);
		slot.id = ItemID.SHARK;
		slot.selfHidden = false;
		assertShowsItem(InventoryID.INV);
	}

	@Test
	public void incompleteInventoryCannotHideAContradictingOccupiedSlot()
	{
		showInventory();
		widgets.get(InterfaceID.Inventory.ITEMS).children.remove(0);
		Node slot = widgets.get(InterfaceID.Inventory.ITEMS).children.get(27);
		slot.id = ItemID.SHARK;
		slot.selfHidden = false;
		assertShowsItem(InventoryID.INV);
	}

	@Test
	public void incompleteEquipmentCannotHideAContradictingOccupiedAmmoSlot()
	{
		showEquipment();
		widgets.remove(InterfaceID.Wornitems.SLOT0);
		Node slot = widgets.get(InterfaceID.Wornitems.SLOT13).children.get(1);
		slot.id = ItemID.COINS;
		slot.selfHidden = false;
		assertShowsItem(InventoryID.WORN);
	}

	@Test
	public void missingEquipmentSlotDoesNotProveEmptiness()
	{
		showEquipment();
		widgets.remove(InterfaceID.Wornitems.SLOT13);
		assertUnknown(InventoryID.WORN);
	}

	@Test
	public void defaultEquipmentChildrenDoNotProveEmptiness()
	{
		showEquipment();
		widgets.get(InterfaceID.Wornitems.SLOT0).children.get(1).selfHidden = false;
		assertUnknown(InventoryID.WORN);
	}

	@Test
	public void hiddenEquipmentEmptyIconDoesNotProveEmptiness()
	{
		showEquipment();
		widgets.get(InterfaceID.Wornitems.SLOT0).children.get(2).selfHidden = true;
		assertUnknown(InventoryID.WORN);
	}

	@Test
	public void missingEquipmentEmptySpriteDoesNotProveEmptiness()
	{
		showEquipment();
		widgets.get(InterfaceID.Wornitems.SLOT0).children.get(2).sprite = -1;
		assertUnknown(InventoryID.WORN);
	}

	@Test
	public void equipmentRingAndAmmoCannotBeInferredFromPlayerAppearance()
	{
		showEquipment();
		Node ammo = widgets.get(InterfaceID.Wornitems.SLOT13).children.get(1);
		ammo.id = ItemID.COINS;
		ammo.selfHidden = false;
		assertShowsItem(InventoryID.WORN);
		ammo.selfHidden = true;
		Node ring = widgets.get(InterfaceID.Wornitems.SLOT12).children.get(1);
		ring.id = ItemID.COINS;
		ring.selfHidden = false;
		assertShowsItem(InventoryID.WORN);
	}

	@Test
	public void equipmentHiddenItemMayRetainItsFormerItemIdAfterUnequipping()
	{
		showEquipment();
		widgets.get(InterfaceID.Wornitems.SLOT3).children.get(1).id = ItemID.ABYSSAL_WHIP;
		assertSettling(InventoryID.WORN);
		tick();
		assertTrue(personal.items(InventoryID.WORN).isEmpty());
	}

	@Test
	public void interfaceOrSessionInvalidationRequiresNewEmptyVerification()
	{
		showInventory();
		assertSettling(InventoryID.INV);
		tick();
		assertNotNull(personal.items(InventoryID.INV));
		verifier.reset();
		assertSettling(InventoryID.INV);
		tick();
		assertNotNull(personal.items(InventoryID.INV));
	}

	@Test
	public void aRealPopulatedObservationInvalidatesEarlierEmptyVerification()
	{
		showInventory();
		assertSettling(InventoryID.INV);
		tick();
		assertNotNull(personal.items(InventoryID.INV));
		containers.put(InventoryID.INV, Collections.singletonList(new ObservedItem(ItemID.SHARK, 1, false)));
		assertEquals(1, personal.items(InventoryID.INV).size());
		containers.put(InventoryID.INV, null);
		assertSettling(InventoryID.INV);
	}

	@Test
	public void emptyVerifiedPreviewStillRequiresExplicitConfirmationAndRoundTrips() throws Exception
	{
		LegacySnapshot snapshot = assertCombination(false, false, true);
		InitializationSession session = new InitializationSession(SnapshotFixtures.CHARACTER_A, null);
		session.begin();
		session.observe(snapshot);
		assertNull(session.getSaved());
		RunRecord candidate = session.prepareActivation(session.getReviewToken(), snapshot, Instant.now());
		assertEquals(0, candidate.getEconomy().getSpendableGp());
		assertEquals(candidate, SnapshotFixtures.codec().decode(SnapshotFixtures.codec().encode(candidate), SnapshotFixtures.CHARACTER_A));
		assertNull("Preparing a candidate must not commit it", session.getSaved());
	}

	private static List<ObservedItem> contents(boolean populated, boolean nullWhenEmpty, int id)
	{
		return populated ? Collections.singletonList(new ObservedItem(id, 1, false)) : nullWhenEmpty ? null : Collections.emptyList();
	}

	private void showInventory()
	{
		mounted.add(InterfaceID.INVENTORY);
		Node root = new Node();
		for (int index = 0; index < 28; index++)
		{
			Node slot = new Node();
			slot.id = ItemID.BLANKOBJECT;
			slot.quantity = 1;
			slot.selfHidden = true;
			root.children.put(index, slot);
		}
		widgets.put(InterfaceID.Inventory.ITEMS, root);
	}

	private void showEquipment()
	{
		mounted.add(InterfaceID.WORNITEMS);
		widgets.put(InterfaceID.Wornitems.UNIVERSE, new Node());
		int[] components = {InterfaceID.Wornitems.SLOT0, InterfaceID.Wornitems.SLOT1, InterfaceID.Wornitems.SLOT2,
			InterfaceID.Wornitems.SLOT3, InterfaceID.Wornitems.SLOT4, InterfaceID.Wornitems.SLOT5, InterfaceID.Wornitems.SLOT7,
			InterfaceID.Wornitems.SLOT9, InterfaceID.Wornitems.SLOT10, InterfaceID.Wornitems.SLOT12, InterfaceID.Wornitems.SLOT13};
		for (int component : components)
		{
			Node parent = new Node();
			Node item = new Node();
			item.selfHidden = true;
			Node icon = new Node();
			icon.sprite = 1; // Synthetic initialized empty-slot sprite; not a game item.
			parent.children.put(1, item);
			parent.children.put(2, icon);
			widgets.put(component, parent);
		}
	}

	private void openBank()
	{
		mounted.clear();
		mounted.add(InterfaceID.BANKMAIN);
		widgets.put(InterfaceID.Bankmain.ITEMS_CONTAINER, new Node());
		bank.interfaceChanged();
	}

	private void tick() { verifier.gameTick(); bank.gameTick(); }
	private void pollBeforeBank()
	{
		tick();
		try { bank.capture(); fail("Bank is still closed"); }
		catch (IllegalStateException expected) { assertTrue(expected.getMessage().contains("Open the normal bank")); }
	}
	private void assertSettling(int id)
	{
		try { personal.items(id); fail("One sample is insufficient"); }
		catch (IllegalStateException expected) { assertTrue(expected.getMessage().contains("settling")); }
	}
	private void assertUnknown(int id)
	{
		assertNull(personal.items(id));
		tick();
		assertNull(personal.items(id));
	}
	private void assertShowsItem(int id)
	{
		try { personal.items(id); fail("Populated UI with a missing container must remain blocked"); }
		catch (IllegalStateException expected) { assertTrue(expected.getMessage().contains("shows an item")); }
	}

	private static final class Node implements EmptyContainerVerifier.Node
	{
		private final Map<Integer, Node> children = new HashMap<>();
		private boolean hidden;
		private boolean selfHidden;
		private int id = -1;
		private int quantity;
		private int sprite = -1;
		public boolean hidden() { return hidden; }
		public boolean selfHidden() { return selfHidden; }
		public int type() { return WidgetType.GRAPHIC; }
		public int itemId() { return id; }
		public int quantity() { return quantity; }
		public int spriteId() { return sprite; }
		public Node child(int index) { return children.get(index); }
	}
}
