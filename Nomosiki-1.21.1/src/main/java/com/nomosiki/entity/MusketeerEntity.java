package com.nomosiki.entity;

import com.nomosiki.compat.gun.GunsCompat;
import com.nomosiki.entity.ai.MusketeerBayonetMeleeGoal;
import com.nomosiki.entity.ai.MusketeerGunAttackGoal;
import com.nomosiki.entity.ai.NomosikFollowOwnerGoal;
import com.nomosiki.entity.ai.NomosikGuardGoal;
import com.nomosiki.entity.ai.NomosikHurtByTargetGoal;
import com.nomosiki.entity.ai.NomosikMoveToBlockGoal;
import com.nomosiki.entity.ai.NomosikSeekAttackZoneGoal;
import com.nomosiki.entity.ai.NomosikSeekFormationGoal;
import com.nomosiki.entity.ai.NomosikSquadIdleStrollGoal;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

public abstract class MusketeerEntity extends NomosikEntity {
	protected MusketeerEntity(EntityType<? extends MusketeerEntity> type, Level level) {
		super(type, level);
	}

	@Override
	public boolean isRangedUnit() {
		return true;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new MusketeerBayonetMeleeGoal(this));
		this.goalSelector.addGoal(2, new OpenDoorGoal(this, true));
		this.goalSelector.addGoal(2, new MusketeerGunAttackGoal(this, 1.0));
		this.goalSelector.addGoal(3, new NomosikSeekFormationGoal(this));
		this.goalSelector.addGoal(4, new NomosikMoveToBlockGoal(this));
		this.goalSelector.addGoal(5, new NomosikSeekAttackZoneGoal(this));
		this.goalSelector.addGoal(6, new NomosikGuardGoal(this));
		this.goalSelector.addGoal(7, new NomosikFollowOwnerGoal(this, 1.05F, 10.0F, 2.5F));
		this.goalSelector.addGoal(8, new NomosikSquadIdleStrollGoal(this, 0.9));
		this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new NomosikHurtByTargetGoal(this));
	}

	@Nullable
	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData spawnData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData);
		GunsCompat.giveDefaultLoadout(this);
		return data;
	}
}
