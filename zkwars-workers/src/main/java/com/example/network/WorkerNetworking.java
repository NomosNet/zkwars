package com.example.network;

import com.example.advancement.WorkerActions;
import com.example.advancement.WorkerAdvancements;
import com.example.block.WorkerKind;
import com.example.block.entity.WorkerBlockEntity;
import com.example.block.work.BuilderTasks;
import com.example.inventory.WorkerMenu;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class WorkerNetworking {
	private WorkerNetworking() {
	}

	public static void register() {
		PayloadTypeRegistry.playC2S().register(QuarryBoundsPayload.TYPE, QuarryBoundsPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(WorkRadiusPayload.TYPE, WorkRadiusPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(PenLimitPayload.TYPE, PenLimitPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(BuilderCaptureChunkPayload.TYPE, BuilderCaptureChunkPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(QuarryBoundsPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			if (!(player.containerMenu instanceof WorkerMenu menu) || menu.kind() != WorkerKind.QUARRY) {
				return;
			}
			if (!payload.pos().equals(menu.pos()) || !menu.stillValid(player)) {
				return;
			}
			BlockEntity blockEntity = player.level().getBlockEntity(payload.pos());
			if (!(blockEntity instanceof WorkerBlockEntity worker) || worker.kind() != WorkerKind.QUARRY) {
				return;
			}
			worker.applyQuarryBounds(payload.minX(), payload.maxX(), payload.minY(), payload.maxY(), payload.minZ(), payload.maxZ());
			WorkerAdvancements.grant(player, WorkerActions.QUARRY_APPLIED);
		});
		ServerPlayNetworking.registerGlobalReceiver(WorkRadiusPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			if (!(player.containerMenu instanceof WorkerMenu menu) || !menu.kind().editableRadius()) {
				return;
			}
			if (!payload.pos().equals(menu.pos()) || !menu.stillValid(player)) {
				return;
			}
			BlockEntity blockEntity = player.level().getBlockEntity(payload.pos());
			if (!(blockEntity instanceof WorkerBlockEntity worker) || !worker.kind().editableRadius()) {
				return;
			}
			worker.applyWorkRadius(payload.clampedRadius());
		});
		ServerPlayNetworking.registerGlobalReceiver(PenLimitPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			if (!(player.containerMenu instanceof WorkerMenu menu) || !menu.kind().isPen()) {
				return;
			}
			if (!payload.pos().equals(menu.pos()) || !menu.stillValid(player)) {
				return;
			}
			BlockEntity blockEntity = player.level().getBlockEntity(payload.pos());
			if (!(blockEntity instanceof WorkerBlockEntity worker) || !worker.kind().isPen()) {
				return;
			}
			worker.applyAdultLimit(payload.clampedLimit());
		});
		ServerPlayNetworking.registerGlobalReceiver(BuilderCaptureChunkPayload.TYPE, (payload, context) -> {
			context.player().getServer().execute(() -> {
				if (!(context.player().level() instanceof net.minecraft.server.level.ServerLevel level)) {
					return;
				}
				BuilderTasks.get(level).accept(context.player(), payload);
			});
		});
	}
}
