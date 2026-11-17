package com.nomosiki.entity.ai;

import com.nomosiki.squad.SquadFormationLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class StandCells {
	private StandCells() {
	}

	/** The slot itself, then the four horizontal neighbours, each snapped to a nearby floor. */
	public static List<BlockPos> around(Level level, BlockPos slot) {
		LinkedHashSet<BlockPos> cells = new LinkedHashSet<>();
		addFloor(cells, level, slot);
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			addFloor(cells, level, slot.relative(direction));
		}
		if (cells.isEmpty()) {
			cells.add(slot.immutable());
		}
		return new ArrayList<>(cells);
	}

	public static BlockPos floorNear(Level level, BlockPos pos) {
		return SquadFormationLayout.surfaceAt(level, pos);
	}

	/** Two blocks to the side of the line toward {@code stand}. */
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
		if (SquadFormationLayout.isStandable(level, left) && !left.equals(stand)) {
			return left;
		}
		BlockPos right = floorNear(level, origin.offset(-ox, 0, -oz));
		if (SquadFormationLayout.isStandable(level, right) && !right.equals(stand)) {
			return right;
		}
		return null;
	}

	public static @Nullable BlockPos firstReachable(Mob mob, List<BlockPos> candidates) {
		for (BlockPos candidate : candidates) {
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

	private static void addFloor(LinkedHashSet<BlockPos> cells, Level level, BlockPos pos) {
		BlockPos floor = SquadFormationLayout.surfaceAt(level, pos);
		if (SquadFormationLayout.isStandable(level, floor)) {
			cells.add(floor.immutable());
		}
	}
}
