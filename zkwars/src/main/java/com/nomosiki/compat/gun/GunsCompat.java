package com.nomosiki.compat.gun;

import com.nomosiki.entity.NomosikCombatMode;
import com.nomosiki.entity.NomosikEntity;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Soft compat with zkwars-items firearms. Public methods stay on vanilla types so gun classes
 * are resolved only when the mod is present.
 */
public final class GunsCompat {
	public static final String MOD_ID = "zkwars-items";
	public static final float RADIUS_MUSKET = 15.0F;
	public static final float RADIUS_SCOPED = 20.0F;
	public static final float RADIUS_SHORT = 8.0F;
	public static final int STARTER_CARTRIDGES = 16;

	private GunsCompat() {
	}

	public static boolean isLoaded() {
		return FabricLoader.getInstance().isModLoaded(MOD_ID);
	}

	public static ResourceLocation itemId(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}

	public static boolean isHoldingGun(LivingEntity entity) {
		return isLoaded() && entity != null && GunsCompatAccess.isHoldingGun(entity);
	}

	public static boolean canUseGun(LivingEntity entity) {
		return isLoaded() && entity != null && GunsCompatAccess.canUseGun(entity);
	}

	public static boolean isHoldingBayonet(LivingEntity entity) {
		return isLoaded() && entity != null && GunsCompatAccess.isHoldingBayonet(entity);
	}

	public static boolean isGunReady(LivingEntity entity) {
		return isLoaded() && entity != null && GunsCompatAccess.isGunReady(entity);
	}

	public static boolean isGunLoaded(ItemStack stack) {
		return isLoaded() && stack != null && GunsCompatAccess.isGunLoaded(stack);
	}

	public static float attackRadius(LivingEntity entity) {
		if (!isLoaded() || entity == null) {
			return RADIUS_MUSKET;
		}
		return GunsCompatAccess.attackRadius(entity);
	}

	public static void mobReload(LivingEntity entity) {
		if (!isLoaded() || entity == null) {
			return;
		}
		GunsCompatAccess.mobReload(entity);
	}

	public static void unloadHeldGun(LivingEntity entity) {
		if (!isLoaded() || entity == null) {
			return;
		}
		GunsCompatAccess.unloadHeldGun(entity);
	}

	public static void fireAt(LivingEntity shooter, LivingEntity target) {
		if (!isLoaded() || shooter == null || target == null) {
			return;
		}
		GunsCompatAccess.fireAt(shooter, target);
	}

	public static void fireAt(LivingEntity shooter, Vec3 aimPoint) {
		if (!isLoaded() || shooter == null || aimPoint == null) {
			return;
		}
		GunsCompatAccess.fireAt(shooter, aimPoint);
	}

	public static ItemStack musketStack() {
		return isLoaded() ? GunsCompatAccess.stack("musket", 1) : ItemStack.EMPTY;
	}

	public static ItemStack bayonetStack() {
		return isLoaded() ? GunsCompatAccess.stack("musket_with_bayonet", 1) : ItemStack.EMPTY;
	}

	public static ItemStack scopedMusketStack() {
		return isLoaded() ? GunsCompatAccess.stack("musket_with_scope", 1) : ItemStack.EMPTY;
	}

	public static ItemStack blunderbussStack() {
		return isLoaded() ? GunsCompatAccess.stack("blunderbuss", 1) : ItemStack.EMPTY;
	}

	public static ItemStack pistolStack() {
		return isLoaded() ? GunsCompatAccess.stack("pistol", 1) : ItemStack.EMPTY;
	}

	public static ItemStack cartridgeStack(int count) {
		return isLoaded() ? GunsCompatAccess.stack("cartridge", count) : ItemStack.EMPTY;
	}

	public static boolean isGun(ItemStack stack) {
		return isLoaded() && stack != null && GunsCompatAccess.isGun(stack);
	}

	public static boolean isCartridge(ItemStack stack) {
		return isLoaded() && stack != null && GunsCompatAccess.isCartridge(stack);
	}

	public static void giveDefaultLoadout(@Nullable NomosikEntity entity) {
		if (entity == null) {
			return;
		}
		EquipmentSlot gunSlot = gunHandSlot(entity);
		EquipmentSlot ammoSlot = ammoHandSlot(entity);
		entity.setItemSlot(gunSlot, musketStack());
		entity.setItemSlot(ammoSlot, cartridgeStack(STARTER_CARTRIDGES));
	}

	public static void applyStanceHands(@Nullable NomosikEntity entity) {
		if (entity == null || entity.level().isClientSide()) {
			return;
		}
		EquipmentSlot left = leftHandSlot(entity);
		EquipmentSlot right = rightHandSlot(entity);
		boolean gunInLeft = entity.getCombatMode() == NomosikCombatMode.PASSIVE;
		ItemStack leftItem = entity.getItemBySlot(left);
		ItemStack rightItem = entity.getItemBySlot(right);
		boolean leftGun = isGun(leftItem);
		boolean rightGun = isGun(rightItem);
		boolean leftAmmo = isCartridge(leftItem);
		boolean rightAmmo = isCartridge(rightItem);
		if (!leftGun && !rightGun && !leftAmmo && !rightAmmo) {
			return;
		}
		boolean shouldSwap = gunInLeft
			? rightGun || leftAmmo
			: leftGun || rightAmmo;
		if (!shouldSwap) {
			return;
		}
		entity.stopUsingItem();
		entity.setItemSlot(left, rightItem.copy());
		entity.setItemSlot(right, leftItem.copy());
	}

	private static EquipmentSlot gunHandSlot(NomosikEntity entity) {
		return entity.getCombatMode() == NomosikCombatMode.PASSIVE ? leftHandSlot(entity) : rightHandSlot(entity);
	}

	private static EquipmentSlot ammoHandSlot(NomosikEntity entity) {
		return entity.getCombatMode() == NomosikCombatMode.PASSIVE ? rightHandSlot(entity) : leftHandSlot(entity);
	}

	private static EquipmentSlot leftHandSlot(LivingEntity entity) {
		return entity.getMainArm() == HumanoidArm.RIGHT ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
	}

	private static EquipmentSlot rightHandSlot(LivingEntity entity) {
		return entity.getMainArm() == HumanoidArm.RIGHT ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
	}
}
