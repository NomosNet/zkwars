package com.nomosiki.entity;

import net.minecraft.world.entity.LivingEntity;

/**
 * Melee reach helper aligned with vanilla {@link net.minecraft.world.entity.ai.goal.MeleeAttackGoal} sizing.
 */
public final class NomosikAttackReach {
	private NomosikAttackReach() {
	}

	public static double attackReachSqr(NomosikEntity self, LivingEntity target) {
		double reach = self.getBbWidth() * 2.0 + target.getBbWidth();
		return reach * reach;
	}
}
