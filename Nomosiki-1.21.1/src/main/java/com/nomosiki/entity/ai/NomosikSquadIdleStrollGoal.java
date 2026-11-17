package com.nomosiki.entity.ai;

import com.nomosiki.entity.NomosikEntity;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;

/** Suppresses idle wandering while a formation slot is active. */
public class NomosikSquadIdleStrollGoal extends WaterAvoidingRandomStrollGoal {
	private final NomosikEntity nomosik;

	public NomosikSquadIdleStrollGoal(NomosikEntity nomosik, double speedModifier) {
		super(nomosik, speedModifier);
		this.nomosik = nomosik;
	}

	@Override
	public boolean canUse() {
		if (this.nomosik.getSquadFormationTarget() != null) {
			return false;
		}
		if (this.nomosik.isRidingHeldSeat()) {
			return false;
		}
		return super.canUse();
	}

	@Override
	public boolean canContinueToUse() {
		if (this.nomosik.getSquadFormationTarget() != null) {
			return false;
		}
		if (this.nomosik.isRidingHeldSeat()) {
			return false;
		}
		return super.canContinueToUse();
	}
}
