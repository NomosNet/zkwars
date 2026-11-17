package com.nomosiki.entity;

import com.nomosiki.config.NomosikiServerConfig;
import com.nomosiki.squad.SquadType;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

public class NinjaEntity extends ArcherEntity {
	private static final int MAP_COMBAT_GRACE_TICKS = 40;

	private static final EntityDataAccessor<Boolean> DATA_MAP_REVEALED =
		SynchedEntityData.defineId(NinjaEntity.class, EntityDataSerializers.BOOLEAN);

	public NinjaEntity(EntityType<? extends NinjaEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return ArcherEntity.createAttributes()
			.add(Attributes.MAX_HEALTH, 50.0)
			.add(Attributes.MOVEMENT_SPEED, NomosikiServerConfig.INSTANCE.ninja.movementSpeed);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_MAP_REVEALED, false);
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide()) {
			this.refreshMapRevealed();
		}
	}

	public boolean isMapRevealed() {
		return this.entityData.get(DATA_MAP_REVEALED);
	}

	private void refreshMapRevealed() {
		this.entityData.set(DATA_MAP_REVEALED, this.inMapCombat());
	}

	private boolean inMapCombat() {
		LivingEntity target = this.getTarget();
		if (target != null && target.isAlive() && !target.isRemoved()) {
			return true;
		}
		int now = this.tickCount;
		LivingEntity lastHurtBy = this.getLastHurtByMob();
		if (lastHurtBy != null && now - this.getLastHurtByMobTimestamp() < MAP_COMBAT_GRACE_TICKS) {
			return true;
		}
		LivingEntity lastHurt = this.getLastHurtMob();
		return lastHurt != null && now - this.getLastHurtMobTimestamp() < MAP_COMBAT_GRACE_TICKS;
	}

	@Override
	public SquadType getSquadBranchType() {
		return SquadType.NINJA;
	}

	@Override
	public NomosikCulture getUnitCulture() {
		return NomosikCulture.NINJA;
	}
}
