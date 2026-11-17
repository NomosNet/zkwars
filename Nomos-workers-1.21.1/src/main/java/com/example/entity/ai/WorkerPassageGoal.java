package com.example.entity.ai;

import com.example.entity.HumanWorkerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.ArrayList;
import java.util.List;

public class WorkerPassageGoal extends Goal {
	private static final double RADIUS = 2.0;
	private static final double RADIUS_SQR = RADIUS * RADIUS;

	private final HumanWorkerEntity worker;
	private final List<BlockPos> opened = new ArrayList<>();

	public WorkerPassageGoal(HumanWorkerEntity worker) {
		this.worker = worker;
	}

	@Override
	public boolean canUse() {
		return this.worker.isAlive() && !this.worker.isSleeping() && this.worker.opensPassages();
	}

	@Override
	public boolean canContinueToUse() {
		return this.canUse() || !this.opened.isEmpty();
	}

	@Override
	public void stop() {
		this.closeAll();
	}

	@Override
	public void tick() {
		if (this.worker.isSleeping() || !this.worker.opensPassages()) {
			this.closeAll();
			return;
		}
		this.closeDistant();
		this.openNearby();
	}

	private void openNearby() {
		Level level = this.worker.level();
		BlockPos center = this.worker.blockPosition();
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, -1, -2), center.offset(2, 1, 2))) {
			if (this.worker.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > RADIUS_SQR) {
				continue;
			}
			BlockState state = level.getBlockState(pos);
			if (!isClosedPassage(state) || this.alreadyOpened(pos)) {
				continue;
			}
			BlockPos immutable = pos.immutable();
			setOpen(level, immutable, state, true);
			this.opened.add(immutable);
		}
	}

	private void closeDistant() {
		Level level = this.worker.level();
		this.opened.removeIf(pos -> {
			if (this.worker.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= RADIUS_SQR) {
				return false;
			}
			setOpen(level, pos, level.getBlockState(pos), false);
			return true;
		});
	}

	private void closeAll() {
		Level level = this.worker.level();
		for (BlockPos pos : this.opened) {
			setOpen(level, pos, level.getBlockState(pos), false);
		}
		this.opened.clear();
	}

	private boolean alreadyOpened(BlockPos pos) {
		for (BlockPos openedPos : this.opened) {
			if (openedPos.equals(pos)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isClosedPassage(BlockState state) {
		if (state.getBlock() instanceof DoorBlock && !state.is(Blocks.IRON_DOOR)) {
			return state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER && !state.getValue(DoorBlock.OPEN);
		}
		return state.getBlock() instanceof FenceGateBlock && !state.getValue(FenceGateBlock.OPEN);
	}

	private void setOpen(Level level, BlockPos pos, BlockState state, boolean open) {
		if (state.getBlock() instanceof DoorBlock door && !state.is(Blocks.IRON_DOOR)) {
			BlockPos lower = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
			BlockState lowerState = level.getBlockState(lower);
			if (!(lowerState.getBlock() instanceof DoorBlock) || lowerState.is(Blocks.IRON_DOOR)) {
				return;
			}
			if (lowerState.getValue(DoorBlock.OPEN) == open) {
				return;
			}
			door.setOpen(this.worker, level, lowerState, lower, open);
			BlockPos upper = lower.above();
			BlockState upperState = level.getBlockState(upper);
			if (upperState.getBlock() instanceof DoorBlock && !upperState.is(Blocks.IRON_DOOR) && upperState.getValue(DoorBlock.OPEN) != open) {
				level.setBlock(upper, upperState.setValue(DoorBlock.OPEN, open), 10);
			}
			return;
		}
		if (state.getBlock() instanceof FenceGateBlock && state.getValue(FenceGateBlock.OPEN) != open) {
			level.setBlock(pos, state.setValue(FenceGateBlock.OPEN, open), 10);
			level.playSound(null, pos, open ? SoundEvents.FENCE_GATE_OPEN : SoundEvents.FENCE_GATE_CLOSE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
			level.gameEvent(this.worker, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
		}
	}
}
