package com.nomosiki.entity.ai;

import com.nomosiki.NomosikMainThread;
import com.nomosiki.compat.melodies.NomosikImmersiveMelodiesCompat;
import com.nomosiki.entity.NomosikEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Async mod ticks mob AI on worker threads. {@link MeleeAttackGoal} calls {@code swing()} and
 * {@code doHurtTarget()} there; the swing animation packet can be lost while damage still applies.
 * Marshal the full attack to the main server thread when needed.
 */
public class NomosikMeleeAttackGoal extends MeleeAttackGoal {
	/** Vanilla melee is 20 ticks (1s). 32 ticks = one swing every 1.6s. */
	private static final int ATTACK_INTERVAL_TICKS = 32;
	private final AtomicBoolean attackQueued = new AtomicBoolean();
	private int attackCooldown;

	public NomosikMeleeAttackGoal(PathfinderMob mob, double speedModifier, boolean followingTargetEvenIfNotSeen) {
		super(mob, speedModifier, followingTargetEvenIfNotSeen);
	}

	@Override
	public void start() {
		super.start();
		this.attackCooldown = 0;
	}

	@Override
	public void tick() {
		if (this.attackCooldown > 0) {
			this.attackCooldown--;
		}
		super.tick();
	}

	@Override
	protected void resetAttackCooldown() {
		this.attackCooldown = this.adjustedTickDelay(ATTACK_INTERVAL_TICKS);
	}

	@Override
	protected boolean isTimeToAttack() {
		return this.attackCooldown <= 0;
	}

	@Override
	public boolean canUse() {
		if (this.mob instanceof NomosikEntity nomosik && NomosikImmersiveMelodiesCompat.isHoldingInstrument(nomosik)) {
			return false;
		}
		if (this.mob instanceof NomosikEntity nomosik && nomosik.shouldBlockWithShieldNow()) {
			return false;
		}
		if (!super.canUse()) {
			return false;
		}
		if (this.mob instanceof NomosikEntity nomosik) {
			return NomosikFormationCombat.mayChaseTargetForMelee(nomosik);
		}
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		if (this.mob instanceof NomosikEntity nomosik && NomosikImmersiveMelodiesCompat.isHoldingInstrument(nomosik)) {
			return false;
		}
		if (this.mob instanceof NomosikEntity nomosik && nomosik.shouldBlockWithShieldNow()) {
			return false;
		}
		if (!super.canContinueToUse()) {
			return false;
		}
		if (this.mob instanceof NomosikEntity nomosik) {
			return NomosikFormationCombat.mayChaseTargetForMelee(nomosik);
		}
		return true;
	}

	@Override
	protected void checkAndPerformAttack(LivingEntity target) {
		if (!(this.mob.level() instanceof ServerLevel serverLevel)) {
			super.checkAndPerformAttack(target);
			return;
		}
		if (NomosikMainThread.isServerMainThread(serverLevel)) {
			super.checkAndPerformAttack(target);
			return;
		}
		if (!this.isTimeToAttack() || !this.attackQueued.compareAndSet(false, true)) {
			return;
		}
		NomosikMainThread.run(serverLevel, () -> {
			try {
				if (target.isAlive() && !target.isRemoved() && this.mob.isAlive()) {
					super.checkAndPerformAttack(target);
				}
			} finally {
				this.attackQueued.set(false);
			}
		});
	}
}
