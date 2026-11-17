package com.nomosiki.screen;

import com.nomosiki.registry.ModScreenHandlers;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class FactionCreateMenu extends AbstractContainerMenu {
	public static final int BANNER_SLOT_INDEX = 0;
	public static final int BANNER_SLOT_X = 80;
	public static final int BANNER_SLOT_Y = 34;
	public static final int PLAYER_LEFT = 8;
	public static final int PLAYER_TOP = 124;
	public static final int HOTBAR_TOP = 182;
	public static final int IMAGE_WIDTH = 176;
	public static final int IMAGE_HEIGHT = 208;

	private final Container bannerContainer = new SimpleContainer(1);

	public FactionCreateMenu(int syncId, Inventory playerInventory) {
		super(ModScreenHandlers.FACTION_CREATE, syncId);
		this.addSlot(new Slot(this.bannerContainer, 0, BANNER_SLOT_X, BANNER_SLOT_Y) {
			@Override
			public boolean mayPlace(@NotNull ItemStack stack) {
				return stack.getItem() instanceof BannerItem;
			}
		});
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(playerInventory, col + row * 9 + 9, PLAYER_LEFT + col * 18, PLAYER_TOP + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(playerInventory, col, PLAYER_LEFT + col * 18, HOTBAR_TOP));
		}
	}

	@Override
	public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
		ItemStack result = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack taken = slot.getItem();
		result = taken.copy();
		if (index == BANNER_SLOT_INDEX) {
			if (!this.moveItemStackTo(taken, 1, this.slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (!this.moveItemStackTo(taken, BANNER_SLOT_INDEX, BANNER_SLOT_INDEX + 1, false)) {
			if (index < 28) {
				if (!this.moveItemStackTo(taken, 28, this.slots.size(), false)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.moveItemStackTo(taken, 1, 28, false)) {
				return ItemStack.EMPTY;
			}
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
		return result;
	}

	public ItemStack getBannerStack() {
		return this.bannerContainer.getItem(0);
	}

	public void consumeBanner() {
		this.bannerContainer.setItem(0, ItemStack.EMPTY);
	}

	@Override
	public boolean stillValid(@NotNull Player player) {
		return true;
	}
}
