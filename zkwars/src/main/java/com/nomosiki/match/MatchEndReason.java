package com.nomosiki.match;

public enum MatchEndReason {
	NONE,
	HQ_DESTROYED,
	POINTS,
	FORFEIT,
	FORCE_END;

	public byte id() {
		return (byte) ordinal();
	}

	public static MatchEndReason fromId(byte id) {
		MatchEndReason[] values = values();
		int i = id & 0xFF;
		if (i < 0 || i >= values.length) {
			return NONE;
		}
		return values[i];
	}
}
