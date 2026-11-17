package com.nomosiki.compat.gun;

import com.nomosiki.NomosikiMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Reflective access to zk-nomosiki-items {@code GunItem}. Kept off the compile classpath because the
 * work jar is built with a newer Loom than Nomosiki.
 */
final class GunsCompatAccess {
	private static final double BULLET_GRAVITY = 0.05;

	private static boolean resolved;
	private static boolean available;
	private static MethodHandle isHoldingGun;
	private static MethodHandle getHoldingHand;
	private static MethodHandle canUse;
	private static MethodHandle isReady;
	private static MethodHandle isLoaded;
	private static MethodHandle setLoaded;
	private static MethodHandle setLoadingStage;
	private static MethodHandle mobReload;
	private static MethodHandle mobUse;
	private static MethodHandle aimAtEntity;
	private static MethodHandle bulletSpeed;
	private static MethodHandle bulletDropReduction;

	private GunsCompatAccess() {
	}

	private static boolean ensureResolved() {
		if (resolved) {
			return available;
		}
		resolved = true;
		try {
			Class<?> gunItem = Class.forName("com.nomosiki.work.gun.GunItem");
			MethodHandles.Lookup lookup = MethodHandles.lookup();
			isHoldingGun = lookup.findStatic(gunItem, "isHoldingGun", MethodType.methodType(boolean.class, LivingEntity.class));
			getHoldingHand = lookup.findStatic(gunItem, "getHoldingHand", MethodType.methodType(InteractionHand.class, LivingEntity.class));
			canUse = lookup.findStatic(gunItem, "canUse", MethodType.methodType(boolean.class, LivingEntity.class));
			isReady = lookup.findStatic(gunItem, "isReady", MethodType.methodType(boolean.class, ItemStack.class));
			isLoaded = lookup.findStatic(gunItem, "isLoaded", MethodType.methodType(boolean.class, ItemStack.class));
			setLoaded = lookup.findStatic(gunItem, "setLoaded", MethodType.methodType(void.class, ItemStack.class, boolean.class));
			setLoadingStage = lookup.findStatic(gunItem, "setLoadingStage", MethodType.methodType(void.class, ItemStack.class, int.class));
			mobReload = lookup.findStatic(gunItem, "mobReload", MethodType.methodType(void.class, LivingEntity.class, InteractionHand.class));
			mobUse = lookup.findVirtual(gunItem, "mobUse", MethodType.methodType(void.class, LivingEntity.class, InteractionHand.class, Vec3.class));
			aimAtEntity = lookup.findVirtual(gunItem, "aimAt", MethodType.methodType(Vec3.class, LivingEntity.class, LivingEntity.class));
			bulletSpeed = lookup.findVirtual(gunItem, "bulletSpeed", MethodType.methodType(float.class));
			bulletDropReduction = lookup.findVirtual(gunItem, "bulletDropReduction", MethodType.methodType(float.class));
			available = true;
		} catch (ReflectiveOperationException ex) {
			NomosikiMod.LOGGER.warn("nomosiki: zk-nomosiki-items gun API is present but could not be bound", ex);
			available = false;
		}
		return available;
	}

	static boolean isHoldingGun(LivingEntity entity) {
		if (!ensureResolved()) {
			return false;
		}
		try {
			return (boolean) isHoldingGun.invoke(entity);
		} catch (Throwable ignored) {
			return false;
		}
	}

	static boolean canUseGun(LivingEntity entity) {
		if (!ensureResolved()) {
			return false;
		}
		try {
			return (boolean) isHoldingGun.invoke(entity) && (boolean) canUse.invoke(entity);
		} catch (Throwable ignored) {
			return false;
		}
	}

	static boolean isHoldingBayonet(LivingEntity entity) {
		ItemStack stack = heldGun(entity);
		if (stack.isEmpty()) {
			return false;
		}
		return GunsCompat.itemId("musket_with_bayonet").equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
	}

	static boolean isGunReady(LivingEntity entity) {
		if (!ensureResolved()) {
			return false;
		}
		ItemStack stack = heldGun(entity);
		if (stack.isEmpty()) {
			return false;
		}
		try {
			return (boolean) isReady.invoke(stack);
		} catch (Throwable ignored) {
			return false;
		}
	}

	static boolean isGunLoaded(ItemStack stack) {
		if (!ensureResolved() || stack == null || stack.isEmpty()) {
			return false;
		}
		try {
			return (boolean) isLoaded.invoke(stack);
		} catch (Throwable ignored) {
			return false;
		}
	}

