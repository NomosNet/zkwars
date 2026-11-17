package com.nomosiki.screen;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public record TesterSquadShopMenuProvider(BlockPos pos) implements ExtendedScreenHandlerFactory<TesterSquadShopMenuData> {
	@Override
	public TesterSquadShopMenuData getScreenOpeningData(ServerPlayer player) {
		return new TesterSquadShopMenuData(this.pos);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("screen.nomosiki.tester_shop.title");
	}

	@Override
	public AbstractContainerMenu createMenu(int syncId, Inventory inventory, Player player) {
		return new TesterSquadShopScreenHandler(
			com.nomosiki.registry.ModScreenHandlers.TESTER_SQUAD_SHOP,
			syncId,
			inventory,
			this.pos
		);
	}
}
