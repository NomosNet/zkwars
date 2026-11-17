package com.nomosiki.entity;

import com.nomosiki.entity.ai.CaptainHelmGoal;
import com.nomosiki.entity.ai.NomosikFollowOwnerGoal;
import com.nomosiki.entity.ai.NomosikGuardGoal;
import com.nomosiki.entity.ai.NomosikHurtByTargetGoal;
import com.nomosiki.entity.ai.NomosikMeleeAttackGoal;
import com.nomosiki.entity.ai.NomosikMoveToBlockGoal;
import com.nomosiki.entity.ai.NomosikSeekAttackZoneGoal;
import com.nomosiki.entity.ai.NomosikSeekFormationGoal;
import com.nomosiki.entity.ai.NomosikShieldBlockGoal;
import com.nomosiki.entity.ai.NomosikSquadIdleStrollGoal;
import com.nomosiki.squad.SquadType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class CaptainEntity extends NomosikEntity {
	private boolean wantsHelm = true;

	public CaptainEntity(EntityType<? extends CaptainEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return NomosikEntity.createAttributes();
	}

	@Override
	public SquadType getSquadBranchType() {
		return SquadType.CAPTAIN;
	}

	@Override
	public boolean isCaptain() {
		return true;
	}

	@Override
	public boolean wantsHelm() {
		return this.wantsHelm;
	}

	@Override
	public void setWantsHelm(boolean wants) {
		this.wantsHelm = wants;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new CaptainHelmGoal(this));
		this.goalSelector.addGoal(2, new OpenDoorGoal(this, true));
		this.goalSelector.addGoal(2, new NomosikMeleeAttackGoal(this, 1.05, true));
		this.goalSelector.addGoal(3, new NomosikShieldBlockGoal(this));
		this.goalSelector.addGoal(4, new NomosikSeekFormationGoal(this));
		this.goalSelector.addGoal(5, new NomosikMoveToBlockGoal(this));
		this.goalSelector.addGoal(6, new NomosikSeekAttackZoneGoal(this));
		this.goalSelector.addGoal(7, new NomosikGuardGoal(this));
		this.goalSelector.addGoal(8, new NomosikFollowOwnerGoal(this, 1.05F, 10.0F, 2.5F));
		this.goalSelector.addGoal(9, new NomosikSquadIdleStrollGoal(this, 0.9));
		this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new NomosikHurtByTargetGoal(this));
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putBoolean("WantsHelm", this.wantsHelm);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		this.wantsHelm = !tag.contains("WantsHelm") || tag.getBoolean("WantsHelm");
	}
}
