package com.nomosiki.entity.ai;

import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.entity.SquadOrder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Moves the nomosik to a server-assigned line-formation slot (see {@link com.nomosiki.squad.SquadBrain}).
 */
public class NomosikSeekFormationGoal extends Goal {
	private static final double MOVE_TO_ANCHOR_DONE_SQR = 10.0;
	private static final int FACING_ALIGN_TICKS = 20;
	private final NomosikEntity nomosik;
	private final RouteWalker route = new RouteWalker();
	private boolean slotFacingAligned;
	private boolean paused;

	public NomosikSeekFormationGoal(NomosikEntity nomosik) {
		this.nomosik = nomosik;
		this.setFlags(EnumSet.of(Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		if (this.nomosik.isRidingHeldSeat()) {
			return false;
		}
		if (this.formationTarget() == null || !this.orderAllowsFormation()) {
			return false;
		}
		// Attack Zone engage: leave the slot — do not tug navigation against melee/ranged chase.
		LivingEntity target = this.nomosik.getTarget();
		if (target != null && NomosikFormationCombat.isAttackZoneEngageTarget(this.nomosik, target)) {
			return false;
		}
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return this.canUse();
	}

	@Override
	public void start() {
		this.slotFacingAligned = false;
		this.paused = false;
		this.route.reset();
	}

	private boolean orderAllowsFormation() {
		SquadOrder o = this.nomosik.getSquadOrder();
		return o == SquadOrder.HOLD
			|| o == SquadOrder.MOVE_TO
			|| o == SquadOrder.ATTACK_ZONE
			|| o == SquadOrder.ATTACK_TARGET
			|| o == SquadOrder.FOLLOW;
	}

	private @Nullable Vec3 formationTarget() {
		return this.nomosik.getSquadFormationTarget();
	}

	@Override
	public void tick() {
		Vec3 target = this.formationTarget();
		if (target == null) {
			return;
		}
		long now = this.nomosik.level().getGameTime();
		if (this.nomosik.isFormationRejectPaused(now)) {
			this.paused = true;
			this.nomosik.getNavigation().stop();
			return;
		}
		if (this.paused) {
			this.paused = false;
			this.route.reset();
		}
		if (this.nomosik.distanceToSqr(target) > NomosikEntity.SQUAD_FORMATION_SLOT_TOL_SQR) {
			this.slotFacingAligned = false;
			if (this.route.tick(this.nomosik, target, 1.08) == RouteWalker.Status.BLOCKED) {
				this.nomosik.rejectCurrentFormationSlot();
			}
			return;
		}
		this.route.reset();
		this.nomosik.getNavigation().stop();
		if (!this.slotFacingAligned) {
			this.nomosik.beginFormationFacing(FACING_ALIGN_TICKS);
			this.slotFacingAligned = true;
		}
		if (this.nomosik.getSquadOrder() == SquadOrder.MOVE_TO && this.nomosik.getSquadAnchorPos() != null) {
			Vec3 anchor = Vec3.atBottomCenterOf(this.nomosik.getSquadAnchorPos());
			if (this.nomosik.distanceToSqr(anchor) <= MOVE_TO_ANCHOR_DONE_SQR) {
				this.nomosik.applySquadOrder(
					SquadOrder.HOLD,
					this.nomosik.getSquadAnchorPos(),
					this.nomosik.getSquadZoneRadiusForGui()
				);
			}
		}
	}

	@Override
	public void stop() {
		this.slotFacingAligned = false;
		this.paused = false;
		this.route.reset();
		this.nomosik.getNavigation().stop();
	}
}
