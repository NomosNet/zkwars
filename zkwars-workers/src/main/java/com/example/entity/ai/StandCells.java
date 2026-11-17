package com.example.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class StandCells {
	private StandCells() {
	}

	public static boolean isStandable(Level level, BlockPos pos) {
		if (!level.isLoaded(pos)) {
			return false;
		}
		BlockPos below = pos.below();
		BlockState ground = level.getBlockState(below);
		return ground.isFaceSturdy(level, below, Direction.UP)
				&& level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
				&& level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
	}

	/** Nearest standable floor within six blocks of the given Y. Prefers downward at the same distance. */
	public static BlockPos floorNear(Level level, BlockPos pos) {
		if (isStandable(level, pos)) {
			return pos;
		}
		for (int dy = 1; dy <= 6; dy++) {
			BlockPos down = pos.below(dy);
			if (isStandable(level, down)) {
				return down;
			}
			BlockPos up = pos.above(dy);
			if (isStandable(level, up)) {
				return up;
			}
		}
		return pos;
	}

	/**
	 * Stand cells beside {@code target} from two blocks below to two blocks above.
	 * The first cell with a path is kept; {@code skip} is tried only if nothing else can be reached.
	 */
	public static BlockPos beside(Mob mob, BlockPos target, @Nullable BlockPos skip) {
		List<BlockPos> candidates = new ArrayList<>();
		for (int dy = -2; dy <= 2; dy++) {
			for (Direction direction : Direction.Plane.HORIZONTAL) {
				BlockPos pos = target.relative(direction).offset(0, dy, 0);
				if (isStandable(mob.level(), pos)) {
					candidates.add(pos.immutable());
				}
			}
		}
		if (candidates.isEmpty()) {
			return target;
		}
		candidates.sort(Comparator.comparingDouble(pos -> mob.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5)));
		BlockPos reachable = firstReachable(mob, candidates, skip);
		if (reachable != null) {
			return reachable;
		}
		if (skip != null) {
			reachable = firstReachable(mob, candidates, null);
			if (reachable != null) {
				return reachable;
			}
		}
		for (BlockPos candidate : candidates) {
			if (skip == null || !skip.equals(candidate)) {
				return candidate;
			}
		}
		return candidates.get(0);
	}

	public static @Nullable BlockPos sideStep(Mob mob, BlockPos stand) {
		double dx = stand.getX() + 0.5 - mob.getX();
		double dz = stand.getZ() + 0.5 - mob.getZ();
		double len = Math.sqrt(dx * dx + dz * dz);
		if (len < 1.0E-4) {
			dx = 1.0;
			dz = 0.0;
			len = 1.0;
		}
		int ox = (int) Math.round(-dz / len * 2.0);
		int oz = (int) Math.round(dx / len * 2.0);
		if (ox == 0 && oz == 0) {
			ox = 2;
		}
		Level level = mob.level();
		BlockPos origin = mob.blockPosition();
		BlockPos left = floorNear(level, origin.offset(ox, 0, oz));
		if (isStandable(level, left) && !left.equals(stand)) {
			return left;
		}
		BlockPos right = floorNear(level, origin.offset(-ox, 0, -oz));
		if (isStandable(level, right) && !right.equals(stand)) {
			return right;
		}
		return null;
	}

	private static @Nullable BlockPos firstReachable(Mob mob, List<BlockPos> candidates, @Nullable BlockPos skip) {
		for (BlockPos candidate : candidates) {
			if (skip != null && skip.equals(candidate)) {
				continue;
			}
			if (!mob.level().isLoaded(candidate)) {
				continue;
			}
			Path path = mob.getNavigation().createPath(candidate, 1);
			if (path != null && path.canReach()) {
				return candidate;
			}
		}
		return null;
	}
}
