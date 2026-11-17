package com.nomosiki.faction;

/**
 * Viewer-relative stance for tactical-map banners.
 * Network ids: 0 NEUTRAL, 1 ALLY, 2 WAR, 3 MILITARY_ALLY, 4 FACTION.
 * {@link #OWN} is client-only ({@code owner == local player}) and is never sent.
 * Do not decode these ids through {@link FactionRelationStatus#fromId}.
 */
public enum SquadBannerStance {
	NEUTRAL((byte) 0),
	ALLY((byte) 1),
	WAR((byte) 2),
	MILITARY_ALLY((byte) 3),
	FACTION((byte) 4),
	OWN((byte) 5);

	private final byte id;

	SquadBannerStance(byte id) {
		this.id = id;
	}

	public byte id() {
		return this.id;
	}

	public static SquadBannerStance fromId(byte id) {
		return switch (id) {
			case 1 -> ALLY;
			case 2 -> WAR;
			case 3 -> MILITARY_ALLY;
			case 4 -> FACTION;
			case 5 -> OWN;
			default -> NEUTRAL;
		};
	}

	public static SquadBannerStance fromRelation(FactionRelationStatus status) {
		if (status == null) {
			return NEUTRAL;
		}
		return switch (status) {
			case ALLY -> ALLY;
			case MILITARY_ALLY -> MILITARY_ALLY;
			case AT_WAR -> WAR;
			case NEUTRAL -> NEUTRAL;
		};
	}

	public boolean allowsAttack() {
		return this == WAR || this == NEUTRAL;
	}

	public boolean isFriendly() {
		return this == FACTION || this == ALLY || this == MILITARY_ALLY;
	}
}
