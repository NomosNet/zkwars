package com.nomosiki.client.mixin.rts;

import com.nomosiki.client.rts.TacticalCameraController;
import com.nomosiki.config.NomosikiConfig;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	protected abstract void setPosition(double x, double y, double z);

	@Shadow
	protected abstract void setRotation(float yaw, float pitch);

	@Inject(method = "setup", at = @At("TAIL"))
	private void nomosiki$applyTacticalCamera(
		BlockGetter area,
		Entity focusedEntity,
		boolean thirdPerson,
		boolean inverseView,
		float tickDelta,
		CallbackInfo ci
	) {
		if (!TacticalCameraController.isActive()) {
			return;
		}
		this.setPosition(
			TacticalCameraController.cameraX(),
			TacticalCameraController.cameraY(),
			TacticalCameraController.cameraZ()
		);
		this.setRotation(TacticalCameraController.cameraYaw(), NomosikiConfig.INSTANCE.rtsTacticalPitch);
	}
}
