package com.example.inventory;

import com.example.advancement.WorkerAdvancements;
import com.example.entity.HumanWorkerEntity;
import com.example.item.CourierScrollItem;
import com.example.item.SmelterScrollItem;
import com.example.item.WorkerScrollItem;
import com.example.registry.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

public class WorkerInspectMenu extends AbstractContainerMenu {
	public static final int IMAGE_WIDTH = 256;
	public static final int IMAGE_HEIGHT = 240;
	public static final int PORTRAIT_X = 7;
	public static final int PORTRAIT_Y = 18;
	public static final int PORTRAIT_W = 54;
	public static final int PORTRAIT_H = 70;
	public static final int HELD_X = 70;
	public static final int HELD_Y = 40;
	public static final int SCROLL_X = 70;
	public static final int SCROLL_Y = 68;
	public static final int TEXT_X = 8;
	public static final int TEXT_Y = 94;
	public static final int LINE = 10;
	public static final int PLAYER_X = 47;
	public static final int PLAYER_Y = 158;
	public static final int HOTBAR_Y = 216;
	public static final int INVENTORY_LABEL_Y = 146;

	private final Container container;
	private final WorkerInspectData data;
	private final @Nullable HumanWorkerEntity worker;
	private final int heldSlots;

	public WorkerInspectMenu(int containerId, Inventory playerInventory, Container container, WorkerInspectData data, @Nullable HumanWorkerEntity worker) {
		super(ModMenus.WORKER_INSPECT, containerId);
		this.container = container;
		this.data = data;
		this.worker = worker;
		this.heldSlots = data.courier() ? HumanWorkerEntity.CARGO_LIMIT : data.smelter() ? HumanWorkerEntity.SMELT_CARRY_LIMIT : 1;
		checkContainerSize(container, this.heldSlots);
		container.startOpen(playerInventory.player);
		for (int index = 0; index < this.heldSlots; index++) {
			this.addSlot(new HeldSlot(container, index, HELD_X + index * 18, HELD_Y, data.courier(), data.smelter()));
		}
		this.addSlot(new ScrollSlot(worker, playerInventory.player, SCROLL_X, SCROLL_Y));
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				this.addSlot(new Slot(playerInventory, column + row * 9 + 9, PLAYER_X + column * 18, PLAYER_Y + row * 18));
			}
		}
		for (int column = 0; column < 9; column++) {
			this.addSlot(new Slot(playerInventory, column, PLAYER_X + column * 18, HOTBAR_Y));
		}
	}

	public static WorkerInspectMenu open(int containerId, Inventory playerInventory, HumanWorkerEntity worker) {
		Container container = worker.isCourier()
				? new WorkerHeldItems.Cargo(worker)
				: worker.isSmelter() ? new WorkerHeldItems.SmeltCarry(worker) : new WorkerHeldItems.Hand(worker);
		return new WorkerInspectMenu(containerId, playerInventory, container, WorkerInspectData.capture(worker), worker);
	}

	public static WorkerInspectMenu create(int containerId, Inventory playerInventory, WorkerInspectData data) {
		Entity entity = playerInventory.player.level().getEntity(data.entityId());
		HumanWorkerEntity worker = entity instanceof HumanWorkerEntity found ? found : null;
		int slots = data.courier() ? HumanWorkerEntity.CARGO_LIMIT : data.smelter() ? HumanWorkerEntity.SMELT_CARRY_LIMIT : 1;
		return new WorkerInspectMenu(containerId, playerInventory, new SimpleContainer(slots), data, worker);
	}

	public WorkerInspectData data() {
		return this.data;
	}

	public @Nullable HumanWorkerEntity worker() {
		return this.worker;
	}

	public int heldSlots() {
		return this.heldSlots;
	}

	public int scrollSlot() {
		return this.heldSlots;
	}

	@Override
	public boolean stillValid(Player player) {
		return this.worker != null && this.worker.isAlive() && player.distanceToSqr(this.worker) <= 64.0;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		ItemStack original = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		int contentSlots = this.heldSlots + 1;
		if (slot != null && slot.hasItem()) {
			ItemStack stack = slot.getItem();
			original = stack.copy();
			if (index < contentSlots) {
				if (!this.moveItemStackTo(stack, contentSlots, this.slots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else if (isScroll(stack)) {
				if (!this.moveItemStackTo(stack, this.scrollSlot(), this.scrollSlot() + 1, false)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.moveItemStackTo(stack, 0, this.heldSlots, false)) {
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

	private static boolean isScroll(ItemStack stack) {
		return stack.getItem() instanceof WorkerScrollItem
				|| stack.getItem() instanceof CourierScrollItem
				|| stack.getItem() instanceof SmelterScrollItem;
	}

	private static final class HeldSlot extends Slot {
		private final boolean courier;
		private final boolean smelter;

		private HeldSlot(Container container, int index, int x, int y, boolean courier, boolean smelter) {
			super(container, index, x, y);
			this.courier = courier;
			this.smelter = smelter;
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			if (isScroll(stack) || this.smelter) {
				return false;
			}
			if (!this.courier || this.hasItem()) {
				return true;
			}
			for (int earlier = 0; earlier < this.getContainerSlot(); earlier++) {
				if (this.container.getItem(earlier).isEmpty()) {
					return false;
				}
			}
			return this.getContainerSlot() < HumanWorkerEntity.CARGO_LIMIT;
		}

		@Override
		public int getMaxStackSize() {
			return this.courier || this.smelter ? super.getMaxStackSize() : 1;
		}

		@Override
		public int getMaxStackSize(ItemStack stack) {
			return this.courier || this.smelter ? super.getMaxStackSize(stack) : 1;
		}
	}

	private static final class ScrollSlot extends Slot {
		private final @Nullable HumanWorkerEntity worker;
		private final Player player;

		private ScrollSlot(@Nullable HumanWorkerEntity worker, Player player, int x, int y) {
			super(new SimpleContainer(1), 0, x, y);
			this.worker = worker;
			this.player = player;
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return this.worker != null && this.worker.acceptsScroll(stack);
		}

		@Override
		public ItemStack getItem() {
			if (this.worker == null) {
				return ItemStack.EMPTY;
			}
			if (this.worker.level().isClientSide) {
				return super.getItem();
			}
			return this.worker.assignmentScroll();
		}

		@Override
		public void set(ItemStack stack) {
			if (this.worker != null && !this.worker.level().isClientSide) {
				if (!stack.isEmpty()) {
					WorkerAdvancements.onAssigned(this.player, this.worker, stack);
				}
				this.worker.applyScroll(stack);
				return;
			}
			super.set(stack);
		}

		@Override
		public ItemStack remove(int amount) {
			if (this.worker != null && !this.worker.level().isClientSide) {
				if (amount <= 0) {
					return ItemStack.EMPTY;
				}
				ItemStack current = this.worker.assignmentScroll();
				if (current.isEmpty()) {
					return ItemStack.EMPTY;
				}
				this.worker.applyScroll(ItemStack.EMPTY);
				return current;
			}
			return super.remove(amount);
		}

		@Override
		public int getMaxStackSize() {
			return 1;
		}

		@Override
		public int getMaxStackSize(ItemStack stack) {
			return 1;
		}
	}
}
