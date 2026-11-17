package com.nomosiki.screen;

import com.nomosiki.registry.ModBlocks;
import com.nomosiki.registry.ModScreenHandlers;
import com.nomosiki.shop.TesterSquadShop;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class TesterSquadShopScreenHandler extends AbstractContainerMenu {
	public static final int PLAYER_SLOT_COUNT = 36;
	public static final int PLAYER_LEFT = 8;
	public static final int PLAYER_TOP = 160;
	public static final int HOTBAR_TOP = 218;
	public static final int IMAGE_WIDTH = 176;
	public static final int IMAGE_HEIGHT = 242;
	public static final int INVENTORY_LABEL_Y = PLAYER_TOP - 11;

	private final BlockPos shopPos;

	public static TesterSquadShopScreenHandler create(int syncId, Inventory playerInventory, TesterSquadShopMenuData data) {
		return new TesterSquadShopScreenHandler(ModScreenHandlers.TESTER_SQUAD_SHOP, syncId, playerInventory, data.pos());
	}

	public TesterSquadShopScreenHandler(MenuType<?> type, int syncId, Inventory playerInventory, BlockPos shopPos) {
		super(type, syncId);
		this.shopPos = shopPos.immutable();
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(playerInventory, col + row * 9 + 9, PLAYER_LEFT + col * 18, PLAYER_TOP + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(playerInventory, col, PLAYER_LEFT + col * 18, HOTBAR_TOP));
		}
	}

	public BlockPos getShopPos() {
		return this.shopPos;
	}

	@Override
	public boolean stillValid(@NotNull Player player) {
		if (!player.level().getBlockState(this.shopPos).is(ModBlocks.TESTER_SQUAD_SHOP)) {
			return false;
		}
		return player.distanceToSqr(
			this.shopPos.getX() + 0.5,
			this.shopPos.getY() + 0.5,
			this.shopPos.getZ() + 0.5
		) <= TesterSquadShop.USE_RANGE_SQR;
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
