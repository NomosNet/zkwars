package com.nomosiki.mixin;

import com.nomosiki.faction.FactionNameColors;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerDisplayNameMixin {
	@Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
	private void nomosiki$factionDisplayName(CallbackInfoReturnable<Component> cir) {
		Player self = (Player) (Object) this;
		Integer rgb = FactionNameColors.rgb(self);
		if (rgb == null) {
			return;
		}
		cir.setReturnValue(Component.literal(self.getGameProfile().getName()).withColor(rgb));
	}
}
