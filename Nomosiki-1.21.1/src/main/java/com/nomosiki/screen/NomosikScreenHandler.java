package com.nomosiki.screen;

import com.nomosiki.advancement.NomosikiActions;
import com.nomosiki.advancement.NomosikiAdvancements;
import com.nomosiki.compat.melodies.NomosikImmersiveMelodiesCompat;
import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.registry.ModScreenHandlers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class NomosikScreenHandler extends AbstractContainerMenu {
	public static final int NOMOSIK_SLOTS = 6;
	public static final int PORTRAIT_X = 4;
	public static final int PORTRAIT_Y = 22;
	public static final int PORTRAIT_W = 90;
	public static final int PORTRAIT_H = 80;
	public static final int EQUIP_LEFT = 98;
	public static final int EQUIP_TOP = 28;
	public static final int STATS_X = 140;
	public static final int PLAYER_LEFT = 57;
	public static final int PLAYER_TOP = 138;

	private final NomosikEntity nomosik;
	private boolean showPlayerInventory = true;
	private final int unitSlotCount;

	public static NomosikScreenHandler create(int syncId, Inventory playerInventory, NomosikMenuData data) {
		Entity entity = playerInventory.player.level().getEntity(data.entityId());
		if (!(entity instanceof NomosikEntity w)) {
			throw new IllegalStateException("nomosiki:nomosik entity not found for menu");
		}
		return new NomosikScreenHandler(ModScreenHandlers.NOMOSIK, syncId, playerInventory, w);
	}

	public NomosikScreenHandler(MenuType<?> type, int syncId, Inventory playerInventory, NomosikEntity nomosik) {
		super(type, syncId);
		this.nomosik = nomosik;
		this.unitSlotCount = NOMOSIK_SLOTS;
		Container inv = nomosik.getEquipmentInventory();
		nomosik.getEquipmentInventory().startOpen(playerInventory.player);

		addEquipmentSlot(inv, 0, EquipmentSlot.HEAD, EQUIP_LEFT, EQUIP_TOP, nomosik);
		addEquipmentSlot(inv, 1, EquipmentSlot.CHEST, EQUIP_LEFT + 18, EQUIP_TOP, nomosik);
		addEquipmentSlot(inv, 2, EquipmentSlot.LEGS, EQUIP_LEFT, EQUIP_TOP + 18, nomosik);
		addEquipmentSlot(inv, 3, EquipmentSlot.FEET, EQUIP_LEFT + 18, EQUIP_TOP + 18, nomosik);
		addEquipmentSlot(inv, 4, EquipmentSlot.MAINHAND, EQUIP_LEFT, EQUIP_TOP + 36, nomosik);
		addEquipmentSlot(inv, 5, EquipmentSlot.OFFHAND, EQUIP_LEFT + 18, EQUIP_TOP + 36, nomosik);

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new PlayerInvSlot(playerInventory, col + row * 9 + 9, PLAYER_LEFT + col * 18, PLAYER_TOP + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			this.addSlot(new PlayerInvSlot(playerInventory, col, PLAYER_LEFT + col * 18, PLAYER_TOP + 58));
		}
	}

	public NomosikEntity getNomosik() {
		return this.nomosik;
	}

	public void setShowPlayerInventory(boolean show) {
		this.showPlayerInventory = show;
	}

	public boolean showPlayerInventory() {
		return this.showPlayerInventory;
	}

	private final class PlayerInvSlot extends Slot {
		PlayerInvSlot(Inventory inventory, int index, int x, int y) {
			super(inventory, index, x, y);
		}

		@Override
		public boolean isActive() {
			return NomosikScreenHandler.this.showPlayerInventory;
		}
	}

	private void addEquipmentSlot(Container container, int index, EquipmentSlot intended, int x, int y, NomosikEntity owner) {
		this.addSlot(new Slot(container, index, x, y) {
			@Override
			public boolean mayPlace(@NotNull ItemStack stack) {
				if ((intended == EquipmentSlot.MAINHAND || intended == EquipmentSlot.OFFHAND)
					&& NomosikImmersiveMelodiesCompat.isInstrument(stack)) {
					return true;
				}
				return owner.getEquipmentSlotForItem(stack) == intended;
			}

			@Override
			public void setChanged() {
				super.setChanged();
				if (this.getItem().isEmpty() || owner.getOwnerUUID() == null) {
					return;
				}
				if (owner.level() instanceof ServerLevel level) {
					NomosikiAdvancements.grant(level.getServer(), owner.getOwnerUUID(), NomosikiActions.EQUIP);
				}
			}
		});
	}

	@Override
	public void removed(@NotNull Player player) {
		super.removed(player);
		this.nomosik.getEquipmentInventory().stopOpen(player);
	}

	@Override
	public boolean stillValid(@NotNull Player player) {
		return this.nomosik.isAlive() && player.distanceToSqr(this.nomosik) <= 64.0;
	}

	@Override
	public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
		ItemStack result = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if (slot.hasItem()) {
			ItemStack taken = slot.getItem();
			result = taken.copy();
			if (index < this.unitSlotCount) {
				if (!this.moveItemStackTo(taken, this.unitSlotCount, this.slots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.moveItemStackTo(taken, 0, this.unitSlotCount, false)) {
				return ItemStack.EMPTY;
			}
			if (taken.isEmpty()) {
				slot.setByPlayer(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}
			if (taken.getCount() == result.getCount()) {
				return ItemStack.EMPTY;
			}
			slot.onTake(player, taken);
		}
		return result;
	}
}
