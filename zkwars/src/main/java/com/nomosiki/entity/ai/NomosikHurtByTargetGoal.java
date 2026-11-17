package com.nomosiki.entity.ai;

import com.nomosiki.entity.NomosikCombatMode;
import com.nomosiki.entity.NomosikEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;

public class NomosikHurtByTargetGoal extends HurtByTargetGoal {
	private final NomosikEntity nomosik;

	public NomosikHurtByTargetGoal(NomosikEntity nomosik) {
		super(nomosik);
		this.nomosik = nomosik;
		setAlertOthers();
	}

	@Override
	public boolean canUse() {
		if (this.nomosik.getCombatMode() == NomosikCombatMode.PASSIVE) {
			return false;
		}
		if (!super.canUse()) {
			return false;
		}
		return this.isRevengeTarget(this.nomosik.getLastHurtByMob());
	}

	/**
	 * Vanilla drops the target past {@code FOLLOW_RANGE}. A shot from outside that
	 * range still counts while the attacker stays inside the revenge radius.
	 */
	@Override
	public boolean canContinueToUse() {
		if (this.nomosik.getCombatMode() == NomosikCombatMode.PASSIVE) {
			return false;
		}
		LivingEntity attacker = this.nomosik.getLastHurtByMob();
		if (this.isRevengeTarget(attacker) && this.nomosik.distanceToSqr(attacker) <= NomosikEntity.REVENGE_RANGE_SQR) {
			this.nomosik.setTarget(attacker);
			return true;
		}
		return super.canContinueToUse();
	}

	private boolean isRevengeTarget(LivingEntity attacker) {
		if (attacker == null || this.nomosik.isAllyOrSelf(attacker)) {
			return false;
		}
		return this.nomosik.canApplyAutoTarget(attacker);
	}
}
