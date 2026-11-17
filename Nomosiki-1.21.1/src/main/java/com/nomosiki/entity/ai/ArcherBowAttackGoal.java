package com.nomosiki.entity.ai;

import com.nomosiki.NomosikMainThread;
import com.nomosiki.compat.melodies.NomosikImmersiveMelodiesCompat;
import com.nomosiki.combat.StrategicFireController;
import com.nomosiki.config.NomosikiServerConfig;
import com.nomosiki.entity.ArcherEntity;
import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.entity.NomosikRangedFireMode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class ArcherBowAttackGoal<T extends Mob & RangedAttackMob> extends Goal {
	private final T mob;
	private final double speedModifier;
	private final int attackInterval;
	private final float attackRadius;
	private final float attackRadiusSqr;
	private int attackTime = -1;
	private int seeTime;
	@org.jetbrains.annotations.Nullable
	private Vec3 strategicAimPoint;

	public ArcherBowAttackGoal(T mob, double speedModifier, int attackInterval, float attackRadius) {
		this.mob = mob;
		this.speedModifier = speedModifier;
		this.attackInterval = attackInterval;
		this.attackRadius = attackRadius;
		this.attackRadiusSqr = attackRadius * attackRadius;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (NomosikImmersiveMelodiesCompat.isHoldingInstrument(this.mob)) {
			return false;
		}
		if (!this.mayShoot()) {
			return false;
		}
		if (!this.isHoldingBow()) {
			return false;
		}
		if (this.isStrategicFire()) {
			return StrategicFireController.activeZone(this.asNomosik()) != null;
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
		if (!this.isHoldingBow()) {
			return false;
		}
		if (this.isStrategicFire()) {
			return this.canUse();
		}
		return this.canUse() || !this.mob.getNavigation().isDone();
	}

	private boolean isStrategicFire() {
		NomosikEntity nomosik = this.asNomosik();
		return nomosik != null && nomosik.getRangedFireMode() == NomosikRangedFireMode.STRATEGIC_FIRE;
	}

	@org.jetbrains.annotations.Nullable
	private NomosikEntity asNomosik() {
		return this.mob instanceof NomosikEntity nomosik ? nomosik : null;
	}

	private boolean isHoldingBow() {
		return this.mob.isHolding(stack -> stack.getItem() instanceof BowItem);
	}

	private InteractionHand bowHand() {
		return this.mob.getMainHandItem().getItem() instanceof BowItem
			? InteractionHand.MAIN_HAND
			: InteractionHand.OFF_HAND;
	}

	private boolean mayShoot() {
		NomosikEntity nomosik = this.asNomosik();
		if (nomosik == null || !nomosik.isRangedUnit()) {
			return true;
		}
		NomosikRangedFireMode mode = nomosik.getRangedFireMode();
		if (mode == NomosikRangedFireMode.HOLD_FIRE) {
			return false;
		}
		if (mode == NomosikRangedFireMode.STRATEGIC_FIRE) {
			AABB zone = StrategicFireController.activeZone(nomosik);
			if (zone == null) {
				return false;
			}
			double maxRange = NomosikiServerConfig.INSTANCE.combat.bow.maxRange;
			return StrategicFireController.withinBowRange(nomosik, zone, maxRange);
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
		this.attackTime = -1;
		this.strategicAimPoint = null;
		this.mob.stopUsingItem();
	}

	@Override
	public void tick() {
		if (this.mob.level() instanceof ServerLevel serverLevel && !NomosikMainThread.isServerMainThread(serverLevel)) {
			NomosikMainThread.run(serverLevel, this::tickBowAttack);
			return;
		}
		this.tickBowAttack();
	}

	private void tickBowAttack() {
		int drawTicksMin = NomosikiServerConfig.INSTANCE.combat.bow.drawTicksMin;

		if (this.isStrategicFire()) {
			this.tickStrategicBowAttack(drawTicksMin);
			return;
		}

		LivingEntity target = this.mob.getTarget();
		if (target == null) {
			return;
		}

		double distSqr = this.mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
		boolean canSee = this.mob.getSensing().hasLineOfSight(target);
		if (canSee) {
			++this.seeTime;
		} else {
			this.seeTime = 0;
		}

		if (distSqr <= this.attackRadiusSqr && this.seeTime >= 5) {
			this.mob.getNavigation().stop();
		} else if (!(this.mob instanceof NomosikEntity nomosik && NomosikFormationCombat.shouldHoldFormationSlot(nomosik))) {
			this.mob.getNavigation().moveTo(target, this.speedModifier);
		}

		this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

		if (this.mob.isUsingItem()) {
			if (!canSee) {
				this.mob.stopUsingItem();
				return;
			}
			int useTicks = this.mob.getTicksUsingItem();
			if (useTicks >= drawTicksMin) {
				this.mob.stopUsingItem();
				this.mob.performRangedAttack(target, BowItem.getPowerForTime(useTicks));
				this.attackTime = this.attackInterval;
			}
			return;
		}

		if (--this.attackTime <= 0 && canSee) {
			InteractionHand hand = this.bowHand();
			this.mob.startUsingItem(hand);
		}
	}

	private void tickStrategicBowAttack(int drawTicksMin) {
		NomosikEntity nomosik = this.asNomosik();
		if (nomosik == null) {
			return;
		}

		if (this.strategicAimPoint == null) {
			this.strategicAimPoint = StrategicFireController.pickAimPoint(nomosik);
		}

		Vec3 aim = this.strategicAimPoint;
		boolean canSee = StrategicFireController.bombardmentArcClear(nomosik, aim);
		if (canSee) {
			++this.seeTime;
		} else {
			this.seeTime = 0;
			this.strategicAimPoint = StrategicFireController.pickAimPoint(nomosik);
			aim = this.strategicAimPoint;
			canSee = StrategicFireController.bombardmentArcClear(nomosik, aim);
		}

		this.mob.getNavigation().stop();
		StrategicFireController.faceZone(nomosik, aim);
		this.mob.getLookControl().setLookAt(aim.x, aim.y, aim.z, 30.0F, 30.0F);

		if (this.mob.isUsingItem()) {
			if (!canSee) {
				this.mob.stopUsingItem();
				return;
			}
			int useTicks = this.mob.getTicksUsingItem();
			if (useTicks >= drawTicksMin) {
				this.mob.stopUsingItem();
				if (this.mob instanceof ArcherEntity archer) {
					archer.performStrategicRangedAttackAt(aim, BowItem.getPowerForTime(useTicks));
				}
				this.attackTime = this.attackInterval;
				this.strategicAimPoint = null;
			}
			return;
		}

		if (--this.attackTime <= 0 && canSee) {
			InteractionHand hand = this.bowHand();
			this.mob.startUsingItem(hand);
		}
	}
}
