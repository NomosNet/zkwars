package com.nomosiki.work.registry;

import com.nomosiki.work.NomosikiWork;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class ModItemGroups {
	public static final ResourceKey<CreativeModeTab> MAIN = ResourceKey.create(
		Registries.CREATIVE_MODE_TAB,
		NomosikiWork.id("main"));

	private ModItemGroups() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, MAIN, FabricItemGroup.builder()
			.icon(() -> new ItemStack(ModItems.KATANA))
			.title(Component.translatable("itemGroup.zk-nomosiki-items.main"))
			.displayItems((params, entries) -> ModItems.appendCreativeTabItems(entries))
			.build());
	}
}
