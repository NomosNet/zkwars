package com.nomosiki.entity.ai;

import com.nomosiki.NomosikMainThread;
import com.nomosiki.compat.gun.GunsCompat;
import com.nomosiki.compat.melodies.NomosikImmersiveMelodiesCompat;
import com.nomosiki.combat.StrategicFireController;
import com.nomosiki.config.NomosikiServerConfig;
import com.nomosiki.entity.MusketeerEntity;
import com.nomosiki.entity.NomosikRangedFireMode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class MusketeerGunAttackGoal extends Goal {
	private static final int SEE_TIME_REQUIRED = 5;
	private static final int READY_DELAY_TICKS = 10;

	private final MusketeerEntity mob;
	private final double speedModifier;
	private int seeTime;
	private int attackDelay;
	@Nullable
	private Vec3 strategicAimPoint;

	public MusketeerGunAttackGoal(MusketeerEntity mob, double speedModifier) {
		this.mob = mob;
		this.speedModifier = speedModifier;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (NomosikImmersiveMelodiesCompat.isHoldingInstrument(this.mob)) {
			return false;
		}
		if (!this.mayShoot()) {
			return false;
		}
		if (!GunsCompat.canUseGun(this.mob)) {
			return false;
		}
		if (this.isStrategicFire()) {
			return StrategicFireController.activeZone(this.mob) != null;
		}
		return this.mob.getTarget() != null;
	}

	@Override
	public boolean canContinueToUse() {
		if (NomosikImmersiveMelodiesCompat.isHoldingInstrument(this.mob)) {
			return false;
		}
		if (!this.mayShoot()) {
			return false;
		}
		if (!GunsCompat.canUseGun(this.mob)) {
			return false;
		}
		if (this.isStrategicFire()) {
			return this.canUse();
		}
		return this.canUse() || !this.mob.getNavigation().isDone();
	}

	private boolean isStrategicFire() {
		return this.mob.getRangedFireMode() == NomosikRangedFireMode.STRATEGIC_FIRE;
	}

	private boolean mayShoot() {
		NomosikRangedFireMode mode = this.mob.getRangedFireMode();
		if (mode == NomosikRangedFireMode.HOLD_FIRE) {
			return false;
		}
		if (mode == NomosikRangedFireMode.STRATEGIC_FIRE) {
			AABB zone = StrategicFireController.activeZone(this.mob);
			if (zone == null) {
				return false;
			}
			return StrategicFireController.withinBowRange(this.mob, zone, GunsCompat.attackRadius(this.mob));
		}
		return true;
	}

	@Override
	public void start() {
		this.mob.setAggressive(true);
		this.strategicAimPoint = null;
	}

	@Override
	public void stop() {
		this.mob.setAggressive(false);
		this.seeTime = 0;
		this.attackDelay = 0;
		this.strategicAimPoint = null;
		this.mob.stopUsingItem();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (this.mob.level() instanceof ServerLevel serverLevel && !NomosikMainThread.isServerMainThread(serverLevel)) {
			NomosikMainThread.run(serverLevel, this::tickGunAttack);
			return;
		}
		this.tickGunAttack();
	}

	private void tickGunAttack() {
		this.finishReloadIfReady();
		if (this.isStrategicFire()) {
			this.tickStrategicFire();
			return;
		}

		LivingEntity target = this.mob.getTarget();
		if (target == null) {
			return;
		}

		float radius = GunsCompat.attackRadius(this.mob);
		double distSqr = this.mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
		boolean canSee = this.mob.getSensing().hasLineOfSight(target);
		if (canSee) {
			++this.seeTime;
		} else {
			this.seeTime = 0;
		}

		if (distSqr <= radius * radius && this.seeTime >= SEE_TIME_REQUIRED) {
			this.mob.getNavigation().stop();
		} else if (!NomosikFormationCombat.shouldHoldFormationSlot(this.mob)) {
			this.mob.getNavigation().moveTo(target, this.speedModifier);
		}

		this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
		this.tryShoot(canSee, () -> GunsCompat.fireAt(this.mob, target));
	}

	private void tickStrategicFire() {
		if (this.strategicAimPoint == null) {
			this.strategicAimPoint = StrategicFireController.pickAimPoint(this.mob);
		}

		Vec3 aim = this.strategicAimPoint;
		boolean canSee = StrategicFireController.hasLineOfSightTo(this.mob, aim);
		if (canSee) {
			++this.seeTime;
		} else {
			this.seeTime = 0;
			this.strategicAimPoint = StrategicFireController.pickAimPoint(this.mob);
			aim = this.strategicAimPoint;
			canSee = StrategicFireController.hasLineOfSightTo(this.mob, aim);
		}

		this.mob.getNavigation().stop();
		StrategicFireController.faceZone(this.mob, aim);
		this.mob.getLookControl().setLookAt(aim.x, aim.y, aim.z, 30.0F, 30.0F);
		Vec3 firePoint = aim;
		this.tryShoot(canSee, () -> {
			GunsCompat.fireAt(this.mob, firePoint);
			this.strategicAimPoint = null;
		});
	}

	private void finishReloadIfReady() {
		if (!this.mob.isUsingItem() || !GunsCompat.isGunLoaded(this.mob.getUseItem())) {
			return;
		}
		if (this.mob.getRangedShotCooldown() > READY_DELAY_TICKS) {
			this.mob.releaseUsingItem();
			GunsCompat.unloadHeldGun(this.mob);
			GunsCompat.mobReload(this.mob);
			return;
		}
		this.mob.releaseUsingItem();
		this.attackDelay = Math.max(this.attackDelay, READY_DELAY_TICKS);
	}

	private void tryShoot(boolean canSee, Runnable fire) {
		if (this.attackDelay > 0) {
			this.attackDelay--;
		}
		if (GunsCompat.isGunReady(this.mob)) {
			if (canSee && this.attackDelay <= 0 && this.mob.getRangedShotCooldown() <= 0) {
				fire.run();
				this.attackDelay = READY_DELAY_TICKS;
				this.mob.setRangedShotCooldown(NomosikiServerConfig.INSTANCE.combat.musketeerReloadTicks);
			}
			return;
		}
		if (!this.mob.isUsingItem()) {
			GunsCompat.mobReload(this.mob);
		}
	}
}
