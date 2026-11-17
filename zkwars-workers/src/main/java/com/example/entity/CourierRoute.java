package com.example.entity;

import com.example.advancement.WorkerActions;
import com.example.advancement.WorkerAdvancements;
import com.example.block.WorkerBlock;
import com.example.block.WorkerKind;
import com.example.block.entity.WorkerBlockEntity;
import com.example.block.work.WorkerSupport;
import com.example.inventory.WorkerSlots;
import com.example.item.CourierCargo;
import com.example.item.CourierSortScrollItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class CourierRoute {
	private CourierRoute() {
	}

	public static void tick(HumanWorkerEntity courier) {
		if (courier.chestPos() == null || courier.route().isEmpty()) {
			courier.getNavigation().stop();
			return;
		}
		if (courier.courierSort()) {
			sort(courier);
			return;
		}
		if (courier.hasCargo()) {
			if (courier.returningCargo() || !courier.courierSupply()) {
				deliverToChest(courier);
			} else {
				deliverToBlock(courier);
			}
			return;
		}
		if (courier.courierSupply()) {
			collectFromChest(courier);
		} else {
			collectFromBlock(courier);
		}
	}

	private static void collectFromBlock(HumanWorkerEntity courier) {
		BlockPos target = current(courier);
		if (!courier.approach(target)) {
			return;
		}
		BlockEntity blockEntity = courier.level().getBlockEntity(target);
		if (!(blockEntity instanceof WorkerBlockEntity worker)) {
			courier.advanceRoute();
			return;
		}
		boolean took = false;
		while (courier.hasCargoRoom()) {
			ItemStack taken = takeExport(worker);
			if (taken.isEmpty()) {
				break;
			}
			courier.addCargo(taken, target, null);
			courier.setRejectMark(-1);
			took = true;
		}
		if (!took) {
			courier.advanceRoute();
		}
	}

	private static void deliverToChest(HumanWorkerEntity courier) {
		BlockPos chestPos = courier.chestPos();
		if (chestPos == null || !courier.approach(chestPos)) {
			return;
		}
		Container chest = chest(courier.level(), chestPos);
		if (chest == null) {
			return;
		}
		boolean exportRun = !courier.courierSupply() && !courier.returningCargo();
		int safety = HumanWorkerEntity.CARGO_LIMIT;
		while (courier.hasCargo() && safety-- > 0) {
			ItemStack stack = courier.cargoStack();
			int before = stack.getCount();
			ItemStack left = insert(chest, stack);
			if (left.isEmpty()) {
				courier.removeCargoHead();
				courier.setCargoStuck(false);
				if (exportRun) {
					WorkerAdvancements.grant(courier, WorkerActions.COURIER_DELIVERED);
				}
				continue;
			}
			if (left.getCount() < before) {
				courier.replaceCargoHead(left);
				courier.setCargoStuck(false);
				if (exportRun) {
					WorkerAdvancements.grant(courier, WorkerActions.COURIER_DELIVERED);
				}
				return;
			}
			if (exportRun && returnToSource(courier, courier.cargoSource(), left)) {
				courier.removeCargoHead();
				courier.setCargoStuck(false);
				continue;
			}
			courier.replaceCargoHead(left);
			courier.setCargoStuck(true);
			return;
		}
		if (!courier.hasCargo()) {
			courier.setReturningCargo(false);
			courier.setRejectMark(-1);
			courier.setCargoStuck(false);
			if (exportRun) {
				courier.advanceRoute();
			}
		}
	}

	private static void collectFromChest(HumanWorkerEntity courier) {
		BlockPos chestPos = courier.chestPos();
		if (chestPos == null || !courier.approach(chestPos)) {
			return;
		}
		Container chest = chest(courier.level(), chestPos);
		if (chest == null) {
			return;
		}
		int stops = courier.route().size();
		int guard = 0;
		while (courier.hasCargoRoom() && guard < stops) {
			BlockPos target = current(courier);
			WorkerKind kind = kindAt(courier, target);
			if (kind != null) {
				ItemStack taken = takeSupply(chest, kind, courier.level(), workerAt(courier, target));
				if (!taken.isEmpty()) {
					courier.setRejectMark(-1);
					courier.addCargo(taken, chestPos, target);
					continue;
				}
			}
			courier.advanceRoute();
			guard++;
		}
	}

	private static void deliverToBlock(HumanWorkerEntity courier) {
		BlockPos target = courier.cargoDest();
		if (target == null) {
			target = current(courier);
		}
		if (!courier.approach(target)) {
			return;
		}
		BlockEntity blockEntity = courier.level().getBlockEntity(target);
		ItemStack hand = courier.cargoStack();
		if (!(blockEntity instanceof WorkerBlockEntity worker) || !acceptsSupply(worker, hand)) {
			courier.setReturningCargo(true);
			return;
		}
		int before = hand.getCount();
		ItemStack left = insertSupply(worker, hand);
		if (courier.courierSupply() && left.getCount() < before) {
			WorkerAdvancements.grant(courier, WorkerActions.SUPPLY_DELIVERED);
		}
		if (left.isEmpty()) {
			courier.removeCargoHead();
			courier.setCargoStuck(false);
			courier.setRejectMark(-1);
			return;
		}
		courier.replaceCargoHead(left);
		if (left.getCount() == before) {
			courier.setReturningCargo(true);
		}
	}

	private static boolean returnToSource(HumanWorkerEntity courier, BlockPos source, ItemStack stack) {
		if (source == null || stack.isEmpty()) {
			return false;
		}
		BlockEntity blockEntity = courier.level().getBlockEntity(source);
		if (!(blockEntity instanceof WorkerBlockEntity worker)) {
			return false;
		}
		if (!WorkerSupport.canFitOutput(worker, List.of(stack))) {
			return false;
		}
		WorkerSupport.insertOutput(worker, List.of(stack));
		return true;
	}

	private static ItemStack takeExport(WorkerBlockEntity block) {
		WorkerKind kind = block.kind();
		for (int slot = WorkerSlots.outputStart(kind); slot < block.getContainerSize(); slot++) {
			ItemStack stack = block.getItem(slot);
			if (stack.isEmpty()) {
				continue;
			}
			if (kind == WorkerKind.CRAFTER || CourierCargo.isExport(kind, stack)) {
				return block.removeItem(slot, stack.getCount());
			}
		}
		return ItemStack.EMPTY;
	}

	private static ItemStack takeSupply(Container chest, WorkerKind kind, Level level, @Nullable WorkerBlockEntity destination) {
		for (int slot = 0; slot < chest.getContainerSize(); slot++) {
			ItemStack stack = chest.getItem(slot);
			if (!wantsSupply(level, kind, stack, destination)) {
				continue;
			}
			int leave = sameItemRemains(chest, slot, stack) ? 0 : 1;
			int available = stack.getCount() - leave;
			if (available <= 0) {
				continue;
			}
			ItemStack taken = chest.removeItem(slot, Math.min(available, stack.getMaxStackSize()));
			chest.setChanged();
			return taken;
		}
		return ItemStack.EMPTY;
	}

	private static boolean sameItemRemains(Container chest, int skip, ItemStack stack) {
		for (int slot = 0; slot < chest.getContainerSize(); slot++) {
			if (slot != skip && ItemStack.isSameItemSameComponents(chest.getItem(slot), stack)) {
				return true;
			}
		}
		return false;
	}

	private static ItemStack insertSupply(WorkerBlockEntity block, ItemStack stack) {
		ItemStack remaining = stack.copy();
		WorkerKind kind = block.kind();
		if (kind == WorkerKind.CRAFTER) {
			if (!WorkerSlots.matchesPattern(block, remaining)) {
				return remaining;
			}
			int start = WorkerSlots.resourceStart(kind);
			int end = start + WorkerSlots.resourceCount(kind);
			for (int slot = start; slot < end && !remaining.isEmpty(); slot++) {
				remaining = merge(block, slot, remaining);
			}
			return remaining;
		}
		if (kind == WorkerKind.SMELTER) {
			boolean fuelOnly = WorkerSlots.isFuel(remaining) && !WorkerSlots.isSmeltable(block.getLevel(), remaining);
			int start = fuelOnly ? WorkerSlots.fuelStart(kind) : 0;
			int end = fuelOnly ? start + WorkerSlots.fuelCount(kind) : WorkerSlots.inputCount(kind);
			for (int slot = start; slot < end && !remaining.isEmpty(); slot++) {
				if (WorkerSlots.mayPlace(kind, slot, remaining, block.getLevel())) {
					remaining = merge(block, slot, remaining);
				}
			}
			return remaining;
		}
		boolean tool = CourierCargo.isTool(remaining);
		for (int slot = 0; slot < block.getContainerSize() && !remaining.isEmpty(); slot++) {
			if (kind.restrictsInput() && tool && WorkerSlots.isInputSlot(kind, slot)) {
				continue;
			}
			if (kind.restrictsInput() && !tool && !WorkerSlots.isInputSlot(kind, slot)) {
				continue;
			}
			if (WorkerSlots.mayPlace(kind, slot, remaining)) {
				remaining = merge(block, slot, remaining);
			}
		}
		return remaining;
	}

	private static boolean wantsSupply(@Nullable Level level, WorkerKind kind, ItemStack stack, @Nullable WorkerBlockEntity destination) {
		if (kind == WorkerKind.CRAFTER) {
			return destination != null && WorkerSlots.matchesPattern(destination, stack);
		}
		return CourierCargo.isSupply(level, kind, stack);
	}

	private static boolean acceptsSupply(WorkerBlockEntity worker, ItemStack stack) {
		return wantsSupply(worker.getLevel(), worker.kind(), stack, worker);
	}

	private static @Nullable WorkerBlockEntity workerAt(HumanWorkerEntity courier, BlockPos pos) {
		if (!courier.level().isLoaded(pos)) {
			return null;
		}
		BlockEntity blockEntity = courier.level().getBlockEntity(pos);
		return blockEntity instanceof WorkerBlockEntity worker ? worker : null;
	}

	private static ItemStack insert(Container container, ItemStack stack) {
		ItemStack remaining = stack.copy();
		for (int slot = 0; slot < container.getContainerSize() && !remaining.isEmpty(); slot++) {
			remaining = merge(container, slot, remaining);
		}
		container.setChanged();
		return remaining;
	}

	private static ItemStack merge(Container container, int slot, ItemStack stack) {
		if (!container.canPlaceItem(slot, stack)) {
			return stack;
		}
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

	private static BlockPos current(HumanWorkerEntity courier) {
		int index = Math.floorMod(courier.routeIndex(), courier.route().size());
		return courier.route().get(index);
	}

	private static WorkerKind kindAt(HumanWorkerEntity courier, BlockPos pos) {
		BlockState state = courier.level().getBlockState(pos);
		if (state.getBlock() instanceof WorkerBlock workerBlock) {
			return workerBlock.kind();
		}
		return null;
	}

	private static void sort(HumanWorkerEntity courier) {
		if (courier.hasCargo()) {
			deliverSort(courier);
			return;
		}
		collectSort(courier);
	}

	private static void collectSort(HumanWorkerEntity courier) {
		BlockPos chestPos = courier.chestPos();
		if (chestPos == null || !courier.approach(chestPos)) {
			return;
		}
		Container chest = container(courier.level(), chestPos);
		if (chest == null) {
			return;
		}
		for (int slot = 0; slot < chest.getContainerSize(); slot++) {
			ItemStack stack = chest.getItem(slot);
			if (stack.isEmpty()) {
				continue;
			}
			SortPick pick = pickDestination(courier, stack);
			if (pick.pos == null) {
				continue;
			}
			ItemStack taken = chest.removeItem(slot, Math.min(stack.getCount(), stack.getMaxStackSize()));
			chest.setChanged();
			if (taken.isEmpty()) {
				return;
			}
			courier.addCargo(taken, chestPos, pick.pos);
			courier.setCargoStuck(false);
			courier.setRejectMark(-1);
			return;
		}
	}

	private static void deliverSort(HumanWorkerEntity courier) {
		ItemStack hand = courier.cargoStack();
		if (hand.isEmpty()) {
			courier.removeCargoHead();
			return;
		}
		SortPick pick = pickDestination(courier, hand);
		if (pick.pos == null) {
			if (!pick.unloaded) {
				returnSortToSource(courier);
			}
			return;
		}
		if (!courier.approach(pick.pos)) {
			return;
		}
		Container box = container(courier.level(), pick.pos);
		if (box == null || bin(box, hand) == Bin.REJECT) {
			return;
		}
		int before = hand.getCount();
		ItemStack left = insert(box, hand);
		if (left.getCount() < before) {
			courier.setCargoStuck(false);
			WorkerAdvancements.grant(courier, WorkerActions.SORT_DELIVERED);
		}
		if (left.isEmpty()) {
			courier.removeCargoHead();
			return;
		}
		courier.replaceCargoHead(left);
		if (left.getCount() == before) {
			courier.setCargoStuck(true);
		}
	}

	private static void returnSortToSource(HumanWorkerEntity courier) {
		BlockPos chestPos = courier.chestPos();
		if (chestPos == null || !courier.approach(chestPos)) {
			return;
		}
		Container chest = container(courier.level(), chestPos);
		ItemStack hand = courier.cargoStack();
		if (chest == null || hand.isEmpty()) {
			courier.setCargoStuck(true);
			return;
		}
		int before = hand.getCount();
		ItemStack left = insert(chest, hand);
		if (left.isEmpty()) {
			courier.removeCargoHead();
			courier.setCargoStuck(false);
			return;
		}
		courier.replaceCargoHead(left);
		courier.setCargoStuck(left.getCount() == before);
	}

	private static SortPick pickDestination(HumanWorkerEntity courier, ItemStack stack) {
		Level level = courier.level();
		BlockPos match = null;
		BlockPos empty = null;
		boolean unloaded = false;
		for (BlockPos pos : courier.route()) {
			if (isSortSource(level, courier.chestPos(), pos)) {
				continue;
			}
			if (!level.isLoaded(pos)) {
				unloaded = true;
				continue;
			}
			Container box = container(level, pos);
			if (box == null) {
				continue;
			}
			Bin kind = bin(box, stack);
			if (kind == Bin.MATCH && match == null) {
				match = pos;
			} else if (kind == Bin.EMPTY && empty == null) {
				empty = pos;
			}
		}
		if (match != null) {
			return new SortPick(match, false);
		}
		if (empty != null) {
			return new SortPick(empty, false);
		}
		return new SortPick(null, unloaded);
	}

	private static Bin bin(Container container, ItemStack stack) {
		boolean anyItem = false;
		boolean other = false;
		boolean room = false;
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			ItemStack current = container.getItem(slot);
			if (!current.isEmpty()) {
				anyItem = true;
				if (!ItemStack.isSameItemSameComponents(current, stack)) {
					other = true;
					continue;
				}
			}
			if (!container.canPlaceItem(slot, stack)) {
				continue;
			}
			if (current.isEmpty()) {
				room = true;
				continue;
			}
			int space = Math.min(current.getMaxStackSize(), container.getMaxStackSize()) - current.getCount();
			if (space > 0) {
				room = true;
			}
		}
		if (other) {
			return Bin.REJECT;
		}
		if (!anyItem) {
			return room ? Bin.EMPTY : Bin.REJECT;
		}
		return room ? Bin.MATCH : Bin.REJECT;
	}

	private static boolean isSortSource(Level level, @Nullable BlockPos source, BlockPos pos) {
		if (source == null) {
			return false;
		}
		if (source.equals(pos)) {
			return true;
		}
		if (!level.isLoaded(source)) {
			return false;
		}
		BlockPos twin = connectedChest(level, source);
		return twin != null && twin.equals(pos);
	}

	private static @Nullable BlockPos connectedChest(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof ChestBlock) || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
			return null;
		}
		return pos.relative(ChestBlock.getConnectedDirection(state));
	}

	private static Container container(Level level, BlockPos pos) {
		if (!level.isLoaded(pos)) {
			return null;
		}
		BlockState state = level.getBlockState(pos);
		if (!CourierSortScrollItem.isStorage(state)) {
			return null;
		}
		if (state.getBlock() instanceof ChestBlock chestBlock) {
			return ChestBlock.getContainer(chestBlock, state, level, pos, false);
		}
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity instanceof Container found) {
			return found;
		}
		return null;
	}

	private static Container chest(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof ChestBlock chestBlock)) {
			return null;
		}
		return ChestBlock.getContainer(chestBlock, state, level, pos, false);
	}

	private enum Bin {
		REJECT, EMPTY, MATCH
	}

	private static final class SortPick {
		private final @Nullable BlockPos pos;
		private final boolean unloaded;

		private SortPick(@Nullable BlockPos pos, boolean unloaded) {
			this.pos = pos;
			this.unloaded = unloaded;
		}
	}
}
