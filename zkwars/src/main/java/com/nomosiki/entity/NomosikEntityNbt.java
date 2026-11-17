package com.nomosiki.entity;

import com.nomosiki.compat.gun.GunsCompat;
import com.nomosiki.faction.FactionColor;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import java.util.Optional;
import java.util.UUID;

final class NomosikEntityNbt {
	private static final int LEGACY_ARTILLERY_AMMO_SLOTS = 4;

	private NomosikEntityNbt() {
	}

	static void write(NomosikEntity entity, CompoundTag tag) {
		if (entity.getOwnerUUID() != null) {
			tag.putUUID("Owner", entity.getOwnerUUID());
		}
		if (entity.squadId != null) {
			tag.putUUID("SquadId", entity.squadId);
		}
		tag.putBoolean("ShouldFollow", entity.getEntityData().get(NomosikEntity.DATA_SHOULD_FOLLOW));
		tag.putByte("SquadOrder", entity.squadOrder.id());
		tag.putByte("SquadFormation", entity.squadFormation.id());
		tag.putDouble("SquadFormSpacing", entity.squadFormationSpacing);
		tag.putInt("SquadFormMaxInRow", entity.squadFormationMaxInRow);
		tag.putInt("SquadFormSlot", entity.squadFormationSlot);
		tag.putBoolean("SquadInFormation", entity.squadInFormation);
		tag.putBoolean("SquadHoldFormation", entity.squadHoldFormation);
		tag.putFloat("SquadFormationFacingYaw", entity.squadFormationFacingYaw);
		if (entity.squadAnchorPos != null) {
			tag.put("SquadAnchor", NbtUtils.writeBlockPos(entity.squadAnchorPos));
		}
		tag.putInt("SquadZoneRadius", entity.squadZoneRadius);
		if (entity.squadAttackTargetId != null) {
			tag.putUUID("SquadAttackTarget", entity.squadAttackTargetId);
			tag.putBoolean("SquadAttackTargetIsPlayer", entity.squadAttackTargetIsPlayer);
		}
		tag.putByte("CombatMode", entity.combatMode.id());
		tag.putBoolean("ShieldStance", entity.shieldStance);
		if (entity.canUseRangedFireOrders()) {
			tag.putByte("RangedFireMode", entity.rangedFireMode.id());
		}
		if (entity.rangedFireAnchor != null) {
			tag.put("RangedFireAnchor", NbtUtils.writeBlockPos(entity.rangedFireAnchor));
			tag.putInt("RangedFireRadius", entity.rangedFireRadius);
		}
		if (entity.rangedShotCooldown > 0) {
			tag.putInt("RangedShotCooldown", entity.rangedShotCooldown);
		}
		tag.putByte(NomosikEntity.NBT_RACE, NomosikAppearanceLogic.getNomosikRace(entity).id());
		tag.putByte(NomosikEntity.NBT_HUMAN_SKIN, (byte) NomosikAppearanceLogic.getHumanSkinVariant(entity));
		FactionColor override = entity.getFactionColorOverride();
		if (override != null) {
			tag.putString(NomosikEntity.NBT_FACTION_COLOR, override.id());
		}
		ContainerHelper.saveAllItems(tag, NomosikEquipmentLogic.equipmentAsNonNullList(entity), entity.registryAccess());
	}

