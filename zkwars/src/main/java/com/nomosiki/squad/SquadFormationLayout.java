package com.nomosiki.squad;

import com.nomosiki.entity.SquadFormation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * World slot positions for squad formations. Line layout matches the original Nomosiki behavior;
 * other shapes port geometry from Recruits {@code FormationUtils} (no ship/captain branches).
 * Forward on XZ points from {@code squadCenter} toward {@code anchorCenter}, matching {@link #lineSlots}.
 */
public final class SquadFormationLayout {
	public static final double DEFAULT_SPACING = 2.0;
	public static final int DEFAULT_MAX_IN_ROW = 10;

	/** Matches Recruits using {@code 2.5 * spacingMultiplier} when Nomosiki passes {@code ~2.0} as layout spacing. */
	private static final double RECRUITS_SHAPE_SCALE = 1.25;

	private SquadFormationLayout() {
	}

	/**
	 * Dispatches to the slot generator for the given formation. {@link SquadFormation#NONE} returns an empty list.
	 */
	public static List<Vec3> slotsForFormation(
		SquadFormation formation,
		Level level,
		Vec3 anchorCenter,
		Vec3 squadCenter,
		int unitCount,
		double spacing,
		int maxInRowForLine
	) {
		if (formation == null || formation == SquadFormation.NONE || unitCount <= 0) {
			return List.of();
		}
		return switch (formation) {
			case NONE -> List.of();
			case LINE -> lineSlots(level, anchorCenter, squadCenter, unitCount, spacing, maxInRowForLine);
			case SQUARE -> squareSlots(level, anchorCenter, squadCenter, unitCount, spacing);
			case TRIANGLE -> triangleSlots(level, anchorCenter, squadCenter, unitCount, spacing);
			case HOLLOW_CIRCLE -> hollowCircleSlots(level, anchorCenter, squadCenter, unitCount, spacing);
			case HOLLOW_SQUARE -> hollowSquareSlots(level, anchorCenter, squadCenter, unitCount, spacing);
			case V -> vSlots(level, anchorCenter, squadCenter, unitCount, spacing);
			case CIRCLE -> circleSlots(level, anchorCenter, squadCenter, unitCount, spacing);
			case MOVEMENT -> movementSlots(level, anchorCenter, squadCenter, unitCount, spacing);
		};
	}

	public static List<Vec3> lineSlots(Level level, Vec3 anchorCenter, Vec3 squadCenter, int unitCount, double spacing, int maxInRow) {
		if (unitCount <= 0) {
			return List.of();
		}
		Axes axes = axesTowardAnchor(anchorCenter, squadCenter);
		int maxRow = Math.max(1, maxInRow);
		List<Vec3> raw = new ArrayList<>(unitCount);
		for (int i = 0; i < unitCount; i++) {
			int row = i / maxRow;
			int inRow = i % maxRow;
			int rowCount = Math.min(maxRow, unitCount - row * maxRow);
			double centerOffset = (rowCount - 1) / 2.0;
			Vec3 base = anchorCenter.add(axes.forward.scale(-row * spacing));
			Vec3 offset = axes.left.scale((inRow - centerOffset) * spacing);
			raw.add(base.add(offset));
		}
		return snapXZ(level, raw);
	}

	/** Recruits square grid: ceil(sqrt(n)) side, cell step {@code 2.5 * mult} → {@link #recruitsStep(double)}. */
	public static List<Vec3> squareSlots(Level level, Vec3 anchorCenter, Vec3 squadCenter, int unitCount, double layoutSpacing) {
		if (unitCount <= 0) {
			return List.of();
		}
		Axes axes = axesTowardAnchor(anchorCenter, squadCenter);
		double cell = recruitsStep(layoutSpacing);
		int sideLength = (int) Math.ceil(Math.sqrt(unitCount));
		List<Vec3> raw = new ArrayList<>(unitCount);
		for (int i = 0; i < unitCount; i++) {
			int row = i / sideLength;
			int col = i % sideLength;
			Vec3 rowOffset = axes.forward.scale(-row * cell);
			Vec3 colOffset = axes.left.scale((col - sideLength / 2.0F) * cell);
			raw.add(anchorCenter.add(rowOffset).add(colOffset));
		}
		return snapXZ(level, raw);
	}

	/** Recruits triangle / wedge ranks. */
	public static List<Vec3> triangleSlots(Level level, Vec3 anchorCenter, Vec3 squadCenter, int unitCount, double layoutSpacing) {
		if (unitCount <= 0) {
			return List.of();
		}
		Axes axes = axesTowardAnchor(anchorCenter, squadCenter);
		double spacing = recruitsStep(layoutSpacing);
		double rowDistance = spacing * 1.5;
		List<Vec3> raw = new ArrayList<>(unitCount);
		int index = 0;
		int rowCount = 1;
		while (index < unitCount) {
			for (int positionInRow = 0; positionInRow < rowCount && index < unitCount; positionInRow++, index++) {
				Vec3 basePos = anchorCenter.add(axes.forward.scale(-rowDistance * (rowCount - 1)));
				Vec3 offset = axes.left.scale((positionInRow - (rowCount - 1) / 2.0F) * spacing);
				raw.add(basePos.add(offset));
			}
			rowCount++;
		}
		return snapXZ(level, raw);
	}

	/** Recruits hollow circle: radius from arc length. */
	public static List<Vec3> hollowCircleSlots(Level level, Vec3 anchorCenter, Vec3 squadCenter, int unitCount, double layoutSpacing) {
		if (unitCount <= 0) {
			return List.of();
		}
		double spacing = recruitsStep(layoutSpacing);
		double radius = spacing * unitCount / (2.0 * Math.PI);
		List<Vec3> raw = new ArrayList<>(unitCount);
		for (int i = 0; i < unitCount; i++) {
			double angle = (2.0 * Math.PI / unitCount) * i;
			double x = anchorCenter.x + radius * Math.cos(angle);
			double z = anchorCenter.z + radius * Math.sin(angle);
			raw.add(new Vec3(x, anchorCenter.y, z));
		}
		return snapXZ(level, raw);
	}

	/**
	 * Recruits hollow square (double perimeter band). Small squads that yield no Recruits-style points fall back to {@link #squareSlots}.
	 */
	public static List<Vec3> hollowSquareSlots(Level level, Vec3 anchorCenter, Vec3 squadCenter, int unitCount, double layoutSpacing) {
		if (unitCount <= 0) {
			return List.of();
		}
		if (unitCount == 1) {
			return snapXZ(level, List.of(anchorCenter));
		}
		Axes axes = axesTowardAnchor(anchorCenter, squadCenter);
		double spacing = recruitsStep(layoutSpacing);
		int recruitsPerSide = Math.max(2, unitCount / 4);
		int totalRecruitsNeeded = recruitsPerSide * 4;
		if (totalRecruitsNeeded > unitCount) {
			recruitsPerSide = unitCount / 4;
			totalRecruitsNeeded = recruitsPerSide * 4;
		}
		List<Vec3> candidates = new ArrayList<>();
		for (int row = 0; row < 2; row++) {
			if (recruitsPerSide <= 0) {
				break;
			}
			double offset = (spacing * recruitsPerSide) / 2.0;
			for (int i = 0; i < recruitsPerSide; i++) {
				double positionOffset = i * spacing - offset;
				candidates.add(anchorCenter.add(axes.forward.scale(-offset - row * spacing)).add(axes.left.scale(positionOffset)));
				candidates.add(anchorCenter.add(axes.forward.scale(offset + row * spacing)).add(axes.left.scale(positionOffset)));
				candidates.add(anchorCenter.add(axes.left.scale(-offset - row * spacing)).add(axes.forward.scale(positionOffset)));
				candidates.add(anchorCenter.add(axes.left.scale(offset + row * spacing)).add(axes.forward.scale(positionOffset)));
			}
		}
		if (candidates.isEmpty()) {
			return squareSlots(level, anchorCenter, squadCenter, unitCount, layoutSpacing);
		}
		return snapXZ(level, resampleToCount(candidates, unitCount));
	}

	/** Recruits V: interleaved wings, optional center for odd count. */
	public static List<Vec3> vSlots(Level level, Vec3 anchorCenter, Vec3 squadCenter, int unitCount, double layoutSpacing) {
		if (unitCount <= 0) {
			return List.of();
		}
		Axes axes = axesTowardAnchor(anchorCenter, squadCenter);
		double spacing = recruitsStep(layoutSpacing);
		int recruitsPerWing = unitCount / 2;
		List<Vec3> raw = new ArrayList<>(unitCount);
		for (int i = 0; i < recruitsPerWing; i++) {
			double offset = i * spacing;
			raw.add(anchorCenter.add(axes.forward.scale(offset)).add(axes.left.scale(offset)));
			raw.add(anchorCenter.add(axes.forward.scale(offset)).subtract(axes.left.scale(offset)));
		}
		if (unitCount % 2 != 0) {
			raw.add(anchorCenter);
		}
		return snapXZ(level, raw);
	}

	/** Recruits filled circle: inner (max 5), middle (max 10), outer remainder. */
	public static List<Vec3> circleSlots(Level level, Vec3 anchorCenter, Vec3 squadCenter, int unitCount, double layoutSpacing) {
		if (unitCount <= 0) {
			return List.of();
		}
		double spacing = recruitsStep(layoutSpacing);
		int innerRingCount = Math.min(5, unitCount);
		int middleRingCount = Math.min(10, unitCount - innerRingCount);
		int outerRingCount = unitCount - innerRingCount - middleRingCount;
		double innerRadius = innerRingCount > 0 ? spacing * innerRingCount / (2.0 * Math.PI) : 0.0;
		double middleRadius = middleRingCount > 0 ? spacing * middleRingCount / (2.0 * Math.PI) : 0.0;
		double outerRadius = outerRingCount > 0 ? spacing * outerRingCount / (2.0 * Math.PI) : 0.0;
		List<Vec3> raw = new ArrayList<>(unitCount);
		for (int i = 0; i < innerRingCount; i++) {
			double angle = (2.0 * Math.PI / innerRingCount) * i;
			raw.add(ringPoint(anchorCenter, innerRadius, angle));
		}
		for (int i = 0; i < middleRingCount; i++) {
			double angle = (2.0 * Math.PI / middleRingCount) * i;
			raw.add(ringPoint(anchorCenter, middleRadius, angle));
		}
		for (int i = 0; i < outerRingCount; i++) {
			double angle = (2.0 * Math.PI / outerRingCount) * i;
			raw.add(ringPoint(anchorCenter, outerRadius, angle));
		}
		return snapXZ(level, raw);
	}

	/** Recruits movement: shallow line, max 3 per row; uses {@link #lineSlots} with tighter column. */
	public static List<Vec3> movementSlots(Level level, Vec3 anchorCenter, Vec3 squadCenter, int unitCount, double layoutSpacing) {
		if (unitCount <= 0) {
			return List.of();
		}
		// Recruits passes 2.0 * spacingMult into lineFormation as lateral spacing; rows use *1.75 inside recruits.
		double lateral = Math.max(0.5, layoutSpacing);
		return lineSlots(level, anchorCenter, squadCenter, unitCount, lateral * 2.0, 3);
	}

	private static Vec3 ringPoint(Vec3 center, double radius, double angle) {
		return new Vec3(
			center.x + radius * Math.cos(angle),
			center.y,
			center.z + radius * Math.sin(angle)
		);
	}

	private static double recruitsStep(double layoutSpacing) {
		return layoutSpacing * RECRUITS_SHAPE_SCALE;
	}

	private record Axes(Vec3 forward, Vec3 left) {
	}

	/**
	 * Yaw (degrees) for units facing along the formation forward axis (toward the anchor).
	 */
	public static float facingYawDegrees(Vec3 anchorCenter, Vec3 squadCenter) {
		Vec3 forward = axesTowardAnchor(anchorCenter, squadCenter).forward();
		return (float) Math.toDegrees(Math.atan2(-forward.x, forward.z));
	}

	private static Axes axesTowardAnchor(Vec3 anchorCenter, Vec3 squadCenter) {
		Vec3 toAnchor = anchorCenter.subtract(squadCenter);
		Vec3 forward = toAnchor.lengthSqr() > 1.0e-4
			? new Vec3(toAnchor.x, 0.0, toAnchor.z).normalize()
			: new Vec3(0.0, 0.0, 1.0);
		Vec3 left = new Vec3(-forward.z, 0.0, forward.x);
		return new Axes(forward, left);
	}

	private static List<Vec3> resampleToCount(List<Vec3> dense, int unitCount) {
		if (dense.size() == unitCount) {
			return new ArrayList<>(dense);
		}
		if (dense.size() > unitCount) {
			List<Vec3> out = new ArrayList<>(unitCount);
			if (unitCount == 1) {
				out.add(dense.get(0));
				return out;
			}
			for (int i = 0; i < unitCount; i++) {
				int idx = (int) Math.round((double) i * (dense.size() - 1) / (unitCount - 1));
				out.add(dense.get(idx));
			}
			return out;
		}
		// Too few candidates: pad by repeating last (rare)
		List<Vec3> out = new ArrayList<>(dense);
		Vec3 last = dense.get(dense.size() - 1);
		while (out.size() < unitCount) {
			out.add(last);
		}
		return out;
	}

	private static List<Vec3> snapXZ(Level level, List<Vec3> raw) {
		List<Vec3> out = new ArrayList<>(raw.size());
		for (Vec3 p : raw) {
			BlockPos snapped = surfaceAt(level, BlockPos.containing(p.x, p.y, p.z));
			out.add(new Vec3(p.x, snapped.getY(), p.z));
		}
		return out;
	}

	/** Snaps Y to the nearest standable floor within six blocks. Downward wins a tie. */
	public static BlockPos surfaceAt(Level level, BlockPos pos) {
		return positionOrSurface(level, pos);
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

	private static BlockPos positionOrSurface(Level level, BlockPos pos) {
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
}
