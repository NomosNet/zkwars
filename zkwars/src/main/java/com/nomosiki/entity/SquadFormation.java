package com.nomosiki.entity;

import net.minecraft.network.chat.Component;

/**
 * How squad members arrange around the anchor while holding / moving / attacking a zone.
 * Byte ids 0–1 are the original release; 2–8 align with Recruits {@code CommandEvents.applyFormation} order (square … movement).
 */
public enum SquadFormation {
	/** No formation slots; members only use anchor / zone AI. */
	NONE((byte) 0),
	/** Rank(s) perpendicular to the anchor direction (classic battle line). */
	LINE((byte) 1),
	/** Solid square grid (Recruits case 2). */
	SQUARE((byte) 2),
	/** Triangular / wedge ranks (Recruits case 3). */
	TRIANGLE((byte) 3),
	/** Units on a hollow ring (Recruits case 4). */
	HOLLOW_CIRCLE((byte) 4),
	/** Units on a hollow rectangle (Recruits case 5). */
	HOLLOW_SQUARE((byte) 5),
	/** V shape (Recruits case 6). */
	V((byte) 6),
	/** Filled concentric rings (Recruits case 7). */
	CIRCLE((byte) 7),
	/** Short marching line (Recruits case 8). */
	MOVEMENT((byte) 8);

	private final byte id;

	SquadFormation(byte id) {
		this.id = id;
	}

	public byte id() {
		return this.id;
	}

	/**
	 * Resolves a persisted or network id. Unknown ids fall back to {@link #LINE} so older worlds stay playable.
	 */
	public static SquadFormation fromId(byte id) {
		for (SquadFormation f : values()) {
			if (f.id == id) {
				return f;
			}
		}
		return LINE;
	}

	/**
	 * Whether {@link com.nomosiki.squad.SquadBrain} assigns per-member world slot targets for this formation.
	 * {@link #NONE} uses anchor / zone AI only.
	 */
	public boolean assignsWorldSlotTargets() {
		return this != NONE;
	}

	/** Suffix for {@code screen.nomosiki.squad_formations.<suffix>} (buttons, chat). */
	public String formationsScreenSuffix() {
		return switch (this) {
			case NONE -> "loose";
			case LINE -> "line";
			case SQUARE -> "square";
			case TRIANGLE -> "triangle";
			case HOLLOW_CIRCLE -> "hollow_circle";
			case HOLLOW_SQUARE -> "hollow_square";
			case V -> "v";
			case CIRCLE -> "circle";
			case MOVEMENT -> "movement";
		};
	}

	public Component formationsScreenLabel() {
		return Component.translatable("screen.nomosiki.squad_formations." + this.formationsScreenSuffix());
	}

	/** Suffix for {@code screen.nomosiki.nomosik.squad_formation.<suffix>} (recruit GUI stat line). */
	public String nomosikStatSuffix() {
		return this.formationsScreenSuffix();
	}

	public Component nomosikStatLabel() {
		return Component.translatable("screen.nomosiki.nomosik.squad_formation." + this.nomosikStatSuffix());
	}
}
