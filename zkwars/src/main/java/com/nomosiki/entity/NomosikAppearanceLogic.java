package com.nomosiki.entity;

import com.nomosiki.faction.FactionColor;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

final class NomosikAppearanceLogic {
	private NomosikAppearanceLogic() {
	}

	static NomosikRace getNomosikRace(NomosikEntity entity) {
		return NomosikRace.fromId(entity.getEntityData().get(NomosikEntity.DATA_NOMOSIK_RACE));
	}

	static int getHumanSkinVariant(NomosikEntity entity) {
		int v = entity.getEntityData().get(NomosikEntity.DATA_HUMAN_SKIN_VARIANT);
		if (v >= 0) {
			return Math.floorMod(v, NomosikEntity.HUMAN_SKIN_VARIANT_COUNT);
		}
		return Math.floorMod(entity.getUUID().hashCode(), NomosikEntity.HUMAN_SKIN_VARIANT_COUNT);
	}

	static void setHumanSkinVariant(NomosikEntity entity, int index) {
		if (entity.level().isClientSide()) {
			return;
		}
		entity.getEntityData().set(NomosikEntity.DATA_HUMAN_SKIN_VARIANT, Math.floorMod(index, NomosikEntity.HUMAN_SKIN_VARIANT_COUNT));
	}

	static void rollHumanSkinIfNeeded(NomosikEntity entity) {
		if (entity.level().isClientSide()) {
			return;
		}
		if (entity.getEntityData().get(NomosikEntity.DATA_HUMAN_SKIN_VARIANT) < 0) {
			entity.getEntityData().set(NomosikEntity.DATA_HUMAN_SKIN_VARIANT, entity.getRandom().nextInt(NomosikEntity.HUMAN_SKIN_VARIANT_COUNT));
		}
	}

	static void configureGuiPreview(NomosikEntity entity, NomosikRace race, int humanSkinVariant, @Nullable FactionColor factionColor) {
		if (!entity.level().isClientSide()) {
			return;
		}
		entity.guiPreview = true;
		entity.guiPreviewFactionColor = factionColor;
		entity.getEntityData().set(NomosikEntity.DATA_NOMOSIK_RACE, NomosikRace.normalizeForPlay(race).id());
		entity.getEntityData().set(NomosikEntity.DATA_HUMAN_SKIN_VARIANT, Math.floorMod(humanSkinVariant, NomosikEntity.HUMAN_SKIN_VARIANT_COUNT));
		entity.refreshDimensions();
	}

	static void setNomosikRace(NomosikEntity entity, NomosikRace race) {
		if (entity.level().isClientSide()) {
			return;
		}
		entity.getEntityData().set(NomosikEntity.DATA_NOMOSIK_RACE, NomosikRace.normalizeForPlay(race).id());
		entity.refreshDimensions();
		if (entity.level() instanceof ServerLevel serverLevel) {
			NomosikiHorseMount.ensureMountMatchesRole(entity, serverLevel);
		}
		rollHumanSkinIfNeeded(entity);
	}

	static void tickServer(NomosikEntity entity) {
		rollHumanSkinIfNeeded(entity);
	}
}
