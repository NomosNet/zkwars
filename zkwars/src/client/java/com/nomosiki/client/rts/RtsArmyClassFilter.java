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
			case INFANTRY -> type == SquadType.INFANTRY || type == SquadType.CAPTAIN;
			case RANGED -> type.isRanged();
			case CAVALRY -> type.isCavalry();
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
