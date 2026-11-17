package com.nomosiki.work.mixin;

import com.nomosiki.work.armor.FactionArmorTint;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Player.class, Mob.class})
abstract class FactionArmorEquipMixin {
	@Inject(method = "setItemSlot", at = @At("HEAD"))
	private void zk$factionSamuraiColor(EquipmentSlot slot, ItemStack stack, CallbackInfo ci) {
		FactionArmorTint.apply((LivingEntity) (Object) this, slot, stack);
	}
}
