package com.example.block.work;

import com.example.advancement.WorkerActions;
import com.example.advancement.WorkerAdvancements;
import com.example.block.entity.WorkerBlockEntity;
import com.example.entity.HumanWorkerEntity;
import com.example.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public final class QuarryWork {
	public static final int BREAK_COOLDOWN = 20;
	private static final int SKIP_BUDGET = 512;

	private QuarryWork() {
	}

	public static void tick(ServerLevel level, BlockPos origin, WorkerBlockEntity block) {
		HumanWorkerEntity miner = WorkerSupport.readyWorker(level, origin, ModEntities.HUMAN_MINER, block);
		if (miner == null) {
			return;
		}
		if (block.quarryBreakCooldown() > 0) {
			block.tickQuarryBreakCooldown();
		}
		QuarryBounds.Box box = block.quarryBounds();
		int top = origin.getY() + box.maxY();
		int bottom = origin.getY() + box.minY();
		int layer = box.layerSize();
		int y = block.quarryY();
		int index = block.quarryIndex();
		if (y > top || y < bottom || index < 0 || index >= layer) {
			y = top;
			index = 0;
		}
		if (y == top && index == 0) {
			block.setQuarryClearSweep(true);
		}
		boolean canBreak = block.quarryBreakCooldown() <= 0;
		for (int checks = 0; y >= bottom && checks < SKIP_BUDGET; checks++) {
			BlockPos target = cell(origin, box, y, index);
			BlockState state = level.getBlockState(target);
			if (target.equals(origin) || skip(level, target, state) || besideFluid(level, target)) {
				int[] next = advance(box, y, index);
				y = next[0];
				index = next[1];
				continue;
			}
			if (!canBreak) {
				block.setQuarryCursor(y, index, false);
				return;
			}
			List<ItemStack> drops = Block.getDrops(state, level, target, level.getBlockEntity(target), miner, miner.getMainHandItem());
			if (!WorkerSupport.canFitOutput(block, drops)) {
				block.setQuarryCursor(y, index, false);
				return;
			}
			level.levelEvent(2001, target, Block.getId(state));
			if (!level.destroyBlock(target, false, miner)) {
				int[] next = advance(box, y, index);
				store(block, miner, top, bottom, next[0], next[1]);
				return;
			}
			WorkerSupport.insertOutput(block, drops);
			if (drops.stream().anyMatch(stack -> !stack.isEmpty())) {
				WorkerAdvancements.grant(miner, WorkerActions.QUARRY_MINED);
			}
			miner.wearEquippedTool(block);
			miner.playWorkSwing(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
			block.setQuarryClearSweep(false);
			block.setQuarryBreakCooldown(BREAK_COOLDOWN);
			int[] next = advance(box, y, index);
			store(block, miner, top, bottom, next[0], next[1]);
			return;
		}
		store(block, miner, top, bottom, y, index);
	}

	private static void store(WorkerBlockEntity block, HumanWorkerEntity miner, int top, int bottom, int y, int index) {
		boolean wasExhausted = block.quarryExhausted();
		if (y < bottom) {
			block.setQuarryCursor(top, 0, block.quarryClearSweep());
		} else {
			boolean exhausted = block.quarryClearSweep() && block.quarryExhausted();
			block.setQuarryCursor(y, index, exhausted);
		}
		if (!wasExhausted && block.quarryExhausted()) {
			WorkerAdvancements.grant(miner, WorkerActions.QUARRY_BOTTOM);
		}
	}

	private static int[] advance(QuarryBounds.Box box, int y, int index) {
		index++;
		if (index >= box.layerSize()) {
			return new int[] {y - 1, 0};
		}
		return new int[] {y, index};
	}

	private static BlockPos cell(BlockPos origin, QuarryBounds.Box box, int y, int index) {
		int dx = box.minX() + index % box.width();
		int dz = box.minZ() + index / box.width();
		return new BlockPos(origin.getX() + dx, y, origin.getZ() + dz);
	}

	private static boolean besideFluid(ServerLevel level, BlockPos pos) {
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			if (!level.getBlockState(pos.relative(direction)).getFluidState().isEmpty()) {
				return true;
			}
		}
		return false;
	}

	private static boolean skip(ServerLevel level, BlockPos pos, BlockState state) {
		if (state.isAir() || state.is(Blocks.BEDROCK)) {
			return true;
		}
		boolean emptyCollision = state.getCollisionShape(level, pos).isEmpty();
		if (!state.getFluidState().isEmpty() && emptyCollision) {
			return true;
		}
		if (state.getDestroySpeed(level, pos) < 0.0F) {
			return true;
		}
		return emptyCollision;
	}
}
