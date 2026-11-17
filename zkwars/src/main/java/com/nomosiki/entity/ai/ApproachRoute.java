package com.nomosiki.entity.ai;

import com.nomosiki.squad.SquadFormationLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

/**
 * Walks to a floor cell at the target, then to the four neighbouring floors when the route gives up.
 */
public final class ApproachRoute {
	private static final int RETRY_PAUSE = 40;

	private final RouteWalker route = new RouteWalker();
	private @Nullable BlockPos origin;
	private int approach;
	private int pause;

	public void reset(Mob mob) {
		this.route.reset();
		this.origin = null;
		this.approach = 0;
		this.pause = 0;
		mob.getNavigation().stop();
	}

	public void tick(Mob mob, Vec3 dest, double speed) {
		if (this.pause > 0) {
			this.pause--;
			mob.getNavigation().stop();
			return;
		}
		BlockPos key = BlockPos.containing(dest).immutable();
		if (this.origin == null || !this.origin.equals(key)) {
			this.origin = key;
			this.approach = 0;
			this.route.reset();
		}
		BlockPos cell = this.approachCell(mob, key);
		RouteWalker.Status status = this.route.tick(mob, Vec3.atBottomCenterOf(cell), speed);
		if (status != RouteWalker.Status.BLOCKED) {
			return;
		}
		this.approach++;
		this.route.reset();
		if (this.approach > 4) {
			this.approach = 0;
			this.pause = RETRY_PAUSE;
		}
	}

	private BlockPos approachCell(Mob mob, BlockPos key) {
		BlockPos raw = this.approach == 0 ? key : key.relative(Direction.from2DDataValue((this.approach - 1) & 3));
		return SquadFormationLayout.surfaceAt(mob.level(), raw);
	}
}
