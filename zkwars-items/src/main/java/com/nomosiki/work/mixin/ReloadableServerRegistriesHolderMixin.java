package com.nomosiki.work.mixin;

import com.nomosiki.work.gun.ILootTableId;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ReloadableServerRegistries.Holder.class)
abstract class ReloadableServerRegistriesHolderMixin {
	@Unique
	private ResourceLocation nomosikiLocation;

	@Inject(method = "getLootTable", at = @At("HEAD"))
	private void nomosikiCaptureLootTable(ResourceKey<LootTable> key, CallbackInfoReturnable<LootTable> ci) {
		nomosikiLocation = key.location();
	}

	@Inject(method = "getLootTable", at = @At("RETURN"))
	private void nomosikiAssignLootTableId(CallbackInfoReturnable<LootTable> ci) {
		((ILootTableId) ci.getReturnValue()).setLocation(nomosikiLocation);
	}
}
