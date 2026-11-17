package com.nomosiki.screen;

import com.nomosiki.entity.NomosikEntity;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public record NomosikHireMenuProvider(NomosikEntity nomosik) implements ExtendedScreenHandlerFactory<HireMenuData> {
	@Override
	public HireMenuData getScreenOpeningData(ServerPlayer player) {
		return new HireMenuData(this.nomosik.getId());
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("screen.nomosiki.hire.title");
	}

	@Override
	public AbstractContainerMenu createMenu(int syncId, Inventory inventory, Player player) {
		return new HireScreenHandler(com.nomosiki.registry.ModScreenHandlers.HIRE, syncId, inventory, this.nomosik);
	}
}
