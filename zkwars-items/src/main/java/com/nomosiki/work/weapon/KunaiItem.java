package com.nomosiki.work.weapon;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;

public class KunaiItem extends SwordItem {
	private final float throwDamage;

	public KunaiItem(Tier tier, float throwDamage, Properties properties) {
		super(tier, properties);
		this.throwDamage = throwDamage;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		ThrownWeaponEntity.throwFrom(player, stack, throwDamage);
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}
}
