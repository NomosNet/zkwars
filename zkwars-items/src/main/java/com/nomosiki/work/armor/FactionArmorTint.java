package com.nomosiki.work.armor;

import com.mojang.datafixers.util.Pair;
import com.nomosiki.work.NomosikiWork;
import com.nomosiki.work.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Recolors worn steel and iron samurai armor to the wearer's Nomosiki faction dye.
 * Nomosiki is optional: with it absent, armor keeps the color it was crafted with.
 */
public final class FactionArmorTint {
	private static final String API = "com.nomosiki.api.NomosikiFactionApi";
	private static final int DYE_COUNT = 4;
	private static volatile Method armorDyeName;
	private static volatile boolean resolved;

	private FactionArmorTint() {
	}

	/**
	 * Clears any previous dye and applies four items of {@code color}, the vanilla leather mix.
	 * Four of the same dye on a clean piece is that dye, so the worn texture name matches the banner.
	 *
	 * @return {@code true} when the stack's dye component changed
	 */
	public static boolean dye(ItemStack stack, DyeColor color) {
		if (stack == null || stack.isEmpty() || color == null || !isFactionSamurai(stack)) {
			return false;
		}
		int rgb = fourDyeRgb(stack, color);
		if (rgb < 0 || alreadyDyed(stack, rgb)) {
			return false;
		}
		stack.set(DataComponents.DYED_COLOR, new DyedItemColor(rgb, true));
		return true;
	}

	public static void apply(LivingEntity entity, EquipmentSlot slot, ItemStack stack) {
		if (entity == null || stack == null || stack.isEmpty() || slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) {
			return;
		}
		if (entity.level().isClientSide() || !isFactionSamurai(stack)) {
			return;
		}
		DyeColor color = factionDye(entity);
		if (color == null) {
			return;
		}
		dye(stack, color);
	}

	public static void refreshWorn(LivingEntity entity) {
		refreshWorn(entity, false);
	}

	public static void refreshLoaded(MinecraftServer server) {
		recolorLoaded(server, false);
	}

	/** Rewrites every worn faction piece and tells tracking clients, even when the dye already matches. */
	public static int recolorLoaded(MinecraftServer server) {
		return recolorLoaded(server, true);
	}

	private static int recolorLoaded(MinecraftServer server, boolean force) {
		if (server == null) {
			return 0;
		}
		List<LivingEntity> living = new ArrayList<>();
		for (ServerLevel level : server.getAllLevels()) {
			for (Entity entity : level.getAllEntities()) {
				if (entity instanceof LivingEntity livingEntity) {
					living.add(livingEntity);
				}
			}
		}
		int updated = 0;
		for (LivingEntity entity : living) {
			updated += refreshWorn(entity, force);
		}
		return updated;
	}

	private static int refreshWorn(LivingEntity entity, boolean force) {
		if (entity == null || entity.level().isClientSide() || !wearsFactionSamurai(entity)) {
			return 0;
		}
		DyeColor color = factionDye(entity);
		if (color == null) {
			return 0;
		}
		List<Pair<EquipmentSlot, ItemStack>> changed = new ArrayList<>();
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) {
				continue;
			}
			ItemStack stack = entity.getItemBySlot(slot);
			if (!isFactionSamurai(stack)) {
				continue;
			}
			boolean wrote = dye(stack, color);
			if (!wrote && !force) {
				continue;
			}
			ItemStack worn = stack.copy();
			dye(worn, color);
			entity.setItemSlot(slot, worn);
			changed.add(Pair.of(slot, worn));
		}
		if (!changed.isEmpty()) {
			broadcast(entity, changed);
		}
		return changed.size();
	}

	private static void broadcast(LivingEntity entity, List<Pair<EquipmentSlot, ItemStack>> slots) {
		if (!(entity.level() instanceof ServerLevel level) || slots.isEmpty()) {
			return;
		}
		level.getChunkSource().broadcastAndSend(
			entity,
			new ClientboundSetEquipmentPacket(entity.getId(), slots)
		);
	}

	private static int fourDyeRgb(ItemStack stack, DyeColor color) {
		ItemStack clean = stack.copy();
		clean.remove(DataComponents.DYED_COLOR);
		DyeItem dyeItem = DyeItem.byColor(color);
		List<DyeItem> dyes = List.of(dyeItem, dyeItem, dyeItem, dyeItem);
		ItemStack dyed = DyedItemColor.applyDyes(clean, dyes);
		if (dyed.isEmpty()) {
			return color.getTextureDiffuseColor();
		}
		DyedItemColor component = dyed.get(DataComponents.DYED_COLOR);
		return component == null ? color.getTextureDiffuseColor() : component.rgb();
	}

	private static boolean alreadyDyed(ItemStack stack, int rgb) {
		DyedItemColor current = stack.get(DataComponents.DYED_COLOR);
		return current != null && current.rgb() == rgb;
	}

	private static boolean wearsFactionSamurai(LivingEntity entity) {
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && isFactionSamurai(entity.getItemBySlot(slot))) {
				return true;
			}
		}
		return false;
	}

	private static boolean isFactionSamurai(ItemStack stack) {
		Item item = stack.getItem();
		return item == ModItems.STEEL_SAMURAI_HELMET
			|| item == ModItems.STEEL_SAMURAI_CHESTPLATE
			|| item == ModItems.STEEL_SAMURAI_LEGGINGS
			|| item == ModItems.STEEL_SAMURAI_BOOTS
			|| item == ModItems.IRON_SAMURAI_HELMET
			|| item == ModItems.IRON_SAMURAI_CHESTPLATE
			|| item == ModItems.IRON_SAMURAI_LEGGINGS
			|| item == ModItems.IRON_SAMURAI_BOOTS;
	}

	private static DyeColor factionDye(Entity entity) {
		Level level = entity.level();
		MinecraftServer server = level.getServer();
		if (server == null) {
			return null;
		}
		Method method = armorDyeName();
		if (method == null) {
			return null;
		}
		try {
			Object value = method.invoke(null, server, entity);
			if (!(value instanceof String name) || name.isEmpty()) {
				return null;
			}
			return DyeColor.byName(name, null);
		} catch (ReflectiveOperationException exception) {
			NomosikiWork.LOGGER.warn("Could not read the faction dye for samurai armor", exception);
			return null;
		}
	}

	private static Method armorDyeName() {
		if (resolved) {
			return armorDyeName;
		}
		resolved = true;
		try {
			Class<?> api = Class.forName(API);
			armorDyeName = api.getMethod("armorDyeName", MinecraftServer.class, Entity.class);
		} catch (ReflectiveOperationException exception) {
			NomosikiWork.LOGGER.warn("Faction dye name is not available; samurai armor keeps its crafted color", exception);
			armorDyeName = null;
		}
		return armorDyeName;
	}
}
