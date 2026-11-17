package com.example.block.work;

import com.example.block.entity.WorkerBlockEntity;
import com.example.entity.HumanWorkerEntity;
import com.example.inventory.WorkerSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public final class WorkerSupport {
	private WorkerSupport() {
	}

	public static HumanWorkerEntity findAssigned(ServerLevel level, BlockPos pos, EntityType<HumanWorkerEntity> type) {
		AABB box = new AABB(pos).inflate(96.0);
		for (HumanWorkerEntity worker : level.getEntitiesOfClass(HumanWorkerEntity.class, box, entity -> entity.getType() == type && pos.equals(entity.getWorkBlockPos()))) {
			return worker;
		}
		return null;
	}

	public static int fittingCycles(WorkerBlockEntity block, ItemStack cycle, int maxCycles) {
		if (cycle.isEmpty() || maxCycles <= 0) {
			return 0;
		}
		List<ItemStack> slots = copyOutput(block);
		int fit = 0;
		for (int i = 0; i < maxCycles; i++) {
			if (!place(slots, cycle.copy())) {
				break;
			}
			fit++;
		}
		return fit;
	}

	public static HumanWorkerEntity readyWorker(ServerLevel level, BlockPos pos, EntityType<HumanWorkerEntity> type, WorkerBlockEntity block) {
		HumanWorkerEntity worker = findWorker(level, pos, type, false);
		if (worker == null) {
			return null;
		}
		worker.refillTool(block);
		return worker.hasIntactTool() ? worker : null;
	}

	public static HumanWorkerEntity findWorker(ServerLevel level, BlockPos pos, EntityType<HumanWorkerEntity> type, boolean requireTool) {
		AABB box = new AABB(pos).inflate(4.0);
		for (HumanWorkerEntity worker : level.getEntitiesOfClass(HumanWorkerEntity.class, box)) {
			if (worker.getType() != type || !pos.equals(worker.getWorkBlockPos())) {
				continue;
			}
			if (requireTool ? worker.isAtWorkPost() : worker.isStationed()) {
				return worker;
			}
		}
		return null;
	}

	public static boolean canFitOutput(WorkerBlockEntity block, List<ItemStack> drops) {
		List<ItemStack> slots = copyOutput(block);
		for (ItemStack drop : drops) {
			if (!drop.isEmpty() && !place(slots, drop.copy())) {
				return false;
			}
		}
		return true;
	}

	public static void insertOutput(WorkerBlockEntity block, List<ItemStack> drops) {
		int start = WorkerSlots.outputStart(block.kind());
		int end = block.getContainerSize();
		for (ItemStack drop : drops) {
			ItemStack remaining = drop.copy();
			for (int slot = start; slot < end && !remaining.isEmpty(); slot++) {
				remaining = mergeSlot(block, slot, remaining);
			}
		}
	}

	private static List<ItemStack> copyOutput(WorkerBlockEntity block) {
		List<ItemStack> slots = new ArrayList<>();
		int start = WorkerSlots.outputStart(block.kind());
		for (int slot = start; slot < block.getContainerSize(); slot++) {
			slots.add(block.getItem(slot).copy());
		}
		return slots;
	}

	private static boolean place(List<ItemStack> slots, ItemStack stack) {
		for (int index = 0; index < slots.size() && !stack.isEmpty(); index++) {
			ItemStack slot = slots.get(index);
			if (slot.isEmpty()) {
				int move = Math.min(stack.getCount(), stack.getMaxStackSize());
				ItemStack placed = stack.copy();
				placed.setCount(move);
				slots.set(index, placed);
				stack.shrink(move);
			} else if (ItemStack.isSameItemSameComponents(slot, stack)) {
				int space = slot.getMaxStackSize() - slot.getCount();
				int move = Math.min(space, stack.getCount());
				if (move > 0) {
					slot.grow(move);
					stack.shrink(move);
				}
			}
		}
		return stack.isEmpty();
	}

	private static ItemStack mergeSlot(WorkerBlockEntity block, int slot, ItemStack stack) {
		ItemStack current = block.getItem(slot);
		if (current.isEmpty()) {
			int move = Math.min(stack.getCount(), stack.getMaxStackSize());
			ItemStack placed = stack.copy();
			placed.setCount(move);
			block.setItem(slot, placed);
			stack.shrink(move);
			return stack;
		}
		if (!ItemStack.isSameItemSameComponents(current, stack)) {
			return stack;
		}
		int space = current.getMaxStackSize() - current.getCount();
		int move = Math.min(space, stack.getCount());
		if (move <= 0) {
			return stack;
		}
		current.grow(move);
		block.setItem(slot, current);
		stack.shrink(move);
		return stack;
	}
}
