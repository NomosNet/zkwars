package com.nomosiki.entity;

import java.util.List;
import java.util.Locale;

/**
 * Visual / gameplay lineage for nomosiki. Serialized to NBT and network as a stable byte id (not {@link Enum#ordinal()}).
 * <p>
 * Historical roster is humans only. Legacy fantasy race ids 1–6 still parse and map to {@link #HUMAN}.
 */
public enum NomosikRace {
	HUMAN(0, true);

	/** Inclusive highest id that was ever written to disk/network (legacy fantasy races). */
	public static final byte MAX_WIRE_ID = 6;

	private static final List<NomosikRace> PLAYABLE = List.of(HUMAN);

	/** Vanilla humanoid standing height the {@link net.minecraft.world.entity.EntityType} is built for (see {@link com.nomosiki.registry.ModEntities}). */
	public static final float DEFAULT_STANDING_HEIGHT = 1.8F;

	private final byte wireId;
	private final boolean playable;

	NomosikRace(int wireId, boolean playable) {
		this.wireId = (byte) wireId;
		this.playable = playable;
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
		return HUMAN;
	}

	public byte id() {
		return this.wireId;
	}

	public String translationKey() {
		return "nomosiki.race." + name().toLowerCase(Locale.ROOT);
	}

	public float standingHeightScale() {
		return 1.0F;
	}

	/**
	 * Main tint for {@link net.minecraft.world.item.SpawnEggItem} rows (creative / inventory silhouette).
	 */
	public int spawnEggShellRgb() {
		return 0x8B5A2B;
	}

	/** Spot color on spawn eggs (secondary layer). */
	public int spawnEggSpotRgb() {
		return 0x5D4037;
	}
}
