package com.nomosiki.client;

import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.entity.NomosikRace;
import com.nomosiki.entity.SamuraiEntity;
import com.nomosiki.faction.FactionColor;
import com.nomosiki.registry.ModEntities;
import com.nomosiki.squad.SamuraiGearLoadout;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/** Client-only nomosik models for faction GUI previews (not spawned in the world). */
public final class FactionPreviewEntities {
	private FactionPreviewEntities() {
	}

	@Nullable
	public static NomosikEntity create(Minecraft minecraft, String factionName) {
		return create(minecraft, factionName, null);
	}

	@Nullable
	public static NomosikEntity create(Minecraft minecraft, String factionName, @Nullable String colorId) {
		Level level = minecraft.level;
		if (level == null) {
			return null;
		}
		NomosikEntity entity = new NomosikEntity(ModEntities.NOMOSIK, level);
		entity.configureGuiPreview(NomosikRace.HUMAN, skinVariantFor(factionName), colorFromId(colorId));
		return entity;
	}

	/** Standing samurai in dyed steel armor from {@code zk-nomosiki-items}, for diplomacy portraits. */
	@Nullable
	public static NomosikEntity createSamuraiPortrait(
		Minecraft minecraft,
		String factionName,
		@Nullable String colorId
	) {
		Level level = minecraft.level;
		if (level == null) {
			return null;
		}
		SamuraiEntity samurai = new SamuraiEntity(ModEntities.SAMURAI, level);
		FactionColor color = colorFromId(colorId);
		samurai.configureGuiPreview(NomosikRace.HUMAN, skinVariantFor(factionName), color);
		if (SamuraiGearLoadout.isAvailable()) {
			SamuraiGearLoadout.equip(samurai, color);
		}
		return samurai;
	}

	@Nullable
	public static FactionColor colorFromId(@Nullable String colorId) {
		if (colorId == null || colorId.isBlank()) {
			return null;
		}
		return FactionColor.fromId(colorId);
	}

	public static int skinVariantFor(String factionName) {
		if (factionName == null || factionName.isBlank()) {
			return 0;
		}
		return Math.floorMod(factionName.toLowerCase(Locale.ROOT).hashCode(), NomosikEntity.HUMAN_SKIN_VARIANT_COUNT);
	}
}
