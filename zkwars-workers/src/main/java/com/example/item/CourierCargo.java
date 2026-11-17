package com.example.item;

import com.example.block.WorkerKind;
import com.example.compat.GensokyoFoodCompat;
import com.example.entity.HumanWorkerEntity;
import com.example.inventory.WorkerSlots;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

public final class CourierCargo {
	private CourierCargo() {
	}

	public static boolean isExport(WorkerKind kind, ItemStack stack) {
		if (stack.isEmpty() || isTool(stack)) {
			return false;
		}
		return switch (kind) {
			case QUARRY -> isOreOrStone(stack);
			case LUMBERJACK -> isLog(stack);
			case FARMER -> isFarmProduce(stack);
			case CHICKEN, COW, SHEEP, PIG -> isPenProduct(stack);
			case BUILDER -> false;
			case SMELTER, CRAFTER -> true;
			case FISHER -> isFish(stack);
		};
	}

	public static boolean isSupply(@Nullable Level level, WorkerKind kind, ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		if (isTool(stack) && !HumanWorkerEntity.hasRemainingDurability(stack)) {
			return false;
		}
		return switch (kind) {
			case FARMER -> WorkerSlots.isCropSeed(stack) || stack.is(Items.BONE_MEAL) || stack.getItem() instanceof HoeItem;
			case CHICKEN -> stack.is(ItemTags.CHICKEN_FOOD) || stack.getItem() instanceof SwordItem;
			case COW -> stack.is(ItemTags.COW_FOOD) || stack.getItem() instanceof SwordItem;
			case SHEEP -> stack.is(ItemTags.SHEEP_FOOD) || stack.getItem() instanceof SwordItem;
			case PIG -> stack.is(ItemTags.PIG_FOOD) || stack.getItem() instanceof SwordItem;
			case QUARRY -> stack.getItem() instanceof PickaxeItem;
			case LUMBERJACK -> stack.getItem() instanceof AxeItem;
			case BUILDER -> stack.getItem() instanceof BlockItem || stack.getItem() instanceof HammerItem;
			case SMELTER -> WorkerSlots.isSmeltable(level, stack) || WorkerSlots.isFuel(stack);
			case FISHER -> stack.getItem() instanceof net.minecraft.world.item.FishingRodItem;
			case CRAFTER -> false;
		};
	}

	public static boolean isTool(ItemStack stack) {
		return stack.getItem() instanceof PickaxeItem
				|| stack.getItem() instanceof AxeItem
				|| stack.getItem() instanceof HoeItem
				|| stack.getItem() instanceof SwordItem
				|| stack.getItem() instanceof HammerItem
				|| stack.getItem() instanceof net.minecraft.world.item.FishingRodItem;
	}

	private static boolean isFish(ItemStack stack) {
		if (stack.is(Items.COD) || stack.is(Items.SALMON) || stack.is(Items.TROPICAL_FISH) || stack.is(Items.PUFFERFISH)) {
			return true;
		}
		return GensokyoFoodCompat.isCaughtFish(stack);
	}

	private static boolean isLog(ItemStack stack) {
		return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock().defaultBlockState().is(BlockTags.LOGS);
	}

	private static boolean isFarmProduce(ItemStack stack) {
		return stack.is(Items.WHEAT)
				|| stack.is(Items.BEETROOT)
				|| stack.is(Items.CARROT)
				|| stack.is(Items.POTATO)
				|| stack.is(Items.POISONOUS_POTATO)
				|| stack.is(Items.PUMPKIN)
				|| stack.is(Items.MELON)
				|| stack.is(Items.TORCHFLOWER)
				|| stack.is(Items.SWEET_BERRIES)
				|| stack.is(Items.NETHER_WART)
				|| GensokyoFoodCompat.isFarmProduce(stack);
	}

	private static boolean isPenProduct(ItemStack stack) {
		return stack.is(Items.EGG)
				|| stack.is(Items.FEATHER)
				|| stack.is(Items.LEATHER)
				|| stack.is(ItemTags.WOOL)
				|| stack.is(Items.PORKCHOP)
				|| stack.is(Items.BEEF)
				|| stack.is(Items.CHICKEN)
				|| stack.is(Items.MUTTON);
	}

	private static boolean isOreOrStone(ItemStack stack) {
		if (stack.is(Items.COAL) || stack.is(Items.RAW_IRON) || stack.is(Items.RAW_COPPER) || stack.is(Items.RAW_GOLD)
				|| stack.is(Items.DIAMOND) || stack.is(Items.EMERALD) || stack.is(Items.LAPIS_LAZULI)
				|| stack.is(Items.REDSTONE) || stack.is(Items.QUARTZ) || stack.is(Items.ANCIENT_DEBRIS)
				|| stack.is(Items.COBBLESTONE) || stack.is(Items.STONE) || stack.is(Items.DEEPSLATE)
				|| stack.is(Items.COBBLED_DEEPSLATE) || stack.is(Items.ANDESITE) || stack.is(Items.DIORITE)
				|| stack.is(Items.GRANITE) || stack.is(Items.TUFF) || stack.is(Items.CALCITE)
				|| stack.is(Items.DRIPSTONE_BLOCK) || stack.is(Items.BLACKSTONE) || stack.is(Items.BASALT)
				|| stack.is(Items.NETHERRACK) || stack.is(Items.END_STONE)) {
			return true;
		}
		if (!(stack.getItem() instanceof BlockItem blockItem)) {
			return false;
		}
		BlockState state = blockItem.getBlock().defaultBlockState();
		return state.is(BlockTags.COAL_ORES)
				|| state.is(BlockTags.IRON_ORES)
				|| state.is(BlockTags.COPPER_ORES)
				|| state.is(BlockTags.GOLD_ORES)
				|| state.is(BlockTags.REDSTONE_ORES)
				|| state.is(BlockTags.LAPIS_ORES)
				|| state.is(BlockTags.DIAMOND_ORES)
				|| state.is(BlockTags.EMERALD_ORES)
				|| state.is(BlockTags.BASE_STONE_OVERWORLD);
	}
}
