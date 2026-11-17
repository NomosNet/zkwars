package com.nomosiki.client;

import com.nomosiki.NomosikiMod;
import com.nomosiki.entity.NomosikCulture;
import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.entity.NomosikRace;
import com.nomosiki.faction.FactionColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;

/**
 * Player-style skins per culture. Placeholders live under
 * {@code assets/nomosiki/textures/entity/race/}.
 * <p>
 * Unfactioned humans use a single {@code human.png} per {@link Profile} (no numbered variants).
 */
public final class NomosikTextures {
	private static final String PREFIX = "textures/entity/race/";

	/** Subfolder per unit branch; file name is {@code <race>.png} (e.g. {@code human.png}). */
	public enum Profile {
		DEFAULT("default"),
		ARCHER("archer"),
		NOMAD("nomad"),
		KNIGHT("knight"),
		BRITISH("british"),
		SPANISH("spanish"),
		NINJA("ninja"),
		MONK("monk");

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

	/**
	 * Full-body faction skin for the unit's culture and color.
	 * Ashigaru/samurai/captain use {@code asigaru/}, {@code samurai/}, and {@code captain/} color PNGs.
	 * British/Spanish/ninja/monk use matching color PNG folders when present; otherwise {@code null}
	 * so the overlay layer can tint the base skin.
	 */
	@Nullable
	public static ResourceLocation resolveFactionUnitSkin(NomosikEntity entity, ResourceManager resources) {
		return switch (entity.getUnitCulture()) {
			case ASHIGARU -> presentOrNull(resources, resolveFactionColor(
				entity, FactionColor::ashigaruTexture, FactionClientState::getAshigaruTextureForPlayer));
			case SAMURAI -> presentOrNull(resources, resolveFactionColor(
				entity, FactionColor::samuraiTexture, FactionClientState::getSamuraiTextureForPlayer));
			case BRITISH -> presentOrNull(resources, resolveFactionColor(
				entity, FactionColor::britishTexture, FactionClientState::getBritishTextureForPlayer));
			case SPANISH -> presentOrNull(resources, resolveFactionColor(
				entity, FactionColor::spanishTexture, FactionClientState::getSpanishTextureForPlayer));
			case NINJA -> presentOrNull(resources, resolveFactionColor(
				entity, FactionColor::ninjaTexture, FactionClientState::getNinjaTextureForPlayer));
			case MONK -> presentOrNull(resources, resolveFactionColor(
				entity, FactionColor::monkTexture, FactionClientState::getMonkTextureForPlayer));
			case CAPTAIN -> presentOrNull(resources, resolveFactionColor(
				entity, FactionColor::captainTexture, FactionClientState::getCaptainTextureForPlayer));
		};
	}

	/** @see #resolveFactionUnitSkin(NomosikEntity, ResourceManager) without a resource check (layer skip). */
	@Nullable
	public static ResourceLocation resolveFactionUnitSkin(NomosikEntity entity) {
		return switch (entity.getUnitCulture()) {
			case ASHIGARU -> resolveFactionColor(
				entity, FactionColor::ashigaruTexture, FactionClientState::getAshigaruTextureForPlayer);
			case SAMURAI -> resolveFactionColor(
				entity, FactionColor::samuraiTexture, FactionClientState::getSamuraiTextureForPlayer);
			case BRITISH -> resolveFactionColor(
				entity, FactionColor::britishTexture, FactionClientState::getBritishTextureForPlayer);
			case SPANISH -> resolveFactionColor(
				entity, FactionColor::spanishTexture, FactionClientState::getSpanishTextureForPlayer);
			case NINJA -> resolveFactionColor(
				entity, FactionColor::ninjaTexture, FactionClientState::getNinjaTextureForPlayer);
			case MONK -> resolveFactionColor(
				entity, FactionColor::monkTexture, FactionClientState::getMonkTextureForPlayer);
			case CAPTAIN -> resolveFactionColor(
				entity, FactionColor::captainTexture, FactionClientState::getCaptainTextureForPlayer);
		};
	}

	@Nullable
	private static ResourceLocation presentOrNull(ResourceManager resources, @Nullable ResourceLocation location) {
		if (location == null) {
			return null;
		}
		return resources.getResource(location).isPresent() ? location : null;
	}

	@Nullable
	private static ResourceLocation resolveFactionColor(
		NomosikEntity entity,
		Function<FactionColor, ResourceLocation> fromColor,
		Function<UUID, ResourceLocation> fromOwner
	) {
		FactionColor color = entity.getGuiPreviewFactionColor();
		if (color == null) {
			color = entity.getFactionColorOverride();
		}
		if (color != null) {
			return fromColor.apply(color);
		}
		UUID ownerId = entity.getOwnerUUID();
		return ownerId == null ? null : fromOwner.apply(ownerId);
	}

	public static ResourceLocation skin(NomosikRace race, Profile profile) {
		String racePath = race.name().toLowerCase(Locale.ROOT);
		return NomosikiMod.id(PREFIX + profile.dir() + "/" + racePath + ".png");
	}

	@Nullable
	public static ResourceLocation cultureDefaultTexture(NomosikCulture culture) {
		return switch (culture) {
			case ASHIGARU -> FactionColor.RED.ashigaruTexture();
			case SAMURAI -> FactionColor.RED.samuraiTexture();
			case BRITISH -> skin(NomosikRace.HUMAN, Profile.BRITISH);
			case SPANISH -> skin(NomosikRace.HUMAN, Profile.SPANISH);
			case NINJA -> FactionColor.RED.ninjaTexture();
			case MONK -> FactionColor.RED.monkTexture();
			case CAPTAIN -> FactionColor.RED.captainTexture();
		};
	}

	/**
	 * Unfactioned look: culture default (ashigaru/samurai/ninja/monk/captain red, british/spanish {@code human.png}),
	 * else {@code <profile>/human.png}.
	 */
	public static ResourceLocation resolveSkin(ResourceManager resources, NomosikEntity entity, Profile profile) {
		ResourceLocation cultureDefault = cultureDefaultTexture(entity.getUnitCulture());
		if (cultureDefault != null && resources.getResource(cultureDefault).isPresent()) {
			return cultureDefault;
		}
		ResourceLocation fromProfile = resolveSkin(resources, entity.getNomosikRace(), profile, 0);
		if (resources.getResource(fromProfile).isPresent()) {
			return fromProfile;
		}
		if (entity.getUnitCulture() == NomosikCulture.MONK) {
			ResourceLocation samurai = FactionColor.RED.samuraiTexture();
			if (samurai != null && resources.getResource(samurai).isPresent()) {
				return samurai;
			}
		}
		return fromProfile;
	}

	/**
	 * Resolution order: top-level {@code <race>.png}, then {@code human.png} fallback.
	 */
	public static ResourceLocation resolveSkin(ResourceManager resources, NomosikRace race, Profile profile, int variantIndex) {
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
