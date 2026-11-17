package com.nomosiki.faction;

/** Target alliance tier for a pending request or formed relation. */
public enum FactionAllianceTier {
	REGULAR,
	MILITARY;

	public static FactionAllianceTier fromId(byte id) {
		FactionAllianceTier[] values = values();
		if (id < 0 || id >= values.length) {
			return REGULAR;
		}
		return values[id];
	}

	public byte id() {
		return (byte) ordinal();
	}

	public FactionRelationStatus toRelationStatus() {
		return this == MILITARY ? FactionRelationStatus.MILITARY_ALLY : FactionRelationStatus.ALLY;
	}
}
