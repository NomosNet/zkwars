package com.nomosiki.work.client.mixin;

import com.nomosiki.work.gun.ClientUtilities;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
abstract class HumanoidModelMixin {
	@Unique
	private LivingEntity nomosikiEntity;
	@Unique
	private ArmPose nomosikiLeftArmPose;
	@Unique
	private ArmPose nomosikiRightArmPose;

	@Inject(method = "setupAnim", at = @At("HEAD"))
	private void nomosikiSetupAnimHead(LivingEntity entity, float f, float g, float h, float i, float j, CallbackInfo ci) {
		this.nomosikiEntity = entity;
		HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
		nomosikiLeftArmPose = model.leftArmPose;
		nomosikiRightArmPose = model.rightArmPose;
	}

	@Inject(method = "poseRightArm", at = @At("TAIL"))
	private void nomosikiPoseRightArm(LivingEntity entity, CallbackInfo ci) {
		HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
		if (nomosikiEntity != null && ClientUtilities.poseArm(nomosikiEntity, model, model.rightArm)) {
			model.rightArmPose = ArmPose.SPYGLASS;
		}
	}

	@Inject(method = "poseLeftArm", at = @At("TAIL"))
	private void nomosikiPoseLeftArm(LivingEntity entity, CallbackInfo ci) {
		HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
		if (nomosikiEntity != null && ClientUtilities.poseArm(nomosikiEntity, model, model.leftArm)) {
			model.leftArmPose = ArmPose.SPYGLASS;
		}
	}

	@Inject(method = "setupAnim", at = @At("TAIL"))
	private void nomosikiSetupAnimTail(LivingEntity entity, float f, float g, float h, float i, float j, CallbackInfo ci) {
		HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
		model.rightArmPose = nomosikiRightArmPose;
		model.leftArmPose = nomosikiLeftArmPose;
	}
}
