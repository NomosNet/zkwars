package com.nomosiki.squad;

import java.util.List;
import java.util.Locale;

/**
 * Branch label for a squad. Wire format and {@link com.nomosiki.squad.SquadSavedData NBT "Type"} use
 * explicit byte ids — not {@link Enum#ordinal()}.
 * <ul>
 *   <li>0 — {@link #INFANTRY}</li>
 *   <li>1 — {@link #ARCHER}</li>
 *   <li>2 — {@link #KNIGHT}</li>
 *   <li>4 — {@link #NOMAD}</li>
 *   <li>15 — {@link #ARTILLERIST}</li>
 *   <li>20 — {@link #CAPTAIN}</li>
 * </ul>
 * Retired culture ids fold onto these roles: samurai and other foot cultures to infantry, archers and
 * musketeers to archer, mounted melee to knight, mounted archers to nomad, siege crews to artillerist.
 * Legacy fantasy ids 3, 5 and 6 also map to {@link #INFANTRY}.
 */
public enum SquadType {
	INFANTRY(0),
	ARCHER(1),
	/** Mounted melee; wire id 2 is stable on disk/network. */
	KNIGHT(2),
	/** Mounted archer. */
	NOMAD(4),
	/** Siege crew; wire id 15. */
	ARTILLERIST(15),
	/** Ship helm; wire id 20. */
	CAPTAIN(20);

	/** Highest id that was ever written to disk and network; inclusive. */
	public static final byte MAX_WIRE_ID = 22;

	private static final List<SquadType> PLAYABLE = List.of(
		INFANTRY, ARCHER, KNIGHT, NOMAD, ARTILLERIST, CAPTAIN);

	private final byte wireId;

	SquadType(int wireId) {
		this.wireId = (byte) wireId;
	}

	public static SquadType fromId(byte id) {
		return switch (id) {
			case 1, 10, 13, 17, 18, 19, 21 -> ARCHER;
			case 2, 11, 22 -> KNIGHT;
			case 4, 12 -> NOMAD;
			case 15, 16 -> ARTILLERIST;
			case 20 -> CAPTAIN;
			default -> INFANTRY;
		};
	}

	public byte id() {
		return this.wireId;
	}

	public static List<SquadType> playableValues() {
		return PLAYABLE;
	}

	public boolean isPlayable() {
		return true;
	}

	public boolean isArtillerist() {
		return this == ARTILLERIST;
	}

	public boolean isRanged() {
		return this == ARCHER || this == NOMAD || this == ARTILLERIST;
	}

	public boolean isCavalry() {
		return this == KNIGHT || this == NOMAD;
	}

	public boolean isCaptain() {
		return this == CAPTAIN;
	}

	/** For {@link net.minecraft.network.chat.Component#translatable(String)} (e.g. GUI and commands). */
	public String translationKey() {
		return switch (this) {
			case KNIGHT -> "nomosiki.squad.branch.cavalry";
			case NOMAD -> "nomosiki.squad.branch.nomad";
			case CAPTAIN -> "nomosiki.squad.branch.captain";
			case ARTILLERIST -> "nomosiki.squad.branch.artillerist";
			default -> "nomosiki.squad.branch." + name().toLowerCase(Locale.ROOT);
		};
	}
}