	static void read(NomosikEntity entity, CompoundTag tag) {
		if (tag.hasUUID("Owner")) {
			entity.setOwnerUUID(tag.getUUID("Owner"));
		}
		if (tag.hasUUID("SquadId")) {
			entity.squadId = tag.getUUID("SquadId");
		} else {
			entity.squadId = null;
		}
		entity.getEntityData().set(NomosikEntity.DATA_SQUAD_ID, entity.squadId == null ? Optional.empty() : Optional.of(entity.squadId));
		if (tag.contains("ShouldFollow")) {
			entity.setShouldFollowOwner(tag.getBoolean("ShouldFollow"));
		}
		if (tag.contains("SquadOrder")) {
			entity.squadOrder = SquadOrder.fromId(tag.getByte("SquadOrder"));
		}
		if (tag.contains("SquadFormation", Tag.TAG_BYTE)) {
			entity.squadFormation = SquadFormation.fromId(tag.getByte("SquadFormation"));
			entity.getEntityData().set(NomosikEntity.DATA_SQUAD_FORMATION, entity.squadFormation.id());
		}
		if (tag.contains("SquadFormSpacing", Tag.TAG_DOUBLE) || tag.contains("SquadFormSpacing", Tag.TAG_FLOAT)) {
			entity.squadFormationSpacing = NomosikSquadLogic.clampSquadFormationSpacing(tag.getDouble("SquadFormSpacing"));
		}
		if (tag.contains("SquadFormMaxInRow", Tag.TAG_INT)) {
			entity.squadFormationMaxInRow = NomosikSquadLogic.clampSquadFormationMaxInRow(tag.getInt("SquadFormMaxInRow"));
		}
		if (tag.contains("SquadFormSlot", Tag.TAG_INT)) {
			entity.squadFormationSlot = tag.getInt("SquadFormSlot");
		} else {
			entity.squadFormationSlot = -1;
		}
		if (tag.contains("SquadInFormation")) {
			entity.squadInFormation = tag.getBoolean("SquadInFormation");
		}
		if (tag.contains("SquadHoldFormation")) {
			entity.squadHoldFormation = tag.getBoolean("SquadHoldFormation");
		}
		if (tag.contains("SquadFormationFacingYaw", Tag.TAG_FLOAT)) {
			entity.squadFormationFacingYaw = tag.getFloat("SquadFormationFacingYaw");
		}
		if (tag.contains("SquadAnchor")) {
			entity.squadAnchorPos = NbtUtils.readBlockPos(tag, "SquadAnchor").orElse(null);
		}
		if (tag.contains("SquadZoneRadius")) {
			entity.squadZoneRadius = tag.getInt("SquadZoneRadius");
		}
		if (tag.hasUUID("SquadAttackTarget")) {
			entity.squadAttackTargetId = tag.getUUID("SquadAttackTarget");
			entity.squadAttackTargetIsPlayer = tag.getBoolean("SquadAttackTargetIsPlayer");
		} else {
			entity.squadAttackTargetId = null;
			entity.squadAttackTargetIsPlayer = false;
		}
		if (tag.contains("CombatMode")) {
			entity.combatMode = NomosikCombatMode.fromId(tag.getByte("CombatMode"));
			entity.getEntityData().set(NomosikEntity.DATA_COMBAT_MODE, entity.combatMode.id());
		}
		if (tag.contains("ShieldStance")) {
			entity.shieldStance = tag.getBoolean("ShieldStance");
			entity.getEntityData().set(NomosikEntity.DATA_SHIELD_STANCE, entity.shieldStance);
		}
		if (tag.contains("RangedFireMode")) {
			entity.rangedFireMode = NomosikRangedFireMode.fromId(tag.getByte("RangedFireMode"));
			entity.getEntityData().set(NomosikEntity.DATA_RANGED_FIRE_MODE, entity.rangedFireMode.id());
		}
		if (tag.contains("RangedFireAnchor")) {
			entity.rangedFireAnchor = NbtUtils.readBlockPos(tag, "RangedFireAnchor").orElse(null);
		}
		if (tag.contains("RangedFireRadius", Tag.TAG_INT)) {
			entity.rangedFireRadius = tag.getInt("RangedFireRadius");
		}
		NomosikSquadLogic.restoreRangedFireZone(entity);
		if (tag.contains("RangedShotCooldown", Tag.TAG_INT)) {
			entity.rangedShotCooldown = Math.max(0, tag.getInt("RangedShotCooldown"));
		}
		if (tag.contains(NomosikEntity.NBT_RACE, Tag.TAG_BYTE)) {
			NomosikRace loaded = NomosikRace.normalizeForPlay(NomosikRace.fromId(tag.getByte(NomosikEntity.NBT_RACE)));
			entity.getEntityData().set(NomosikEntity.DATA_NOMOSIK_RACE, loaded.id());
		} else {
			entity.getEntityData().set(NomosikEntity.DATA_NOMOSIK_RACE, NomosikRace.HUMAN.id());
		}
		if (tag.contains(NomosikEntity.NBT_HUMAN_SKIN, Tag.TAG_INT)) {
			entity.getEntityData().set(NomosikEntity.DATA_HUMAN_SKIN_VARIANT, Math.floorMod(tag.getInt(NomosikEntity.NBT_HUMAN_SKIN), NomosikEntity.HUMAN_SKIN_VARIANT_COUNT));
		} else if (tag.contains(NomosikEntity.NBT_HUMAN_SKIN, Tag.TAG_BYTE)) {
			int v = Byte.toUnsignedInt(tag.getByte(NomosikEntity.NBT_HUMAN_SKIN)) % NomosikEntity.HUMAN_SKIN_VARIANT_COUNT;
			entity.getEntityData().set(NomosikEntity.DATA_HUMAN_SKIN_VARIANT, v);
		}
		if (tag.contains(NomosikEntity.NBT_FACTION_COLOR, Tag.TAG_STRING)) {
			String colorId = tag.getString(NomosikEntity.NBT_FACTION_COLOR);
			if (colorId != null && !colorId.isBlank()) {
				entity.setFactionColorOverride(FactionColor.fromId(colorId));
			}
		}
		NomosikSquadLogic.rebuildSquadAttackZoneFromLoadedState(entity);
		NonNullList<ItemStack> loaded = NonNullList.withSize(NomosikEntity.SLOT_COUNT, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(tag, loaded, entity.registryAccess());
		for (int i = 0; i < NomosikEntity.SLOT_COUNT; i++) {
			entity.equipmentInventory.setItem(i, loaded.get(i));
		}
		NomosikEquipmentLogic.syncVanillaEquipmentFromContainer(entity);
		migrateLegacyArtilleryAmmo(entity, tag);
		GunsCompat.applyStanceHands(entity);
		NomosikSquadLogic.syncSquadStateToData(entity);
		entity.refreshDimensions();
		if (entity.level() instanceof ServerLevel serverLevel) {
			NomosikiHorseMount.ensureMountMatchesRole(entity, serverLevel);
		}
		NomosikAppearanceLogic.rollHumanSkinIfNeeded(entity);
	}

	private static void migrateLegacyArtilleryAmmo(NomosikEntity entity, CompoundTag tag) {
		if (!entity.isArtillerist() || !tag.contains("ArtilleryAmmo", Tag.TAG_COMPOUND)) {
			return;
		}
		NonNullList<ItemStack> ammoLoaded = NonNullList.withSize(LEGACY_ARTILLERY_AMMO_SLOTS, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(tag.getCompound("ArtilleryAmmo"), ammoLoaded, entity.registryAccess());
		for (ItemStack stack : ammoLoaded) {
			if (stack.isEmpty()) {
				continue;
			}
			ItemStack remaining = stack.copy();
			NomosikEquipmentLogic.insertIntoHands(entity, remaining);
			if (!remaining.isEmpty()) {
				entity.spawnAtLocation(remaining);
			}
		}
	}
}
