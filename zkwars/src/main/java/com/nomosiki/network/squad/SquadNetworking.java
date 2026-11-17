package com.nomosiki.network.squad;

import com.nomosiki.entity.NomosikCombatMode;
import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.entity.NomosikRangedFireMode;
import com.nomosiki.entity.SquadFormation;
import com.nomosiki.entity.SquadOrder;
import com.nomosiki.squad.SquadManager;
import com.nomosiki.squad.NomosikSquad;
import com.nomosiki.network.squad.payload.c2s.sync.SquadArmyStateRequestPayload;
import com.nomosiki.network.squad.payload.c2s.sync.SquadStateRequestPayload;
import com.nomosiki.network.squad.payload.s2c.NinjaMarkedPlayersPayload;
import com.nomosiki.network.squad.payload.s2c.SquadArmyStatePayload;
import com.nomosiki.network.squad.payload.s2c.SquadOrderAckPayload;
import com.nomosiki.network.squad.payload.s2c.SquadStatePayload;
import com.nomosiki.squad.NinjaMarkService;
import com.nomosiki.squad.SquadService;
import com.nomosiki.squad.SquadWaypointService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class SquadNetworking {
	private static final long MIN_ARMY_STATE_INTERVAL_MS = 500L;
	private static final int MAX_ARMY_SQUADS = 16;

	private static final Map<UUID, Long> LAST_ARMY_STATE_MS = new HashMap<>();

	private SquadNetworking() {
	}

	public static void register() {
		PayloadTypeRegistry.playC2S().register(SquadStateRequestPayload.TYPE, SquadStateRequestPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(SquadArmyStateRequestPayload.TYPE, SquadArmyStateRequestPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(SquadStatePayload.TYPE, SquadStatePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(SquadOrderAckPayload.TYPE, SquadOrderAckPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(SquadArmyStatePayload.TYPE, SquadArmyStatePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(NinjaMarkedPlayersPayload.TYPE, NinjaMarkedPlayersPayload.CODEC);
		SquadClanNetworking.register();

		ServerPlayNetworking.registerGlobalReceiver(SquadStateRequestPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			SquadManager mgr = SquadService.get(player.server);
			sendSquadState(player, mgr);
			sendArmyState(player, mgr, true);
		});

		ServerPlayNetworking.registerGlobalReceiver(SquadArmyStateRequestPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			sendArmyState(player, SquadService.get(player.server), true);
		});
	}

	public static void sendSquadState(ServerPlayer player, SquadManager manager) {
		UUID commander = player.getUUID();
		Optional<UUID> active = manager.getActiveSquadId(commander);
		List<NomosikSquad> squads = manager.listCommanderSquads(commander);
		List<SquadStatePayload.Brief> briefs = new ArrayList<>(squads.size());
		for (NomosikSquad s : squads) {
			if (s.reservePool()) {
				continue;
			}
			briefs.add(new SquadStatePayload.Brief(s.id(), s.type().id(), s.memberCount(), s.name(), s.iconId()));
		}
		ServerPlayNetworking.send(player, new SquadStatePayload(active, List.copyOf(briefs)));
	}

	public static SquadArmyStatePayload buildArmyState(ServerPlayer player, SquadManager manager) {
		return buildArmyStateFor(player, player.getUUID(), manager);
	}

	public static SquadArmyStatePayload buildArmyStateFor(ServerPlayer viewer, UUID commander, SquadManager manager) {
		ServerLevel level = viewer.serverLevel();
		Optional<UUID> active = manager.getActiveSquadId(commander);
		List<SquadArmyStatePayload.SquadRow> rows = new ArrayList<>();
		for (NomosikSquad squad : manager.listCommanderSquads(commander)) {
			if (squad.reservePool()) {
				continue;
			}
			rows.add(buildSquadRow(viewer, level, squad, manager));
		}
		rows.sort(Comparator
			.comparing((SquadArmyStatePayload.SquadRow row) -> row.name().isEmpty() ? "zzz" : row.name().toLowerCase())
			.thenComparing(SquadArmyStatePayload.SquadRow::squadId));
		if (rows.size() > MAX_ARMY_SQUADS) {
			rows = new ArrayList<>(rows.subList(0, MAX_ARMY_SQUADS));
		}
		return new SquadArmyStatePayload(active, List.copyOf(rows), viewer.server.getTickCount());
	}

	public static void sendArmyState(ServerPlayer player, SquadManager manager, boolean force) {
		if (!force && !isArmyStateDue(player)) {
			return;
		}
		ServerPlayNetworking.send(player, buildArmyState(player, manager));
		NinjaMarkService.sendTo(player);
		markArmyStateSent(player);
	}

	public static void sendArmyStateAfterOrder(ServerPlayer player, SquadManager manager) {
		sendArmyState(player, manager, false);
	}

	private static SquadArmyStatePayload.SquadRow buildSquadRow(
		ServerPlayer player,
		ServerLevel level,
		NomosikSquad squad,
		SquadManager manager
	) {
		List<NomosikEntity> members = manager.resolveMembersInLevel(level, squad.id());
		int total = squad.memberCount();
		int alive = 0;
		float healthSum = 0f;
		Map<Byte, Integer> orderVotes = new HashMap<>();
		Map<Byte, Integer> stanceVotes = new HashMap<>();
		Map<Byte, Integer> formationVotes = new HashMap<>();
		Map<Byte, Integer> fireVotes = new HashMap<>();
		Map<Byte, Integer> shieldVotes = new HashMap<>();
		float spacingSum = 0f;
		int spacingCount = 0;
		int maxInRow = com.nomosiki.squad.SquadFormationLayout.DEFAULT_MAX_IN_ROW;
		Vec3 centerSum = Vec3.ZERO;
		BlockPos fireAnchor = BlockPos.ZERO;
		int fireRadius = 0;
		boolean hasFireZone = false;

		for (NomosikEntity nomosik : members) {
			if (!nomosik.isAlive()) {
				continue;
			}
			alive++;
			float maxHp = nomosik.getMaxHealth();
			if (maxHp > 0f) {
				healthSum += nomosik.getHealth() / maxHp;
			}
			byte orderId = nomosik.getSquadOrderForGui().id();
			orderVotes.merge(orderId, 1, Integer::sum);
			byte stanceId = nomosik.getCombatMode().id();
			stanceVotes.merge(stanceId, 1, Integer::sum);
			byte formationId = nomosik.getSquadFormationForGui().id();
			formationVotes.merge(formationId, 1, Integer::sum);
			if (nomosik.isRangedUnit() || nomosik.isArtillerist()) {
				fireVotes.merge(nomosik.getRangedFireMode().id(), 1, Integer::sum);
			}
			if (!hasFireZone
				&& nomosik.getRangedFireMode() == NomosikRangedFireMode.STRATEGIC_FIRE
				&& nomosik.getRangedFireAnchor() != null) {
				fireAnchor = nomosik.getRangedFireAnchor();
				fireRadius = nomosik.getRangedFireRadius();
				hasFireZone = true;
			}
			shieldVotes.merge((byte) (nomosik.isShieldStance() ? 1 : 0), 1, Integer::sum);
			spacingSum += nomosik.getSquadFormationSpacingForGui();
			spacingCount++;
			if (alive == 1) {
				maxInRow = nomosik.getSquadFormationMaxInRow();
			}
			centerSum = centerSum.add(nomosik.position());
		}

		float avgHealth = alive > 0 ? healthSum / alive : 0f;
		byte orderId = majorityVote(orderVotes, SquadOrder.FOLLOW.id());
		byte stanceId = majorityVote(stanceVotes, NomosikCombatMode.NEUTRAL.id());
		byte formationId = majorityVote(formationVotes, SquadFormation.LINE.id());
		byte fireModeId = majorityVote(fireVotes, NomosikRangedFireMode.FIRE_AT_WILL.id());
		if (fireModeId != NomosikRangedFireMode.STRATEGIC_FIRE.id()) {
			hasFireZone = false;
			fireAnchor = BlockPos.ZERO;
			fireRadius = 0;
		}
		boolean shieldStance = majorityVote(shieldVotes, (byte) 0) != 0;
		float spacing = spacingCount > 0 ? spacingSum / spacingCount : (float) com.nomosiki.squad.SquadFormationLayout.DEFAULT_SPACING;

		Vec3 center = alive > 0 ? centerSum.scale(1.0 / alive) : player.position();
		double distance = player.position().distanceTo(center);

		return new SquadArmyStatePayload.SquadRow(
			squad.id(),
			squad.type().id(),
			squad.iconId(),
			squad.name(),
			alive,
			total,
			avgHealth,
			orderId,
			stanceId,
			formationId,
			spacing,
			maxInRow,
			center.x,
			center.y,
			center.z,
			distance,
			fireModeId,
			shieldStance,
			SquadWaypointService.snapshot(squad.id()),
			hasFireZone,
			fireAnchor,
			fireRadius
		);
	}

	private static byte majorityVote(Map<Byte, Integer> votes, byte fallback) {
		byte best = fallback;
		int bestCount = 0;
		for (Map.Entry<Byte, Integer> e : votes.entrySet()) {
			if (e.getValue() > bestCount) {
				bestCount = e.getValue();
				best = e.getKey();
			}
		}
		return best;
	}

	private static boolean isArmyStateDue(ServerPlayer player) {
		Long last = LAST_ARMY_STATE_MS.get(player.getUUID());
		if (last == null) {
			return true;
		}
		return System.currentTimeMillis() - last >= MIN_ARMY_STATE_INTERVAL_MS;
	}

	private static void markArmyStateSent(ServerPlayer player) {
		LAST_ARMY_STATE_MS.put(player.getUUID(), System.currentTimeMillis());
	}
}
