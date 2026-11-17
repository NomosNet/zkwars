package com.nomosiki.entity;

public enum SquadOrder {
	FOLLOW,
	/** Держать позицию блока (якорь — куда смотрит игрок). */
	HOLD,
	/** Идти к блоку по взгляду; по прибытии — удерживать позицию (HOLD). */
	MOVE_TO,
	/** Искать цели и сражаться только в объёме вокруг якоря. */
	ATTACK_ZONE,
	/** Преследовать конкретный вражеский отряд; зона следует за его центром. */
	ATTACK_TARGET;

	public boolean usesAttackVolume() {
		return this == ATTACK_ZONE || this == ATTACK_TARGET;
	}

	public static SquadOrder fromId(byte id) {
		SquadOrder[] values = values();
		if (id < 0 || id >= values.length) {
			return FOLLOW;
		}
		return values[id];
	}

	public byte id() {
		return (byte) ordinal();
	}
}
