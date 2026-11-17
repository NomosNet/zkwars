package com.nomosiki.entity.ai;

import com.nomosiki.entity.SquadOrder;
import com.nomosiki.entity.NomosikEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class NomosikFollowOwnerGoal extends Goal {
	private final NomosikEntity nomosik;
	private LivingEntity owner;
	private final double speedModifier;
	private int timeUntilRecalc;
	private final float stopDistance;
	private final float startDistance;
	private long lastGameTimeCheck;

	public NomosikFollowOwnerGoal(NomosikEntity nomosik, double speedModifier, float startDistance, float stopDistance) {
		this.nomosik = nomosik;
		this.speedModifier = speedModifier;
		this.startDistance = startDistance;
		this.stopDistance = stopDistance;
		this.setFlags(EnumSet.of(Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		long time = this.nomosik.level().getGameTime();
		if (time - this.lastGameTimeCheck < 10L) {
			return false;
		}
		this.lastGameTimeCheck = time;
		if (this.nomosik.getSquadOrder() != SquadOrder.FOLLOW) {
			return false;
		}
		if (this.nomosik.isRidingHeldSeat()) {
			return false;
		}
		// Formation-follow: slots are driven by SquadBrain + NomosikSeekFormationGoal.
		if (this.nomosik.getSquadFormationTarget() != null) {
			return false;
		}
		LivingEntity o = this.nomosik.getOwner();
		if (o == null || o.isSpectator()) {
			return false;
		}
		if (this.nomosik.distanceToSqr(o) < (double) this.startDistance * this.startDistance) {
			return false;
		}
		this.owner = o;
		return this.nomosik.shouldFollowOwner() && this.nomosik.getTarget() == null;
	}

	@Override
	public boolean canContinueToUse() {
		if (this.owner == null || !this.owner.isAlive()) {
			return false;
		}
		if (this.nomosik.getSquadFormationTarget() != null) {
			return false;
		}
		if (this.nomosik.getNavigation().isDone()) {
			return false;
		}
		return this.nomosik.getSquadOrder() == SquadOrder.FOLLOW
			&& !(this.nomosik.distanceToSqr(this.owner) <= (double) this.stopDistance * this.stopDistance)
			&& this.nomosik.shouldFollowOwner()
			&& this.nomosik.getTarget() == null;
	}

	@Override
	public void start() {
		this.timeUntilRecalc = 0;
	}

	@Override
	public void stop() {
		this.owner = null;
		this.nomosik.getNavigation().stop();
	}

	@Override
	public void tick() {
		if (--this.timeUntilRecalc <= 0) {
			this.timeUntilRecalc = this.adjustedTickDelay(this.nomosik.isCavalryMounted() ? 5 : 10);
			if (this.owner != null) {
				this.nomosik.getLookControl().setLookAt(this.owner, 10.0F, (float) this.nomosik.getMaxHeadXRot());
				this.nomosik.getNavigation().moveTo(this.owner, this.speedModifier);
			}
		}
	}
}
