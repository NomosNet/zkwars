package com.nomosiki.entity;

import com.nomosiki.squad.SquadType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

public class MonkCavalryEntity extends NomosikEntity {
	public MonkCavalryEntity(EntityType<? extends MonkCavalryEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return NomosikEntity.createAttributes()
			.add(Attributes.MAX_HEALTH, 80.0);
	}

	@Override
	public SquadType getSquadBranchType() {
		return SquadType.MONK_CAVALRY;
	}

	@Override
	public NomosikCulture getUnitCulture() {
		return NomosikCulture.MONK;
	}

	@Nullable
	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData spawnData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData);
		if (level instanceof ServerLevel serverLevel) {
			NomosikiHorseMount.spawnTamedMountFor(this, serverLevel);
		}
		return data;
	}
}
