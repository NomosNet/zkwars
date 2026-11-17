package com.nomosiki.entity;

import com.nomosiki.combat.CombatRelation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

final class NomosikCombatLogic {
	private NomosikCombatLogic() {
	}

	/** Атакующий — владелец, другой воин того же владельца, союзный маунт или союзник по фракции. */
	static boolean isAllyOrSelf(NomosikEntity entity, LivingEntity other) {
		UUID ownerId = entity.getOwnerUUID();
		if (ownerId == null) {
			return false;
		}
		if (other instanceof Player p && ownerId.equals(p.getUUID())) {
			return true;
		}
		if (other instanceof NomosikEntity ally && ownerId.equals(ally.getOwnerUUID())) {
			return true;
		}
		if (NomosikiHorseMount.isNomosikiMountEntity(other)) {
			Entity passenger = other.getFirstPassenger();
			if (passenger instanceof LivingEntity living) {
				return isAllyOrSelf(entity, living);
			}
			return true;
		}
		return CombatRelation.isFactionAlly(entity, other);
	}

	static boolean isAllyDamage(NomosikEntity entity, DamageSource source) {
		Entity direct = source.getDirectEntity();
		if (direct instanceof LivingEntity living && isAllyOrSelf(entity, living)) {
			return true;
		}
		Entity attacker = source.getEntity();
		return attacker instanceof LivingEntity living && isAllyOrSelf(entity, living);
	}
}
