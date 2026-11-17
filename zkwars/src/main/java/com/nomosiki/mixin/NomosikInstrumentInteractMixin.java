package com.nomosiki.mixin;

import com.nomosiki.compat.melodies.NomosikImmersiveMelodiesCompat;
import com.nomosiki.entity.NomosikEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Immersive Melodies injects {@code Mob.interact} at HEAD and drops held instruments.
 * {@code Mob.interact} is final in 1.21.1, so Nomosiki cannot override it.
 * Higher priority than IM (default 1000) so this HEAD callback runs first and cancels the drop.
 */
@Mixin(value = Mob.class, priority = 1100)
public abstract class NomosikInstrumentInteractMixin {
	@Inject(method = "interact", at = @At("HEAD"), cancellable = true)
	private void nomosiki$keepInstrument(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		Mob self = (Mob) (Object) this;
		if (self instanceof NomosikEntity nomosik && NomosikImmersiveMelodiesCompat.isHoldingInstrument(nomosik)) {
			cir.setReturnValue(nomosik.interactKeepingInstrument(player, hand));
		}
	}
}
