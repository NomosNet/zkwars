package com.nomosiki.entity;

public enum NomosikRangedFireMode {
	HOLD_FIRE,
	FIRE_AT_WILL,
	STRATEGIC_FIRE;

	public static NomosikRangedFireMode fromId(byte id) {
		NomosikRangedFireMode[] values = values();
		if (id < 0 || id >= values.length) {
			return FIRE_AT_WILL;
		}
		return values[id];
	}

	public byte id() {
		return (byte) ordinal();
	}

	public NomosikRangedFireMode toggleHoldFire() {
		return this == HOLD_FIRE ? FIRE_AT_WILL : HOLD_FIRE;
	}
}
