package com.nomosiki.compat.emote.server;

import com.nomosiki.compat.emote.NomosikEmoteCompat;
import com.nomosiki.compat.emote.NomosikEmoteEligibility;
import com.nomosiki.compat.emote.NomosikEmoteIds;
import com.nomosiki.compat.emote.network.NomosikSquadEmoteAckS2CPayload;
import com.nomosiki.compat.emote.network.NomosikSquadEmoteBatchS2CPayload;
import com.nomosiki.config.NomosikiServerConfig;
import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.squad.SquadManager;
import com.nomosiki.squad.SquadService;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NomosikSquadEmoteService {
	private record ActiveEmote(UUID emoteId, String emoteKey, long startGameTime, int entityId) {
	}

	private static final Map<UUID, ActiveEmote> ACTIVE = new ConcurrentHashMap<>();

	private NomosikSquadEmoteService() {
	}

	public static void play(ServerPlayer player, UUID emoteId, String emoteKey, List<UUID> squadIds) {
		if (!NomosikEmoteCompat.isServerEnabled()) {
			player.displayClientMessage(Component.translatable("message.nomosiki.emote.disabled"), true);
			return;
		}
		var resolved = NomosikEmoteIds.resolveAllowed(emoteId, emoteKey);
		if (resolved.isEmpty()) {
			return;
		}
		NomosikEmoteIds.Resolved emote = resolved.get();
		if (squadIds.isEmpty()) {
			return;
		}
		ServerLevel level = player.serverLevel();
		SquadManager squads = SquadService.get(player.server);
		UUID ownerId = player.getUUID();
		int applied = 0;
		int skipped = 0;
		List<NomosikSquadEmoteBatchS2CPayload.Entry> entries = new ArrayList<>();
		long now = level.getGameTime();

		for (UUID squadId : squadIds) {
			if (!squads.isCommanderOf(player, squadId)) {
				continue;
			}
			for (NomosikEntity nomosik : squads.resolveMembersInLevel(level, squadId)) {
				if (!ownerId.equals(nomosik.getOwnerUUID())) {
					continue;
				}
				if (player.distanceToSqr(nomosik) > rangeSqr()) {
					continue;
				}
				if (!NomosikEmoteEligibility.canPlayPlayerModelEmote(nomosik)) {
					skipped++;
					continue;
				}
				ACTIVE.put(nomosik.getUUID(), new ActiveEmote(
					emote.id(), emote.key(), now, nomosik.getId()));
				entries.add(new NomosikSquadEmoteBatchS2CPayload.Entry(
					nomosik.getId(), emote.id(), emote.key(), 0));
				applied++;
			}
		}

		broadcast(level, false, entries);
		ServerPlayNetworking.send(player, new NomosikSquadEmoteAckS2CPayload(applied, skipped, false));
		if (applied == 0 && skipped == 0) {
			player.displayClientMessage(Component.translatable("message.nomosiki.rts.out_of_range"), true);
		}
	}

	public static void stopSquads(ServerPlayer player, List<UUID> squadIds) {
		if (squadIds.isEmpty()) {
			return;
		}
		ServerLevel level = player.serverLevel();
		SquadManager squads = SquadService.get(player.server);
		UUID ownerId = player.getUUID();
		int applied = 0;
		int skipped = 0;
		List<NomosikSquadEmoteBatchS2CPayload.Entry> entries = new ArrayList<>();

		for (UUID squadId : squadIds) {
			if (!squads.isCommanderOf(player, squadId)) {
				continue;
			}
			for (NomosikEntity nomosik : squads.resolveMembersInLevel(level, squadId)) {
				if (!ownerId.equals(nomosik.getOwnerUUID())) {
					continue;
				}
				if (!NomosikEmoteEligibility.canPlayPlayerModelEmote(nomosik)) {
					skipped++;
					continue;
				}
				if (ACTIVE.remove(nomosik.getUUID()) != null) {
					entries.add(new NomosikSquadEmoteBatchS2CPayload.Entry(
						nomosik.getId(), new UUID(0L, 0L), "", 0));
					applied++;
				}
			}
		}

		broadcast(level, true, entries);
		ServerPlayNetworking.send(player, new NomosikSquadEmoteAckS2CPayload(applied, skipped, true));
	}

	public static void interruptEntity(NomosikEntity entity) {
		if (entity.level().isClientSide) {
			return;
		}
		ActiveEmote removed = ACTIVE.remove(entity.getUUID());
		if (removed == null) {
			return;
		}
		if (!(entity.level() instanceof ServerLevel level)) {
			return;
		}
		broadcast(level, true, List.of(
			new NomosikSquadEmoteBatchS2CPayload.Entry(entity.getId(), new UUID(0L, 0L), "", 0)));
	}

	public static void interruptIfConfiguredOnOrder(NomosikEntity entity) {
		if (NomosikiServerConfig.INSTANCE.emoteInterruptOnOrder) {
			interruptEntity(entity);
		}
	}

	public static void interruptIfConfiguredOnDamage(NomosikEntity entity) {
		if (NomosikiServerConfig.INSTANCE.emoteInterruptOnDamage) {
			interruptEntity(entity);
		}
	}

	public static void resendIfPlaying(Entity tracked, ServerPlayer watcher) {
		if (!(tracked instanceof NomosikEntity nomosik)) {
			return;
		}
		ActiveEmote active = ACTIVE.get(nomosik.getUUID());
		if (active == null) {
			return;
		}
		long elapsed = nomosik.level().getGameTime() - active.startGameTime();
		int tick = (int) Math.max(0L, Math.min(Integer.MAX_VALUE, elapsed));
		ServerPlayNetworking.send(watcher, new NomosikSquadEmoteBatchS2CPayload(false, List.of(
			new NomosikSquadEmoteBatchS2CPayload.Entry(
				nomosik.getId(), active.emoteId(), active.emoteKey(), tick))));
	}

	public static void serverTick(MinecraftServer server) {
		int maxTicks = Math.max(20, NomosikiServerConfig.INSTANCE.emoteMaxDurationTicks);
		Iterator<Map.Entry<UUID, ActiveEmote>> it = ACTIVE.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, ActiveEmote> e = it.next();
			ActiveEmote active = e.getValue();
			ServerLevel level = null;
			for (ServerLevel candidate : server.getAllLevels()) {
				if (candidate.getEntity(e.getKey()) instanceof NomosikEntity nomosik) {
					level = candidate;
					long elapsed = candidate.getGameTime() - active.startGameTime();
					if (elapsed > maxTicks || !nomosik.isAlive()) {
						it.remove();
						broadcast(candidate, true, List.of(
							new NomosikSquadEmoteBatchS2CPayload.Entry(nomosik.getId(), new UUID(0L, 0L), "", 0)));
					}
					break;
				}
			}
			if (level == null) {
				// Entity unloaded — drop silently; client will clear on remove.
				it.remove();
			}
		}
	}

	public static void clearAll() {
		ACTIVE.clear();
	}

	private static double rangeSqr() {
		double r = NomosikiServerConfig.INSTANCE.rtsCommandRange;
		return r * r;
	}

	/** Sends one or more packets so large armies are never truncated. */
	private static void broadcast(ServerLevel level, boolean stop, List<NomosikSquadEmoteBatchS2CPayload.Entry> entries) {
		if (entries.isEmpty()) {
			return;
		}
		int max = NomosikSquadEmoteBatchS2CPayload.MAX_ENTRIES;
		for (int i = 0; i < entries.size(); i += max) {
			List<NomosikSquadEmoteBatchS2CPayload.Entry> chunk =
				List.copyOf(entries.subList(i, Math.min(i + max, entries.size())));
			NomosikSquadEmoteBatchS2CPayload payload = new NomosikSquadEmoteBatchS2CPayload(stop, chunk);
			for (ServerPlayer viewer : PlayerLookup.world(level)) {
				ServerPlayNetworking.send(viewer, payload);
			}
		}
	}
}
