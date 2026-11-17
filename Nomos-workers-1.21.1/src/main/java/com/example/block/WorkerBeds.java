package com.example.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

import org.jetbrains.annotations.Nullable;

public final class WorkerBeds {
	private WorkerBeds() {
	}

	public static boolean waitingForChunk(Level level, BlockPos pos) {
		if (!level.isLoaded(pos)) {
			return true;
		}
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof BedBlock)) {
			return false;
		}
		return !level.isLoaded(partner(pos, state));
	}

	public static @Nullable BlockPos head(Level level, BlockPos pos) {
		if (!level.isLoaded(pos) || waitingForChunk(level, pos)) {
			return null;
		}
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof BedBlock)) {
			return null;
		}
		BlockPos head = state.getValue(BedBlock.PART) == BedPart.HEAD ? pos : partner(pos, state);
		BlockPos foot = state.getValue(BedBlock.PART) == BedPart.FOOT ? pos : partner(pos, state);
		if (!isPart(level, head, BedPart.HEAD) || !isPart(level, foot, BedPart.FOOT)) {
			return null;
		}
		return head;
	}

	public static @Nullable BlockPos otherHalf(Level level, BlockPos pos) {
		if (!level.isLoaded(pos)) {
			return null;
		}
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof BedBlock)) {
			return null;
		}
		BedPart part = state.getValue(BedBlock.PART);
		BlockPos other = partner(pos, state);
		BedPart expected = part == BedPart.FOOT ? BedPart.HEAD : BedPart.FOOT;
		if (!isPart(level, other, expected)) {
			return null;
		}
		return other;
	}

	private static BlockPos partner(BlockPos pos, BlockState state) {
		Direction facing = state.getValue(BedBlock.FACING);
		return state.getValue(BedBlock.PART) == BedPart.FOOT ? pos.relative(facing) : pos.relative(facing.getOpposite());
	}

	private static boolean isPart(Level level, BlockPos pos, BedPart part) {
		if (!level.isLoaded(pos)) {
			return false;
		}
		BlockState state = level.getBlockState(pos);
		return state.getBlock() instanceof BedBlock && state.getValue(BedBlock.PART) == part;
	}
}
