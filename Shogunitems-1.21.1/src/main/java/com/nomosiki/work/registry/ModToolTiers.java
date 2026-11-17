package com.nomosiki.work.registry;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

public record ModToolTiers(
	int uses,
	float speed,
	float attackDamageBonus,
	TagKey<Block> incorrectBlocksForDrops,
	int enchantmentValue,
	Supplier<Ingredient> repair
) implements Tier {
	public static final ModToolTiers STEEL = new ModToolTiers(
		1561,
		12.0F,
		5.0F,
		BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
		12,
		() -> Ingredient.of(ModItems.STEEL_INGOT));
	public static final ModToolTiers NETHERITE = new ModToolTiers(
		2561,
		12.0F,
		5.0F,
		BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
		25,
		() -> Ingredient.of(net.minecraft.world.item.Items.NETHERITE_INGOT));

	@Override
	public int getUses() {
		return uses;
	}

	@Override
	public float getSpeed() {
		return speed;
	}

	@Override
	public float getAttackDamageBonus() {
		return attackDamageBonus;
	}

	@Override
	public TagKey<Block> getIncorrectBlocksForDrops() {
		return incorrectBlocksForDrops;
	}

	@Override
	public int getEnchantmentValue() {
		return enchantmentValue;
	}

	@Override
	public Ingredient getRepairIngredient() {
		return repair.get();
	}
}
