package com.nomosiki.entity;

import com.nomosiki.config.NomosikiServerConfig;
import com.nomosiki.squad.SquadType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Spawns a tamed horse for cavalry / nomads.
 * <p>
 * Mob passengers steer via AI {@code MoveControl}, not player {@code travelRidden}.
 * That path is slower than a player gallop at the same attribute, so defaults are calibrated
 * so cavalry feels like an average player-ridden vanilla horse (~9.5 blocks/s), not a crawl.
 */
public final class NomosikiHorseMount {
	/** Scoreboard tag on mounts spawned for nomosiki cavalry/nomads (cleanup / ownership). */
	public static final String MOUNT_TAG = "nomosiki_cavalry_mount";
	public static final double MOUNT_SEARCH_RADIUS = 24.0;

	private NomosikiHorseMount() {
	}

	public static void spawnTamedMountFor(NomosikEntity rider, ServerLevel level) {
		if (!rider.isAlive() || rider.isRemoved()) {
			return;
		}
		if (!needsExternalMount(rider)) {
			return;
		}
		Mob mount = createMount(level);
		if (mount == null) {
			return;
		}
		mount.moveTo(rider.getX(), rider.getY(), rider.getZ(), rider.getYRot(), rider.getXRot());
		prepareMount(mount);
		level.addFreshEntity(mount);
		rider.startRiding(mount, true);
	}

	/**
	 * Keeps a living tagged horse usable and ejects the rider in water or when the mount dies.
	 * Cushions and other vehicles are left alone. Never spawns a replacement horse.
	 */
	public static void ensureMountMatchesRole(NomosikEntity rider, ServerLevel level) {
		if (!rider.isAlive() || rider.isRemoved()) {
			return;
		}
		Entity vehicle = rider.getVehicle();
		if (!(vehicle instanceof AbstractHorse)) {
			return;
		}
		if (shouldDismountVehicle(vehicle)) {
			dismountKeepMount(rider);
			return;
		}
		if (vehicle.getType() == EntityType.HORSE && isNomosikiMountEntity(vehicle)) {
			if (vehicle instanceof Mob mob && mob.getTarget() != null) {
				mob.setTarget(null);
			}
		}
	}

	public static void dismountKeepMount(NomosikEntity rider) {
		rider.stopRiding();
	}

	/**
	 * Seats each unmounted rider on the nearest free horse within {@code radius}.
	 * Each horse is claimed at most once.
	 */
	public static int tryMountNearby(Collection<NomosikEntity> riders, ServerLevel level, double radius) {
		Set<Integer> claimedHorseIds = new HashSet<>();
		int mounted = 0;
		double radiusSqr = radius * radius;
		for (NomosikEntity rider : riders) {
			if (!rider.isAlive() || rider.isRemoved() || rider.isPassenger()) {
				continue;
			}
			AbstractHorse horse = findNearestFreeHorse(rider, level, radius, radiusSqr, claimedHorseIds);
			if (horse == null) {
				continue;
			}
			prepareMount(horse);
			if (rider.startRiding(horse, true)) {
				claimedHorseIds.add(horse.getId());
				mounted++;
			}
		}
		return mounted;
	}

	private static AbstractHorse findNearestFreeHorse(NomosikEntity rider, ServerLevel level, double radius,
		double radiusSqr, Set<Integer> claimedHorseIds) {
		AABB box = rider.getBoundingBox().inflate(radius);
		List<AbstractHorse> horses = level.getEntitiesOfClass(AbstractHorse.class, box,
			horse -> isFreeMountCandidate(horse) && !claimedHorseIds.contains(horse.getId()));
		AbstractHorse nearest = null;
		double nearestDist = radiusSqr;
		for (AbstractHorse horse : horses) {
			double dist = rider.distanceToSqr(horse);
			if (dist <= nearestDist) {
				nearestDist = dist;
				nearest = horse;
			}
		}
		return nearest;
	}

	private static boolean isFreeMountCandidate(AbstractHorse horse) {
		return horse.getType() == EntityType.HORSE
			&& horse.isAlive()
			&& !horse.isRemoved()
			&& horse.getPassengers().isEmpty()
			&& !isVehicleInWater(horse);
	}

	private static boolean shouldDismountVehicle(Entity vehicle) {
		return !vehicle.isAlive() || vehicle.isRemoved() || isVehicleInWater(vehicle);
	}

	private static boolean isVehicleInWater(Entity vehicle) {
		return vehicle.isInWater() || vehicle.isInWaterOrBubble();
	}

	private static void prepareMount(Mob mount) {
		mount.setNoAi(false);
		mount.setPersistenceRequired();
		mount.addTag(MOUNT_TAG);
		applyCavalryMountAttributes(mount);
		if (mount instanceof AbstractHorse horse) {
			horse.setTamed(true);
			if (!horse.isSaddled() && horse instanceof Saddleable saddleable && saddleable.isSaddleable()) {
				saddleable.equipSaddle(new ItemStack(Items.SADDLE), SoundSource.NEUTRAL);
			}
		}
	}

	private static boolean needsExternalMount(NomosikEntity rider) {
		SquadType branch = rider.getSquadBranchType();
		return branch.isCavalry();
	}

	@Nullable
	private static Mob createMount(ServerLevel level) {
		Entity created = EntityType.HORSE.create(level);
		return created instanceof Mob mob ? mob : null;
	}

	public static boolean isNomosikiMount(AbstractHorse mount) {
		return isNomosikiMountEntity(mount);
	}

	public static boolean isNomosikiMountEntity(Entity entity) {
		return entity != null && entity.getTags().contains(MOUNT_TAG);
	}

	/** Discards the rider's mount if it is a tagged nomosiki cavalry mount. */
	public static void discardMountOf(NomosikEntity rider) {
		Entity vehicle = rider.getVehicle();
		if (vehicle == null) {
			return;
		}
		rider.stopRiding();
		if (!isNomosikiMountEntity(vehicle)) {
			return;
		}
		vehicle.discard();
	}

	public static void applyCavalryMountAttributes(Mob mount) {
		NomosikiServerConfig.CavalrySettings cavalry = NomosikiServerConfig.INSTANCE.combat.cavalry;
		applyCavalryLocomotionAttributes(cavalry.horseMovementSpeed, cavalry.horseStepHeight,
			mount.getAttribute(Attributes.MOVEMENT_SPEED), mount.getAttribute(Attributes.STEP_HEIGHT));
	}

	/** @deprecated use {@link #applyCavalryMountAttributes(Mob)} */
	@Deprecated
	public static void applyCavalryMountAttributes(AbstractHorse mount, boolean donkey) {
		applyCavalryMountAttributes(mount);
	}

	public static void resetDefaultLocomotionAttributes(NomosikEntity unit) {
		applyCavalryLocomotionAttributes(NomosikEntity.DEFAULT_MOVEMENT_SPEED, NomosikEntity.DEFAULT_STEP_HEIGHT,
			unit.getAttribute(Attributes.MOVEMENT_SPEED), unit.getAttribute(Attributes.STEP_HEIGHT));
	}

	private static void applyCavalryLocomotionAttributes(double movementSpeed, double stepHeight,
		@Nullable AttributeInstance speed, @Nullable AttributeInstance step) {
		if (speed != null) {
			speed.setBaseValue(movementSpeed);
		}
		if (step != null) {
			step.setBaseValue(stepHeight);
		}
	}
}
