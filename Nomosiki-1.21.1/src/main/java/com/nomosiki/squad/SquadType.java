package com.nomosiki.squad;

import java.util.List;
import java.util.Locale;

/**
 * Branch label / default inclination for a squad (reserve pools, UI defaults). Squads may contain
 * recruits of any {@link com.nomosiki.entity.NomosikEntity#getSquadBranchType()} when the commander adds them manually.
 * <p>
 * Wire format and {@link com.nomosiki.squad.SquadSavedData NBT "Type"} use explicit byte ids — not {@link Enum#ordinal()},
 * so enum member order can change in source without invalidating worlds or packets.
 * <ul>
 *   <li>0 — {@link #INFANTRY}</li>
 *   <li>1 — {@link #ARCHER}</li>
 *   <li>2 — {@link #KNIGHT}</li>
 *   <li>4 — {@link #NOMAD}</li>
 *   <li>7 — {@link #SAMURAI}</li>
 *   <li>8 — {@link #BRITISH}</li>
 *   <li>9 — {@link #SPANISH}</li>
 *   <li>10 — {@link #SAMURAI_ARCHER}</li>
 *   <li>11 — {@link #SAMURAI_CAVALRY}</li>
 *   <li>12 — {@link #SAMURAI_ARCHER_CAVALRY}</li>
 *   <li>13 — {@link #NINJA}</li>
 *   <li>14 — {@link #MONK}</li>
 *   <li>21 — {@link #MONK_ARCHER}</li>
 *   <li>22 — {@link #MONK_CAVALRY}</li>
 *   <li>15 — {@link #ARTILLERIST}</li>
 *   <li>16 — {@link #SPANISH_ARTILLERIST}</li>
 *   <li>17 — {@link #ASHIGARU_MUSKETEER}</li>
 *   <li>18 — {@link #SAMURAI_MUSKETEER}</li>
 *   <li>19 — {@link #BRITISH_MUSKETEER}</li>
 *   <li>20 — {@link #CAPTAIN}</li>
 * </ul>
 * Legacy fantasy ids 3 (mage), 5 (giant), 6 (spore rider) map to {@link #INFANTRY} on load.
 */
public enum SquadType {
	INFANTRY(0),
	ARCHER(1),
	/** Mounted melee; wire id 2 is stable on disk/network. */
	KNIGHT(2),
	/** Mounted archer. */
	NOMAD(4),
	/** Elite foot melee; wire id 7 is new (do not reuse retired fantasy ids). */
	SAMURAI(7),
	/** Heavy foot melee; wire id 8 is new. */
	BRITISH(8),
	/** Heavy foot melee; wire id 9 is new. */
	SPANISH(9),
	/** Samurai archer; wire id 10. */
	SAMURAI_ARCHER(10),
	/** Samurai mounted melee; wire id 11. */
	SAMURAI_CAVALRY(11),
	/** Samurai mounted archer; wire id 12. */
	SAMURAI_ARCHER_CAVALRY(12),
	/** Ninja archer; wire id 13. */
	NINJA(13),
	/** Monk foot melee; wire id 14. */
	MONK(14),
	/** Monk archer; wire id 21. */
	MONK_ARCHER(21),
	/** Monk mounted melee; wire id 22. */
	MONK_CAVALRY(22),
	/** Ashigaru siege crew; wire id 15. */
	ARTILLERIST(15),
	/** Spanish siege crew; wire id 16. */
	SPANISH_ARTILLERIST(16),
	/** Ashigaru firearm infantry; wire id 17. */
	ASHIGARU_MUSKETEER(17),
	/** Samurai firearm infantry; wire id 18. */
	SAMURAI_MUSKETEER(18),
	/** British firearm infantry; wire id 19. */
	BRITISH_MUSKETEER(19),
	/** Ship helm; wire id 20. */
	CAPTAIN(20);

	/** Highest id written to disk and network; inclusive (includes retired fantasy branches). */
	public static final byte MAX_WIRE_ID = 22;

	private static final List<SquadType> PLAYABLE = List.of(
		INFANTRY, ARCHER, KNIGHT, SAMURAI, SAMURAI_ARCHER, SAMURAI_CAVALRY, SAMURAI_ARCHER_CAVALRY, NOMAD, BRITISH, SPANISH, NINJA, MONK, MONK_ARCHER, MONK_CAVALRY, ARTILLERIST, SPANISH_ARTILLERIST,
		ASHIGARU_MUSKETEER, SAMURAI_MUSKETEER, BRITISH_MUSKETEER, CAPTAIN);

	private final byte wireId;

	SquadType(int wireId) {
		this.wireId = (byte) wireId;
	}

	public static SquadType fromId(byte id) {
		return switch (id) {
			case 1 -> ARCHER;
			case 2 -> KNIGHT;
			case 4 -> NOMAD;
			case 7 -> SAMURAI;
			case 8 -> BRITISH;
			case 9 -> SPANISH;
			case 10 -> SAMURAI_ARCHER;
			case 11 -> SAMURAI_CAVALRY;
			case 12 -> SAMURAI_ARCHER_CAVALRY;
			case 13 -> NINJA;
			case 14 -> MONK;
			case 21 -> MONK_ARCHER;
			case 22 -> MONK_CAVALRY;
			case 15 -> ARTILLERIST;
			case 16 -> SPANISH_ARTILLERIST;
			case 17 -> ASHIGARU_MUSKETEER;
			case 18 -> SAMURAI_MUSKETEER;
			case 19 -> BRITISH_MUSKETEER;
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
		return this == ARTILLERIST || this == SPANISH_ARTILLERIST;
	}

	public boolean isMusketeer() {
		return this == ASHIGARU_MUSKETEER || this == SAMURAI_MUSKETEER || this == BRITISH_MUSKETEER;
	}

	public boolean isCaptain() {
		return this == CAPTAIN;
	}

	/** For {@link net.minecraft.network.chat.Component#translatable(String)} (e.g. GUI and commands). */
	public String translationKey() {
		return switch (this) {
			case KNIGHT, SAMURAI_CAVALRY -> "nomosiki.squad.branch.cavalry";
			case NOMAD -> "nomosiki.squad.branch.nomad";
			case SAMURAI_ARCHER -> "nomosiki.squad.branch.samurai_archer";
			case SAMURAI_ARCHER_CAVALRY -> "nomosiki.squad.branch.samurai_archer_cavalry";
			case NINJA -> "nomosiki.squad.branch.ninja";
			case MONK -> "nomosiki.squad.branch.monk";
			case MONK_ARCHER -> "nomosiki.squad.branch.monk_archer";
			case MONK_CAVALRY -> "nomosiki.squad.branch.monk_cavalry";
			case CAPTAIN -> "nomosiki.squad.branch.captain";
			case ARTILLERIST -> "nomosiki.squad.branch.artillerist";
			case SPANISH_ARTILLERIST -> "nomosiki.squad.branch.spanish_artillerist";
			default -> "nomosiki.squad.branch." + name().toLowerCase(Locale.ROOT);
		};
	}
}
