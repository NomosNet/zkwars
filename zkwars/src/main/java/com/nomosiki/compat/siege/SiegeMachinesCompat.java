package com.nomosiki.compat.siege;

import com.nomosiki.entity.NomosikEntity;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Soft compat with Medieval Siege Machines. Public methods stay on vanilla types so the
 * access class is only loaded when the mod is present.
 */
public final class SiegeMachinesCompat {
	public static final String MOD_ID = "siegemachines";
	public static final double SEARCH_RADIUS = 24.0;
	public static final double MOUNT_REACH = 4.0;

	private SiegeMachinesCompat() {
	}

	public static boolean isLoaded() {
		return FabricLoader.getInstance().isModLoaded(MOD_ID);
	}

	public static ResourceLocation machineId(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}

	public static boolean isAllowedMachine(NomosikEntity gunner, Entity machine) {
		if (!isLoaded() || gunner == null || machine == null) {
			return false;
		}
		return SiegeMachinesCompatAccess.isAllowedMachine(gunner, machine);
	}

	public static boolean isMountedOnAllowed(NomosikEntity gunner) {
		if (!isLoaded() || gunner == null) {
			return false;
		}
		Entity vehicle = gunner.getVehicle();
		return vehicle != null && isAllowedMachine(gunner, vehicle);
	}

	@Nullable
	public static Entity findNearestAllowedEmpty(NomosikEntity gunner, double radius, Set<Integer> claimedIds) {
		if (!isLoaded() || gunner == null || !(gunner.level() instanceof ServerLevel)) {
			return null;
		}
		return SiegeMachinesCompatAccess.findNearestAllowedEmpty(gunner, radius, claimedIds);
	}

	public static int tryMountNearby(Collection<NomosikEntity> gunners, ServerLevel level, double radius) {
		if (!isLoaded() || gunners == null || gunners.isEmpty()) {
			return 0;
		}
		Set<Integer> claimed = new HashSet<>();
		int mounted = 0;
		for (NomosikEntity gunner : gunners) {
			if (gunner == null || !gunner.isAlive() || gunner.isRemoved() || gunner.isPassenger()) {
				continue;
			}
			Entity machine = findNearestAllowedEmpty(gunner, radius, claimed);
			if (machine == null) {
				continue;
			}
			if (tryMount(gunner, machine)) {
				claimed.add(machine.getId());
				mounted++;
			}
		}
		return mounted;
	}

	public static boolean tryMount(NomosikEntity gunner, Entity machine) {
		if (!isLoaded() || gunner == null || machine == null || gunner.isPassenger()) {
			return false;
		}
		if (!isAllowedMachine(gunner, machine) || !machine.getPassengers().isEmpty()) {
			return false;
		}
		return gunner.startRiding(machine, true);
	}

	public static void dismount(NomosikEntity gunner) {
		if (gunner == null || !gunner.isPassenger()) {
			return;
		}
		gunner.stopRiding();
	}

	public static boolean transferAmmoFrom(NomosikEntity gunner, Entity machine) {
		if (!isLoaded()) {
			return false;
		}
		return SiegeMachinesCompatAccess.transferAmmoFrom(gunner, machine);
	}

	public static boolean hasAmmoReady(Entity machine) {
		return isLoaded() && SiegeMachinesCompatAccess.hasAmmoReady(machine);
	}

	public static boolean aimAt(Entity machine, Vec3 target) {
		return isLoaded() && SiegeMachinesCompatAccess.aimAt(machine, target);
	}

	public static boolean isAimedAt(Entity machine, Vec3 target, float toleranceDeg) {
		return isLoaded() && SiegeMachinesCompatAccess.isAimedAt(machine, target, toleranceDeg);
	}

	public static boolean isReadyToFire(Entity machine) {
		return isLoaded() && SiegeMachinesCompatAccess.isReadyToFire(machine);
	}

	public static boolean tryFire(Entity machine, NomosikEntity gunner) {
		return isLoaded() && SiegeMachinesCompatAccess.tryFire(machine, gunner);
	}

	public static void giveStarterAmmo(NomosikEntity gunner) {
		if (!isLoaded() || gunner == null) {
			return;
		}
		gunner.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.COBBLESTONE, 8));
		Item cannonball = SiegeMachinesCompatAccess.cannonballItem();
		gunner.setItemSlot(EquipmentSlot.OFFHAND,
			cannonball != null && cannonball != Items.AIR ? new ItemStack(cannonball, 8) : new ItemStack(Items.GUNPOWDER, 8));
	}

	public static boolean usesFixedArc(Entity machine) {
		return isLoaded() && SiegeMachinesCompatAccess.usesFixedArc(machine);
	}
}
