package com.example.inventory;

import com.example.entity.HumanWorkerEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

abstract class WorkerHeldItems implements Container {
	protected final HumanWorkerEntity worker;

	WorkerHeldItems(HumanWorkerEntity worker) {
		this.worker = worker;
	}

	@Override
	public boolean stillValid(Player player) {
		return this.worker.isAlive() && player.distanceToSqr(this.worker) <= 64.0;
	}

	@Override
	public boolean isEmpty() {
		for (int slot = 0; slot < this.getContainerSize(); slot++) {
			if (!this.getItem(slot).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void setChanged() {
	}

	@Override
	public void clearContent() {
		for (int slot = 0; slot < this.getContainerSize(); slot++) {
			this.setItem(slot, ItemStack.EMPTY);
		}
	}

	static final class Hand extends WorkerHeldItems {
		Hand(HumanWorkerEntity worker) {
			super(worker);
		}

		@Override
		public int getContainerSize() {
			return 1;
		}

		@Override
		public int getMaxStackSize() {
			return 1;
		}

		@Override
		public ItemStack getItem(int slot) {
			return slot == 0 ? this.worker.getMainHandItem() : ItemStack.EMPTY;
		}

		@Override
		public void setItem(int slot, ItemStack stack) {
			if (slot != 0) {
				return;
			}
			if (stack.isEmpty()) {
				this.worker.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
				return;
			}
			if (stack.getCount() > 1) {
				stack.setCount(1);
			}
			this.worker.setItemSlot(EquipmentSlot.MAINHAND, stack);
		}

		@Override
		public ItemStack removeItem(int slot, int amount) {
			if (slot != 0 || amount <= 0) {
				return ItemStack.EMPTY;
			}
			ItemStack stack = this.worker.getMainHandItem();
			if (stack.isEmpty()) {
				return ItemStack.EMPTY;
			}
			ItemStack taken = stack.split(Math.min(amount, stack.getCount()));
			if (stack.isEmpty()) {
				this.worker.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			}
			return taken;
		}

		@Override
		public ItemStack removeItemNoUpdate(int slot) {
			if (slot != 0) {
				return ItemStack.EMPTY;
			}
			ItemStack stack = this.worker.getMainHandItem();
			this.worker.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			return stack;
		}
	}

	static final class Cargo extends WorkerHeldItems {
		Cargo(HumanWorkerEntity worker) {
			super(worker);
		}

		@Override
		public int getContainerSize() {
			return HumanWorkerEntity.CARGO_LIMIT;
		}

		@Override
		public ItemStack getItem(int slot) {
			return this.worker.cargoPiece(slot);
		}

		@Override
		public void setItem(int slot, ItemStack stack) {
			this.worker.setCargoPiece(slot, stack);
		}

		@Override
		public ItemStack removeItem(int slot, int amount) {
			if (amount <= 0) {
				return ItemStack.EMPTY;
			}
			ItemStack stack = this.worker.cargoPiece(slot);
			if (stack.isEmpty()) {
				return ItemStack.EMPTY;
			}
			ItemStack taken = stack.split(Math.min(amount, stack.getCount()));
			if (stack.isEmpty()) {
				this.worker.removeCargoPiece(slot);
			} else {
				this.worker.refreshCargoHand();
			}
			return taken;
		}

		@Override
		public ItemStack removeItemNoUpdate(int slot) {
			return this.worker.removeCargoPiece(slot);
		}

		@Override
		public void setChanged() {
			this.worker.refreshCargoHand();
		}
	}

	static final class SmeltCarry extends WorkerHeldItems {
		SmeltCarry(HumanWorkerEntity worker) {
			super(worker);
		}

		@Override
		public int getContainerSize() {
			return HumanWorkerEntity.SMELT_CARRY_LIMIT;
		}

		@Override
		public ItemStack getItem(int slot) {
			return this.worker.smeltCarryStack(slot);
		}

		@Override
		public void setItem(int slot, ItemStack stack) {
			this.worker.setSmeltCarryStack(slot, stack);
			this.worker.compactSmeltCarry();
		}

		@Override
		public ItemStack removeItem(int slot, int amount) {
			if (amount <= 0) {
				return ItemStack.EMPTY;
			}
			ItemStack stack = this.worker.smeltCarryStack(slot);
			if (stack.isEmpty()) {
				return ItemStack.EMPTY;
			}
			ItemStack taken = stack.split(Math.min(amount, stack.getCount()));
			if (stack.isEmpty()) {
				this.worker.setSmeltCarryStack(slot, ItemStack.EMPTY);
			}
			this.worker.compactSmeltCarry();
			return taken;
		}

		@Override
		public ItemStack removeItemNoUpdate(int slot) {
			ItemStack stack = this.worker.smeltCarryStack(slot).copy();
			this.worker.setSmeltCarryStack(slot, ItemStack.EMPTY);
			this.worker.compactSmeltCarry();
			return stack;
		}

		@Override
		public void setChanged() {
			this.worker.compactSmeltCarry();
		}
	}
}
