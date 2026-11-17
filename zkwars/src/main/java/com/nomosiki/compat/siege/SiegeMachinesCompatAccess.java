package com.nomosiki.compat.siege;

import com.nomosiki.entity.NomosikEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import ru.magistu.siegemachines.api.entity.Shootable;
import ru.magistu.siegemachines.api.entity.Useable;
import ru.magistu.siegemachines.entity.machine.Machine;
import ru.magistu.siegemachines.entity.machine.ShootingMachine;

import java.util.List;
import java.util.Set;

final class SiegeMachinesCompatAccess {
	private static final ResourceLocation TREBUCHET = SiegeMachinesCompat.machineId("trebuchet");
	private static final ResourceLocation CATAPULT = SiegeMachinesCompat.machineId("catapult");
	private static final ResourceLocation MORTAR = SiegeMachinesCompat.machineId("mortar");
	private static final ResourceLocation CULVERIN = SiegeMachinesCompat.machineId("culverin");
	private static final ResourceLocation CANNONBALL = SiegeMachinesCompat.machineId("cannonball");

	private SiegeMachinesCompatAccess() {
	}

	static boolean isAllowedMachine(NomosikEntity gunner, Entity machine) {
		if (!(machine instanceof ShootingMachine) || !machine.isAlive() || machine.isRemoved()) {
			return false;
		}
		ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(machine.getType());
		if (id == null) {
			return false;
		}
		return TREBUCHET.equals(id) || CATAPULT.equals(id) || MORTAR.equals(id) || CULVERIN.equals(id);
	}

	@Nullable
	static Entity findNearestAllowedEmpty(NomosikEntity gunner, double radius, Set<Integer> claimedIds) {
		AABB box = gunner.getBoundingBox().inflate(radius);
		List<Entity> found = gunner.level().getEntities(gunner, box, machine ->
			isAllowedMachine(gunner, machine)
				&& machine.getPassengers().isEmpty()
				&& (claimedIds == null || !claimedIds.contains(machine.getId())));
		Entity nearest = null;
		double best = radius * radius;
		for (Entity machine : found) {
			double dist = gunner.distanceToSqr(machine);
			if (dist <= best) {
				best = dist;
				nearest = machine;
			}
		}
		return nearest;
	}

	static boolean transferAmmoFrom(NomosikEntity gunner, Entity machine) {
		if (!(machine instanceof Shootable shootable) || gunner == null) {
			return hasAmmoReady(machine);
		}
		if (machine instanceof ShootingMachine sm && sm.type.usesgunpowder && !sm.inventory.containsItem(Items.GUNPOWDER)) {
			moveOneFromUnit(gunner, shootable, Items.GUNPOWDER);
		}
		if (!hasProjectileAmmo(machine)) {
			moveMatchingProjectile(gunner, shootable);
		}
		return hasAmmoReady(machine);
	}

	static boolean hasAmmoReady(Entity machine) {
		if (!(machine instanceof Shootable shootable) || !shootable.hasAmmo()) {
			return false;
		}
		if (machine instanceof ShootingMachine sm && sm.type.usesgunpowder && !sm.inventory.containsItem(Items.GUNPOWDER)) {
			return false;
		}
		return true;
	}

	static boolean aimAt(Entity machine, Vec3 target) {
		if (!(machine instanceof Useable useable)) {
			return false;
		}
		Vec3 from = machine.position();
		if (machine instanceof Shootable shootable) {
			from = shootable.getShotPos();
		}
		double dx = target.x - from.x;
		double dy = target.y - from.y;
		double dz = target.z - from.z;
		float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
		float horiz = (float) Math.sqrt(dx * dx + dz * dz);
		float pitch;
		if (machine instanceof Shootable shootable && usesGunpowder(machine)) {
			pitch = ballisticPitch(horiz, dy, shootable.getProjectileInitSpeed(), 0.03F);
		} else {
			pitch = (float) (-Mth.atan2(dy, horiz) * (180.0 / Math.PI));
		}
		useable.setYawDest(yaw);
		useable.setTurretRotationsDest(pitch, 0.0F);
		return true;
	}

