package com.example.block.work;

import com.example.advancement.WorkerActions;
import com.example.advancement.WorkerAdvancements;
import com.example.block.entity.WorkerBlockEntity;
import com.example.entity.HumanWorkerEntity;
import com.example.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;

public final class BuildWork {
	public static final int PLACE_COOLDOWN = 20;
	private static final int SKIP_BUDGET = 512;

	private BuildWork() {
	}

	public static void tick(ServerLevel level, BlockPos origin, WorkerBlockEntity block) {
		HumanWorkerEntity builder = WorkerSupport.findWorker(level, origin, ModEntities.HUMAN_BUILDER, true);
		if (builder == null) {
			return;
		}
		if (block.builderCooldown() > 0) {
			block.tickBuilderCooldown();
			return;
		}
		BuilderTasks tasks = BuilderTasks.get(level);
		BuilderTasks.Task task = tasks.task(origin);
		if (task == null || task.size() == 0) {
			return;
		}
		int size = task.size();
		int index = Math.floorMod(task.cursor(), size);
		boolean missing = false;
		int checks = Math.min(SKIP_BUDGET, size);
		for (int n = 0; n < checks; n++) {
			Step step = tryPlace(level, origin, block, builder, task, index);
			index = (index + 1) % size;
			if (step == Step.PLACED) {
				task.notePlaced();
				task.setCursor(index);
				task.setWaiting(false);
				tasks.changed();
				block.setBuilderCooldown(PLACE_COOLDOWN);
				WorkerAdvancements.grant(builder, WorkerActions.BUILDER_PLACED);
				return;
			}
			if (step == Step.PRESENT) {
				task.notePresent();
			} else {
				task.noteBreak();
				if (step == Step.MISSING) {
					missing = true;
				}
			}
			if (task.finished()) {
				WorkerAdvancements.grant(builder, WorkerActions.BUILDER_DONE);
			}
		}
		task.setCursor(index);
		task.setWaiting(missing);
		tasks.changed();
	}

	private static Step tryPlace(ServerLevel level, BlockPos origin, WorkerBlockEntity block, HumanWorkerEntity builder, BuilderTasks.Task task, int index) {
		BlockState target = task.state(level, index);
		if (target == null) {
			return Step.SKIP;
		}
		BlockPos pos = task.pos(origin, index);
		BlockPos otherPos = pairPos(target, pos);
		BlockState otherTarget = otherPos == null ? null : pairState(target);
		if (pos.equals(origin) || (otherPos != null && otherPos.equals(origin))) {
			return Step.PRESENT;
		}
		if (matches(level, pos, target) && (otherPos == null || matches(level, otherPos, otherTarget))) {
			return Step.PRESENT;
		}
		if (!room(level, pos, target) || (otherPos != null && !room(level, otherPos, otherTarget))) {
			return Step.SKIP;
		}
		Item item = target.getBlock().asItem();
		int need = needsTwo(target) ? 2 : 1;
		if (!has(block, item, need)) {
			return Step.MISSING;
		}
		place(level, pos, target, otherPos, otherTarget);
		take(block, item, need);
		builder.wearEquippedTool(block);
		builder.playWorkSwing(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
		level.levelEvent(2001, pos, Block.getId(target));
		return Step.PLACED;
	}

	private static void place(ServerLevel level, BlockPos pos, BlockState target, BlockPos otherPos, BlockState otherTarget) {
		if (otherPos == null) {
			level.setBlock(pos, target, Block.UPDATE_ALL);
			return;
		}
		level.setBlock(pos, target, Block.UPDATE_CLIENTS);
		level.setBlock(otherPos, otherTarget, Block.UPDATE_CLIENTS);
		level.updateNeighborsAt(pos, target.getBlock());
		level.updateNeighborsAt(otherPos, otherTarget.getBlock());
	}

	private static boolean matches(ServerLevel level, BlockPos pos, BlockState target) {
		return level.getBlockState(pos).equals(target);
	}

	private static boolean room(ServerLevel level, BlockPos pos, BlockState target) {
		BlockState now = level.getBlockState(pos);
		return now.equals(target) || now.isAir() || now.canBeReplaced();
	}

	private static boolean needsTwo(BlockState state) {
		return state.getBlock() instanceof SlabBlock && state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE;
	}

	private static BlockPos pairPos(BlockState state, BlockPos pos) {
		if (state.getBlock() instanceof DoorBlock) {
			return state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
		}
		if (state.getBlock() instanceof DoublePlantBlock) {
			return state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
		}
		if (state.getBlock() instanceof BedBlock) {
			Direction facing = state.getValue(BedBlock.FACING);
			return state.getValue(BedBlock.PART) == BedPart.FOOT ? pos.relative(facing) : pos.relative(facing.getOpposite());
		}
		return null;
	}

	private static BlockState pairState(BlockState state) {
		if (state.getBlock() instanceof DoorBlock) {
			DoubleBlockHalf half = state.getValue(DoorBlock.HALF);
			return state.setValue(DoorBlock.HALF, half == DoubleBlockHalf.LOWER ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER);
		}
		if (state.getBlock() instanceof DoublePlantBlock) {
			DoubleBlockHalf half = state.getValue(DoublePlantBlock.HALF);
			return state.setValue(DoublePlantBlock.HALF, half == DoubleBlockHalf.LOWER ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER);
		}
		if (state.getBlock() instanceof BedBlock) {
			BedPart part = state.getValue(BedBlock.PART);
			return state.setValue(BedBlock.PART, part == BedPart.FOOT ? BedPart.HEAD : BedPart.FOOT);
		}
		return state;
	}

	private static boolean has(WorkerBlockEntity block, Item item, int count) {
		int found = 0;
		for (int slot = 0; slot < block.getContainerSize(); slot++) {
			ItemStack stack = block.getItem(slot);
			if (stack.is(item)) {
				found += stack.getCount();
				if (found >= count) {
					return true;
				}
			}
		}
		return false;
	}

	private static void take(WorkerBlockEntity block, Item item, int count) {
		int left = count;
		for (int slot = 0; slot < block.getContainerSize() && left > 0; slot++) {
			ItemStack stack = block.getItem(slot);
			if (!stack.is(item)) {
				continue;
			}
			int remove = Math.min(left, stack.getCount());
			block.removeItem(slot, remove);
			left -= remove;
		}
	}

	private enum Step {
		PLACED,
		PRESENT,
		MISSING,
		SKIP
	}
}
