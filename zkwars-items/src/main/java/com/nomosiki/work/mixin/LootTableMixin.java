package com.nomosiki.work.mixin;

import com.nomosiki.work.gun.ILootTableId;
import com.nomosiki.work.gun.VanillaHelper;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LootTable.class)
abstract class LootTableMixin implements ILootTableId {
	@Unique
	private ResourceLocation nomosikiLocation;
	@Unique
	private LootContext nomosikiContext;

	@Override
	public void setLocation(ResourceLocation location) {
		this.nomosikiLocation = location;
	}

	@Inject(
		method = "getRandomItems(Lnet/minecraft/world/level/storage/loot/LootContext;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;",
		at = @At("HEAD"))
	private void nomosikiCaptureContext(LootContext context, CallbackInfoReturnable<ObjectArrayList<ItemStack>> ci) {
		this.nomosikiContext = context;
	}

	@Inject(
		method = "getRandomItems(Lnet/minecraft/world/level/storage/loot/LootContext;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;",
		at = @At("RETURN"))
	private void nomosikiInjectLoot(CallbackInfoReturnable<ObjectArrayList<ItemStack>> ci) {
		if (nomosikiLocation != null) {
			ObjectArrayList<ItemStack> items = ci.getReturnValue();
			VanillaHelper.modifyLootTableItems(nomosikiLocation, nomosikiContext, items::add);
		}
	}
}