	static float attackRadius(LivingEntity entity) {
		ItemStack stack = heldGun(entity);
		if (stack.isEmpty()) {
			return GunsCompat.RADIUS_MUSKET;
		}
		ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		if (id == null || !GunsCompat.MOD_ID.equals(id.getNamespace())) {
			return GunsCompat.RADIUS_MUSKET;
		}
		return switch (id.getPath()) {
			case "pistol", "blunderbuss" -> GunsCompat.RADIUS_SHORT;
			case "musket_with_scope" -> GunsCompat.RADIUS_SCOPED;
			default -> GunsCompat.RADIUS_MUSKET;
		};
	}

	static void unloadHeldGun(LivingEntity entity) {
		if (!ensureResolved() || entity == null) {
			return;
		}
		ItemStack stack = heldGun(entity);
		if (stack.isEmpty()) {
			return;
		}
		try {
			setLoaded.invoke(stack, false);
			setLoadingStage.invoke(stack, 0);
		} catch (Throwable ignored) {
		}
	}

	static void mobReload(LivingEntity entity) {
		if (!ensureResolved()) {
			return;
		}
		InteractionHand hand = holdingHand(entity);
		if (hand == null) {
			return;
		}
		try {
			mobReload.invoke(entity, hand);
		} catch (Throwable ignored) {
		}
	}

	static void fireAt(LivingEntity shooter, LivingEntity target) {
		if (!ensureResolved()) {
			return;
		}
		InteractionHand hand = holdingHand(shooter);
		ItemStack stack = hand == null ? ItemStack.EMPTY : shooter.getItemInHand(hand);
		Object gun = gunItem(stack);
		if (hand == null || gun == null) {
			return;
		}
		try {
			if (!(boolean) isLoaded.invoke(stack)) {
				return;
			}
			Vec3 direction = (Vec3) aimAtEntity.invoke(gun, shooter, target);
			mobUse.invoke(gun, shooter, hand, direction);
		} catch (Throwable ignored) {
		}
	}

	static void fireAt(LivingEntity shooter, Vec3 aimPoint) {
		if (!ensureResolved()) {
			return;
		}
		InteractionHand hand = holdingHand(shooter);
		ItemStack stack = hand == null ? ItemStack.EMPTY : shooter.getItemInHand(hand);
		Object gun = gunItem(stack);
		if (hand == null || gun == null) {
			return;
		}
		try {
			if (!(boolean) isLoaded.invoke(stack)) {
				return;
			}
			mobUse.invoke(gun, shooter, hand, aimDirection(shooter, gun, aimPoint));
		} catch (Throwable ignored) {
		}
	}

	static ItemStack stack(String path, int count) {
		Item item = BuiltInRegistries.ITEM.get(GunsCompat.itemId(path));
		if (item == null || item == Items.AIR || count <= 0) {
			return ItemStack.EMPTY;
		}
		return new ItemStack(item, Math.min(count, item.getDefaultMaxStackSize()));
	}

	static boolean isGun(ItemStack stack) {
		return gunItem(stack) != null;
	}

	static boolean isCartridge(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}
		return GunsCompat.itemId("cartridge").equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
	}

	private static ItemStack heldGun(LivingEntity entity) {
		InteractionHand hand = holdingHand(entity);
		return hand == null ? ItemStack.EMPTY : entity.getItemInHand(hand);
	}

	private static InteractionHand holdingHand(LivingEntity entity) {
		if (!ensureResolved()) {
			return null;
		}
		try {
			return (InteractionHand) getHoldingHand.invoke(entity);
		} catch (Throwable ignored) {
			return null;
		}
	}

	private static Object gunItem(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return null;
		}
		Item item = stack.getItem();
		return item.getClass().getName().startsWith("com.nomosiki.work.gun.") ? item : null;
	}

	private static Vec3 aimDirection(LivingEntity shooter, Object gun, Vec3 target) {
		float speed = 180.0F;
		float dropReduction = 0.0F;
		try {
			speed = (float) bulletSpeed.invoke(gun);
			dropReduction = (float) bulletDropReduction.invoke(gun);
		} catch (Throwable ignored) {
		}
		double dist = shooter.getEyePosition().distanceTo(target);
		double ticks = 20.0 * dist / Math.max(1.0F, speed);
		double bulletDrop = 0.5 * ticks * ticks * BULLET_GRAVITY * (1.0 - dropReduction);
		return new Vec3(
			target.x - shooter.getX(),
			target.y + bulletDrop - shooter.getEyeY(),
			target.z - shooter.getZ()
		).normalize();
	}
}
