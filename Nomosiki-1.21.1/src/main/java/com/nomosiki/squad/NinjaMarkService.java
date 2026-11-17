package com.nomosiki.squad;

import com.nomosiki.advancement.NomosikiActions;
import com.nomosiki.advancement.NomosikiAdvancements;
import com.nomosiki.combat.CombatRelation;
import com.nomosiki.config.NomosikiServerConfig;
import com.nomosiki.entity.NinjaEntity;
import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.faction.FactionManager;
import com.nomosiki.faction.FactionService;
import com.nomosiki.faction.SquadBannerStance;
import com.nomosiki.network.squad.payload.c2s.command.NinjaMarkPlayersPayload;
import com.nomosiki.network.squad.payload.s2c.NinjaMarkedPlayersPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ninja tactical mark: reveal nearby enemy players on the commander's map for a duration.
 */
public final class NinjaMarkService {
	private static final Map<UUID, List<Mark>> marksByCommander = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> cooldownUntilTickBySquad = new ConcurrentHashMap<>();

	private NinjaMarkService() {
	}

	private record Mark(UUID playerId, long expireTick) {
	}

	public static void tick(MinecraftServer server) {
		int tick = server.getTickCount();
		List<UUID> notify = new ArrayList<>();
		for (Iterator<Map.Entry<UUID, List<Mark>>> it = marksByCommander.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, List<Mark>> entry = it.next();
			List<Mark> kept = prune(entry.getValue(), tick);
			if (kept.isEmpty()) {
				it.remove();
				notify.add(entry.getKey());
			} else if (kept.size() != entry.getValue().size()) {
				entry.setValue(kept);
				notify.add(entry.getKey());
			}
		}
		cooldownUntilTickBySquad.entrySet().removeIf(e -> e.getValue() <= tick);
		for (UUID commanderId : notify) {
			ServerPlayer player = server.getPlayerList().getPlayer(commanderId);
			if (player != null) {
				sendTo(player);
			}
		}
	}

	public static void onDisconnect(UUID commanderId) {
		marksByCommander.remove(commanderId);
	}

	public static void handleMark(NinjaMarkPlayersPayload payload, ServerPlayer player) {
		List<UUID> requested = payload.squadIds();
		if (requested.isEmpty()) {
			return;
		}
		MinecraftServer server = player.server;
		SquadManager squads = SquadService.get(server);
		NomosikiServerConfig.NinjaSettings settings = NomosikiServerConfig.INSTANCE.ninja;
		int tick = server.getTickCount();
		ServerLevel level = player.serverLevel();
		UUID commander = player.getUUID();

		List<UUID> ninjaSquads = new ArrayList<>();
		boolean anyOnCooldown = false;
		for (UUID squadId : requested) {
			if (!squads.isCommanderOf(player, squadId)) {
				continue;
			}
			if (squads.data().getSquad(squadId).map(s -> s.type() != SquadType.NINJA).orElse(true)) {
				continue;
			}
			Long until = cooldownUntilTickBySquad.get(squadId);
			if (until != null && until > tick) {
				anyOnCooldown = true;
				continue;
			}
			if (livingNinjas(level, squads, squadId, commander).isEmpty()) {
				continue;
			}
			ninjaSquads.add(squadId);
		}
		if (ninjaSquads.isEmpty()) {
			if (anyOnCooldown) {
				player.displayClientMessage(Component.translatable("message.nomosiki.rts.ninja_mark_cooldown"), true);
			} else {
				player.displayClientMessage(Component.translatable("message.nomosiki.rts.ninja_mark_need_ninja"), true);
			}
			return;
		}

		double range = settings.markRange;
		double rangeSqr = range * range;
		List<NinjaEntity> scouts = new ArrayList<>();
		for (UUID squadId : ninjaSquads) {
			scouts.addAll(livingNinjas(level, squads, squadId, commander));
		}
		if (scouts.isEmpty()) {
			player.displayClientMessage(Component.translatable("message.nomosiki.rts.ninja_mark_need_ninja"), true);
			return;
		}

		AABB search = boundingBoxOf(scouts).inflate(range);
		List<ServerPlayer> found = new ArrayList<>();
		for (ServerPlayer target : level.players()) {
			if (target.getUUID().equals(commander) || !target.isAlive() || target.isRemoved()) {
				continue;
			}
			if (!search.contains(target.position())) {
				continue;
			}
			if (!isMarkable(commander, target)) {
				continue;
			}
			boolean inRange = false;
			for (NinjaEntity ninja : scouts) {
				if (ninja.distanceToSqr(target) <= rangeSqr) {
					inRange = true;
					break;
				}
			}
			if (inRange) {
				found.add(target);
			}
		}

		if (found.isEmpty()) {
			player.displayClientMessage(Component.translatable("message.nomosiki.rts.ninja_mark_none"), true);
			return;
		}

		long expireTick = tick + (long) settings.markDurationTicks;
		List<Mark> marks = new ArrayList<>(marksByCommander.getOrDefault(commander, List.of()));
		marks = prune(marks, tick);
		for (ServerPlayer target : found) {
			marks.removeIf(mark -> mark.playerId().equals(target.getUUID()));
			marks.add(new Mark(target.getUUID(), expireTick));
		}
		marksByCommander.put(commander, marks);

		long cooldownUntil = tick + (long) settings.markCooldownTicks;
		for (UUID squadId : ninjaSquads) {
			cooldownUntilTickBySquad.put(squadId, cooldownUntil);
		}

		sendTo(player);
		NomosikiAdvancements.grant(player, NomosikiActions.NINJA_MARK);
		player.displayClientMessage(
			Component.translatable("message.nomosiki.rts.ninja_mark_ok", found.size()),
			true
		);
	}

