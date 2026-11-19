package com.nomosiki.match;

public enum MatchPhase {
	IDLE,
	LOBBY,
	COUNTDOWN,
	RUNNING,
	ENDED;

	public byte id() {
		return (byte) ordinal();
	}

	public static MatchPhase fromId(byte id) {
		MatchPhase[] values = values();
		int i = id & 0xFF;
		if (i < 0 || i >= values.length) {
			return IDLE;
		}
		return values[i];
	}
}
