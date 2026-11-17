package com.example.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * Soft compat with gensokyofood crops. Item and block classes are never referenced, so this mod
 * still loads when gensokyofood is absent.
 */
public final class GensokyoFoodCompat {
	public static final String MOD_ID = "gensokyofood";

	private static final Set<String> PRODUCE = Set.of(
			"cabbage",
			"tomato",
			"cucumber",
			"tea_leaves",
			"udumbara_flower",
			"red_grape",
			"black_grape",
			"white_grape",
			"soybean",
			"redbean",
			"onion",
			"rice",
			"straw"
	);

	private static final Map<String, String> IDLE_PRODUCTS = Map.ofEntries(
			Map.entry("cabbage_seeds", "cabbage"),
			Map.entry("tomato_seeds", "tomato"),
			Map.entry("cucumber_seeds", "cucumber"),
			Map.entry("tea_seeds", "tea_leaves"),
			Map.entry("udumbara_leaves", "udumbara_flower"),
			Map.entry("red_grape_seeds", "red_grape"),
			Map.entry("black_grape_seeds", "black_grape"),
			Map.entry("white_grape_seeds", "white_grape"),
			Map.entry("soybean", "soybean"),
			Map.entry("redbean", "redbean"),
			Map.entry("onion", "onion"),
			Map.entry("rice", "rice"),
			Map.entry("rice_seeds", "rice")
	);

	private GensokyoFoodCompat() {
	}

	public static boolean isLoaded() {
		return FabricLoader.getInstance().isModLoaded(MOD_ID);
	}

	public static boolean isRiceSeed(ItemStack stack) {
		if (!isLoaded() || stack.isEmpty()) {
			return false;
		}
		ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		return MOD_ID.equals(id.getNamespace()) && id.getPath().equals("rice_seeds");
	}

	public static boolean isRicePanicles(Block block) {
		if (!isLoaded() || block == null || block == Blocks.AIR) {
			return false;
		}
		ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
		return MOD_ID.equals(id.getNamespace()) && id.getPath().equals("rice_panicles");
	}

	public static boolean isFarmProduce(ItemStack stack) {
		if (!isLoaded() || stack.isEmpty()) {
			return false;
		}
		ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		return MOD_ID.equals(id.getNamespace()) && PRODUCE.contains(id.getPath());
	}

	public static ItemStack idleProduct(Item seed) {
		if (!isLoaded() || seed == null || seed == Items.AIR) {
			return ItemStack.EMPTY;
		}
		ResourceLocation id = BuiltInRegistries.ITEM.getKey(seed);
		if (!MOD_ID.equals(id.getNamespace())) {
			return ItemStack.EMPTY;
		}
		String productPath = IDLE_PRODUCTS.get(id.getPath());
		if (productPath == null) {
			return ItemStack.EMPTY;
		}
		Item product = item(productPath);
		if (product == null) {
			return ItemStack.EMPTY;
		}
		return new ItemStack(product);
	}

	public static void addCaughtFish(java.util.List<Item> fish) {
		if (!isLoaded()) {
			return;
		}
		for (String path : new String[] {"raw_tuna", "raw_lamprey", "crab"}) {
			Item item = item(path);
			if (item != null) {
				fish.add(item);
			}
		}
	}

	public static boolean isCaughtFish(ItemStack stack) {
		if (!isLoaded() || stack.isEmpty()) {
			return false;
		}
		ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		if (!MOD_ID.equals(id.getNamespace())) {
			return false;
		}
		String path = id.getPath();
		return path.equals("raw_tuna") || path.equals("raw_lamprey") || path.equals("crab");
	}

	public static @Nullable Block riceBlock() {
		if (!isLoaded()) {
			return null;
		}
		return block("rice");
	}

	private static @Nullable Item item(String path) {
		Item item = BuiltInRegistries.ITEM.get(id(path));
		if (item == null || item == Items.AIR) {
			return null;
		}
		return item;
	}

	private static @Nullable Block block(String path) {
		Block block = BuiltInRegistries.BLOCK.get(id(path));
		if (block == null || block == Blocks.AIR) {
			return null;
		}
		return block;
	}

	private static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
