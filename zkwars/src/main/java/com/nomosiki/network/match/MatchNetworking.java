package com.nomosiki.network.match;

import com.nomosiki.match.MatchBuildService;
import com.nomosiki.match.MatchBuildingCatalog;
import com.nomosiki.match.MatchRecruitment;
import com.nomosiki.match.MatchService;
import com.nomosiki.network.match.payload.c2s.MatchBuildPlacePayload;
import com.nomosiki.network.match.payload.c2s.MatchRecruitPayload;
import com.nomosiki.network.match.payload.c2s.MatchReinforcePayload;
import com.nomosiki.network.match.payload.c2s.MatchWargearPayload;
import com.nomosiki.network.match.payload.s2c.MatchHudPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class MatchNetworking {
	private MatchNetworking() {
	}

	public static void register() {
		PayloadTypeRegistry.playS2C().register(MatchHudPayload.TYPE, MatchHudPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(MatchRecruitPayload.TYPE, MatchRecruitPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(MatchBuildPlacePayload.TYPE, MatchBuildPlacePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(MatchWargearPayload.TYPE, MatchWargearPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(MatchReinforcePayload.TYPE, MatchReinforcePayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(MatchRecruitPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> {
				MatchRecruitment.Role role = MatchRecruitment.Role.fromId(payload.roleId());
				if (role == null) {
					player.displayClientMessage(Component.translatable("message.nomosiki.match.unknown_role", payload.roleId()), true);
					return;
				}
				player.displayClientMessage(MatchService.get(player.server).recruit(player, role), true);
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(MatchBuildPlacePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> {
				MatchBuildingCatalog.Kind kind = MatchBuildingCatalog.Kind.fromId(payload.buildingId());
				if (kind == null) {
					player.displayClientMessage(Component.translatable("message.nomosiki.match.unknown_building", payload.buildingId()), true);
					return;
				}
				player.displayClientMessage(
					MatchBuildService.place(MatchService.get(player.server), player, kind, payload.pos()),
					true
				);
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(MatchWargearPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> player.displayClientMessage(
				MatchService.get(player.server).buyWargear(player, payload.squadId(), payload.upgradeId()),
				true
			));
		});

		ServerPlayNetworking.registerGlobalReceiver(MatchReinforcePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> player.displayClientMessage(
				MatchService.get(player.server).reinforce(player, payload.squadId()),
				true
			));
		});
	}
}
