package com.nomosiki.mixin;

import com.nomosiki.entity.NomosikiHorseMount;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla Peaceful deletes anything {@code instanceof Enemy} (spiders, hoglins).
 * Nomosiki cavalry mounts must survive, or riders flicker / stand beside missing mounts.
 */
@Mixin(Mob.class)
public abstract class NomosikiMountPeacefulMixin {
	@Inject(method = "shouldDespawnInPeaceful", at = @At("HEAD"), cancellable = true)
	private void nomosiki$keepCavalryMount(CallbackInfoReturnable<Boolean> cir) {
		if (NomosikiHorseMount.isNomosikiMountEntity((Entity) (Object) this)) {
			cir.setReturnValue(false);
		}
	}
}
