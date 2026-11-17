package com.nomosiki.mixin;

import com.nomosiki.entity.NomosikEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ensures cavalry/nomad riders steer their horse. Without a controlling passenger,
 * the mount's AI idles while the rider's pathfinding barely nudges the pair — looks like crawling.
 */
@Mixin(AbstractHorse.class)
public abstract class AbstractHorseMixin {
	@Inject(method = "getControllingPassenger", at = @At("HEAD"), cancellable = true)
	private void nomosiki$nomosikControlsMount(CallbackInfoReturnable<LivingEntity> cir) {
		Entity passenger = ((AbstractHorse) (Object) this).getFirstPassenger();
		if (passenger instanceof NomosikEntity nomosik) {
			cir.setReturnValue(nomosik);
		}
	}
}
