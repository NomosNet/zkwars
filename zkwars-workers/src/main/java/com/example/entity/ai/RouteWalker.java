package com.example.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

/**
 * Issues a vanilla ground path only when the target cell changes, the current path ends,
 * the previous search failed, or the mob has been stuck. Climbs a nearby ladder when the
 * direct path cannot change floors.
 */
public final class RouteWalker {
	public enum Status {
		MOVING,
		BLOCKED
	}

	private static final int REPATH_INTERVAL = 10;
	private static final int STUCK_TICKS = 40;
	private static final double STUCK_DISTANCE = 0.5;
	private static final int LADDER_RADIUS = 16;
	private static final int LADDER_RADIUS_SQR = LADDER_RADIUS * LADDER_RADIUS;

	private int cooldown;
	private @Nullable BlockPos lastDestCell;
	private @Nullable BlockPos standCell;
	private @Nullable BlockPos ladderColumn;
	private @Nullable BlockPos ladderCell;
	private int climbTargetY;
	private int climbStillTicks;
	private double climbMarkY;
	private boolean climbMarkSet;
	private boolean climbRetried;
	private boolean pathOpen;
	private boolean retryNext;
	private boolean nullPathRetried;
	private boolean triedSidestep;
	private boolean forceRepath;
	private boolean blocked;
	private int stuckTicks;
	private @Nullable Vec3 stuckOrigin;

	public void reset() {
		this.cooldown = 0;
		this.lastDestCell = null;
		this.standCell = null;
		this.clearLadder();
		this.pathOpen = false;
		this.retryNext = false;
		this.nullPathRetried = false;
		this.triedSidestep = false;
		this.forceRepath = false;
		this.blocked = false;
		this.stuckTicks = 0;
		this.stuckOrigin = null;
	}

	public Status tick(Mob mob, Vec3 dest, double speed) {
		Level level = mob.level();
		BlockPos cell = BlockPos.containing(dest);
		if (!level.isLoaded(cell)) {
			return Status.MOVING;
		}
		if (this.blocked && cell.equals(this.lastDestCell)) {
			mob.getNavigation().stop();
			return Status.BLOCKED;
		}
		boolean cellChanged = this.lastDestCell == null || !this.lastDestCell.equals(cell);
		if (cellChanged) {
			this.lastDestCell = cell.immutable();
			this.standCell = StandCells.floorNear(level, cell).immutable();
			this.clearLadder();
			this.pathOpen = false;
			this.retryNext = false;
			this.nullPathRetried = false;
			this.triedSidestep = false;
			this.blocked = false;
			this.stuckTicks = 0;
			this.stuckOrigin = mob.position();
			this.forceRepath = true;
		}

		if (this.ladderColumn != null) {
			if (Math.abs(mob.blockPosition().getY() - this.climbTargetY) <= 1) {
				this.clearLadder();
				this.forceRepath = true;
			} else if (this.climbingNow(mob)) {
				this.tickClimb(mob, speed);
				return this.blocked ? Status.BLOCKED : Status.MOVING;
			}
		}

		if (this.tickStuck(mob, speed)) {
			return this.blocked ? Status.BLOCKED : Status.MOVING;
		}

		boolean finished = this.pathOpen && mob.getNavigation().isDone();
		boolean due = cellChanged || finished || this.retryNext || this.forceRepath || this.cooldown <= 0;
		if (!due) {
			this.cooldown--;
			return Status.MOVING;
		}
		this.cooldown = REPATH_INTERVAL;
		this.retryNext = false;
		this.forceRepath = false;
		return this.issuePath(mob, speed);
	}

	private boolean climbingNow(Mob mob) {
		if (mob.onClimbable()) {
			return true;
		}
		BlockPos column = this.ladderColumn;
		if (column == null) {
			return false;
		}
		return mob.distanceToSqr(column.getX() + 0.5, mob.getY(), column.getZ() + 0.5) <= 2.25;
	}

