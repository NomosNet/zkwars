package com.nomosiki.work.weapon;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ShurikenItem extends Item {
	public static final float THROW_DAMAGE = 4.0F;

	public ShurikenItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		ThrownWeaponEntity.throwFrom(player, stack, THROW_DAMAGE);
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}
}
