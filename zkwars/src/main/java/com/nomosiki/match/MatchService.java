package com.nomosiki.match;

import com.nomosiki.config.NomosikiServerConfig;
import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.entity.NomosikRace;
import com.nomosiki.match.entity.BuildingHeartEntity;
import com.nomosiki.match.entity.CaptureBeaconEntity;
import com.nomosiki.network.match.payload.s2c.MatchHudPayload;
import com.nomosiki.network.squad.SquadNetworking;
import com.nomosiki.squad.NomosikSquad;
import com.nomosiki.squad.SquadManager;
import com.nomosiki.squad.SquadService;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MatchService {
	public static final int POP_CAP = 100;
	private static final Map<MinecraftServer, MatchService> BY_SERVER = new ConcurrentHashMap<>();

	private final MinecraftServer server;
	private final MatchSession session = new MatchSession();
	private boolean warnedFive;
	private boolean warnedOne;

	private MatchService(MinecraftServer server) {
		this.server = server;
	}

	public static MatchService get(MinecraftServer server) {
		return BY_SERVER.computeIfAbsent(server, MatchService::new);
	}

	public static void forget(MinecraftServer server) {
		BY_SERVER.remove(server);
	}

	public MatchSession session() {
		return this.session;
	}

	public boolean isRunning() {
		return this.session.phase == MatchPhase.RUNNING;
	}

	@Nullable
	public Byte sideOfPlayer(UUID playerId) {
		if (playerId.equals(this.session.side0PlayerId)) {
			return (byte) 0;
		}
		if (playerId.equals(this.session.side1PlayerId)) {
			return (byte) 1;
		}
		return null;
	}

	public boolean areMatchEnemies(@Nullable UUID ownerA, @Nullable UUID ownerB) {
		if (!this.isRunning() || ownerA == null || ownerB == null || ownerA.equals(ownerB)) {
			return false;
		}
		byte a = this.session.sideOfOwner(ownerA);
		byte b = this.session.sideOfOwner(ownerB);
		return a != CaptureBeaconEntity.SIDE_NONE && b != CaptureBeaconEntity.SIDE_NONE && a != b;
	}

	public boolean isMatchParticipant(@Nullable UUID ownerId) {
		return this.isRunning() && this.session.sideOfOwner(ownerId) != CaptureBeaconEntity.SIDE_NONE;
	}

	public Component startSolo(ServerPlayer player, NomosikRace race) {
		if (this.session.phase == MatchPhase.RUNNING || this.session.phase == MatchPhase.COUNTDOWN) {
			return Component.translatable("message.nomosiki.match.already_running");
		}
		NomosikRace chosen = NomosikRace.normalizeForPlay(race);
		NomosikRace opponent = chosen == NomosikRace.ORC ? NomosikRace.HUMAN : NomosikRace.ORC;
		Component missingHq = com.nomosiki.match.schematic.SchematicLibrary.missingHq(player.serverLevel(), chosen, opponent);
		if (missingHq != null) {
			return missingHq;
		}
		this.cleanupWorld(player.serverLevel(), false);
		this.session.reset();
		this.warnedFive = false;
		this.warnedOne = false;

		NomosikiServerConfig.MatchSettings cfg = NomosikiServerConfig.INSTANCE.match;
		this.session.phase = MatchPhase.RUNNING;
		this.session.side0PlayerId = player.getUUID();
		this.session.side1IsBot = true;
		this.session.race0 = chosen;
		this.session.race1 = opponent;
		this.session.authority0 = 60;
		this.session.authority1 = 60;
		this.session.startedAtMs = System.currentTimeMillis();
		int durationSec = cfg.debugCommands ? Math.max(30, cfg.testDurationSeconds) : Math.max(60, cfg.durationSeconds);
		this.session.endsAtMs = this.session.startedAtMs + durationSec * 1000L;

		ServerLevel level = player.serverLevel();
		Vec3 origin = player.position();
		this.session.origin = origin;
		List<MatchMapLayout.Start> starts = MatchMapLayout.startsAround(origin);
		this.session.start0 = MatchSpawner.onSurface(level, starts.get(0).pos());
		this.session.start1 = MatchSpawner.onSurface(level, starts.get(1).pos());

		MatchSpawner.spawnHq(level, this.session, (byte) 0, this.session.start0);
		MatchSpawner.spawnHq(level, this.session, (byte) 1, this.session.start1);
		for (MatchMapLayout.CaptureSpot spot : MatchMapLayout.beaconsAround(origin)) {
			MatchSpawner.spawnBeacon(level, this.session, spot);
		}
		MatchSpawner.spawnStartingForces(level, this.session, (byte) 0, this.session.start0, player);
		MatchSpawner.spawnStartingForces(level, this.session, (byte) 1, this.session.start1, null);
		this.recomputeIncome();
		Vec3 yard = MatchSpawner.yardForSide(level, this.session, (byte) 0);
		player.teleportTo(level, yard.x, yard.y + 1.0, yard.z, player.getYRot(), player.getXRot());
		this.broadcast(Component.translatable("message.nomosiki.match.solo_started", durationSec / 60));
		this.syncHud(false);
		return Component.translatable("message.nomosiki.match.solo_ok");
	}

	public Component forfeit(ServerPlayer player) {
		Byte side = this.sideOfPlayer(player.getUUID());
		if (side == null || this.session.phase != MatchPhase.RUNNING) {
			return Component.translatable("message.nomosiki.match.not_in_match");
		}
		byte winner = (byte) (side == 0 ? 1 : 0);
		this.endMatch(winner, MatchEndReason.FORFEIT);
		return Component.translatable("message.nomosiki.match.forfeit_done");
	}

	public Component forceEnd() {
		if (this.session.phase != MatchPhase.RUNNING && this.session.phase != MatchPhase.COUNTDOWN) {
			return Component.translatable("message.nomosiki.match.not_running");
		}
		this.endMatch(CaptureBeaconEntity.SIDE_NONE, MatchEndReason.FORCE_END);
		return Component.translatable("message.nomosiki.match.force_ended");
	}

	public Component cleanupOnly(ServerPlayer player) {
		this.cleanupWorld(player.serverLevel(), false);
		this.session.reset();
		this.syncHud(false);
		return Component.translatable("message.nomosiki.match.cleaned");
	}

	public void tick() {
		if (this.session.phase == MatchPhase.RUNNING) {
			MatchBuildService.tick(this, this.server.overworld());
		}
		if (this.server.getTickCount() % 20 != 0) {
			return;
		}
		if (this.session.phase != MatchPhase.RUNNING) {
			if (this.session.phase == MatchPhase.ENDED) {
				this.syncHud(true);
			}
			return;
		}
		ServerLevel level = this.server.overworld();
		this.session.addAuthority((byte) 0, this.session.income0);
		this.session.addAuthority((byte) 1, this.session.income1);
		this.recomputeIncome();
		this.tickSupportAuras(level);
		MatchRaceAbilities.tick(level, this.session);
		if (this.server.getTickCount() % 100 == 0) {
			MatchBotController.tick(level, this.session);
		}

		long remainingMs = this.session.endsAtMs - System.currentTimeMillis();
		int remainingSec = (int) Math.max(0, remainingMs / 1000L);
		if (!this.warnedFive && remainingSec <= 300 && remainingSec > 60) {
			this.warnedFive = true;
			this.broadcast(Component.translatable("message.nomosiki.match.warn_5"));
		}
		if (!this.warnedOne && remainingSec <= 60 && remainingSec > 0) {
			this.warnedOne = true;
			this.broadcast(Component.translatable("message.nomosiki.match.warn_1"));
		}
		if (remainingSec <= 0) {
			this.resolveByPoints();
			return;
		}
		this.syncHud(false);
	}

	public void tickCaptureBeacon(CaptureBeaconEntity beacon) {
		if (this.session.phase != MatchPhase.RUNNING) {
			return;
		}
		if (!(beacon.level() instanceof ServerLevel level)) {
			return;
		}
		NomosikiServerConfig.MatchSettings cfg = NomosikiServerConfig.INSTANCE.match;
		double radius = cfg.captureRadius;
		AABB box = beacon.getBoundingBox().inflate(radius);
		int side0 = 0;
		int side1 = 0;
		for (NomosikEntity unit : level.getEntitiesOfClass(NomosikEntity.class, box, Entity::isAlive)) {
			byte side = this.session.sideOfOwner(unit.getOwnerUUID());
			if (side == 0) {
				side0++;
			} else if (side == 1) {
				side1++;
			}
		}
		for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, box, Entity::isAlive)) {
			Byte side = this.sideOfPlayer(p.getUUID());
			if (side != null && side == 0) {
				side0++;
			} else if (side != null && side == 1) {
				side1++;
			}
		}

		if (side0 > 0 && side1 > 0) {
			return;
		}
		byte present = side0 > 0 ? (byte) 0 : (side1 > 0 ? (byte) 1 : CaptureBeaconEntity.SIDE_NONE);
		if (beacon.holdTicksRemaining() > 0) {
			if (present != CaptureBeaconEntity.SIDE_NONE && present != beacon.capturingSide()) {
				beacon.setHoldTicksRemaining(0);
				beacon.setProgress(50.0f);
				return;
			}
			if (present == CaptureBeaconEntity.SIDE_NONE || present != beacon.capturingSide()) {
				beacon.setHoldTicksRemaining(0);
				beacon.setProgress(50.0f);
				return;
			}
			beacon.setHoldTicksRemaining(beacon.holdTicksRemaining() - 20);
			if (beacon.holdTicksRemaining() <= 0) {
				beacon.setOwnerSide(beacon.capturingSide());
				beacon.setProgress(100.0f);
				beacon.setCapturingSide(CaptureBeaconEntity.SIDE_NONE);
				this.recomputeIncome();
				this.broadcast(Component.translatable(
					"message.nomosiki.match.point_captured",
					beacon.pointId(),
					beacon.ownerSide() + 1
				));
			}
			return;
		}

		if (present == CaptureBeaconEntity.SIDE_NONE) {
			if (beacon.progress() > 0.0f && beacon.ownerSide() == CaptureBeaconEntity.SIDE_NONE) {
				beacon.setProgress(beacon.progress() - 8.0f);
			}
			return;
		}

		if (beacon.ownerSide() == present) {
			beacon.setProgress(100.0f);
			return;
		}

		float speed = captureSpeed(this.session.raceOfSide(present));
		if (beacon.hasBlade()) {
			UUID blade = beacon.bladeOwnerId();
			Byte bladeSide = blade == null ? null : this.sideOfPlayer(blade);
			if (bladeSide != null && bladeSide == present) {
				speed *= cfg.bladeSpeedMultiplier;
			}
		}
		if (beacon.capturingSide() != present) {
			beacon.setCapturingSide(present);
			if (beacon.ownerSide() != CaptureBeaconEntity.SIDE_NONE && beacon.ownerSide() != present) {
				beacon.setProgress(Math.min(beacon.progress(), 40.0f));
			}
		}
		float next = beacon.progress() + speed;
		if (next >= 100.0f) {
			beacon.setProgress(100.0f);
			beacon.setHoldTicksRemaining(cfg.holdSeconds * 20);
		} else {
			beacon.setProgress(next);
		}
	}

	private static float captureSpeed(NomosikRace race) {
		return switch (race) {
			case ORC -> 6.0f;
			case DWARF -> 3.0f;
			default -> 4.5f;
		};
	}

	private void recomputeIncome() {
		int income0 = 2;
		int income1 = 2;
		ServerLevel level = this.server.overworld();
		for (Map.Entry<String, UUID> entry : this.session.beaconIds.entrySet()) {
			Entity entity = level.getEntity(entry.getValue());
			if (!(entity instanceof CaptureBeaconEntity beacon)) {
				continue;
			}
			boolean hasOutpost = false;
			UUID outpostId = this.session.outpostByBeacon.get(entry.getKey());
			if (outpostId != null) {
				Entity outpost = level.getEntity(outpostId);
				hasOutpost = outpost instanceof BuildingHeartEntity heart && heart.isAlive() && heart.isComplete();
			}
			int pointIncome = hasOutpost ? 4 : 2;
			if (beacon.ownerSide() == 0) {
				income0 += pointIncome;
			} else if (beacon.ownerSide() == 1) {
				income1 += pointIncome;
			}
		}
		this.session.setIncome((byte) 0, income0);
		this.session.setIncome((byte) 1, income1);
	}

	private void tickSupportAuras(ServerLevel level) {
		for (UUID id : this.session.trackedEntities) {
			Entity entity = level.getEntity(id);
			if (!(entity instanceof NomosikEntity support) || !support.getTags().contains("zkwars_match_support")) {
				continue;
			}
			if (!support.isAlive()) {
				continue;
			}
			AABB box = support.getBoundingBox().inflate(8.0);
			for (NomosikEntity ally : level.getEntitiesOfClass(NomosikEntity.class, box, e ->
				e.isAlive() && e.getOwnerUUID() != null && e.getOwnerUUID().equals(support.getOwnerUUID()))) {
				if (ally.getHealth() < ally.getMaxHealth()) {
					ally.heal(0.5f);
				}
			}
		}
	}

	public void onBuildingHeartDestroyed(BuildingHeartEntity heart) {
		if (this.session.phase != MatchPhase.RUNNING) {
			return;
		}
		byte loser = heart.matchSide();
		byte winner = (byte) (loser == 0 ? 1 : 0);
		MatchRaceAbilities.onBuildingDestroyed(this.session, winner);
		if (heart.buildingType() == BuildingHeartType.HQ) {
			this.endMatch(winner, MatchEndReason.HQ_DESTROYED);
		}
	}

	public boolean isProtectedShell(BlockPos pos) {
		return this.session.phase == MatchPhase.RUNNING && this.session.trackedBlocks.contains(pos);
	}

	private void resolveByPoints() {
		int p0 = 0;
		int p1 = 0;
		ServerLevel level = this.server.overworld();
		for (UUID id : this.session.beaconIds.values()) {
			Entity entity = level.getEntity(id);
			if (entity instanceof CaptureBeaconEntity beacon) {
				if (beacon.ownerSide() == 0) {
					p0++;
				} else if (beacon.ownerSide() == 1) {
					p1++;
				}
			}
		}
		byte winner;
		if (p0 > p1) {
			winner = 0;
		} else if (p1 > p0) {
			winner = 1;
		} else if (this.session.income0 != this.session.income1) {
			winner = (byte) (this.session.income0 > this.session.income1 ? 0 : 1);
		} else {
			float hq0 = hqHp(level, this.session.hq0Id);
			float hq1 = hqHp(level, this.session.hq1Id);
			winner = hq0 >= hq1 ? (byte) 0 : (byte) 1;
		}
		this.endMatch(winner, MatchEndReason.POINTS);
	}

	private static float hqHp(ServerLevel level, @Nullable UUID id) {
		if (id == null) {
			return 0.0f;
		}
		Entity entity = level.getEntity(id);
		return entity instanceof BuildingHeartEntity heart && heart.isAlive() ? heart.getHealth() : 0.0f;
	}

	private void endMatch(byte winnerSide, MatchEndReason reason) {
		this.session.phase = MatchPhase.ENDED;
		this.session.winnerSide = winnerSide;
		this.session.endReason = reason;
		this.syncHud(true);
		Component msg = switch (reason) {
			case HQ_DESTROYED -> Component.translatable("message.nomosiki.match.end_hq", winnerSide + 1);
			case POINTS -> Component.translatable("message.nomosiki.match.end_points", winnerSide + 1);
			case FORFEIT -> Component.translatable("message.nomosiki.match.end_forfeit", winnerSide + 1);
			default -> Component.translatable("message.nomosiki.match.end_force");
		};
		this.broadcast(msg);
		for (ServerPlayer player : this.server.getPlayerList().getPlayers()) {
			Byte side = this.sideOfPlayer(player.getUUID());
			if (side == null) {
				continue;
			}
			boolean win = side == winnerSide && winnerSide != CaptureBeaconEntity.SIDE_NONE;
			player.displayClientMessage(
				win
					? Component.translatable("message.nomosiki.match.you_win")
					: Component.translatable("message.nomosiki.match.you_lose"),
				false
			);
		}
		ServerLevel level = this.server.overworld();
		this.cleanupWorld(level, true);
		MatchPhase ended = MatchPhase.ENDED;
		MatchEndReason keepReason = reason;
		byte keepWinner = winnerSide;
		UUID p0 = this.session.side0PlayerId;
		UUID p1 = this.session.side1PlayerId;
		this.session.reset();
		this.session.phase = ended;
		this.session.endReason = keepReason;
		this.session.winnerSide = keepWinner;
		this.session.side0PlayerId = p0;
		this.session.side1PlayerId = p1;
		this.server.execute(() -> {
			this.session.phase = MatchPhase.IDLE;
			this.syncHud(false);
		});
	}

	private void cleanupWorld(ServerLevel level, boolean discardEntities) {
		List<UUID> ids = new ArrayList<>(this.session.trackedEntities);
		for (UUID id : ids) {
			Entity entity = level.getEntity(id);
			if (entity instanceof BuildingHeartEntity heart) {
				heart.clearShell(level);
			}
			if (discardEntities && entity != null) {
				entity.discard();
			}
		}
		for (BlockPos pos : new ArrayList<>(this.session.trackedBlocks)) {
			if (BuildingHeartEntity.isShellBlock(level.getBlockState(pos))) {
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
			}
		}
		this.session.trackedEntities.clear();
		this.session.trackedBlocks.clear();
		this.session.beaconIds.clear();
		this.session.workerIds.clear();
		this.session.hq0Id = null;
		this.session.hq1Id = null;
	}

	public Component giveAuthority(ServerPlayer player, int amount) {
		Byte side = this.sideOfPlayer(player.getUUID());
		if (side == null) {
			return Component.translatable("message.nomosiki.match.not_in_match");
		}
		this.session.addAuthority(side, amount);
		this.syncHud(false);
		return Component.translatable("message.nomosiki.match.authority_given", amount);
	}

	public Component setTimerSeconds(int seconds) {
		if (this.session.phase != MatchPhase.RUNNING) {
			return Component.translatable("message.nomosiki.match.not_running");
		}
		this.session.endsAtMs = System.currentTimeMillis() + Math.max(1, seconds) * 1000L;
		this.warnedFive = seconds > 300;
		this.warnedOne = seconds > 60;
		this.syncHud(false);
		return Component.translatable("message.nomosiki.match.timer_set", seconds);
	}

	public Component ownPoint(String pointId, ServerPlayer player) {
		Byte side = this.sideOfPlayer(player.getUUID());
		if (side == null || this.session.phase != MatchPhase.RUNNING) {
			return Component.translatable("message.nomosiki.match.not_in_match");
		}
		UUID id = this.session.beaconIds.get(pointId);
		if (id == null) {
			return Component.translatable("message.nomosiki.match.unknown_point", pointId);
		}
		Entity entity = this.server.overworld().getEntity(id);
		if (!(entity instanceof CaptureBeaconEntity beacon)) {
			return Component.translatable("message.nomosiki.match.unknown_point", pointId);
		}
		beacon.setOwnerSide(side);
		beacon.setProgress(100.0f);
		beacon.setHoldTicksRemaining(0);
		beacon.setCapturingSide(CaptureBeaconEntity.SIDE_NONE);
		this.recomputeIncome();
		this.syncHud(false);
		return Component.translatable("message.nomosiki.match.point_owned", pointId);
	}

	public Component setPointProgress(String pointId, float progress) {
		UUID id = this.session.beaconIds.get(pointId);
		if (id == null) {
			return Component.translatable("message.nomosiki.match.unknown_point", pointId);
		}
		Entity entity = this.server.overworld().getEntity(id);
		if (!(entity instanceof CaptureBeaconEntity beacon)) {
			return Component.translatable("message.nomosiki.match.unknown_point", pointId);
		}
		beacon.setProgress(progress);
		if (progress >= 100.0f && beacon.ownerSide() == CaptureBeaconEntity.SIDE_NONE) {
			beacon.setCapturingSide((byte) 0);
			beacon.setHoldTicksRemaining(NomosikiServerConfig.INSTANCE.match.holdSeconds * 20);
		}
		return Component.literal("OK");
	}

	public Component damageHq(byte side, float amount) {
		UUID id = side == 1 ? this.session.hq1Id : this.session.hq0Id;
		if (id == null) {
			return Component.translatable("message.nomosiki.match.no_hq");
		}
		Entity entity = this.server.overworld().getEntity(id);
		if (!(entity instanceof BuildingHeartEntity heart)) {
			return Component.translatable("message.nomosiki.match.no_hq");
		}
		heart.hurt(heart.damageSources().generic(), amount);
		this.syncHud(false);
		return Component.translatable("message.nomosiki.match.hq_damaged", amount);
	}

	public void syncHudPublic(boolean showEnd) {
		this.syncHud(showEnd);
	}

	public Component recruit(ServerPlayer player, MatchRecruitment.Role role) {
		Byte side = this.sideOfPlayer(player.getUUID());
		if (side == null || this.session.phase != MatchPhase.RUNNING) {
			return Component.translatable("message.nomosiki.match.not_in_match");
		}
		NomosikRace race = this.session.raceOfSide(side);
		if (!role.allowedFor(race)) {
			return Component.translatable("message.nomosiki.match.recruit_denied");
		}
		if (!MatchBuildService.canRecruitRole(player.serverLevel(), this.session, side, role)) {
			return Component.translatable("message.nomosiki.match.recruit_need_building");
		}
		int cost = role.cost(race);
		int size = MatchRecruitment.squadSize(role);
		int popCost = role.pop() * size;
		if (this.session.getPop(side) + popCost > POP_CAP) {
			return Component.translatable("message.nomosiki.match.pop_full");
		}
		if (this.session.getAuthority(side) < cost) {
			return Component.translatable("message.nomosiki.match.need_authority", cost);
		}
		int spawned = MatchSpawner.recruitNearHq(player.serverLevel(), this.session, side, role, player);
		if (spawned <= 0) {
			return Component.translatable("message.nomosiki.match.recruit_failed");
		}
		this.session.addAuthority(side, -cost);
		this.syncHud(false);
		return Component.translatable("message.nomosiki.match.recruited", role.id(), cost);
	}

	public Component buyWargear(ServerPlayer player, UUID squadId, String upgradeId) {
		Byte side = this.sideOfPlayer(player.getUUID());
		if (side == null || this.session.phase != MatchPhase.RUNNING) {
			return Component.translatable("message.nomosiki.match.not_in_match");
		}
		MatchWargear.Upgrade upgrade = MatchWargear.byId(upgradeId);
		if (upgrade == null) {
			return Component.translatable("message.nomosiki.match.wargear_denied");
		}
		SquadManager manager = SquadService.get(player.server);
		if (!manager.isCommanderOf(player, squadId)) {
			return Component.translatable("message.nomosiki.match.wargear_denied");
		}
		NomosikSquad squad = manager.data().getSquad(squadId).orElse(null);
		if (squad == null || !upgrade.role().id().equals(squad.matchRole())) {
			return Component.translatable("message.nomosiki.match.wargear_denied");
		}
		if (squad.hasWargear(upgrade.id())) {
			return Component.translatable("message.nomosiki.match.wargear_owned");
		}
		if (MatchWargear.slotTaken(squad, upgrade)) {
			return Component.translatable("message.nomosiki.match.wargear_slot");
		}
		NomosikRace race = this.session.raceOfSide(side);
		int cost = upgrade.priced(race);
		if (this.session.getAuthority(side) < cost) {
			return Component.translatable("message.nomosiki.match.need_authority", cost);
		}
		this.session.addAuthority(side, -cost);
		squad.addWargear(upgrade.id());
		manager.data().setDirty();
		MatchWargear.equipAll(squad, manager.resolveMembersInLevel(player.serverLevel(), squad.id()));
		SquadNetworking.sendArmyState(player, manager, true);
		this.syncHud(false);
		return Component.translatable("message.nomosiki.match.wargear_bought", cost);
	}

	public Component reinforce(ServerPlayer player, UUID squadId) {
		Byte side = this.sideOfPlayer(player.getUUID());
		if (side == null || this.session.phase != MatchPhase.RUNNING) {
			return Component.translatable("message.nomosiki.match.not_in_match");
		}
		SquadManager manager = SquadService.get(player.server);
		if (!manager.isCommanderOf(player, squadId)) {
			return Component.translatable("message.nomosiki.match.reinforce_denied");
		}
		NomosikSquad squad = manager.data().getSquad(squadId).orElse(null);
		MatchRecruitment.Role role = squad == null ? null : MatchRecruitment.Role.fromId(squad.matchRole());
		if (squad == null || role == null) {
			return Component.translatable("message.nomosiki.match.reinforce_denied");
		}
		int alive = 0;
		for (NomosikEntity member : manager.resolveMembersInLevel(player.serverLevel(), squad.id())) {
			if (member.isAlive()) {
				alive++;
			}
		}
		int missing = MatchRecruitment.squadSize(role) - alive;
		if (missing <= 0) {
			return Component.translatable("message.nomosiki.match.reinforce_full");
		}
		NomosikRace race = this.session.raceOfSide(side);
		int cost = MatchRecruitment.reinforceCost(role, race, missing);
		int popCost = MatchRecruitment.reinforcePop(role, missing);
		if (this.session.getPop(side) + popCost > POP_CAP) {
			return Component.translatable("message.nomosiki.match.pop_full");
		}
		if (this.session.getAuthority(side) < cost) {
			return Component.translatable("message.nomosiki.match.need_authority", cost);
		}
		int spawned = MatchSpawner.reinforceSquad(player, this.session, squad, role, missing, race);
		if (spawned <= 0) {
			return Component.translatable("message.nomosiki.match.recruit_failed");
		}
		int spent = MatchRecruitment.reinforceCost(role, race, spawned);
		this.session.addAuthority(side, -spent);
		this.session.setPop(side, this.session.getPop(side) + role.pop() * spawned);
		MatchWargear.equipAll(squad, manager.resolveMembersInLevel(player.serverLevel(), squad.id()));
		SquadNetworking.sendArmyState(player, manager, true);
		this.syncHud(false);
		return Component.translatable("message.nomosiki.match.reinforced", spawned, spent);
	}

	public Component status(ServerPlayer player) {
		return Component.translatable(
			"message.nomosiki.match.status",
			this.session.phase.name(),
			Math.max(0, (this.session.endsAtMs - System.currentTimeMillis()) / 1000L),
			this.session.beaconIds.size(),
			this.session.trackedEntities.size(),
			this.session.authority0,
			this.session.authority1
		);
	}

	private void syncHud(boolean showEnd) {
		ServerLevel level = this.server.overworld();
		int owned0 = 0;
		int owned1 = 0;
		for (UUID id : this.session.beaconIds.values()) {
			Entity entity = level.getEntity(id);
			if (entity instanceof CaptureBeaconEntity beacon) {
				if (beacon.ownerSide() == 0) {
					owned0++;
				} else if (beacon.ownerSide() == 1) {
					owned1++;
				}
			}
		}
		float hq0 = hqHp(level, this.session.hq0Id);
		float hq1 = hqHp(level, this.session.hq1Id);
		float hq0Max = hqMax(level, this.session.hq0Id);
		float hq1Max = hqMax(level, this.session.hq1Id);
		int remaining = this.session.phase == MatchPhase.RUNNING
			? (int) Math.max(0, (this.session.endsAtMs - System.currentTimeMillis()) / 1000L)
			: 0;
		for (ServerPlayer player : this.server.getPlayerList().getPlayers()) {
			Byte side = this.sideOfPlayer(player.getUUID());
			byte yourSide = side == null ? CaptureBeaconEntity.SIDE_NONE : side;
			int auth = yourSide == 1 ? this.session.authority1 : this.session.authority0;
			int income = yourSide == 1 ? this.session.income1 : this.session.income0;
			int ownPts = yourSide == 1 ? owned1 : owned0;
			float ownHp = yourSide == 1 ? hq1 : hq0;
			float ownMax = yourSide == 1 ? hq1Max : hq0Max;
			float enemyHp = yourSide == 1 ? hq0 : hq1;
			float enemyMax = yourSide == 1 ? hq0Max : hq1Max;
			int pop = yourSide == 1 ? this.session.pop1 : this.session.pop0;
			ServerPlayNetworking.send(player, new MatchHudPayload(
				this.session.phase.id(),
				remaining,
				auth,
				income,
				ownPts,
				this.session.beaconIds.size(),
				ownHp,
				ownMax,
				enemyHp,
				enemyMax,
				this.session.endReason.id(),
				this.session.winnerSide,
				yourSide,
				pop,
				POP_CAP,
				showEnd && yourSide != CaptureBeaconEntity.SIDE_NONE,
				yourSide == CaptureBeaconEntity.SIDE_NONE
					? com.nomosiki.entity.NomosikRace.HUMAN.id()
					: this.session.raceOfSide(yourSide).id()
			));
		}
	}

	private static float hqMax(ServerLevel level, @Nullable UUID id) {
		if (id == null) {
			return BuildingHeartType.HQ.maxHp();
		}
		Entity entity = level.getEntity(id);
		return entity instanceof BuildingHeartEntity heart ? heart.getMaxHealth() : BuildingHeartType.HQ.maxHp();
	}

	private void broadcast(Component message) {
		for (ServerPlayer player : this.server.getPlayerList().getPlayers()) {
			player.displayClientMessage(message, false);
		}
	}

	public void onDisconnect(ServerPlayer player) {
		Byte side = this.sideOfPlayer(player.getUUID());
		if (side != null && this.session.phase == MatchPhase.RUNNING) {
			this.forfeit(player);
		}
	}
}
