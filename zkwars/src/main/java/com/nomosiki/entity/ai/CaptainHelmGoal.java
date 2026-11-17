package com.nomosiki.entity.ai;

import com.nomosiki.advancement.NomosikiActions;
import com.nomosiki.advancement.NomosikiAdvancements;
import com.nomosiki.compat.ship.ZkShipCompat;
import com.nomosiki.entity.CaptainEntity;
import com.nomosiki.entity.SquadOrder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Walks a captain onto a free helm, then sails by the current squad order.
 */
public class CaptainHelmGoal extends Goal {
	public static final double SEARCH_RADIUS = 48.0;
	public static final double CLAIM_REACH = 4.0;
	private static final double STOP_DISTANCE = 10.0;

	private final CaptainEntity captain;

	public CaptainHelmGoal(CaptainEntity captain) {
		this.captain = captain;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (!ZkShipCompat.isLoaded() || !this.captain.wantsHelm()) {
			return false;
		}
		if (ZkShipCompat.isHelmPilot(this.captain)) {
			return true;
		}
		if (!(this.captain.level() instanceof ServerLevel level)) {
			return false;
		}
		return ZkShipCompat.shipNearby(level, this.captain.position(), SEARCH_RADIUS);
	}

	@Override
	public boolean canContinueToUse() {
		return this.canUse();
	}

	@Override
	public void stop() {
		if (!ZkShipCompat.isHelmPilot(this.captain)) {
			this.captain.getNavigation().stop();
		}
	}

	@Override
	public void tick() {
		if (ZkShipCompat.isHelmPilot(this.captain)) {
			this.captain.getNavigation().stop();
			this.steer();
			return;
		}
		if (!(this.captain.level() instanceof ServerLevel level)) {
			return;
		}
		Vec3 helm = ZkShipCompat.nearestFreeHelm(level, this.captain.position(), SEARCH_RADIUS);
		if (helm == null) {
			this.captain.getNavigation().stop();
			return;
		}
		if (this.captain.distanceToSqr(helm) > CLAIM_REACH * CLAIM_REACH) {
			this.captain.getNavigation().moveTo(helm.x, helm.y, helm.z, 1.05);
			return;
		}
		this.captain.getNavigation().stop();
		if (ZkShipCompat.claimNearbyHelm(this.captain, CLAIM_REACH) && this.captain.getOwnerUUID() != null) {
			NomosikiAdvancements.grant(level.getServer(), this.captain.getOwnerUUID(), NomosikiActions.HELM);
		}
	}

	private void steer() {
		Vec3 target = this.courseTarget();
		float forward = 0.0F;
		if (target != null) {
			double dx = target.x - this.captain.getX();
			double dz = target.z - this.captain.getZ();
			double dist = Math.sqrt(dx * dx + dz * dz);
			float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
			this.captain.setYRot(yaw);
			this.captain.yRotO = yaw;
			this.captain.setYHeadRot(yaw);
			if (dist > STOP_DISTANCE) {
				forward = 1.0F;
			}
		}
		ZkShipCompat.steerHelm(this.captain, forward);
	}

	private Vec3 courseTarget() {
		SquadOrder order = this.captain.getSquadOrder();
		if (order == SquadOrder.HOLD) {
			return null;
		}
		if (order == SquadOrder.FOLLOW) {
			Player owner = this.captain.getOwner();
			return owner == null ? null : owner.position();
		}
		if (order == SquadOrder.MOVE_TO || order.usesAttackVolume()) {
			BlockPos anchor = this.captain.getSquadAnchorPos();
			return anchor == null ? null : Vec3.atCenterOf(anchor);
		}
		return null;
	}
}
