package com.nomosiki.compat.emote;

import com.nomosiki.compat.melodies.NomosikImmersiveMelodiesCompat;
import com.nomosiki.entity.NomosikEntity;
import net.minecraft.world.entity.Entity;

/**
 * Humanoid {@code NomosikHumanoidModel} emotes only.
 */
public final class NomosikEmoteEligibility {
	private NomosikEmoteEligibility() {
	}

	public static boolean canPlayPlayerModelEmote(Entity entity) {
		if (!(entity instanceof NomosikEntity nomosik)) {
			return false;
		}
		return canPlayPlayerModelEmote(nomosik);
	}

	public static boolean canPlayPlayerModelEmote(NomosikEntity entity) {
		if (entity.isRemoved() || !entity.isAlive()) {
			return false;
		}
		return !NomosikImmersiveMelodiesCompat.isHoldingInstrument(entity);
	}
}
