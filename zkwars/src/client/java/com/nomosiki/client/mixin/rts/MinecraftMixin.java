package com.nomosiki.client.mixin.rts;

import com.nomosiki.client.rts.RtsClientState;
import com.nomosiki.client.rts.RtsOrderInputHandler;
import com.nomosiki.client.rts.TacticalCameraController;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
	private void nomosiki$cancelAttackInTacticalCamera(CallbackInfoReturnable<Boolean> cir) {
		if (TacticalCameraController.isActive()) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
	private void nomosiki$cancelMiningInTacticalCamera(boolean leftClick, CallbackInfo ci) {
		if (TacticalCameraController.isActive()) {
			ci.cancel();
			return;
		}
		Minecraft mc = (Minecraft) (Object) this;
		if (RtsClientState.bombardDragActive() && mc.screen == null) {
			ci.cancel();
			if (leftClick) {
				BlockPos edge = RtsOrderInputHandler.raycastAnchor();
				if (edge != null) {
					RtsOrderInputHandler.stretchBombardRadius(edge);
				}
				return;
			}
			long now = mc.level == null ? 0L : mc.level.getGameTime();
			if (now <= RtsClientState.bombardDragTick()) {
				return;
			}
			BlockPos center = RtsClientState.bombardDragCenter();
			RtsClientState.clearBombardDrag();
			if (center != null) {
				RtsOrderInputHandler.confirmBombard(mc, center);
			}
			return;
		}
		if (leftClick && RtsClientState.bombardDesignate() && mc.screen == null) {
			ci.cancel();
		}
	}

	@Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
	private void nomosiki$placeBombardOnAttack(CallbackInfoReturnable<Boolean> cir) {
		Minecraft mc = (Minecraft) (Object) this;
		if (!RtsClientState.bombardDesignate() || TacticalCameraController.isActive() || mc.screen != null) {
			return;
		}
		BlockPos look = RtsOrderInputHandler.raycastAnchor();
		long time = mc.level == null ? 0L : mc.level.getGameTime();
		if (look != null) {
			RtsClientState.beginBombardDrag(look, time);
		}
		cir.setReturnValue(false);
	}

	@Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
	private void nomosiki$cancelBombardOnUse(CallbackInfo ci) {
		Minecraft mc = (Minecraft) (Object) this;
		if (!RtsClientState.bombardDesignate() || TacticalCameraController.isActive() || mc.screen != null) {
			return;
		}
		RtsOrderInputHandler.cancelBombardDesignate();
		ci.cancel();
	}
}
