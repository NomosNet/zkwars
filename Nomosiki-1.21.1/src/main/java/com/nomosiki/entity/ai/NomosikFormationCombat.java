package com.nomosiki.entity.ai;

import com.nomosiki.entity.NomosikAttackReach;
import com.nomosiki.entity.NomosikEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * Formation-aware combat rules (Recruits {@code holdFormation} / {@code isInFormation}).
 */
public final class NomosikFormationCombat {
	private static final double CHASE_REACH_MULTIPLIER = 1.5;

	private NomosikFormationCombat() {
	}

	/**
	 * Whether melee AI may chase or engage the current target.
	 * When holding formation, only fight targets already within extended reach —
	 * except an attack-zone target, or the mob who hurt this unit recently.
	 */
	public static boolean mayChaseTargetForMelee(NomosikEntity self) {
		if (!self.isSquadInFormation() || !self.isSquadHoldFormation()) {
			return true;
		}
		LivingEntity target = self.getTarget();
		if (target == null) {
			return false;
		}
		if (self.isRecentAttacker(target) || isAttackZoneEngageTarget(self, target)) {
			return true;
		}
		double reachSqr = NomosikAttackReach.attackReachSqr(self, target);
		return self.distanceToSqr(target) <= reachSqr * CHASE_REACH_MULTIPLIER;
	}

	/**
	 * Ranged units stay on their slot instead of walking toward the target,
	 * unless Attack Zone has an engage target inside the ordered AABB
	 * or the target is the mob who hurt this unit recently.
	 */
	public static boolean shouldHoldFormationSlot(NomosikEntity self) {
		if (!self.isSquadInFormation()
			|| !self.isSquadHoldFormation()
			|| self.getSquadFormationTarget() == null) {
			return false;
		}
		LivingEntity target = self.getTarget();
		if (target != null && (self.isRecentAttacker(target) || isAttackZoneEngageTarget(self, target))) {
			return false;
		}
		return true;
	}

	/**
	 * Attack Zone overrides hold-formation: chase/reposition for a living target inside the zone.
	 */
	public static boolean isAttackZoneEngageTarget(NomosikEntity self, LivingEntity target) {
		if (!self.getSquadOrder().usesAttackVolume()) {
			return false;
		}
		if (!target.isAlive() || target.isRemoved()) {
			return false;
		}
		@Nullable AABB zone = self.getSquadAttackZone();
		if (zone == null) {
			return false;
		}
		var p = target.position();
		return zone.contains(p.x, p.y, p.z);
	}
}
