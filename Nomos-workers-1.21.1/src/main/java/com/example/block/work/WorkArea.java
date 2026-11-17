package com.example.block.work;

import com.example.block.WorkerKind;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public final class WorkArea {
	public static final int MIN_RADIUS = 1;
	public static final int MAX_RADIUS = 16;
	public static final int DEFAULT_RADIUS = 8;
	public static final int LUMBER_MAX_RADIUS = 32;
	public static final int LUMBER_DEFAULT_RADIUS = 16;
	public static final int LUMBER_SCAN_COLUMNS = 160;
	public static final int LUMBER_DOWN = 4;
	public static final int LUMBER_UP = 48;
	public static final int CANOPY = 10;
	public static final int MAX_TREE_LOGS = 256;
	public static final int TRUNK_LAYERS = 3;

	private WorkArea() {
	}

	public static int maxRadius(WorkerKind kind) {
		return kind == WorkerKind.LUMBERJACK ? LUMBER_MAX_RADIUS : MAX_RADIUS;
	}

	public static int defaultRadius(WorkerKind kind) {
		return kind == WorkerKind.LUMBERJACK ? LUMBER_DEFAULT_RADIUS : DEFAULT_RADIUS;
	}

	public static int clampRadius(int radius) {
		return clampRadius(WorkerKind.FARMER, radius);
	}

	public static int clampRadius(WorkerKind kind, int radius) {
		return Math.max(MIN_RADIUS, Math.min(maxRadius(kind), radius));
	}

	public static int radiusFor(WorkerKind kind, int stored) {
		if (kind == WorkerKind.FARMER) {
			return DEFAULT_RADIUS;
		}
		return clampRadius(kind, stored);
	}

	public static QuarryBounds.Box box(WorkerKind kind, int radius) {
		int horizontal = radiusFor(kind, radius);
		if (kind == WorkerKind.LUMBERJACK) {
			return new QuarryBounds.Box(-horizontal, horizontal, -LUMBER_DOWN, LUMBER_UP, -horizontal, horizontal);
		}
		return new QuarryBounds.Box(-horizontal, horizontal, -horizontal, horizontal, -horizontal, horizontal);
	}

	public static AABB volume(BlockPos origin, QuarryBounds.Box box) {
		return new AABB(
				origin.getX() + box.minX(),
				origin.getY() + box.minY(),
				origin.getZ() + box.minZ(),
				origin.getX() + box.maxX() + 1.0,
				origin.getY() + box.maxY() + 1.0,
				origin.getZ() + box.maxZ() + 1.0
		);
	}

	public static boolean contains(BlockPos origin, QuarryBounds.Box box, BlockPos pos) {
		int dx = pos.getX() - origin.getX();
		int dy = pos.getY() - origin.getY();
		int dz = pos.getZ() - origin.getZ();
		return dx >= box.minX() && dx <= box.maxX()
				&& dy >= box.minY() && dy <= box.maxY()
				&& dz >= box.minZ() && dz <= box.maxZ();
	}
}
