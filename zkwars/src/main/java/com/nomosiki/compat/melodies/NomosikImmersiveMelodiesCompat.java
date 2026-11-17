package com.nomosiki.compat.melodies;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Soft compat with Immersive Melodies: detect instruments via item tag, no compile dep.
 * IM itself autoplays for any non-player {@link LivingEntity} holding an instrument.
 */
public final class NomosikImmersiveMelodiesCompat {
	public static final String MOD_ID = "immersive_melodies";
	public static final TagKey<Item> INSTRUMENTS = TagKey.create(
		Registries.ITEM,
		ResourceLocation.fromNamespaceAndPath(MOD_ID, "instruments"));

	private NomosikImmersiveMelodiesCompat() {
	}

	public static boolean isModLoaded() {
		return FabricLoader.getInstance().isModLoaded(MOD_ID);
	}

	public static boolean isInstrument(ItemStack stack) {
		return !stack.isEmpty() && isModLoaded() && stack.is(INSTRUMENTS);
	}

	public static boolean isHoldingInstrument(LivingEntity entity) {
		return isInstrument(entity.getMainHandItem()) || isInstrument(entity.getOffhandItem());
	}
}
