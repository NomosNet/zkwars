package com.nomosiki.faction;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BannerPatterns;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class FactionBannerHelper {
	private FactionBannerHelper() {
	}

	public static boolean isBlank(ItemStack stack) {
		return stack.isEmpty() || !(stack.getItem() instanceof BannerItem) || !hasPatterns(stack);
	}

	public static boolean hasPatterns(ItemStack stack) {
		if (stack.isEmpty() || !(stack.getItem() instanceof BannerItem)) {
			return false;
		}
		var layers = stack.get(DataComponents.BANNER_PATTERNS);
		return layers != null && !layers.layers().isEmpty();
	}

	public static boolean isSameBanner(@Nullable CompoundTag left, @Nullable CompoundTag right) {
		if (left == null || left.isEmpty()) {
			return right == null || right.isEmpty();
		}
		if (right == null || right.isEmpty()) {
			return false;
		}
		return left.equals(right);
	}

	public static DyeColor getBaseColor(ItemStack stack) {
		if (stack.getItem() instanceof BannerItem banner) {
			return banner.getColor();
		}
		return DyeColor.RED;
	}

	/** Portable banner format for save/network (pattern ids + dye names, no registry codec round-trip). */
	public static CompoundTag serialize(ItemStack stack, HolderLookup.Provider registries) {
		return serializePortable(stack);
	}

	public static ItemStack deserialize(CompoundTag tag, HolderLookup.Provider registries) {
		return stackFromStored(tag, registries);
	}

	public static CompoundTag serializePortable(ItemStack stack) {
		CompoundTag tag = new CompoundTag();
		if (stack.isEmpty() || !(stack.getItem() instanceof BannerItem)) {
			return tag;
		}
		tag.putString("Base", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
		BannerPatternLayers layers = stack.get(DataComponents.BANNER_PATTERNS);
		if (layers != null && !layers.layers().isEmpty()) {
			ListTag patternList = new ListTag();
			for (BannerPatternLayers.Layer layer : layers.layers()) {
				ResourceLocation patternId = layer.pattern().unwrapKey()
					.map(ResourceKey::location)
					.orElse(BannerPatterns.STRIPE_MIDDLE.location());
				CompoundTag layerTag = new CompoundTag();
				layerTag.putString("Pattern", patternId.toString());
				layerTag.putString("Color", layer.color().getName());
				patternList.add(layerTag);
			}
			tag.put("Patterns", patternList);
		}
		return tag;
	}

	public static ItemStack deserializePortable(CompoundTag tag, HolderLookup.Provider registries) {
		if (!isPortableFormat(tag)) {
			return ItemStack.EMPTY;
		}
		ResourceLocation baseId = ResourceLocation.tryParse(tag.getString("Base"));
		if (baseId == null) {
			return ItemStack.EMPTY;
		}
		Item item = BuiltInRegistries.ITEM.get(baseId);
		if (!(item instanceof BannerItem)) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = new ItemStack(item);
		ListTag patternList = tag.getList("Patterns", Tag.TAG_COMPOUND);
		if (patternList.isEmpty()) {
			return stack;
		}
		var patterns = registries.lookupOrThrow(Registries.BANNER_PATTERN);
		List<BannerPatternLayers.Layer> layers = new ArrayList<>(patternList.size());
		for (int i = 0; i < patternList.size(); i++) {
			CompoundTag layerTag = patternList.getCompound(i);
			ResourceLocation patternId = ResourceLocation.tryParse(layerTag.getString("Pattern"));
			if (patternId == null) {
				continue;
			}
			DyeColor dye = dyeFromName(layerTag.getString("Color"));
			patterns.get(ResourceKey.create(Registries.BANNER_PATTERN, patternId)).ifPresent(holder ->
				layers.add(new BannerPatternLayers.Layer(holder, dye))
			);
		}
		if (!layers.isEmpty()) {
			stack.set(DataComponents.BANNER_PATTERNS, new BannerPatternLayers(layers));
		}
		return stack;
	}

	public static ItemStack deserializeVanilla(CompoundTag tag, HolderLookup.Provider registries) {
		return ItemStack.parse(registries, tag).orElse(ItemStack.EMPTY);
	}

	public static boolean isPortableFormat(CompoundTag tag) {
		return tag.contains("Base", Tag.TAG_STRING);
	}

	public static ItemStack stackFromStored(@Nullable CompoundTag stored, HolderLookup.Provider registries) {
		if (stored == null || stored.isEmpty()) {
			return ItemStack.EMPTY;
		}
		ItemStack parsed = isPortableFormat(stored)
			? deserializePortable(stored, registries)
			: deserializeVanilla(stored, registries);
		if (!parsed.isEmpty() && parsed.getItem() instanceof BannerItem && hasPatterns(parsed)) {
			return parsed;
		}
		parsed = isPortableFormat(stored)
			? deserializeVanilla(stored, registries)
			: deserializePortable(stored, registries);
		if (!parsed.isEmpty() && parsed.getItem() instanceof BannerItem && hasPatterns(parsed)) {
			return parsed;
		}
		return ItemStack.EMPTY;
	}

	public static ItemStack bannerStackForDisplay(@Nullable CompoundTag stored, FactionColor color, HolderLookup.Provider registries) {
		ItemStack storedStack = stackFromStored(stored, registries);
		if (!storedStack.isEmpty()) {
			return storedStack;
		}
		return createFallbackBannerStack(color, registries);
	}

	public static CompoundTag bannerNbtForDisplay(@Nullable CompoundTag stored, FactionColor color, HolderLookup.Provider registries) {
		return serialize(bannerStackForDisplay(stored, color, registries), registries);
	}

	public static ItemStack createFallbackBannerStack(FactionColor color, HolderLookup.Provider registries) {
		DyeColor dye = toDye(color);
		ItemStack stack = new ItemStack(bannerItemFor(dye));
		Holder<BannerPattern> stripe = registries.lookupOrThrow(Registries.BANNER_PATTERN).getOrThrow(BannerPatterns.STRIPE_MIDDLE);
		stack.set(DataComponents.BANNER_PATTERNS, new BannerPatternLayers(
			List.of(new BannerPatternLayers.Layer(stripe, contrastDye(dye)))
		));
		return stack;
	}

	public static CompoundTag createFallbackBanner(FactionColor color, HolderLookup.Provider registries) {
		return serialize(createFallbackBannerStack(color, registries), registries);
	}

	private static DyeColor dyeFromName(String name) {
		for (DyeColor dye : DyeColor.values()) {
			if (dye.getName().equalsIgnoreCase(name)) {
				return dye;
			}
		}
		return DyeColor.WHITE;
	}

	private static DyeColor contrastDye(DyeColor base) {
		return base == DyeColor.BLACK || base == DyeColor.BLUE || base == DyeColor.PURPLE ? DyeColor.WHITE : DyeColor.BLACK;
	}

	public static DyeColor toDye(FactionColor color) {
		return switch (color) {
			case WHITE -> DyeColor.WHITE;
			case BLACK, DARK_GREY -> DyeColor.BLACK;
			case RED, DARK_RED, LIGHT_RED -> DyeColor.RED;
			case BLUE, DARK_BLUE -> DyeColor.BLUE;
			case LIGHT_BLUE -> DyeColor.LIGHT_BLUE;
			case BROWN, DARK_BROWN, LIGHT_BROWN -> DyeColor.BROWN;
			case CYAN, DARK_CYAN, LIGHT_CYAN -> DyeColor.CYAN;
			case GREEN, DARK_GREEN -> DyeColor.GREEN;
			case LIGHT_GREEN -> DyeColor.LIME;
			case GREY, LIGHT_GREY -> DyeColor.GRAY;
			case GOLD, YELLOW -> DyeColor.YELLOW;
			case ORANGE -> DyeColor.ORANGE;
			case MAGENTA -> DyeColor.MAGENTA;
			case PURPLE -> DyeColor.PURPLE;
		};
	}

	private static Item bannerItemFor(DyeColor dye) {
		return switch (dye) {
			case WHITE -> Items.WHITE_BANNER;
			case ORANGE -> Items.ORANGE_BANNER;
			case MAGENTA -> Items.MAGENTA_BANNER;
			case LIGHT_BLUE -> Items.LIGHT_BLUE_BANNER;
			case YELLOW -> Items.YELLOW_BANNER;
			case LIME -> Items.LIME_BANNER;
			case PINK -> Items.PINK_BANNER;
			case GRAY -> Items.GRAY_BANNER;
			case LIGHT_GRAY -> Items.LIGHT_GRAY_BANNER;
			case CYAN -> Items.CYAN_BANNER;
			case PURPLE -> Items.PURPLE_BANNER;
			case BLUE -> Items.BLUE_BANNER;
			case BROWN -> Items.BROWN_BANNER;
			case GREEN -> Items.GREEN_BANNER;
			case RED -> Items.RED_BANNER;
			case BLACK -> Items.BLACK_BANNER;
		};
	}
}
