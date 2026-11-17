package com.nomosiki.mixin;

import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.entity.NomosikiHorseMount;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ensures cavalry/nomad riders steer tagged non-horse mounts (hoglin, spider).
 * Horses are handled by {@link AbstractHorseMixin}.
 */
@Mixin(Entity.class)
public abstract class NomosikiMountControlMixin {
	@Inject(method = "getControllingPassenger", at = @At("HEAD"), cancellable = true)
	private void nomosiki$nomosikControlsMount(CallbackInfoReturnable<LivingEntity> cir) {
		Entity self = (Entity) (Object) this;
		if (!(self instanceof Mob) || !NomosikiHorseMount.isNomosikiMountEntity(self)) {
			return;
		}
		Entity passenger = self.getFirstPassenger();
		if (passenger instanceof NomosikEntity nomosik) {
			cir.setReturnValue(nomosik);
		}
	}
}