	static boolean isAimedAt(Entity machine, Vec3 target, float toleranceDeg) {
		if (!(machine instanceof Useable useable)) {
			return false;
		}
		Vec3 from = machine.position();
		if (machine instanceof Shootable shootable) {
			from = shootable.getShotPos();
		}
		double dx = target.x - from.x;
		double dz = target.z - from.z;
		float wantYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
		float yawErr = Mth.abs(Mth.wrapDegrees(useable.getGlobalTurretYaw() - wantYaw));
		if (yawErr > toleranceDeg) {
			return false;
		}
		if (usesFixedArc(machine)) {
			return true;
		}
		double dy = target.y - from.y;
		float horiz = (float) Math.sqrt(dx * dx + dz * dz);
		float wantPitch = (float) (-Mth.atan2(dy, horiz) * (180.0 / Math.PI));
		if (machine instanceof Shootable shootable && usesGunpowder(machine)) {
			wantPitch = ballisticPitch(horiz, dy, shootable.getProjectileInitSpeed(), 0.03F);
		}
		return Mth.abs(Mth.wrapDegrees(useable.getTurretPitch() - wantPitch)) <= toleranceDeg;
	}

	static boolean isReadyToFire(Entity machine) {
		if (!(machine instanceof Useable useable) || !(machine instanceof Shootable shootable)) {
			return false;
		}
		return useable.getDelayTicks() <= 0 && useable.getUseTicks() <= 0 && shootable.getShootingTicks() <= 0;
	}

	static boolean tryFire(Entity machine, NomosikEntity gunner) {
		if (!(machine instanceof Shootable shootable) || gunner == null) {
			return false;
		}
		if (!isReadyToFire(machine) || !hasAmmoReady(machine)) {
			return false;
		}
		shootable.use(gunner);
		return true;
	}

	static boolean usesFixedArc(Entity machine) {
		ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(machine.getType());
		return TREBUCHET.equals(id) || CATAPULT.equals(id);
	}

	static Item cannonballItem() {
		return BuiltInRegistries.ITEM.get(CANNONBALL);
	}

	private static boolean usesGunpowder(Entity machine) {
		return machine instanceof Machine m && m.type.usesgunpowder;
	}

	private static boolean hasProjectileAmmo(Entity machine) {
		if (!(machine instanceof ShootingMachine sm)) {
			return false;
		}
		for (Object raw : sm.inventory.getItems()) {
			if (raw instanceof ItemStack stack && !stack.isEmpty() && stack.getItem() != Items.GUNPOWDER && sm.isValidAmmo(stack)) {
				return true;
			}
		}
		return false;
	}

	private static void moveMatchingProjectile(NomosikEntity gunner, Shootable shootable) {
		List<?> valid = shootable.getValidAmmo();
		if (valid == null) {
			return;
		}
		for (Object entry : valid) {
			if (entry instanceof Item item && item != Items.GUNPOWDER) {
				if (moveOneFromUnit(gunner, shootable, item)) {
					return;
				}
			}
		}
	}

	private static boolean moveOneFromUnit(NomosikEntity gunner, Shootable shootable, Item item) {
		for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND }) {
			ItemStack held = gunner.getItemBySlot(slot);
			if (held.isEmpty() || held.getItem() != item) {
				continue;
			}
			ItemStack one = held.split(1);
			ItemStack leftover = shootable.reload(one);
			if (!leftover.isEmpty()) {
				if (held.isEmpty()) {
					gunner.setItemSlot(slot, leftover);
				} else {
					held.grow(leftover.getCount());
					gunner.setItemSlot(slot, held);
				}
				return false;
			}
			gunner.setItemSlot(slot, held.isEmpty() ? ItemStack.EMPTY : held);
			return true;
		}
		return false;
	}

	private static float ballisticPitch(double distance, double height, float speed, float gravity) {
		if (speed < 0.1F || distance < 0.1) {
			return 0.0F;
		}
		double v2 = speed * speed;
		double g = Math.max(0.001, gravity);
		double disc = v2 * v2 - g * (g * distance * distance + 2.0 * height * v2);
		if (disc < 0) {
			return -45.0F;
		}
		double root = Math.sqrt(disc);
		double low = Math.atan((v2 - root) / (g * distance));
		return (float) (-low * (180.0 / Math.PI));
	}
}
