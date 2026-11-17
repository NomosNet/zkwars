package com.nomosiki.entity;

import com.nomosiki.combat.NomosikProjectileAim;
import com.nomosiki.config.NomosikiServerConfig;
import com.nomosiki.entity.ai.ArcherBowAttackGoal;
import com.nomosiki.entity.ai.NomosikSeekFormationGoal;
import com.nomosiki.entity.ai.NomosikSquadIdleStrollGoal;
import com.nomosiki.squad.SquadType;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import com.nomosiki.entity.ai.NomosikFollowOwnerGoal;
import com.nomosiki.entity.ai.NomosikGuardGoal;
import com.nomosiki.entity.ai.NomosikHurtByTargetGoal;
import com.nomosiki.entity.ai.NomosikMoveToBlockGoal;
import com.nomosiki.entity.ai.NomosikSeekAttackZoneGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class ArcherEntity extends NomosikEntity implements RangedAttackMob {
	public ArcherEntity(EntityType<? extends ArcherEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return NomosikEntity.createAttributes()
			.add(Attributes.FOLLOW_RANGE, 48.0);
	}

	@Override
	public SquadType getSquadBranchType() {
		return SquadType.ARCHER;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new ArcherBowAttackGoal<>(this, 1.0, 25, 44.0F));
		this.goalSelector.addGoal(2, new OpenDoorGoal(this, true));
		this.goalSelector.addGoal(2, new NomosikSeekFormationGoal(this));
		this.goalSelector.addGoal(3, new NomosikMoveToBlockGoal(this));
		this.goalSelector.addGoal(4, new NomosikSeekAttackZoneGoal(this));
		this.goalSelector.addGoal(5, new NomosikGuardGoal(this));
		this.goalSelector.addGoal(6, new NomosikFollowOwnerGoal(this, 1.05F, 10.0F, 2.5F));
		this.goalSelector.addGoal(7, new NomosikSquadIdleStrollGoal(this, 0.9));
		this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new NomosikHurtByTargetGoal(this));
	}

	@Nullable
	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData spawnData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData);
		this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
		this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.ARROW, 16));
		return data;
	}

	@Override
	public void performRangedAttack(LivingEntity target, float distanceFactor) {
		Vec3 aimPoint = NomosikProjectileAim.entityAimPoint(target, true);
		this.performRangedAttackAt(aimPoint, distanceFactor);
	}

	public void performRangedAttackAt(Vec3 aimPoint, float distanceFactor) {
		NomosikiServerConfig.BowSettings cfg = NomosikiServerConfig.INSTANCE.combat.bow;
		ItemStack ammo = new ItemStack(Items.ARROW);
		AbstractArrow arrow = ProjectileUtil.getMobArrow(this, ammo, distanceFactor, this.getMainHandItem());
		arrow.setBaseDamage(cfg.arrowDamage * Math.max(0.2F, distanceFactor));

		float inaccuracy = cfg.inaccuracy;
		if (this.isCavalryMounted()) {
			inaccuracy += cfg.mountedInaccuracyBonus;
		}

		NomosikProjectileAim.AimVector direction = NomosikProjectileAim.directionFrom(this, aimPoint, arrow.getY(), cfg.velocity);
		NomosikProjectileAim.shootArrow(arrow, direction, cfg.velocity, inaccuracy);
		this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
		this.level().addFreshEntity(arrow);
	}

	public void performStrategicRangedAttackAt(Vec3 aimPoint, float distanceFactor) {
		NomosikiServerConfig.BowSettings cfg = NomosikiServerConfig.INSTANCE.combat.bow;
		java.util.Optional<NomosikProjectileAim.AimVector> arc = NomosikProjectileAim.bombardmentArc(this, aimPoint, cfg.velocity);
		if (arc.isEmpty()) {
			return;
		}
		ItemStack ammo = new ItemStack(Items.ARROW);
		AbstractArrow arrow = ProjectileUtil.getMobArrow(this, ammo, distanceFactor, this.getMainHandItem());
		arrow.setBaseDamage(cfg.arrowDamage * Math.max(0.2F, distanceFactor));

		float inaccuracy = cfg.inaccuracy;
		if (this.isCavalryMounted()) {
			inaccuracy += cfg.mountedInaccuracyBonus;
		}

		NomosikProjectileAim.shootArrow(arrow, arc.get(), cfg.velocity, inaccuracy);
		this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
		this.level().addFreshEntity(arrow);
	}

	@Override
	public boolean canFireProjectileWeapon(ProjectileWeaponItem weapon) {
		return weapon instanceof BowItem;
	}
}
