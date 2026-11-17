package com.nomosiki.client;

import com.nomosiki.NomosikiMod;
import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.entity.NomosikRace;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.Locale;

/**
 * One skin file per role and race: {@code textures/entity/race/<role>/<race>.png}.
 * Copies start identical so each file can be redrawn on its own later.
 */
public final class NomosikTextures {
	private static final String PREFIX = "textures/entity/race/";

	/** Subfolder per unit role; file name is {@code <race>.png}. */
	public enum Profile {
		DEFAULT("infantry"),
		ARCHER("archer"),
		NOMAD("nomad"),
		KNIGHT("knight"),
		ARTILLERIST("artillerist"),
		CAPTAIN("captain");

		private final String dir;

		Profile(String dir) {
			this.dir = dir;
		}

		String dir() {
			return this.dir;
		}
	}

	private NomosikTextures() {
	}

	public static ResourceLocation skin(NomosikRace race, Profile profile) {
		String racePath = race.name().toLowerCase(Locale.ROOT);
		return NomosikiMod.id(PREFIX + profile.dir() + "/" + racePath + ".png");
	}

	public static ResourceLocation resolveSkin(ResourceManager resources, NomosikEntity entity, Profile profile) {
		return resolveSkin(resources, entity.getNomosikRace(), profile);
	}

	public static ResourceLocation resolveSkin(ResourceManager resources, NomosikRace race, Profile profile, int variantIndex) {
		return resolveSkin(resources, race, profile);
	}

	public static ResourceLocation resolveSkin(ResourceManager resources, NomosikRace race, Profile profile) {
		ResourceLocation primary = skin(race, profile);
		if (resources.getResource(primary).isPresent()) {
			return primary;
		}
		ResourceLocation human = skin(NomosikRace.HUMAN, profile);
		if (resources.getResource(human).isPresent()) {
			return human;
		}
		return primary;
	}
}
