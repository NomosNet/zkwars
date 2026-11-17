package com.example.advancement;

import com.example.ZKWorkers;
import com.example.block.WorkerKind;
import com.example.entity.HumanWorkerEntity;
import com.example.item.CourierScrollItem;
import com.example.item.CourierSortScrollItem;
import com.example.item.ScrollData;
import com.example.registry.ModItems;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class WorkerAdvancements {
	public static final WorkerActionTrigger ACTION = new WorkerActionTrigger();

	private WorkerAdvancements() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.TRIGGER_TYPES, ZKWorkers.id("action"), ACTION);
	}

	public static void grant(Player player, String action) {
		if (player instanceof ServerPlayer serverPlayer) {
			ACTION.trigger(serverPlayer, action);
		}
	}

	public static void grant(@Nullable HumanWorkerEntity worker, @Nullable String action) {
		if (worker == null || action == null || !(worker.level() instanceof ServerLevel level)) {
			return;
		}
		UUID assigner = worker.assignerId();
		if (assigner == null) {
			return;
		}
		ServerPlayer player = level.getServer().getPlayerList().getPlayer(assigner);
		if (player != null) {
			ACTION.trigger(player, action);
		}
	}

	public static void onAssigned(Player player, HumanWorkerEntity worker, ItemStack stack) {
		if (stack.isEmpty() || !worker.acceptsScroll(stack)) {
			return;
		}
		worker.rememberAssigner(player);
		grant(player, WorkerActions.ASSIGNED);
		if (worker.isCourier()) {
			grant(player, WorkerActions.ASSIGNED_COURIER);
			if (stack.is(ModItems.COURIER_SORT_SCROLL)) {
				grant(player, WorkerActions.SORT_ASSIGNED);
				if (ScrollData.blockList(ScrollData.get(stack)).size() >= CourierSortScrollItem.MAX_CONTAINERS) {
					grant(player, WorkerActions.SORT_FULL);
				}
				return;
			}
			if (ScrollData.blockList(ScrollData.get(stack)).size() >= CourierScrollItem.MAX_WORKER_BLOCKS) {
				grant(player, WorkerActions.COURIER_FIVE);
			}
			if (!stack.is(ModItems.COURIER_SUPPLY_SCROLL)) {
				grant(player, WorkerActions.COURIER_ROUTE);
			}
			return;
		}
		WorkerKind kind = worker.profession();
		if (kind != null) {
			grant(player, WorkerActions.assigned(kind));
		}
	}
}
