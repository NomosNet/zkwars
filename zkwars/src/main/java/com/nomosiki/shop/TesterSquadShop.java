package com.nomosiki.shop;

import com.nomosiki.faction.FactionColor;
import com.nomosiki.faction.FactionService;
import com.nomosiki.hire.NomosikHire;
import com.nomosiki.registry.ModBlocks;
import com.nomosiki.squad.NomosikSquad;
import com.nomosiki.squad.SquadTestSpawn;
import com.nomosiki.squad.SquadType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class TesterSquadShop {
	public static final int EMERALD_COST_PER_UNIT = 1;
	public static final int MAX_UNITS = NomosikSquad.MAX_MEMBERS;
	public static final int DEFAULT_UNITS = 8;
	public static final double USE_RANGE_SQR = 64.0;

	private TesterSquadShop() {
	}

	public static int clampCount(int count) {
		return Math.max(1, Math.min(count, MAX_UNITS));
	}

	public static int costFor(int count) {
		return clampCount(count) * EMERALD_COST_PER_UNIT;
	}

	public static boolean tryBuy(ServerPlayer player, BlockPos pos, String rawName, byte typeId, int requestedCount) {
		if (!player.level().getBlockState(pos).is(ModBlocks.TESTER_SQUAD_SHOP)) {
			return false;
		}
		if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > USE_RANGE_SQR) {
			return false;
		}
		SquadType type = SquadType.fromId(typeId);
		if (!type.isPlayable()) {
			return false;
		}
		int count = clampCount(requestedCount);
		int cost = costFor(count);
		if (NomosikHire.countEmeralds(player.getInventory()) < cost) {
			player.displayClientMessage(Component.translatable("message.nomosiki.tester_shop.not_enough_emeralds", cost), true);
			return false;
		}
		double offsetX = pos.getX() + 0.5 - player.getX();
		double offsetZ = pos.getZ() + 0.5 - player.getZ();
		FactionColor color = FactionService.get(player.server).getFactionColorByOwner(player.getUUID());
		int spawned = SquadTestSpawn.spawnTestSquad(player, type, count, rawName, offsetX, offsetZ, color, true, false);
		if (spawned <= 0) {
			player.displayClientMessage(Component.translatable("message.nomosiki.squad.spawn_test_failed"), true);
			return false;
		}
		int paid = spawned * EMERALD_COST_PER_UNIT;
		NomosikHire.removeEmeralds(player.getInventory(), paid);
		String displayName = rawName == null || rawName.isBlank() ? type.name() : rawName.trim();
		player.displayClientMessage(Component.translatable(
			"message.nomosiki.tester_shop.bought",
			spawned,
			Component.translatable(type.translationKey()),
			displayName,
			paid
		), true);
		player.closeContainer();
		return true;
	}
}
