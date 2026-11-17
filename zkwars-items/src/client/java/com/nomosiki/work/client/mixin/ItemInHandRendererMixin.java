package com.nomosiki.work.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nomosiki.work.gun.ClientUtilities;
import com.nomosiki.work.gun.GunItem;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
abstract class ItemInHandRendererMixin {
	@Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
	private void nomosikiRenderGun(
		AbstractClientPlayer player,
		float tickDelta,
		float pitch,
		InteractionHand hand,
		float swingProgress,
		ItemStack stack,
		float equipProgress,
		PoseStack matrices,
		MultiBufferSource vertexConsumers,
		int light,
		CallbackInfo ci
	) {
		if (stack.getItem() instanceof GunItem) {
			ClientUtilities.renderGunInHand(
				(ItemInHandRenderer) (Object) this,
				player, hand, tickDelta, pitch, swingProgress, equipProgress, stack, matrices, vertexConsumers, light);
			ci.cancel();
		}
	}
}
