package com.nomosiki.entity.ai;

import com.nomosiki.advancement.NomosikiActions;
import com.nomosiki.advancement.NomosikiAdvancements;
import com.nomosiki.compat.siege.SiegeMachinesCompat;
import com.nomosiki.config.NomosikiServerConfig;
import com.nomosiki.entity.ArtilleristEntity;
import com.nomosiki.entity.NomosikRangedFireMode;
import com.nomosiki.entity.SquadOrder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

public class ArtilleristSiegeGoal extends Goal {
	private static final float AIM_TOLERANCE = 6.0F;

	private final ArtilleristEntity gunner;
	private int machineId = -1;
	private Vec3 aimPoint;

	public ArtilleristSiegeGoal(ArtilleristEntity gunner) {
		this.gunner = gunner;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (!SiegeMachinesCompat.isLoaded()) {
			return false;
		}
		if (this.gunner.getRangedFireMode() == NomosikRangedFireMode.HOLD_FIRE) {
			return false;
		}
		if (this.gunner.getRangedFireMode() != NomosikRangedFireMode.STRATEGIC_FIRE) {
			return false;
		}
		if (this.gunner.getEffectiveRangedFireZone() == null) {
			return false;
		}
		if (SiegeMachinesCompat.isMountedOnAllowed(this.gunner)) {
			return true;
		}
		return SiegeMachinesCompat.findNearestAllowedEmpty(this.gunner, SiegeMachinesCompat.SEARCH_RADIUS, Set.of()) != null;
	}

	@Override
	public boolean canContinueToUse() {
		return this.canUse();
	}

	@Override
	public void start() {
		this.aimPoint = null;
		if (this.gunner.getVehicle() != null) {
			this.machineId = this.gunner.getVehicle().getId();
		}
	}

	@Override
	public void stop() {
		this.machineId = -1;
		this.aimPoint = null;
		if (this.shouldLeaveMachine()) {
			SiegeMachinesCompat.dismount(this.gunner);
		}
	}

	@Override
	public void tick() {
		Entity machine = this.currentMachine();
		if (machine == null) {
			machine = SiegeMachinesCompat.findNearestAllowedEmpty(
				this.gunner, SiegeMachinesCompat.SEARCH_RADIUS, Set.of());
			if (machine == null) {
				this.machineId = -1;
				return;
			}
			this.machineId = machine.getId();
			if (this.gunner.distanceToSqr(machine) > SiegeMachinesCompat.MOUNT_REACH * SiegeMachinesCompat.MOUNT_REACH) {
				this.gunner.getNavigation().moveTo(machine, 1.05);
				return;
			}
			this.gunner.getNavigation().stop();
			SiegeMachinesCompat.tryMount(this.gunner, machine);
			return;
		}
		this.gunner.getNavigation().stop();
		AABB zone = this.gunner.getEffectiveRangedFireZone();
		if (zone == null) {
			return;
		}
		if (this.aimPoint == null || this.gunner.getRandom().nextInt(40) == 0) {
			this.aimPoint = zone.getCenter();
		}
		SiegeMachinesCompat.transferAmmoFrom(this.gunner, machine);
		SiegeMachinesCompat.aimAt(machine, this.aimPoint);
		if (this.gunner.getRangedShotCooldown() > 0) {
			return;
		}
		if (!SiegeMachinesCompat.hasAmmoReady(machine) || !SiegeMachinesCompat.isReadyToFire(machine)) {
			return;
		}
		if (!SiegeMachinesCompat.isAimedAt(machine, this.aimPoint, AIM_TOLERANCE)) {
			return;
		}
		if (SiegeMachinesCompat.tryFire(machine, this.gunner)) {
			this.gunner.setRangedShotCooldown(NomosikiServerConfig.INSTANCE.combat.siegeReloadTicks);
			this.aimPoint = null;
			if (this.gunner.getOwnerUUID() != null && this.gunner.level() instanceof ServerLevel level) {
				NomosikiAdvancements.grant(level.getServer(), this.gunner.getOwnerUUID(), NomosikiActions.SIEGE);
			}
		}
	}

	private boolean shouldLeaveMachine() {
		return this.gunner.getRangedFireMode() != NomosikRangedFireMode.STRATEGIC_FIRE
			|| this.gunner.getSquadOrder() == SquadOrder.FOLLOW;
	}

	@Nullable
	private Entity currentMachine() {
		Entity vehicle = this.gunner.getVehicle();
		if (vehicle != null && SiegeMachinesCompat.isAllowedMachine(this.gunner, vehicle)) {
			this.machineId = vehicle.getId();
			return vehicle;
		}
		this.machineId = -1;
		return null;
	}
}