	private void tickClimb(Mob mob, double speed) {
		BlockPos column = this.ladderColumn;
		if (column == null) {
			return;
		}
		mob.getNavigation().stop();
		this.pathOpen = false;
		double direction = Math.signum(this.climbTargetY - mob.getY());
		if (direction == 0.0) {
			direction = this.climbTargetY >= mob.blockPosition().getY() ? 1.0 : -1.0;
		}
		mob.getMoveControl().setWantedPosition(column.getX() + 0.5, mob.getY() + direction, column.getZ() + 0.5, speed);
		if (direction > 0.0 && mob.onClimbable()) {
			Vec3 delta = mob.getDeltaMovement();
			mob.setDeltaMovement(delta.x, 0.2, delta.z);
		}
		if (!this.climbMarkSet) {
			this.climbMarkSet = true;
			this.climbMarkY = mob.getY();
			this.climbStillTicks = 0;
		}
		if (Math.abs(mob.getY() - this.climbMarkY) >= STUCK_DISTANCE) {
			this.climbMarkY = mob.getY();
			this.climbStillTicks = 0;
			this.climbRetried = false;
			return;
		}
		this.climbStillTicks++;
		if (this.climbStillTicks < STUCK_TICKS) {
			return;
		}
		if (this.climbRetried || this.ladderCell == null) {
			this.blocked = true;
			return;
		}
		this.climbRetried = true;
		this.climbStillTicks = 0;
		this.climbMarkY = mob.getY();
		Path path = mob.getNavigation().createPath(this.ladderCell, 1);
		if (path == null || !mob.getNavigation().moveTo(path, speed)) {
			this.blocked = true;
			return;
		}
		this.pathOpen = !mob.getNavigation().isDone();
	}

	/** @return true when this tick already issued a sidestep or gave up */
	private boolean tickStuck(Mob mob, double speed) {
		if (mob.getNavigation().isDone()) {
			return false;
		}
		if (this.stuckOrigin == null) {
			this.stuckOrigin = mob.position();
			this.stuckTicks = 0;
			return false;
		}
		if (mob.position().distanceTo(this.stuckOrigin) >= STUCK_DISTANCE) {
			this.stuckOrigin = mob.position();
			this.stuckTicks = 0;
			this.triedSidestep = false;
			return false;
		}
		this.stuckTicks++;
		if (this.stuckTicks < STUCK_TICKS) {
			return false;
		}
		if (this.triedSidestep || this.standCell == null) {
			this.blocked = true;
			mob.getNavigation().stop();
			return true;
		}
		this.triedSidestep = true;
		this.stuckTicks = 0;
		this.stuckOrigin = mob.position();
		BlockPos side = StandCells.sideStep(mob, this.standCell);
		Path path = side == null ? null : mob.getNavigation().createPath(side, 1);
		if (path == null || !mob.getNavigation().moveTo(path, speed)) {
			this.blocked = true;
			mob.getNavigation().stop();
			return true;
		}
		this.pathOpen = !mob.getNavigation().isDone();
		this.cooldown = REPATH_INTERVAL;
		return true;
	}

	private Status issuePath(Mob mob, double speed) {
		BlockPos stand = this.standCell;
		if (stand == null) {
			this.blocked = true;
			return Status.BLOCKED;
		}
		if (this.ladderColumn != null && this.ladderCell != null) {
			if (!mob.level().isLoaded(this.ladderCell)) {
				return Status.MOVING;
			}
			Path path = mob.getNavigation().createPath(this.ladderCell, 1);
			return this.finishIssue(mob, path != null && mob.getNavigation().moveTo(path, speed));
		}
		if (!mob.level().isLoaded(stand)) {
			return Status.MOVING;
		}
		Path direct = mob.getNavigation().createPath(stand, 1);
		boolean reaches = direct != null && direct.canReach();
		int dy = stand.getY() - mob.blockPosition().getY();
		if (!reaches && Math.abs(dy) > 1) {
			BlockPos ladder = findLadder(mob, stand);
			if (ladder != null) {
				this.beginLadder(mob, stand, ladder);
				Path path = this.ladderCell == null ? null : mob.getNavigation().createPath(this.ladderCell, 1);
				return this.finishIssue(mob, path != null && mob.getNavigation().moveTo(path, speed));
			}
		}
		if (direct != null) {
			return this.finishIssue(mob, mob.getNavigation().moveTo(direct, speed));
		}
		return this.finishIssue(mob, false);
	}

