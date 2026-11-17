package com.example.block.work;

import com.example.advancement.WorkerActions;
import com.example.advancement.WorkerAdvancements;
import com.example.block.WorkerKind;
import com.example.block.entity.WorkerBlockEntity;
import com.example.entity.HumanWorkerEntity;
import com.example.inventory.WorkerSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class SmeltWork {
	public static final int PHASE_NONE = 0;
	public static final int PHASE_EXTRACT = 1;
	public static final int PHASE_WITHDRAW = 2;
	public static final int PHASE_INSERT = 3;
	public static final int PHASE_DEPOSIT = 4;
	public static final String KIND_INPUT = "input";
	public static final String KIND_FUEL = "fuel";
	public static final String KIND_RESULT = "result";

	private static final int SLOT_INPUT = 0;
	private static final int SLOT_FUEL = 1;
	private static final int SLOT_RESULT = 2;

	private SmeltWork() {
	}

	public static void tick(HumanWorkerEntity worker) {
		BlockPos home = worker.getWorkBlockPos();
		if (home == null) {
			worker.getNavigation().stop();
			return;
		}
		int phase = worker.smeltPhase();
		if (phase == PHASE_DEPOSIT || (phase == PHASE_NONE && worker.smeltCarrySize() > 0)) {
			deposit(worker, home);
			return;
		}
		if (phase == PHASE_EXTRACT) {
			extract(worker);
			return;
		}
		if (phase == PHASE_WITHDRAW) {
			withdraw(worker, home);
			return;
		}
		if (phase == PHASE_INSERT) {
			insert(worker, home);
			return;
		}
		pickJob(worker, home);
	}

	private static void pickJob(HumanWorkerEntity worker, BlockPos home) {
		List<BlockPos> furnaces = worker.furnaces();
		WorkerBlockEntity block = workBlock(worker, home);
		if (furnaces.isEmpty() || block == null) {
			worker.approach(home);
			return;
		}
		int count = furnaces.size();
		int start = Math.floorMod(worker.furnaceIndex(), count);
		for (int offset = 0; offset < count; offset++) {
			int index = (start + offset) % count;
			BlockPos pos = furnaces.get(index);
			FurnaceBlockEntity furnace = furnace(worker.level(), pos);
			if (furnace == null) {
				continue;
			}
			ItemStack result = furnace.getItem(SLOT_RESULT);
			if (resultRoom(block, result) > 0) {
				begin(worker, index, pos, PHASE_EXTRACT);
				return;
			}
			if (needsSupply(furnace, block)) {
				begin(worker, index, pos, PHASE_WITHDRAW);
				return;
			}
		}
		worker.approach(home);
	}

	private static void begin(HumanWorkerEntity worker, int index, BlockPos pos, int phase) {
		worker.setFurnaceIndex(index);
		worker.setSmeltTarget(pos);
		worker.setSmeltPhase(phase);
	}

	private static void extract(HumanWorkerEntity worker) {
		BlockPos target = worker.smeltTarget();
		if (target == null || !worker.level().isLoaded(target)) {
			finish(worker);
			return;
		}
		if (!worker.approach(target)) {
			if (worker.cantReach()) {
				finish(worker);
			}
			return;
		}
		FurnaceBlockEntity furnace = furnace(worker.level(), target);
		if (furnace == null) {
			finish(worker);
			return;
		}
		WorkerBlockEntity block = worker.getWorkBlockPos() == null ? null : workBlock(worker, worker.getWorkBlockPos());
		ItemStack result = furnace.getItem(SLOT_RESULT);
		int room = block == null ? 0 : resultRoom(block, result);
		if (room > 0 && worker.smeltCarryRoom()) {
			worker.addSmeltCarry(furnace.removeItem(SLOT_RESULT, room), KIND_RESULT);
		}
		worker.setSmeltPhase(PHASE_DEPOSIT);
	}

	private static void withdraw(HumanWorkerEntity worker, BlockPos home) {
		if (!worker.level().isLoaded(home)) {
			return;
		}
		if (!worker.approach(home)) {
			return;
		}
		WorkerBlockEntity block = workBlock(worker, home);
		FurnaceBlockEntity furnace = worker.smeltTarget() == null ? null : furnace(worker.level(), worker.smeltTarget());
		if (block == null || furnace == null) {
			finish(worker);
			return;
		}
		ItemStack input = furnace.getItem(SLOT_INPUT);
		boolean tookInput = false;
		if (input.isEmpty() || input.getCount() < input.getMaxStackSize()) {
			int room = input.isEmpty() ? 64 : input.getMaxStackSize() - input.getCount();
			ItemStack taken = take(block, 0, WorkerSlots.inputCount(WorkerKind.SMELTER), input, room);
			if (!taken.isEmpty()) {
				worker.addSmeltCarry(taken, KIND_INPUT);
				tookInput = true;
			}
		}
		boolean canBurn = !input.isEmpty() || tookInput;
		ItemStack fuel = furnace.getItem(SLOT_FUEL);
		if (canBurn && worker.smeltCarryRoom() && (fuel.isEmpty() || fuel.getCount() < fuel.getMaxStackSize())) {
			int room = fuel.isEmpty() ? 64 : fuel.getMaxStackSize() - fuel.getCount();
			int start = WorkerSlots.fuelStart(WorkerKind.SMELTER);
			ItemStack taken = take(block, start, start + WorkerSlots.fuelCount(WorkerKind.SMELTER), fuel, room);
			if (!taken.isEmpty()) {
				worker.addSmeltCarry(taken, KIND_FUEL);
			}
		}
		if (worker.smeltCarrySize() == 0) {
			finish(worker);
			return;
		}
		worker.setSmeltPhase(PHASE_INSERT);
	}

	private static void insert(HumanWorkerEntity worker, BlockPos home) {
		BlockPos target = worker.smeltTarget();
		if (target == null || !worker.level().isLoaded(target)) {
			worker.setSmeltPhase(PHASE_DEPOSIT);
			return;
		}
		if (!worker.approach(target)) {
			if (worker.cantReach()) {
				worker.setSmeltPhase(PHASE_DEPOSIT);
			}
			return;
		}
		FurnaceBlockEntity furnace = furnace(worker.level(), target);
		if (furnace == null) {
			worker.setSmeltPhase(PHASE_DEPOSIT);
			return;
		}
		for (int index = 0; index < worker.smeltCarrySize(); index++) {
			String kind = worker.smeltCarryKind(index);
			int slot = KIND_FUEL.equals(kind) ? SLOT_FUEL : SLOT_INPUT;
			if (!KIND_INPUT.equals(kind) && !KIND_FUEL.equals(kind)) {
				continue;
			}
			ItemStack left = insertSlot(furnace, slot, worker.smeltCarryStack(index));
			worker.setSmeltCarryStack(index, left);
		}
		worker.compactSmeltCarry();
		worker.setSmeltPhase(worker.smeltCarrySize() == 0 ? PHASE_NONE : PHASE_DEPOSIT);
		if (worker.smeltPhase() == PHASE_NONE) {
			finish(worker);
		}
	}

	private static void deposit(HumanWorkerEntity worker, BlockPos home) {
		if (!worker.level().isLoaded(home)) {
			return;
		}
		if (!worker.approach(home)) {
			return;
		}
		WorkerBlockEntity block = workBlock(worker, home);
		if (block == null) {
			worker.dropSmeltCarry();
			worker.setSmeltPhase(PHASE_NONE);
			worker.setSmeltTarget(null);
			return;
		}
		List<ItemStack> kept = new ArrayList<>();
		List<String> kinds = new ArrayList<>();
		int count = worker.smeltCarrySize();
		for (int index = 0; index < count; index++) {
			String kind = worker.smeltCarryKind(index);
			ItemStack stack = worker.smeltCarryStack(index).copy();
			int start;
			int end;
			if (KIND_RESULT.equals(kind)) {
				start = WorkerSlots.outputStart(WorkerKind.SMELTER);
				end = block.getContainerSize();
			} else if (KIND_FUEL.equals(kind)) {
				start = WorkerSlots.fuelStart(WorkerKind.SMELTER);
				end = start + WorkerSlots.fuelCount(WorkerKind.SMELTER);
			} else {
				start = 0;
				end = WorkerSlots.inputCount(WorkerKind.SMELTER);
			}
			int before = stack.getCount();
			ItemStack left = insertRange(block, start, end, stack);
			if (KIND_RESULT.equals(kind) && before > 0 && left.getCount() < before) {
				WorkerAdvancements.grant(worker, WorkerActions.SMELT_RETURNED);
			}
			if (left.isEmpty()) {
				continue;
			}
			if (KIND_RESULT.equals(kind)) {
				FurnaceBlockEntity furnace = worker.smeltTarget() == null ? null : furnace(worker.level(), worker.smeltTarget());
				if (furnace != null) {
					left = merge(furnace, SLOT_RESULT, left);
				}
				if (!left.isEmpty()) {
					kept.add(left);
					kinds.add(kind);
				}
			} else {
				worker.spawnAtLocation(left);
			}
		}
		worker.replaceSmeltCarry(kept, kinds);
		if (worker.smeltCarrySize() == 0) {
			finish(worker);
		}
	}

	private static void finish(HumanWorkerEntity worker) {
		int count = worker.furnaces().size();
		if (count > 0) {
			worker.setFurnaceIndex(Math.floorMod(worker.furnaceIndex() + 1, count));
		}
		worker.setSmeltPhase(PHASE_NONE);
		worker.setSmeltTarget(null);
	}

	private static int resultRoom(WorkerBlockEntity block, ItemStack result) {
		if (result.isEmpty()) {
			return 0;
		}
		ItemStack one = result.copy();
		one.setCount(1);
		return WorkerSupport.fittingCycles(block, one, result.getCount());
	}

	private static boolean needsSupply(FurnaceBlockEntity furnace, WorkerBlockEntity block) {
		ItemStack input = furnace.getItem(SLOT_INPUT);
		boolean inputRoom = input.isEmpty() || input.getCount() < input.getMaxStackSize();
		boolean canInput = inputRoom && hasSection(block, 0, WorkerSlots.inputCount(WorkerKind.SMELTER), input);
		ItemStack fuel = furnace.getItem(SLOT_FUEL);
		boolean fuelRoom = fuel.isEmpty() || fuel.getCount() < fuel.getMaxStackSize();
		boolean willHaveInput = !input.isEmpty() || canInput;
		int fuelStart = WorkerSlots.fuelStart(WorkerKind.SMELTER);
		boolean canFuel = willHaveInput && fuelRoom && hasSection(block, fuelStart, fuelStart + WorkerSlots.fuelCount(WorkerKind.SMELTER), fuel);
		return canInput || canFuel;
	}

	private static boolean hasSection(WorkerBlockEntity block, int start, int end, ItemStack existing) {
		for (int slot = start; slot < end; slot++) {
			ItemStack stack = block.getItem(slot);
			if (stack.isEmpty()) {
				continue;
			}
			if (existing.isEmpty() || ItemStack.isSameItemSameComponents(stack, existing)) {
				return true;
			}
		}
		return false;
	}

	private static ItemStack take(WorkerBlockEntity block, int start, int end, ItemStack existing, int room) {
		if (room <= 0) {
			return ItemStack.EMPTY;
		}
		for (int slot = start; slot < end; slot++) {
			ItemStack stack = block.getItem(slot);
			if (stack.isEmpty()) {
				continue;
			}
			if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(stack, existing)) {
				continue;
			}
			int amount = Math.min(room, stack.getCount());
			if (amount <= 0) {
				continue;
			}
			return block.removeItem(slot, amount);
		}
		return ItemStack.EMPTY;
	}

	private static ItemStack insertRange(WorkerBlockEntity block, int start, int end, ItemStack stack) {
		ItemStack remaining = stack.copy();
		for (int slot = start; slot < end && !remaining.isEmpty(); slot++) {
			remaining = merge(block, slot, remaining);
		}
		return remaining;
	}

	private static ItemStack insertSlot(FurnaceBlockEntity furnace, int slot, ItemStack stack) {
		if (stack.isEmpty() || !furnace.canPlaceItem(slot, stack)) {
			return stack;
		}
		return merge(furnace, slot, stack.copy());
	}

	private static ItemStack merge(net.minecraft.world.Container container, int slot, ItemStack stack) {
		ItemStack current = container.getItem(slot);
		if (current.isEmpty()) {
			int move = Math.min(stack.getCount(), Math.min(stack.getMaxStackSize(), container.getMaxStackSize()));
			ItemStack placed = stack.copy();
			placed.setCount(move);
			container.setItem(slot, placed);
			stack.shrink(move);
			return stack;
		}
		if (!ItemStack.isSameItemSameComponents(current, stack)) {
			return stack;
		}
		int space = Math.min(current.getMaxStackSize(), container.getMaxStackSize()) - current.getCount();
		int move = Math.min(space, stack.getCount());
		if (move <= 0) {
			return stack;
		}
		current.grow(move);
		container.setItem(slot, current);
		stack.shrink(move);
		return stack;
	}

	private static @Nullable WorkerBlockEntity workBlock(HumanWorkerEntity worker, BlockPos pos) {
		if (!worker.level().isLoaded(pos)) {
			return null;
		}
		BlockEntity blockEntity = worker.level().getBlockEntity(pos);
		return blockEntity instanceof WorkerBlockEntity block && block.kind() == WorkerKind.SMELTER ? block : null;
	}

	private static @Nullable FurnaceBlockEntity furnace(Level level, BlockPos pos) {
		if (!level.isLoaded(pos) || !(level.getBlockState(pos).getBlock() instanceof FurnaceBlock)) {
			return null;
		}
		BlockEntity blockEntity = level.getBlockEntity(pos);
		return blockEntity instanceof FurnaceBlockEntity furnace ? furnace : null;
	}
}
