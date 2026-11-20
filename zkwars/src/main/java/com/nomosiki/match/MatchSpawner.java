package com.nomosiki.match;

import com.nomosiki.entity.NomosikCombatMode;
import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.entity.NomosikRace;
import com.nomosiki.match.entity.BuildingHeartEntity;
import com.nomosiki.match.entity.CaptureBeaconEntity;
import com.nomosiki.network.squad.SquadNetworking;
import com.nomosiki.registry.ModEntities;
import com.nomosiki.squad.NomosikSquad;
import com.nomosiki.squad.SquadManager;
import com.nomosiki.squad.SquadService;
import com.nomosiki.squad.SquadType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class MatchSpawner {
	public static final double YARD_OFFSET = 6.0;
	public static final double SQUAD_RING_RADIUS = 4.5;
	private static final String WORKERS_SQUAD_NAME = "Workers";

	private MatchSpawner() {
	}

	public static double surfaceY(ServerLevel level, double x, double z) {
		BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(x, 0, z));
		return pos.getY();
	}

	public static Vec3 onSurface(ServerLevel level, Vec3 approx) {
		double y = surfaceY(level, approx.x, approx.z);
		return new Vec3(approx.x, y, approx.z);
	}

	/** Spawn yard outside the HQ shell perimeter (shell is ~2 blocks from center). */
	public static Vec3 yardOutsideShell(ServerLevel level, Vec3 heartOrAt) {
		return onSurface(level, heartOrAt.add(YARD_OFFSET, 0.0, 0.0));
	}

	public static Vec3 yardForSide(ServerLevel level, MatchSession session, byte side) {
		UUID hqId = side == 0 ? session.hq0Id : session.hq1Id;
		if (hqId != null) {
			Entity entity = level.getEntity(hqId);
			if (entity != null) {
				return yardOutsideShell(level, entity.position());
			}
		}
		Vec3 start = side == 0 ? session.start0 : session.start1;
		if (start == null) {
			start = session.origin;
		}
		return yardOutsideShell(level, start);
	}

	public static BuildingHeartEntity spawnHq(ServerLevel level, MatchSession session, byte side, Vec3 at) {
		Vec3 pos = onSurface(level, at);
		BuildingHeartEntity heart = ModEntities.BUILDING_HEART.create(level);
		if (heart == null) {
			return null;
		}
		heart.moveTo(pos.x + 0.5, pos.y, pos.z + 0.5, 0.0f, 0.0f);
		heart.setBuildingType(BuildingHeartType.HQ);
		heart.setBuildKind("hq");
		heart.setMatchSide(side);
		heart.setOwnerUUID(session.ownerOfSide(side));
		heart.setCustomNameVisible(true);
		level.addFreshEntity(heart);
		heart.buildDefaultShell(level);
		session.trackedBlocks.addAll(heart.shellBlocksView());
		session.track(heart.getUUID());
		if (side == 0) {
			session.hq0Id = heart.getUUID();
		} else {
			session.hq1Id = heart.getUUID();
		}
		return heart;
	}

	public static CaptureBeaconEntity spawnBeacon(ServerLevel level, MatchSession session, MatchMapLayout.CaptureSpot spot) {
		Vec3 pos = onSurface(level, spot.pos());
		CaptureBeaconEntity beacon = ModEntities.CAPTURE_BEACON.create(level);
		if (beacon == null) {
			return null;
		}
		beacon.moveTo(pos.x + 0.5, pos.y, pos.z + 0.5, 0.0f, 0.0f);
		beacon.setPointId(spot.id());
		beacon.setCustomNameVisible(true);
		level.addFreshEntity(beacon);
		session.beaconIds.put(spot.id(), beacon.getUUID());
		session.track(beacon.getUUID());
		return beacon;
	}

	public static BuildingHeartEntity spawnBuilding(
		ServerLevel level,
		MatchSession session,
		byte side,
		BuildingHeartType type,
		String buildKind,
		Vec3 at
	) {
		Vec3 pos = onSurface(level, at);
		BuildingHeartEntity heart = ModEntities.BUILDING_HEART.create(level);
		if (heart == null) {
			return null;
		}
		heart.moveTo(pos.x + 0.5, pos.y, pos.z + 0.5, 0.0f, 0.0f);
		heart.setBuildingType(type);
		heart.setMatchSide(side);
		heart.setOwnerUUID(session.ownerOfSide(side));
		heart.setBuildKind(buildKind);
		heart.setCustomNameVisible(true);
		level.addFreshEntity(heart);
		heart.buildDefaultShell(level);
		session.trackedBlocks.addAll(heart.shellBlocksView());
		session.track(heart.getUUID());
		return heart;
	}

	public static int spawnStartingForces(
		ServerLevel level,
		MatchSession session,
		byte side,
		Vec3 at,
		@Nullable ServerPlayer playerCommander
	) {
		NomosikRace race = session.raceOfSide(side);
		UUID owner = session.ownerOfSide(side);
		Vec3 yard = yardOutsideShell(level, at.add(0.5, 0.0, 0.5));
		UUID hqId = side == 0 ? session.hq0Id : session.hq1Id;
		if (hqId != null) {
			Entity hq = level.getEntity(hqId);
			if (hq != null) {
				yard = yardOutsideShell(level, hq.position());
			}
		}
		int spawned = 0;
		spawned += spawnWorker(level, session, side, owner, race, yard, playerCommander);
		if (playerCommander != null && side == 0) {
			spawned += spawnPlayerSquad(playerCommander, session, race, SquadType.INFANTRY, 6, yard, "Match Line", MatchRecruitment.Role.INFANTRY.id());
		} else {
			spawned += spawnOwnedSquad(level, session, side, owner, race, SquadType.INFANTRY, 6, yard, false, false);
		}
		session.setPop(side, session.getPop(side) + 1 + 6);
		return spawned;
	}

	public static int spawnWorker(
		ServerLevel level,
		MatchSession session,
		byte side,
		UUID owner,
		NomosikRace race,
		Vec3 at,
		@Nullable ServerPlayer playerCommander
	) {
		NomosikEntity worker = ModEntities.NOMOSIK.create(level);
		if (worker == null) {
			return 0;
		}
		Vec3 pos = onSurface(level, at);
		worker.moveTo(pos.x, pos.y, pos.z, 0.0f, 0.0f);
		worker.finalizeSpawn(level, level.getCurrentDifficultyAt(worker.blockPosition()), MobSpawnType.COMMAND, null);
		level.addFreshEntity(worker);
		worker.setOwnerUUID(owner);
		worker.setShouldFollowOwner(false);
		worker.setNomosikRace(race);
		worker.setCombatMode(NomosikCombatMode.PASSIVE);
		worker.setCustomName(net.minecraft.network.chat.Component.translatable("nomosiki.match.role.worker"));
		session.workerIds.add(worker.getUUID());
		session.track(worker.getUUID());
		worker.addTag("zkwars_match_worker");

		if (playerCommander != null && playerCommander.getUUID().equals(owner)) {
			assignToWorkersSquad(playerCommander, worker);
		} else {
			worker.joinSquadServer(UUID.randomUUID());
		}
		return 1;
	}

	private static void assignToWorkersSquad(ServerPlayer player, NomosikEntity worker) {
		SquadManager mgr = SquadService.get(player.server);
		UUID workersId = null;
		for (NomosikSquad squad : mgr.listCommanderSquads(player.getUUID())) {
			if (WORKERS_SQUAD_NAME.equalsIgnoreCase(squad.name())) {
				workersId = squad.id();
				break;
			}
		}
		if (workersId == null) {
			var createErr = mgr.createNamedSquad(player, WORKERS_SQUAD_NAME, (byte) 0, SquadType.INFANTRY);
			if (createErr.isPresent()) {
				return;
			}
			workersId = mgr.getActiveSquadId(player.getUUID()).orElse(null);
		}
		if (workersId == null) {
			return;
		}
		UUID assigned = workersId;
		mgr.data().getSquad(assigned).ifPresent(squad -> {
			if (squad.matchRole().isEmpty()) {
				squad.setMatchRole(MatchRecruitment.Role.WORKER.id());
				mgr.data().setDirty();
			}
		});
		String addErr = mgr.tryAddOrTransferMember(player, workersId, worker);
		if (!addErr.isEmpty()) {
			return;
		}
		SquadNetworking.sendSquadState(player, mgr);
		SquadNetworking.sendArmyState(player, mgr, true);
	}

	public static int spawnPlayerSquad(
		ServerPlayer player,
		MatchSession session,
		NomosikRace race,
		SquadType type,
		int count,
		Vec3 at,
		String name,
		@Nullable String matchRole
	) {
		SquadManager mgr = SquadService.get(player.server);
		var createErr = mgr.createNamedSquad(player, name, (byte) 0, type);
		if (createErr.isPresent()) {
			return 0;
		}
		UUID squadId = mgr.getActiveSquadId(player.getUUID()).orElse(null);
		if (squadId == null) {
			return 0;
		}
		if (matchRole != null && !matchRole.isBlank()) {
			mgr.data().getSquad(squadId).ifPresent(squad -> {
				squad.setMatchRole(matchRole);
				mgr.data().setDirty();
			});
		}
		ServerLevel level = player.serverLevel();
		EntityType<? extends NomosikEntity> entityType = entityTypeFor(type);
		int spawned = 0;
		for (int i = 0; i < count; i++) {
			double angle = (Math.PI * 2.0 * i) / count;
			double x = at.x + Math.cos(angle) * SQUAD_RING_RADIUS;
			double z = at.z + Math.sin(angle) * SQUAD_RING_RADIUS;
			double y = surfaceY(level, x, z);
			NomosikEntity entity = entityType.create(level);
			if (entity == null) {
				continue;
			}
			entity.moveTo(x, y, z, player.getYRot(), 0.0f);
			entity.finalizeSpawn(level, level.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.COMMAND, null);
			level.addFreshEntity(entity);
			entity.setOwnerUUID(player.getUUID());
			entity.setShouldFollowOwner(false);
			entity.setNomosikRace(race);
			entity.setCombatMode(NomosikCombatMode.NEUTRAL);
			String addErr = mgr.tryAddOrTransferMember(player, squadId, entity);
			if (!addErr.isEmpty()) {
				entity.discard();
				break;
			}
			session.track(entity.getUUID());
			spawned++;
		}
		SquadNetworking.sendSquadState(player, mgr);
		SquadNetworking.sendArmyState(player, mgr, true);
		return spawned;
	}

	public static int spawnOwnedSquad(
		ServerLevel level,
		MatchSession session,
		byte side,
		UUID owner,
		NomosikRace race,
		SquadType type,
		int count,
		Vec3 at,
		boolean antiArmor,
		boolean support
	) {
		EntityType<? extends NomosikEntity> entityType = entityTypeFor(type);
		UUID squadId = UUID.randomUUID();
		int spawned = 0;
		Vec3 base = onSurface(level, at);
		for (int i = 0; i < count; i++) {
			double angle = (Math.PI * 2.0 * i) / Math.max(1, count);
			double x = base.x + Math.cos(angle) * SQUAD_RING_RADIUS;
			double z = base.z + Math.sin(angle) * SQUAD_RING_RADIUS;
			double y = surfaceY(level, x, z);
			NomosikEntity entity = entityType.create(level);
			if (entity == null) {
				continue;
			}
			entity.moveTo(x, y, z, 0.0f, 0.0f);
			entity.finalizeSpawn(level, level.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.COMMAND, null);
			level.addFreshEntity(entity);
			entity.setOwnerUUID(owner);
			entity.setShouldFollowOwner(false);
			entity.setNomosikRace(race);
			entity.setCombatMode(NomosikCombatMode.AGGRESSIVE);
			entity.joinSquadServer(squadId);
			if (antiArmor) {
				entity.addTag("zkwars_match_anti_armor");
			}
			if (support) {
				entity.addTag("zkwars_match_support");
			}
			if (type == SquadType.CAPTAIN) {
				entity.addTag("zkwars_match_elite");
				entity.setHealth(Math.min(entity.getMaxHealth() * 2.5f, 140.0f));
			}
			session.track(entity.getUUID());
			spawned++;
		}
		return spawned;
	}

	public static int recruitNearHq(
		ServerLevel level,
		MatchSession session,
		byte side,
		MatchRecruitment.Role role,
		@Nullable ServerPlayer player
	) {
		Vec3 at = yardForSide(level, session, side);
		NomosikRace race = session.raceOfSide(side);
		UUID owner = session.ownerOfSide(side);
		int size = MatchRecruitment.squadSize(role);
		int spawned;
		if (role == MatchRecruitment.Role.WORKER) {
			spawned = spawnWorker(level, session, side, owner, race, at, player);
		} else if (player != null && side == 0 && owner != null && owner.equals(player.getUUID())) {
			SquadType type = role.squadType() == null ? SquadType.INFANTRY : role.squadType();
			spawned = spawnPlayerSquad(player, session, race, type, size, at, "Match " + role.id(), role.id());
			tagRole(level, session, role);
		} else {
			SquadType type = role.squadType() == null ? SquadType.INFANTRY : role.squadType();
			spawned = spawnOwnedSquad(
				level,
				session,
				side,
				owner,
				race,
				type,
				size,
				at,
				role == MatchRecruitment.Role.ANTI_ARMOR,
				role == MatchRecruitment.Role.SUPPORT
			);
		}
		if (spawned > 0) {
			session.setPop(side, session.getPop(side) + role.pop() * spawned);
		}
		return spawned;
	}

	public static int reinforceSquad(
		ServerPlayer player,
		MatchSession session,
		NomosikSquad squad,
		MatchRecruitment.Role role,
		int count,
		NomosikRace race
	) {
		if (count <= 0 || squad == null) {
			return 0;
		}
		SquadManager mgr = SquadService.get(player.server);
		ServerLevel level = player.serverLevel();
		Vec3 sum = Vec3.ZERO;
		int living = 0;
		for (NomosikEntity member : mgr.resolveMembersInLevel(level, squad.id())) {
			if (member.isAlive()) {
				sum = sum.add(member.position());
				living++;
			}
		}
		Vec3 at = onSurface(level, living > 0 ? sum.scale(1.0 / living) : player.position());
		SquadType type = role.squadType() == null ? SquadType.INFANTRY : role.squadType();
		EntityType<? extends NomosikEntity> entityType = entityTypeFor(type);
		int spawned = 0;
		for (int i = 0; i < count; i++) {
			double angle = (Math.PI * 2.0 * i) / count;
			double x = at.x + Math.cos(angle) * SQUAD_RING_RADIUS;
			double z = at.z + Math.sin(angle) * SQUAD_RING_RADIUS;
			double y = surfaceY(level, x, z);
			NomosikEntity entity = entityType.create(level);
			if (entity == null) {
				continue;
			}
			entity.moveTo(x, y, z, player.getYRot(), 0.0f);
			entity.finalizeSpawn(level, level.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.COMMAND, null);
			level.addFreshEntity(entity);
			entity.setOwnerUUID(player.getUUID());
			entity.setShouldFollowOwner(false);
			entity.setNomosikRace(race);
			if (role == MatchRecruitment.Role.WORKER) {
				entity.setCombatMode(NomosikCombatMode.PASSIVE);
				entity.setCustomName(net.minecraft.network.chat.Component.translatable("nomosiki.match.role.worker"));
				entity.addTag("zkwars_match_worker");
				session.workerIds.add(entity.getUUID());
			} else {
				entity.setCombatMode(NomosikCombatMode.NEUTRAL);
				if (role == MatchRecruitment.Role.ANTI_ARMOR) {
					entity.addTag("zkwars_match_anti_armor");
				}
				if (role == MatchRecruitment.Role.SUPPORT) {
					entity.addTag("zkwars_match_support");
				}
				if (role == MatchRecruitment.Role.ELITE) {
					entity.addTag("zkwars_match_elite");
					entity.setHealth(Math.min(entity.getMaxHealth() * 2.5f, 140.0f));
				}
			}
			String addErr = mgr.tryAddOrTransferMember(player, squad.id(), entity);
			if (!addErr.isEmpty()) {
				entity.discard();
				break;
			}
			session.track(entity.getUUID());
			spawned++;
		}
		SquadNetworking.sendSquadState(player, mgr);
		SquadNetworking.sendArmyState(player, mgr, true);
		return spawned;
	}

	private static void tagRole(ServerLevel level, MatchSession session, MatchRecruitment.Role role) {
		if (role != MatchRecruitment.Role.ANTI_ARMOR && role != MatchRecruitment.Role.SUPPORT) {
			return;
		}
		for (UUID id : session.trackedEntities) {
			if (!(level.getEntity(id) instanceof NomosikEntity nomosik)) {
				continue;
			}
			if (role == MatchRecruitment.Role.ANTI_ARMOR) {
				nomosik.addTag("zkwars_match_anti_armor");
			}
			if (role == MatchRecruitment.Role.SUPPORT) {
				nomosik.addTag("zkwars_match_support");
			}
		}
	}

	private static EntityType<? extends NomosikEntity> entityTypeFor(SquadType type) {
		return switch (type) {
			case INFANTRY -> ModEntities.NOMOSIK;
			case ARCHER -> ModEntities.ARCHER;
			case KNIGHT -> ModEntities.KNIGHT;
			case NOMAD -> ModEntities.NOMAD;
			case ARTILLERIST -> ModEntities.ARTILLERIST;
			case CAPTAIN -> ModEntities.CAPTAIN;
		};
	}
}
