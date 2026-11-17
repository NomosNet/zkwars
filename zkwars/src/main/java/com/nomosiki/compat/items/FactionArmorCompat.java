package com.nomosiki.compat.items;

import com.nomosiki.NomosikiMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;

/**
 * Optional call into ZK Nomosiki Items. Factions keep working when that mod is absent.
 */
public final class FactionArmorCompat {
	private static final String TINT = "com.nomosiki.work.armor.FactionArmorTint";
	private static volatile Method refreshLoaded;
	private static volatile Method refreshWorn;
	private static volatile Method apply;
	private static volatile Method recolorLoaded;
	private static volatile Method dye;
	private static volatile boolean resolved;

	private FactionArmorCompat() {
	}

	public static void refreshLoaded(MinecraftServer server) {
		Method method = refreshLoaded();
		if (method == null || server == null) {
			return;
		}
		try {
			method.invoke(null, server);
		} catch (ReflectiveOperationException exception) {
			NomosikiMod.LOGGER.warn("Could not recolor faction samurai armor", exception);
		}
	}

	/**
	 * Rewrites worn faction armor and reports how many pieces were sent to clients.
	 * Returns {@code 0} when ZK Nomosiki Items is absent.
	 */
	public static int recolorLoaded(MinecraftServer server) {
		Method method = recolorLoaded();
		if (method == null || server == null) {
			return 0;
		}
		try {
			Object value = method.invoke(null, server);
			return value instanceof Integer count ? count : 0;
		} catch (ReflectiveOperationException exception) {
			NomosikiMod.LOGGER.warn("Could not recolor faction samurai armor", exception);
			return 0;
		}
	}

	public static void refreshWorn(LivingEntity entity) {
		Method method = refreshWorn();
		if (method == null || entity == null) {
			return;
		}
		try {
			method.invoke(null, entity);
		} catch (ReflectiveOperationException exception) {
			NomosikiMod.LOGGER.warn("Could not recolor faction samurai armor", exception);
		}
	}

	public static void apply(LivingEntity entity, EquipmentSlot slot, ItemStack stack) {
		Method method = apply();
		if (method == null || entity == null || slot == null || stack == null || stack.isEmpty()) {
			return;
		}
		try {
			method.invoke(null, entity, slot, stack);
		} catch (ReflectiveOperationException exception) {
			NomosikiMod.LOGGER.warn("Could not recolor faction samurai armor", exception);
		}
	}

	/** Four dyes of {@code color} on a steel or iron samurai piece. No-op when that mod is absent. */
	public static boolean dye(ItemStack stack, DyeColor color) {
		Method method = dye();
		if (method == null || stack == null || stack.isEmpty() || color == null) {
			return false;
		}
		try {
			Object value = method.invoke(null, stack, color);
			return value instanceof Boolean changed && changed;
		} catch (ReflectiveOperationException exception) {
			NomosikiMod.LOGGER.warn("Could not recolor faction samurai armor", exception);
			return false;
		}
	}

	private static Method refreshLoaded() {
		resolve();
		return refreshLoaded;
	}

	private static Method recolorLoaded() {
		resolve();
		return recolorLoaded;
	}

	private static Method refreshWorn() {
		resolve();
		return refreshWorn;
	}

	private static Method apply() {
		resolve();
		return apply;
	}

	private static Method dye() {
		resolve();
		return dye;
	}

	private static void resolve() {
		if (resolved) {
			return;
		}
		resolved = true;
		try {
			Class<?> type = Class.forName(TINT);
			refreshLoaded = method(type, "refreshLoaded", MinecraftServer.class);
			recolorLoaded = method(type, "recolorLoaded", MinecraftServer.class);
			refreshWorn = method(type, "refreshWorn", LivingEntity.class);
			apply = method(type, "apply", LivingEntity.class, EquipmentSlot.class, ItemStack.class);
			dye = method(type, "dye", ItemStack.class, DyeColor.class);
		} catch (ClassNotFoundException ignored) {
			refreshLoaded = null;
			recolorLoaded = null;
			refreshWorn = null;
			apply = null;
			dye = null;
		}
	}

	private static Method method(Class<?> type, String name, Class<?>... params) {
		try {
			return type.getMethod(name, params);
		} catch (ReflectiveOperationException exception) {
			NomosikiMod.LOGGER.warn("Faction armor tint method {} did not bind", name, exception);
			return null;
		}
	}
}
