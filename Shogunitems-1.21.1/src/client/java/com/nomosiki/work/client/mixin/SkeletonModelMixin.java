package com.nomosiki.work.client.mixin;

import com.nomosiki.work.gun.GunItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkeletonModel.class)
abstract class SkeletonModelMixin {
	@Unique
	private Mob nomosikiMob;

	@Inject(method = "prepareMobModel", at = @At("HEAD"))
	private void nomosikiPrepareHead(Mob mob, float f, float g, float h, CallbackInfo ci) {
		this.nomosikiMob = mob;
	}

	@Inject(method = "prepareMobModel", at = @At("TAIL"))
	private void nomosikiPrepareTail(Mob mob, float f, float g, float h, CallbackInfo ci) {
		if (nomosikiMob != null && GunItem.isHoldingGun(nomosikiMob) && nomosikiMob.isUsingItem()) {
			SkeletonModel<?> model = (SkeletonModel<?>) (Object) this;
			if (nomosikiMob.getMainArm() == HumanoidArm.RIGHT) {
				model.rightArmPose = HumanoidModel.ArmPose.CROSSBOW_CHARGE;
			} else {
				model.leftArmPose = HumanoidModel.ArmPose.CROSSBOW_CHARGE;
			}
		}
	}

	@Inject(
		method = "setupAnim",
		cancellable = true,
		at = @At(
			value = "INVOKE",
			shift = At.Shift.BEFORE,
			target = "Lnet/minecraft/world/entity/Mob;getMainHandItem()Lnet/minecraft/world/item/ItemStack;"))
	private void nomosikiSetupAnim(CallbackInfo ci) {
		if (nomosikiMob != null && GunItem.isHoldingGun(nomosikiMob)) {
			ci.cancel();
		}
	}
}
