package com.nomosiki.screen;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class FactionCreateMenuProvider implements MenuProvider {
	@Override
	public Component getDisplayName() {
		return Component.translatable("screen.nomosiki.faction.create_title");
	}

	@Override
	public AbstractContainerMenu createMenu(int syncId, Inventory inventory, Player player) {
		return new FactionCreateMenu(syncId, inventory);
	}
}
