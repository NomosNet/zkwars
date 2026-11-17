package com.nomosiki.entity.ai;

import com.nomosiki.entity.SquadOrder;
import com.nomosiki.entity.NomosikEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class NomosikGuardGoal extends Goal {
	private static final double GUARD_TOLERANCE_SQR = 9.0;
	private final NomosikEntity nomosik;
	private final ApproachRoute route = new ApproachRoute();

	public NomosikGuardGoal(NomosikEntity nomosik) {
		this.nomosik = nomosik;
		this.setFlags(EnumSet.of(Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		return this.shouldGuard();
	}

	@Override
	public boolean canContinueToUse() {
		return this.shouldGuard();
	}

	private boolean shouldGuard() {
		if (this.nomosik.isRidingHeldSeat()) {
			return false;
		}
		if (this.nomosik.getSquadOrder() != SquadOrder.HOLD) {
			return false;
		}
		if (this.nomosik.getSquadFormationTarget() != null) {
			return false;
		}
		@Nullable BlockPos anchor = this.nomosik.getSquadAnchorPos();
		return anchor != null;
	}

	@Override
	public void tick() {
		BlockPos anchor = this.nomosik.getSquadAnchorPos();
		if (anchor == null) {
			return;
		}
		Vec3 target = Vec3.atBottomCenterOf(anchor);
		if (this.nomosik.distanceToSqr(target) > GUARD_TOLERANCE_SQR) {
			this.route.tick(this.nomosik, target, 1.08);
		} else {
			this.route.reset(this.nomosik);
		}
	}

	@Override
	public void stop() {
		this.route.reset(this.nomosik);
	}
}