	private void beginLadder(Mob mob, BlockPos stand, BlockPos ladder) {
		this.ladderColumn = ladder;
		this.ladderCell = climbableAt(mob.level(), ladder.getX(), ladder.getZ(), mob.blockPosition().getY());
		this.climbTargetY = stand.getY();
		this.climbMarkSet = false;
		this.climbRetried = false;
		this.climbStillTicks = 0;
	}

	private Status finishIssue(Mob mob, boolean ok) {
		if (!ok) {
			if (!this.nullPathRetried) {
				this.nullPathRetried = true;
				this.retryNext = true;
				this.pathOpen = false;
				return Status.MOVING;
			}
			this.blocked = true;
			mob.getNavigation().stop();
			this.pathOpen = false;
			return Status.BLOCKED;
		}
		this.nullPathRetried = false;
		this.pathOpen = !mob.getNavigation().isDone();
		return Status.MOVING;
	}

	private void clearLadder() {
		this.ladderColumn = null;
		this.ladderCell = null;
		this.climbMarkSet = false;
		this.climbRetried = false;
		this.climbStillTicks = 0;
	}

	private static @Nullable BlockPos findLadder(Mob mob, BlockPos stand) {
		Level level = mob.level();
		int mobY = mob.blockPosition().getY();
		int targetY = stand.getY();
		if (Math.abs(targetY - mobY) > 32) {
			return null;
		}
		int minY = Math.min(mobY, targetY);
		int maxY = Math.max(mobY, targetY);
		BlockPos origin = mob.blockPosition();
		BlockPos best = null;
		int bestDist = Integer.MAX_VALUE;
		for (int dx = -LADDER_RADIUS; dx <= LADDER_RADIUS; dx++) {
			for (int dz = -LADDER_RADIUS; dz <= LADDER_RADIUS; dz++) {
				int dist = dx * dx + dz * dz;
				if (dist > LADDER_RADIUS_SQR || dist >= bestDist) {
					continue;
				}
				int x = origin.getX() + dx;
				int z = origin.getZ() + dz;
				if (!columnConnects(level, x, z, minY, maxY)) {
					continue;
				}
				bestDist = dist;
				best = new BlockPos(x, mobY, z);
			}
		}
		return best;
	}

	private static boolean columnConnects(Level level, int x, int z, int minY, int maxY) {
		BlockPos feet = new BlockPos(x, minY, z);
		if (!level.isLoaded(feet) || !level.isLoaded(new BlockPos(x, maxY, z))) {
			return false;
		}
		if (!hasClimbableNear(level, x, z, minY) || !hasClimbableNear(level, x, z, maxY)) {
			return false;
		}
		int gap = 0;
		boolean started = false;
		for (int y = minY; y <= maxY; y++) {
			if (level.getBlockState(new BlockPos(x, y, z)).is(BlockTags.CLIMBABLE)) {
				started = true;
				gap = 0;
			} else if (started) {
				gap++;
				if (gap > 1) {
					return false;
				}
			}
		}
		return started;
	}

	private static boolean hasClimbableNear(Level level, int x, int z, int y) {
		for (int dy = -1; dy <= 1; dy++) {
			if (level.getBlockState(new BlockPos(x, y + dy, z)).is(BlockTags.CLIMBABLE)) {
				return true;
			}
		}
		return false;
	}

	private static BlockPos climbableAt(Level level, int x, int z, int y) {
		for (int dy = -1; dy <= 1; dy++) {
			BlockPos pos = new BlockPos(x, y + dy, z);
			if (level.getBlockState(pos).is(BlockTags.CLIMBABLE)) {
				return pos;
			}
		}
		return new BlockPos(x, y, z);
	}
}
