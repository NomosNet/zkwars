package com.nomosiki.entity;

import com.nomosiki.squad.SquadType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

public class KnightEntity extends NomosikEntity {
	public KnightEntity(EntityType<? extends KnightEntity> type, Level level) {
		super(type, level);
	}

	@Override
	public SquadType getSquadBranchType() {
		return SquadType.KNIGHT;
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
