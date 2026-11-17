package com.nomosiki.entity.ai;

import com.nomosiki.compat.gun.GunsCompat;
import com.nomosiki.entity.NomosikAttackReach;
import com.nomosiki.entity.NomosikEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

public class MusketeerBayonetMeleeGoal extends NomosikMeleeAttackGoal {
	private static final double BAYONET_RANGE_MULTIPLIER = 1.5;

	public MusketeerBayonetMeleeGoal(PathfinderMob mob) {
		super(mob, 1.05, true);
	}

	@Override
	public boolean canUse() {
		return this.inBayonetRange() && super.canUse();
	}

	@Override
	public boolean canContinueToUse() {
		return this.inBayonetRange() && super.canContinueToUse();
	}

	private boolean inBayonetRange() {
		if (!GunsCompat.isHoldingBayonet(this.mob)) {
			return false;
		}
		LivingEntity target = this.mob.getTarget();
		if (target == null || !target.isAlive()) {
			return false;
		}
		double reachSqr = this.mob instanceof NomosikEntity nomosik
			? NomosikAttackReach.attackReachSqr(nomosik, target)
			: Math.pow(this.mob.getBbWidth() * 2.0 + target.getBbWidth(), 2);
		return this.mob.distanceToSqr(target) <= reachSqr * BAYONET_RANGE_MULTIPLIER;
	}
}
