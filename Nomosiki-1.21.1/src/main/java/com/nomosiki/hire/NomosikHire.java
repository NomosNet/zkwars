package com.nomosiki.hire;

import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.network.squad.SquadNetworking;
import com.nomosiki.squad.SquadManager;
import com.nomosiki.squad.SquadService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class NomosikHire {
	public static final int EMERALD_COST = 10;

	private NomosikHire() {
	}

	public static int countEmeralds(Inventory inventory) {
		int total = 0;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(Items.EMERALD)) {
				total += stack.getCount();
			}
		}
		return total;
	}

	public static boolean tryHire(ServerPlayer player, NomosikEntity nomosik) {
		if (nomosik.getOwnerUUID() != null) {
			return false;
		}
		if (player.distanceToSqr(nomosik) > 64.0) {
			return false;
		}
		if (countEmeralds(player.getInventory()) < EMERALD_COST) {
			player.displayClientMessage(Component.translatable("message.nomosiki.hire.not_enough_emeralds", EMERALD_COST), true);
			return false;
		}
		if (!removeEmeralds(player.getInventory(), EMERALD_COST)) {
			player.displayClientMessage(Component.translatable("message.nomosiki.hire.not_enough_emeralds", EMERALD_COST), true);
			return false;
		}
		nomosik.setOwnerUUID(player.getUUID());
		nomosik.setPersistenceRequired();
		nomosik.setShouldFollowOwner(true);
		SquadManager mgr = SquadService.get(player.server);
		mgr.autoAssignNewHire(player, nomosik);
		SquadNetworking.sendSquadState(player, mgr);
		player.displayClientMessage(Component.translatable("message.nomosiki.hired"), true);
		player.closeContainer();
		return true;
	}

	public static boolean removeEmeralds(Inventory inventory, int cost) {
		int remaining = cost;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (!stack.is(Items.EMERALD)) {
				continue;
			}
			int take = Math.min(stack.getCount(), remaining);
			stack.shrink(take);
			remaining -= take;
			if (remaining <= 0) {
				return true;
			}
		}
		return false;
	}
}
