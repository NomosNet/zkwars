package com.example.inventory;

import com.example.advancement.WorkerActions;
import com.example.advancement.WorkerAdvancements;
import com.example.block.WorkerKind;
import com.example.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class WorkerMenu extends AbstractContainerMenu {
	public static final int QUARRY_SLOT_Y = 66;
	public static final int QUARRY_PLAYER_Y = 132;
	public static final int QUARRY_HOTBAR_Y = 190;
	public static final int QUARRY_IMAGE_HEIGHT = 214;
	public static final int QUARRY_INVENTORY_LABEL_Y = 122;
	public static final int RADIUS_SLOT_Y = 40;
	public static final int RADIUS_PLAYER_Y = 106;
	public static final int RADIUS_HOTBAR_Y = 164;
	public static final int RADIUS_IMAGE_HEIGHT = 188;
	public static final int RADIUS_INVENTORY_LABEL_Y = 94;
	public static final int CRAFTER_OUTPUT_Y = 146;
	public static final int SMELTER_PLAYER_Y = 176;
	public static final int SMELTER_HOTBAR_Y = 234;
	public static final int SMELTER_IMAGE_HEIGHT = 258;
	public static final int SMELTER_INVENTORY_LABEL_Y = 164;
	public static final int SMELTER_INPUT_LABEL_Y = 18;
	public static final int SMELTER_FUEL_LABEL_Y = 66;
	public static final int SMELTER_OUTPUT_LABEL_Y = 114;
	public static final int CRAFTER_PATTERN_X = 62;
	public static final int CRAFTER_PLAYER_Y = 180;
	public static final int CRAFTER_HOTBAR_Y = 238;
	public static final int CRAFTER_IMAGE_HEIGHT = 262;
	public static final int CRAFTER_INVENTORY_LABEL_Y = 168;
	public static final int CRAFTER_PATTERN_LABEL_Y = 18;
	public static final int CRAFTER_RESOURCE_LABEL_Y = 86;
	public static final int CRAFTER_OUTPUT_LABEL_Y = 136;
	private static final int[] SMELTER_ROW_Y = {28, 46, 76, 94, 124, 142};
	private static final int[] CRAFTER_PATTERN_Y = {28, 46, 64};
	private static final int[] CRAFTER_RESOURCE_Y = {96, 114};

	private final Container container;
	private final WorkerMenuData data;

	public WorkerMenu(int containerId, Inventory playerInventory, Container container, WorkerMenuData data) {
		super(ModMenus.WORKER, containerId);
		this.container = container;
		this.data = data;
		int blockSlots = WorkerSlots.slotCount(data.kind());
		checkContainerSize(container, blockSlots);
		container.startOpen(playerInventory.player);

		if (data.kind() == WorkerKind.SMELTER) {
			for (int row = 0; row < SMELTER_ROW_Y.length; row++) {
				for (int column = 0; column < 9; column++) {
					int index = column + row * 9;
					this.addSlot(new FilteredSlot(container, index, 8 + column * 18, SMELTER_ROW_Y[row], data.kind()));
				}
			}
			this.addPlayerSlots(playerInventory, SMELTER_PLAYER_Y, SMELTER_HOTBAR_Y);
		} else if (data.kind() == WorkerKind.CRAFTER) {
			for (int row = 0; row < CRAFTER_PATTERN_Y.length; row++) {
				for (int column = 0; column < 3; column++) {
					int index = column + row * 3;
					this.addSlot(new FilteredSlot(container, index, CRAFTER_PATTERN_X + column * 18, CRAFTER_PATTERN_Y[row], data.kind()));
				}
			}
			int resources = WorkerSlots.resourceStart(WorkerKind.CRAFTER);
			for (int row = 0; row < CRAFTER_RESOURCE_Y.length; row++) {
				for (int column = 0; column < 9; column++) {
					int index = resources + column + row * 9;
					this.addSlot(new FilteredSlot(container, index, 8 + column * 18, CRAFTER_RESOURCE_Y[row], data.kind()));
				}
			}
			int output = WorkerSlots.outputStart(WorkerKind.CRAFTER);
			for (int column = 0; column < 9; column++) {
				this.addSlot(new FilteredSlot(container, output + column, 8 + column * 18, CRAFTER_OUTPUT_Y, data.kind()));
			}
			this.addPlayerSlots(playerInventory, CRAFTER_PLAYER_Y, CRAFTER_HOTBAR_Y);
		} else if (data.kind() == WorkerKind.QUARRY) {
			for (int row = 0; row < 3; row++) {
				for (int column = 0; column < 9; column++) {
					int index = column + row * 9;
					this.addSlot(new FilteredSlot(container, index, 8 + column * 18, QUARRY_SLOT_Y + row * 18, data.kind()));
				}
			}
			this.addPlayerSlots(playerInventory, QUARRY_PLAYER_Y, QUARRY_HOTBAR_Y);
		} else if (data.kind().editableRadius() || data.kind() == WorkerKind.BUILDER) {
			for (int row = 0; row < 3; row++) {
				for (int column = 0; column < 9; column++) {
					int index = column + row * 9;
					this.addSlot(new FilteredSlot(container, index, 8 + column * 18, RADIUS_SLOT_Y + row * 18, data.kind()));
				}
			}
			this.addPlayerSlots(playerInventory, RADIUS_PLAYER_Y, RADIUS_HOTBAR_Y);
		} else {
			for (int row = 0; row < 3; row++) {
				for (int column = 0; column < 9; column++) {
					int index = column + row * 9;
					this.addSlot(new FilteredSlot(container, index, 8 + column * 18, 18 + row * 18, data.kind()));
				}
			}
			this.addPlayerSlots(playerInventory, 84, 142);
		}
	}

	public static WorkerMenu create(int containerId, Inventory playerInventory, WorkerMenuData data) {
		return new WorkerMenu(containerId, playerInventory, new SimpleContainer(WorkerSlots.slotCount(data.kind())), data);
	}

	public WorkerKind kind() {
		return this.data.kind();
	}

	public BlockPos pos() {
		return this.data.pos();
	}

	public WorkerMenuData data() {
		return this.data;
	}

	private void addPlayerSlots(Inventory playerInventory, int inventoryY, int hotbarY) {
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, inventoryY + row * 18));
			}
		}
		for (int column = 0; column < 9; column++) {
			this.addSlot(new Slot(playerInventory, column, 8 + column * 18, hotbarY));
		}
	}

	@Override
	public boolean stillValid(Player player) {
		return this.container.stillValid(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		ItemStack original = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if (slot != null && slot.hasItem()) {
			ItemStack stack = slot.getItem();
			original = stack.copy();
			int blockSlots = WorkerSlots.slotCount(this.data.kind());
			if (index < blockSlots) {
				if (!this.moveItemStackTo(stack, blockSlots, this.slots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else if (this.data.kind() == WorkerKind.CRAFTER) {
				int start = WorkerSlots.resourceStart(WorkerKind.CRAFTER);
				int end = start + WorkerSlots.resourceCount(WorkerKind.CRAFTER);
				if (!this.moveItemStackTo(stack, start, end, false)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.moveItemStackTo(stack, 0, blockSlots, false)) {
				return ItemStack.EMPTY;
			}

			if (stack.isEmpty()) {
				slot.setByPlayer(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}

			if (stack.getCount() == original.getCount()) {
				return ItemStack.EMPTY;
			}
			slot.onTake(player, stack);
		}
		return original;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		this.container.stopOpen(player);
	}

	private static final class FilteredSlot extends Slot {
		private final WorkerKind kind;

		private FilteredSlot(Container container, int index, int x, int y, WorkerKind kind) {
			super(container, index, x, y);
			this.kind = kind;
		}

		@Override
		public void onTake(Player player, ItemStack stack) {
			super.onTake(player, stack);
			if (WorkerSlots.isOutputSlot(this.kind, this.getContainerSlot())) {
				WorkerAdvancements.grant(player, WorkerActions.OUTPUT_TAKEN);
			}
		}

		@Override
		public int getMaxStackSize() {
			return WorkerSlots.isPatternSlot(this.kind, this.getContainerSlot()) ? 1 : super.getMaxStackSize();
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			Level level = this.container instanceof BlockEntity blockEntity ? blockEntity.getLevel() : null;
			return WorkerSlots.mayPlace(this.kind, this.getContainerSlot(), stack, level);
		}
	}
}
