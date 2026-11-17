package com.nomosiki.faction;

/** Stance between two factions (symmetric, excluding pending alliance). */
public enum FactionRelationStatus {
	NEUTRAL,
	/** Regular alliance: friendly fire off, no automatic war cascade. */
	ALLY,
	AT_WAR,
	/** Military alliance: friendly fire off + war cascade. */
	MILITARY_ALLY;

	public static FactionRelationStatus fromId(byte id) {
		FactionRelationStatus[] values = values();
		if (id < 0 || id >= values.length) {
			return NEUTRAL;
		}
		return values[id];
	}

	public byte id() {
		return (byte) ordinal();
	}

	public boolean isAlliance() {
		return this == ALLY || this == MILITARY_ALLY;
	}

	public boolean isRegularAlliance() {
		return this == ALLY;
	}

	public boolean isMilitaryAlliance() {
		return this == MILITARY_ALLY;
	}
}
