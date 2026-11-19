package com.nomosiki.match;

public enum BuildingHeartType {
	HQ(800.0f),
	BARRACKS(300.0f),
	OUTPOST(250.0f),
	TOWER(200.0f),
	WALL_SECTION(120.0f);

	private final float maxHp;

	BuildingHeartType(float maxHp) {
		this.maxHp = maxHp;
	}

	public float maxHp() {
		return this.maxHp;
	}

	public byte id() {
		return (byte) ordinal();
	}

	public static BuildingHeartType fromId(byte id) {
		BuildingHeartType[] values = values();
		int i = id & 0xFF;
		if (i < 0 || i >= values.length) {
			return HQ;
		}
		return values[i];
	}
}
