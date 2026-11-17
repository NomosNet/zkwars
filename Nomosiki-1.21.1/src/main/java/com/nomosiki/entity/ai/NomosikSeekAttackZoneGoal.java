package com.nomosiki.entity.ai;

import com.nomosiki.entity.SquadOrder;
import com.nomosiki.entity.NomosikEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class NomosikSeekAttackZoneGoal extends Goal {
	private static final double CLOSE_ENOUGH_SQR = 12.0;
	private final NomosikEntity nomosik;
	private final ApproachRoute route = new ApproachRoute();

	public NomosikSeekAttackZoneGoal(NomosikEntity nomosik) {
		this.nomosik = nomosik;
		this.setFlags(EnumSet.of(Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		if (this.nomosik.isRidingHeldSeat()) {
			return false;
		}
		if (this.nomosik.getSquadOrder() != SquadOrder.ATTACK_ZONE
			&& this.nomosik.getSquadOrder() != SquadOrder.ATTACK_TARGET) {
			return false;
		}
		if (this.nomosik.getSquadFormationTarget() != null) {
			return false;
		}
		if (this.nomosik.getTarget() != null && this.nomosik.getTarget().isAlive()) {
			return false;
		}
		@Nullable AABB zone = this.nomosik.getSquadAttackZone();
		if (zone == null) {
			return false;
		}
		Vec3 center = zone.getCenter();
		return this.nomosik.distanceToSqr(center) > CLOSE_ENOUGH_SQR;
	}

	@Override
	public boolean canContinueToUse() {
		return this.canUse();
	}

	@Override
	public void tick() {
		AABB zone = this.nomosik.getSquadAttackZone();
		if (zone == null) {
			return;
		}
		this.route.tick(this.nomosik, zone.getCenter(), 1.14);
	}

	@Override
	public void stop() {
		this.route.reset(this.nomosik);
	}
}
