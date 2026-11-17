package com.nomosiki.work.mixin;

import com.nomosiki.work.NomosikiWork;
import com.nomosiki.work.gun.Config;
import com.nomosiki.work.gun.RangedGunAttackGoal;
import com.nomosiki.work.gun.VanillaHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSkeleton.class)
abstract class AbstractSkeletonMixin {
	@Unique
	private static final ResourceKey<LootTable> NOMOSIKI_SKELETON_WEAPON =
		ResourceKey.create(Registries.LOOT_TABLE, NomosikiWork.id("skeleton_weapon"));

	@Inject(method = "registerGoals", at = @At("TAIL"))
	private void nomosikiRegisterGunGoal(CallbackInfo ci) {
		AbstractSkeleton skeleton = (AbstractSkeleton) (Object) this;
		((MobAccessor) skeleton).nomosiki$getGoalSelector().addGoal(4, new RangedGunAttackGoal<>(skeleton) {
			private static final double SPEED_MODIFIER = 1.0;
			private static final float ATTACK_RADIUS = 15.0f;
			private int seeTime;
			private int attackDelay;
			private boolean strafingClockwise;
			private boolean strafingBackwards;
			private int strafingTime = -1;

			@Override
			public boolean canContinueToUse() {
				return (isTargetValid() || !mob.getNavigation().isDone()) && canUseGun();
			}

			@Override
			public void start() {
				super.start();
				mob.setAggressive(true);
			}

			@Override
			public void stop() {
				super.stop();
				seeTime = 0;
				attackDelay = 0;
			}

			@Override
			public void tick() {
				super.tick();
				LivingEntity target = mob.getTarget();
				if (target == null) {
					return;
				}
				boolean canSee = mob.getSensing().hasLineOfSight(target);
				boolean wasSeeing = seeTime > 0;
				if (canSee != wasSeeing) {
					seeTime = 0;
				}
				if (canSee) {
					seeTime++;
				} else {
					seeTime--;
				}
				float dist = mob.distanceTo(target);
				if (dist < ATTACK_RADIUS && seeTime >= 20) {
					mob.getNavigation().stop();
					strafingTime++;
				} else {
					mob.getNavigation().moveTo(target, SPEED_MODIFIER);
					strafingTime = -1;
				}
				if (strafingTime >= 20) {
					if (mob.getRandom().nextFloat() < 0.3f) {
						strafingClockwise = !strafingClockwise;
					}
					if (mob.getRandom().nextFloat() < 0.3f) {
						strafingBackwards = !strafingBackwards;
					}
					strafingTime = 0;
				}
				if (strafingTime > -1) {
					if (dist > 0.75f * ATTACK_RADIUS) {
						strafingBackwards = false;
					} else if (dist < 0.25f * ATTACK_RADIUS) {
						strafingBackwards = true;
					}
					mob.getMoveControl().strafe(strafingBackwards ? -0.5f : 0.5f, strafingClockwise ? 0.5f : -0.5f);
					if (mob.getControlledVehicle() instanceof Mob vehicle) {
						vehicle.lookAt(target, 30.0f, 30.0f);
					}
					mob.lookAt(target, 30.0f, 30.0f);
				} else {
					mob.getLookControl().setLookAt(target, 30.0f, 30.0f);
				}
				if (seeTime < -60) {
					attackDelay = Math.max(20, attackDelay);
				}
				if (attackDelay > 0) {
					attackDelay--;
				} else if (isReady()) {
					if (canSee) {
						boolean isHard = mob.level().getDifficulty() == Difficulty.HARD;
						fire(isHard ? 2.0f : 6.0f);
						attackDelay = 10;
					}
				} else {
					reload();
				}
			}

			@Override
			public void onReady() {
				boolean isHard = mob.level().getDifficulty() == Difficulty.HARD;
				attackDelay = Math.max(isHard ? 20 : 40, attackDelay);
			}
		});
	}

	@Inject(method = "populateDefaultEquipmentSlots", at = @At("TAIL"))
	private void nomosikiPopulateGun(RandomSource random, DifficultyInstance difficulty, CallbackInfo ci) {
		AbstractSkeleton skeleton = (AbstractSkeleton) (Object) this;
		if (skeleton.level().getDifficulty() != Difficulty.EASY
			&& skeleton.getRandom().nextFloat() < Config.musketSkeletonChance) {
			ItemStack weapon = VanillaHelper.getRandomWeapon(skeleton, NOMOSIKI_SKELETON_WEAPON);
			if (!weapon.isEmpty()) {
				skeleton.setItemSlot(EquipmentSlot.MAINHAND, weapon);
			}
		}
	}
}
