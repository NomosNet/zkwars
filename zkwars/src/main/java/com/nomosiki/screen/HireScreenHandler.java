package com.nomosiki.screen;

import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.registry.ModScreenHandlers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class HireScreenHandler extends AbstractContainerMenu {
	public static final int PLAYER_SLOT_COUNT = 36;
	public static final int PLAYER_LEFT = 49;
	public static final int PLAYER_TOP = 142;
	public static final int HOTBAR_TOP = 200;
	public static final int INVENTORY_LABEL_Y = 130;

	private final NomosikEntity nomosik;

	public static HireScreenHandler create(int syncId, Inventory playerInventory, HireMenuData data) {
		Entity entity = playerInventory.player.level().getEntity(data.entityId());
		if (!(entity instanceof NomosikEntity nomosik)) {
			throw new IllegalStateException("zkwars: nomosik entity not found for hire menu");
		}
		return new HireScreenHandler(ModScreenHandlers.HIRE, syncId, playerInventory, nomosik);
	}

	public HireScreenHandler(MenuType<?> type, int syncId, Inventory playerInventory, NomosikEntity nomosik) {
		super(type, syncId);
		this.nomosik = nomosik;
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(playerInventory, col + row * 9 + 9, PLAYER_LEFT + col * 18, PLAYER_TOP + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(playerInventory, col, PLAYER_LEFT + col * 18, HOTBAR_TOP));
		}
	}

	public NomosikEntity getNomosik() {
		return this.nomosik;
	}

	@Override
	public boolean stillValid(@NotNull Player player) {
		return this.nomosik.isAlive()
			&& this.nomosik.getOwnerUUID() == null
			&& player.distanceToSqr(this.nomosik) <= 64.0;
	}

	@Override
	public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
		ItemStack result = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if (slot.hasItem()) {
			ItemStack taken = slot.getItem();
			result = taken.copy();
			if (!this.moveItemStackTo(taken, 0, PLAYER_SLOT_COUNT, true)) {
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
