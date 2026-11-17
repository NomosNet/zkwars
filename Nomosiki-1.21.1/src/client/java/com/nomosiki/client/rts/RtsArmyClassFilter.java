package com.nomosiki.client.rts;

import com.nomosiki.squad.SquadType;
import net.minecraft.network.chat.Component;

public enum RtsArmyClassFilter {
	ALL,
	INFANTRY,
	RANGED,
	CAVALRY;

	public boolean matches(SquadType type) {
		return switch (this) {
			case ALL -> true;
			case INFANTRY -> type == SquadType.INFANTRY || type == SquadType.SAMURAI
				|| type == SquadType.BRITISH || type == SquadType.SPANISH || type == SquadType.MONK
				|| type == SquadType.CAPTAIN;
			case RANGED -> type == SquadType.ARCHER || type == SquadType.SAMURAI_ARCHER
				|| type == SquadType.SAMURAI_ARCHER_CAVALRY || type == SquadType.NOMAD || type == SquadType.NINJA
				|| type == SquadType.MONK_ARCHER
				|| type == SquadType.ARTILLERIST || type == SquadType.SPANISH_ARTILLERIST
				|| type.isMusketeer();
			case CAVALRY -> type == SquadType.KNIGHT || type == SquadType.SAMURAI_CAVALRY
				|| type == SquadType.SAMURAI_ARCHER_CAVALRY || type == SquadType.NOMAD
				|| type == SquadType.MONK_CAVALRY;
		};
	}

	public RtsArmyClassFilter next() {
		RtsArmyClassFilter[] values = values();
		return values[(ordinal() + 1) % values.length];
	}

	public String translationKey() {
		return "nomosiki.rts.class_filter." + name().toLowerCase();
	}

	public Component label() {
		return Component.translatable(translationKey());
	}
}
