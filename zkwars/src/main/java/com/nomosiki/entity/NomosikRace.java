package com.nomosiki.entity;

import java.util.List;
import java.util.Locale;

/**
 * Visual / gameplay lineage for nomosiki. Serialized to NBT and network as a stable byte id (not {@link Enum#ordinal()}).
 * <p>
 * Legacy fantasy race ids 1–6 still parse and map to {@link #HUMAN}. Orc and dwarf use new ids.
 */
public enum NomosikRace {
	HUMAN(0, true, 1.0F, 0x8B5A2B, 0x5D4037),
	ORC(7, true, 1.0F, 0x4C7A3A, 0x2E4A22),
	DWARF(8, true, 0.75F, 0x8D6E63, 0x5D4037);

	/** Inclusive highest id that was ever written to disk/network. */
	public static final byte MAX_WIRE_ID = 8;

	private static final List<NomosikRace> PLAYABLE = List.of(HUMAN, ORC, DWARF);

	/** Vanilla humanoid standing height the {@link net.minecraft.world.entity.EntityType} is built for (see {@link com.nomosiki.registry.ModEntities}). */
	public static final float DEFAULT_STANDING_HEIGHT = 1.8F;

	private final byte wireId;
	private final boolean playable;
	private final float standingHeightScale;
	private final int spawnEggShellRgb;
	private final int spawnEggSpotRgb;

	NomosikRace(int wireId, boolean playable, float standingHeightScale, int spawnEggShellRgb, int spawnEggSpotRgb) {
		this.wireId = (byte) wireId;
		this.playable = playable;
		this.standingHeightScale = standingHeightScale;
		this.spawnEggShellRgb = spawnEggShellRgb;
		this.spawnEggSpotRgb = spawnEggSpotRgb;
	}

	public static List<NomosikRace> playableValues() {
		return PLAYABLE;
	}

	public boolean isPlayable() {
		return this.playable;
	}

	/** Maps disabled / unknown races to {@link #HUMAN} for new spawns and live entities. */
	public static NomosikRace normalizeForPlay(NomosikRace race) {
		return race != null && race.isPlayable() ? race : HUMAN;
	}

	public static NomosikRace fromId(byte id) {
		return switch (id) {
			case 7 -> ORC;
			case 8 -> DWARF;
			default -> HUMAN;
		};
	}

	public byte id() {
		return this.wireId;
	}

	public String translationKey() {
		return "nomosiki.race." + name().toLowerCase(Locale.ROOT);
	}

	public float standingHeightScale() {
		return this.standingHeightScale;
	}

	/**
	 * Main tint for {@link net.minecraft.world.item.SpawnEggItem} rows (creative / inventory silhouette).
	 */
	public int spawnEggShellRgb() {
		return this.spawnEggShellRgb;
	}

	/** Spot color on spawn eggs (secondary layer). */
	public int spawnEggSpotRgb() {
		return this.spawnEggSpotRgb;
	}
}
