package com.nomosiki.network.squad;

import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.network.squad.payload.c2s.management.SquadAddMemberPayload;
import com.nomosiki.network.squad.payload.c2s.management.SquadAssignNearbyPayload;
import com.nomosiki.network.squad.payload.c2s.management.SquadCreatePayload;
import com.nomosiki.network.squad.payload.c2s.management.SquadDisbandPayload;
import com.nomosiki.network.squad.payload.c2s.management.SquadEditPayload;
import com.nomosiki.network.squad.payload.c2s.management.SquadJoinByEntityPayload;
import com.nomosiki.network.squad.payload.c2s.management.SquadRemoveMemberPayload;
import com.nomosiki.network.squad.payload.c2s.management.SquadSetActivePayload;
import com.nomosiki.squad.SquadManager;
import com.nomosiki.squad.SquadService;
import com.nomosiki.squad.SquadType;
import com.nomosiki.squad.SquadWaypointService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Optional;
import java.util.UUID;

public final class SquadManagementNetworking {
	private static final double SQUAD_COMMAND_RANGE = 64.0;
	private static final double SQUAD_COMMAND_RANGE_SQR = SQUAD_COMMAND_RANGE * SQUAD_COMMAND_RANGE;

	private SquadManagementNetworking() {
	}

	public static void register() {
		PayloadTypeRegistry.playC2S().register(SquadCreatePayload.TYPE, SquadCreatePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(SquadEditPayload.TYPE, SquadEditPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(SquadDisbandPayload.TYPE, SquadDisbandPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(SquadAddMemberPayload.TYPE, SquadAddMemberPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(SquadRemoveMemberPayload.TYPE, SquadRemoveMemberPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(SquadAssignNearbyPayload.TYPE, SquadAssignNearbyPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(SquadJoinByEntityPayload.TYPE, SquadJoinByEntityPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(SquadSetActivePayload.TYPE, SquadSetActivePayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(SquadCreatePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			SquadManager mgr = SquadService.get(player.server);
			SquadType type = SquadType.fromId(payload.typeId());
			Optional<String> err = mgr.createNamedSquad(player, payload.name(), payload.iconId(), type);
			err.ifPresent(key -> player.displayClientMessage(Component.translatable(key), true));
			refreshState(player, mgr);
		});

		ServerPlayNetworking.registerGlobalReceiver(SquadEditPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			SquadManager mgr = SquadService.get(player.server);
			Optional<String> err = mgr.tryEditSquad(player, payload.squadId(), payload.name(), payload.iconId());
			err.ifPresent(key -> player.displayClientMessage(Component.translatable(key), true));
			refreshState(player, mgr);
		});

		ServerPlayNetworking.registerGlobalReceiver(SquadDisbandPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			SquadManager mgr = SquadService.get(player.server);
			if (!mgr.disbandSquad(player, payload.squadId())) {
				player.displayClientMessage(Component.translatable("message.nomosiki.squad.disband_failed"), true);
			}
			SquadWaypointService.clear(payload.squadId());
			refreshState(player, mgr);
		});

		ServerPlayNetworking.registerGlobalReceiver(SquadAddMemberPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			NomosikEntity nomosik = resolveOwnedNomosik(player, payload.entityId());
			if (nomosik == null) {
				return;
			}
			SquadManager mgr = SquadService.get(player.server);
			String key = mgr.tryAddOrTransferMember(player, payload.squadId(), nomosik);
			if (!key.isEmpty()) {
				player.displayClientMessage(Component.translatable(key), true);
			} else {
				player.displayClientMessage(Component.translatable("message.nomosiki.squad.member_added"), true);
			}
			refreshState(player, mgr);
		});

		ServerPlayNetworking.registerGlobalReceiver(SquadRemoveMemberPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			NomosikEntity nomosik = resolveOwnedNomosik(player, payload.entityId());
			if (nomosik == null) {
				return;
			}
			if (!nomosik.getSquadId().filter(id -> id.equals(payload.squadId())).isPresent()) {
				return;
			}
			SquadManager mgr = SquadService.get(player.server);
			mgr.removeNomosikFromSquad(nomosik);
			player.displayClientMessage(Component.translatable("message.nomosiki.squad.member_removed"), true);
			refreshState(player, mgr);
		});

		ServerPlayNetworking.registerGlobalReceiver(SquadAssignNearbyPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			SquadManager mgr = SquadService.get(player.server);
			if (!mgr.isCommanderOf(player, payload.squadId())) {
				return;
			}
			mgr.assignNearbyOwnedToSquad(player, payload.squadId(), payload.radius());
			player.displayClientMessage(Component.translatable("message.nomosiki.squad.assigned_nearby_done", payload.radius()), true);
			refreshState(player, mgr);
		});

		ServerPlayNetworking.registerGlobalReceiver(SquadJoinByEntityPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			NomosikEntity nomosik = resolveOwnedNomosik(player, payload.entityId());
			if (nomosik == null) {
				return;
			}
			SquadManager mgr = SquadService.get(player.server);
			if (payload.squadId().isEmpty()) {
				boolean hadSquad = nomosik.getSquadId().isPresent();
				mgr.removeNomosikFromSquad(nomosik);
				if (hadSquad) {
					player.displayClientMessage(Component.translatable("message.nomosiki.squad.left_unit"), true);
				}
			} else {
				String key = mgr.tryAddOrTransferMember(player, payload.squadId().get(), nomosik);
				if (!key.isEmpty()) {
					player.displayClientMessage(Component.translatable(key), true);
				} else {
					player.displayClientMessage(Component.translatable("message.nomosiki.squad.member_added"), true);
				}
			}
			refreshState(player, mgr);
		});

		ServerPlayNetworking.registerGlobalReceiver(SquadSetActivePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			SquadManager mgr = SquadService.get(player.server);
			Optional<UUID> sid = payload.squadId();
			if (sid.isPresent()) {
				if (!mgr.isCommanderOf(player, sid.get())) {
					return;
				}
				mgr.setActiveSquad(player, sid.get());
			} else {
				mgr.setActiveSquad(player, null);
			}
			refreshState(player, mgr);
		});
	}

	private static void refreshState(ServerPlayer player, SquadManager mgr) {
		SquadNetworking.sendSquadState(player, mgr);
		SquadNetworking.sendArmyState(player, mgr, true);
	}

	private static NomosikEntity resolveOwnedNomosik(ServerPlayer player, int entityId) {
		ServerLevel level = player.serverLevel();
		Entity entity = level.getEntity(entityId);
		if (!(entity instanceof NomosikEntity nomosik)) {
			return null;
		}
		if (!player.getUUID().equals(nomosik.getOwnerUUID())) {
			return null;
		}
		if (player.distanceToSqr(nomosik) > SQUAD_COMMAND_RANGE_SQR) {
			return null;
		}
		return nomosik;
	}
}