	public static void sendTo(ServerPlayer player) {
		ServerPlayNetworking.send(player, buildPayload(player));
	}

	public static NinjaMarkedPlayersPayload buildPayload(ServerPlayer player) {
		MinecraftServer server = player.server;
		int tick = server.getTickCount();
		List<Mark> marks = prune(marksByCommander.getOrDefault(player.getUUID(), List.of()), tick);
		if (marks.isEmpty()) {
			marksByCommander.remove(player.getUUID());
			return new NinjaMarkedPlayersPayload(List.of());
		}
		marksByCommander.put(player.getUUID(), marks);
		FactionManager factions = FactionService.get(server);
		List<NinjaMarkedPlayersPayload.MarkedPlayerRow> rows = new ArrayList<>(marks.size());
		for (Mark mark : marks) {
			ServerPlayer target = server.getPlayerList().getPlayer(mark.playerId());
			if (target == null || !target.isAlive() || target.isRemoved()) {
				continue;
			}
			if (!target.serverLevel().dimension().equals(player.serverLevel().dimension())) {
				continue;
			}
			int remaining = (int) Math.max(0L, mark.expireTick() - tick);
			Vec3 pos = target.position();
			SquadBannerStance stance = stanceToward(factions, player, mark.playerId());
			rows.add(new NinjaMarkedPlayersPayload.MarkedPlayerRow(
				mark.playerId(),
				target.getGameProfile().getName(),
				pos.x,
				pos.y,
				pos.z,
				remaining,
				stance.id()
			));
		}
		return new NinjaMarkedPlayersPayload(List.copyOf(rows));
	}

	private static SquadBannerStance stanceToward(FactionManager factions, ServerPlayer viewer, UUID otherId) {
		String myFaction = factions.getPlayerFaction(viewer.getUUID());
		String theirFaction = factions.getPlayerFaction(otherId);
		if (myFaction != null && myFaction.equalsIgnoreCase(theirFaction)) {
			return SquadBannerStance.FACTION;
		}
		if (myFaction == null || theirFaction == null || theirFaction.isBlank()) {
			return SquadBannerStance.NEUTRAL;
		}
		return SquadBannerStance.fromRelation(FactionService.diplomacy(viewer.server).getRelation(myFaction, theirFaction));
	}

	private static boolean isMarkable(UUID commanderId, ServerPlayer target) {
		return CombatRelation.isFactionEnemyTarget(commanderId, target)
			|| CombatRelation.isFactionNeutralTarget(commanderId, target);
	}

	private static List<NinjaEntity> livingNinjas(
		ServerLevel level,
		SquadManager squads,
		UUID squadId,
		UUID commander
	) {
		List<NinjaEntity> ninjas = new ArrayList<>();
		for (NomosikEntity member : squads.resolveMembersInLevel(level, squadId)) {
			if (member instanceof NinjaEntity ninja
				&& ninja.isAlive()
				&& !ninja.isRemoved()
				&& commander.equals(ninja.getOwnerUUID())) {
				ninjas.add(ninja);
			}
		}
		return ninjas;
	}

	private static AABB boundingBoxOf(List<NinjaEntity> ninjas) {
		AABB box = ninjas.get(0).getBoundingBox();
		for (int i = 1; i < ninjas.size(); i++) {
			box = box.minmax(ninjas.get(i).getBoundingBox());
		}
		return box;
	}

	private static List<Mark> prune(List<Mark> marks, int tick) {
		if (marks == null || marks.isEmpty()) {
			return List.of();
		}
		List<Mark> kept = new ArrayList<>(marks.size());
		for (Mark mark : marks) {
			if (mark.expireTick() > tick) {
				kept.add(mark);
			}
		}
		return kept;
	}
}
