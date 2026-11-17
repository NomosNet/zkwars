package com.nomosiki.registry;

import com.nomosiki.NomosikiMod;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Creative inventory tab for Nomosiki items and spawn eggs.
 * Future races append more eggs via {@link ModItems#appendCreativeTabItems}.
 */
public final class ModItemGroups {
	public static final ResourceKey<CreativeModeTab> MAIN = ResourceKey.create(
		Registries.CREATIVE_MODE_TAB,
		NomosikiMod.id("main"));

	private ModItemGroups() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, MAIN, FabricItemGroup.builder()
			.icon(ModItemGroups::tabIcon)
			.title(Component.translatable("itemGroup.zkwars.main"))
			.displayItems((params, entries) -> ModItems.appendCreativeTabItems(entries))
			.build());
	}

	private static ItemStack tabIcon() {
		List<Item> eggs = ModItems.playableUnitSpawnEggs();
		Item icon = eggs.isEmpty() ? ModItems.NOMOSIK_SPAWN_EGG : eggs.getFirst();
		return new ItemStack(icon);
	}
}
