package com.nomosiki.network.squad;

import com.nomosiki.faction.FactionData;
import com.nomosiki.faction.FactionManager;
import com.nomosiki.faction.FactionService;
import com.nomosiki.faction.WartimeService;
import com.nomosiki.network.squad.payload.c2s.management.SquadGivePayload;
import com.nomosiki.network.squad.payload.c2s.sync.ClanArmyRosterRequestPayload;
import com.nomosiki.network.squad.payload.c2s.sync.ClanArmyWatchPayload;
import com.nomosiki.network.squad.payload.s2c.ClanArmyRosterPayload;
import com.nomosiki.network.squad.payload.s2c.ClanArmyStatePayload;
import com.nomosiki.network.squad.payload.s2c.SquadArmyStatePayload;
import com.nomosiki.squad.NomosikSquad;
import com.nomosiki.squad.SquadManager;
import com.nomosiki.squad.SquadService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class SquadClanNetworking {
	private static final Map<UUID, UUID> WATCHING = new HashMap<>();

	private SquadClanNetworking() {
	}

	public static void register() {
		PayloadTypeRegistry.playC2S().register(ClanArmyRosterRequestPayload.TYPE, ClanArmyRosterRequestPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(ClanArmyWatchPayload.TYPE, ClanArmyWatchPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(SquadGivePayload.TYPE, SquadGivePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(ClanArmyRosterPayload.TYPE, ClanArmyRosterPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(ClanArmyStatePayload.TYPE, ClanArmyStatePayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(ClanArmyRosterRequestPayload.TYPE, (payload, context) ->
			context.server().execute(() -> sendRoster(context.player())));
		ServerPlayNetworking.registerGlobalReceiver(ClanArmyWatchPayload.TYPE, (payload, context) ->
			context.server().execute(() -> watch(context.player(), payload.commanderId())));
		ServerPlayNetworking.registerGlobalReceiver(SquadGivePayload.TYPE, (payload, context) ->
			context.server().execute(() -> give(context.player(), payload.squadId(), payload.targetPlayerId())));
	}

	public static void clearWatch(UUID playerId) {
		WATCHING.remove(playerId);
	}

	private static void sendRoster(ServerPlayer player) {
		FactionManager factions = FactionService.get(player.server);
		String factionName = factions.getPlayerFaction(player.getUUID());
		boolean admin = player.hasPermissions(2);
		List<ClanArmyRosterPayload.Member> members = new ArrayList<>();
		if (factionName != null) {
			factions.getFaction(factionName).ifPresent(faction -> members.addAll(memberRows(factions, faction)));
		}
		members.sort(Comparator.comparing(ClanArmyRosterPayload.Member::name, String.CASE_INSENSITIVE_ORDER));
		ServerPlayNetworking.send(player, new ClanArmyRosterPayload(admin, factionName != null, List.copyOf(members)));
	}

	private static List<ClanArmyRosterPayload.Member> memberRows(FactionManager factions, FactionData faction) {
		List<ClanArmyRosterPayload.Member> members = new ArrayList<>();
		for (UUID memberId : faction.members()) {
			members.add(new ClanArmyRosterPayload.Member(
				memberId,
				factions.memberDisplayName(memberId),
				factions.isPlayerOnline(memberId)
			));
		}
		return members;
	}

	private static void watch(ServerPlayer viewer, Optional<UUID> commanderId) {
		if (!viewer.hasPermissions(2) || commanderId.isEmpty() || commanderId.get().equals(viewer.getUUID())) {
			WATCHING.remove(viewer.getUUID());
			return;
		}
		UUID commander = commanderId.get();
		FactionManager factions = FactionService.get(viewer.server);
		String mine = factions.getPlayerFaction(viewer.getUUID());
		String theirs = factions.getPlayerFaction(commander);
		if (mine == null || theirs == null || !mine.equalsIgnoreCase(theirs)) {
			viewer.displayClientMessage(Component.translatable("message.nomosiki.squad.transfer_faction"), false);
			return;
		}
		WATCHING.put(viewer.getUUID(), commander);
		sendWatched(viewer, commander);
	}

	private static void sendWatched(ServerPlayer viewer, UUID commander) {
		SquadManager manager = SquadService.get(viewer.server);
		SquadArmyStatePayload army = SquadNetworking.buildArmyStateFor(viewer, commander, manager);
		String name = FactionService.get(viewer.server).memberDisplayName(commander);
		ServerPlayNetworking.send(viewer, new ClanArmyStatePayload(commander, name, army));
	}

	private static void give(ServerPlayer actor, UUID squadId, UUID targetId) {
		SquadManager manager = SquadService.get(actor.server);
		Optional<NomosikSquad> squad = manager.data().getSquad(squadId);
		if (squad.isEmpty()) {
			actor.displayClientMessage(Component.translatable("message.nomosiki.squad.not_found"), false);
			return;
		}
		UUID from = squad.get().commanderId();
		boolean admin = actor.hasPermissions(2);
		if (!admin && !actor.getUUID().equals(from)) {
			actor.displayClientMessage(Component.translatable("message.nomosiki.squad.not_commander"), false);
			return;
		}
		if (!admin && WartimeService.get(actor.server).isActive()) {
			actor.displayClientMessage(Component.translatable("message.nomosiki.squad.transfer_wartime"), false);
			return;
		}
		String squadName = squad.get().name().isEmpty() ? squad.get().type().name() : squad.get().name();
		String error = manager.transferSquad(squadId, targetId);
		if (!error.isEmpty()) {
			actor.displayClientMessage(Component.translatable(error), false);
			return;
		}
		FactionManager factions = FactionService.get(actor.server);
		String toName = factions.memberDisplayName(targetId);
		String fromName = factions.memberDisplayName(from);
		actor.displayClientMessage(Component.translatable("message.nomosiki.squad.transfer_ok", squadName, toName), false);
		tell(actor, from, Component.translatable("message.nomosiki.squad.transfer_lost", squadName, toName));
		tell(actor, targetId, Component.translatable("message.nomosiki.squad.transfer_got", squadName, fromName));
		UUID watching = WATCHING.get(actor.getUUID());
		if (watching != null && (watching.equals(from) || watching.equals(targetId))) {
			sendWatched(actor, watching);
		}
		sendRoster(actor);
	}

	private static void tell(ServerPlayer actor, UUID playerId, Component message) {
		if (actor.getUUID().equals(playerId)) {
			return;
		}
		ServerPlayer online = actor.server.getPlayerList().getPlayer(playerId);
		if (online != null) {
			online.displayClientMessage(message, false);
		}
	}
}
