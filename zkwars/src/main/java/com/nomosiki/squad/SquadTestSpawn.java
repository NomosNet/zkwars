package com.nomosiki.squad;

import com.nomosiki.entity.NomosikCombatMode;
import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.faction.FactionColor;
import com.nomosiki.faction.FactionService;
import com.nomosiki.faction.SquadBannerStance;
import com.nomosiki.network.squad.SquadNetworking;
import com.nomosiki.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class SquadTestSpawn {
	public static final int COLOR_PARADE_DEFAULT_MEMBERS = 4;
	public static final int COLOR_PARADE_MAX_MEMBERS = 8;
	private static final int COLOR_GRID = 4;
	private static final int ALL_TYPES_COLS = 5;
	private static final double COLOR_GRID_START = 6.0;
	private static final double COLOR_GRID_SPACING = 8.0;
	private static final double ALL_TYPES_SPACING = 10.0;
	private static final SquadBannerStance[] ALL_TYPES_STANCES = {
		SquadBannerStance.OWN,
		SquadBannerStance.FACTION,
		SquadBannerStance.ALLY,
		SquadBannerStance.MILITARY_ALLY,
		SquadBannerStance.WAR,
		SquadBannerStance.NEUTRAL,
		SquadBannerStance.FACTION,
		SquadBannerStance.ALLY,
		SquadBannerStance.MILITARY_ALLY,
		SquadBannerStance.WAR
	};

	private SquadTestSpawn() {
	}

	public static int spawnTestSquad(ServerPlayer player, SquadType type, int count, @Nullable String rawName) {
		return spawnTestSquad(player, type, count, rawName, 0.0, 0.0, null, true, false);
	}

	public static int spawnGearedSquad(ServerPlayer player, SquadType type, int count, @Nullable String rawName) {
		FactionColor color = FactionService.get(player.server).getFactionColorByOwner(player.getUUID());
		return spawnTestSquad(player, type, count, rawName, 0.0, 0.0, color, true, false);
	}

	public static int spawnTestSquad(
		ServerPlayer player,
		SquadType type,
		int count,
		@Nullable String rawName,
		double offsetX,
		double offsetZ
	) {
		return spawnTestSquad(player, type, count, rawName, offsetX, offsetZ, null, true, false);
	}

	public static int spawnAshigaruColorParade(ServerPlayer player, int membersPerSquad) {
		return spawnColorParade(player, membersPerSquad, SquadType.INFANTRY);
	}

	public static int spawnColorParade(ServerPlayer player, int membersPerSquad, SquadType type) {
		if (!type.isPlayable()) {
			return -1;
		}
		int members = Math.max(1, Math.min(membersPerSquad, COLOR_PARADE_MAX_MEMBERS));
		Vec3 look = player.getLookAngle();
		Vec3 forward = new Vec3(look.x, 0.0, look.z);
		if (forward.lengthSqr() < 1.0e-6) {
			forward = new Vec3(0.0, 0.0, 1.0);
		} else {
			forward = forward.normalize();
		}
		Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
		List<FactionColor> colors = FactionColor.ashigaruColors();
		int total = 0;
		for (int i = 0; i < colors.size(); i++) {
			FactionColor color = colors.get(i);
			int col = i % COLOR_GRID;
			int row = i / COLOR_GRID;
			Vec3 center = forward.scale(COLOR_GRID_START + row * COLOR_GRID_SPACING)
				.add(right.scale((col - (COLOR_GRID - 1) * 0.5) * COLOR_GRID_SPACING));
			int spawned = spawnTestSquad(
				player,
				type,
				members,
				color.id(),
				center.x,
				center.z,
				color,
				false,
				false
			);
			if (spawned < 0) {
				break;
			}
			total += spawned;
		}
		SquadManager mgr = SquadService.get(player.server);
		SquadNetworking.sendSquadState(player, mgr);
		SquadNetworking.sendArmyState(player, mgr, true);
		return total;
	}

	/**
	 * Spawns every playable branch in a grid. The first squad is yours ({@code OWN});
	 * the rest use dummy owners so tactical banners show all six diplomacy colors.
	 */
	public static int spawnAllTypesParade(ServerPlayer player, int membersPerSquad) {
		int members = Math.max(1, Math.min(membersPerSquad, COLOR_PARADE_MAX_MEMBERS));
		Vec3 look = player.getLookAngle();
		Vec3 forward = new Vec3(look.x, 0.0, look.z);
		if (forward.lengthSqr() < 1.0e-6) {
			forward = new Vec3(0.0, 0.0, 1.0);
		} else {
			forward = forward.normalize();
		}
		Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
		List<SquadType> types = SquadType.playableValues();
		Map<UUID, Byte> extras = new HashMap<>();
		int total = 0;
		for (int i = 0; i < types.size(); i++) {
			SquadType type = types.get(i);
			SquadBannerStance stance = ALL_TYPES_STANCES[Math.min(i, ALL_TYPES_STANCES.length - 1)];
			int col = i % ALL_TYPES_COLS;
			int row = i / ALL_TYPES_COLS;
			Vec3 center = forward.scale(COLOR_GRID_START + row * ALL_TYPES_SPACING)
				.add(right.scale((col - (ALL_TYPES_COLS - 1) * 0.5) * ALL_TYPES_SPACING));
			int spawned;
			if (stance == SquadBannerStance.OWN) {
				spawned = spawnTestSquad(
					player,
					type,
					members,
					stance.name(),
					center.x,
					center.z,
					null,
					false,
					false
				);
				if (spawned > 0) {
					setActiveSquadPassive(player);
				}
			} else {
				UUID owner = dummyOwner(stance);
				extras.put(owner, stance.id());
				spawned = spawnDummyOwnedSquad(player, type, members, center.x, center.z, owner);
			}
			if (spawned < 0) {
				break;
			}
			total += spawned;
		}
		FactionService.get(player.server).putTesterStances(player.getUUID(), extras);
		FactionService.get(player.server).sendRenderSyncTo(player);
		SquadManager mgr = SquadService.get(player.server);
		SquadNetworking.sendSquadState(player, mgr);
		SquadNetworking.sendArmyState(player, mgr, true);
		return total;
	}

	public static int spawnTestSquad(
		ServerPlayer player,
		SquadType type,
		int count,
		@Nullable String rawName,
		double offsetX,
		double offsetZ,
		@Nullable FactionColor color,
		boolean sync,
		boolean samuraiGear
	) {
		if (!type.isPlayable()) {
			return -1;
		}
		int members = Math.max(1, Math.min(count, NomosikSquad.MAX_MEMBERS));
		ServerLevel level = player.serverLevel();
		SquadManager mgr = SquadService.get(player.server);

		String squadName = rawName == null ? "" : rawName.trim();
		Optional<String> createErr = mgr.createNamedSquad(player, squadName, (byte) 0, type);
		if (createErr.isPresent()) {
			return -1;
		}
		UUID squadId = mgr.getActiveSquadId(player.getUUID()).orElse(null);
		if (squadId == null) {
			return -1;
		}

		EntityType<? extends NomosikEntity> entityType = entityTypeFor(type);
		int spawned = 0;
		double baseX = player.getX() + offsetX;
		double baseZ = player.getZ() + offsetZ;
		for (int i = 0; i < members; i++) {
			double angle = (Math.PI * 2.0 * i) / members;
			double ring = 2.0 + (i / 8) * 1.5;
			double x = baseX + Math.cos(angle) * ring;
			double z = baseZ + Math.sin(angle) * ring;
			NomosikEntity entity = entityType.create(level);
			if (entity == null) {
				continue;
			}
			entity.moveTo(x, player.getY(), z, player.getYRot(), 0.0f);
			entity.finalizeSpawn(level, level.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.COMMAND, null);
			level.addFreshEntity(entity);
			entity.setOwnerUUID(player.getUUID());
			entity.setShouldFollowOwner(false);
			if (color != null) {
				entity.setFactionColorOverride(color);
			}
			String addErr = mgr.tryAddOrTransferMember(player, squadId, entity);
			if (!addErr.isEmpty()) {
				entity.discard();
				break;
			}
			spawned++;
		}

		if (sync) {
			SquadNetworking.sendSquadState(player, mgr);
			SquadNetworking.sendArmyState(player, mgr, true);
		}
		return spawned;
	}

	private static int spawnDummyOwnedSquad(
		ServerPlayer player,
		SquadType type,
		int members,
		double offsetX,
		double offsetZ,
		UUID ownerId
	) {
		if (!type.isPlayable()) {
			return -1;
		}
		ServerLevel level = player.serverLevel();
		EntityType<? extends NomosikEntity> entityType = entityTypeFor(type);
		UUID squadId = UUID.randomUUID();
		int spawned = 0;
		double baseX = player.getX() + offsetX;
		double baseZ = player.getZ() + offsetZ;
		for (int i = 0; i < members; i++) {
			double angle = (Math.PI * 2.0 * i) / members;
			double ring = 2.0 + (i / 8) * 1.5;
			double x = baseX + Math.cos(angle) * ring;
			double z = baseZ + Math.sin(angle) * ring;
			NomosikEntity entity = entityType.create(level);
			if (entity == null) {
				continue;
			}
			entity.moveTo(x, player.getY(), z, player.getYRot(), 0.0f);
			entity.finalizeSpawn(level, level.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.COMMAND, null);
			level.addFreshEntity(entity);
			entity.setOwnerUUID(ownerId);
			entity.setShouldFollowOwner(false);
			entity.setCombatMode(NomosikCombatMode.PASSIVE);
			entity.joinSquadServer(squadId);
			spawned++;
		}
		return spawned;
	}

	private static void setActiveSquadPassive(ServerPlayer player) {
		SquadManager mgr = SquadService.get(player.server);
		UUID squadId = mgr.getActiveSquadId(player.getUUID()).orElse(null);
		if (squadId == null) {
			return;
		}
		mgr.data().getSquad(squadId).ifPresent(squad -> {
			for (UUID memberId : squad.memberEntityIdsView()) {
				NomosikEntity nomosik = mgr.findNomosikByUuidAnyLevel(memberId);
				if (nomosik != null) {
					nomosik.setCombatMode(NomosikCombatMode.PASSIVE);
				}
			}
		});
	}

	private static UUID dummyOwner(SquadBannerStance stance) {
		return UUID.nameUUIDFromBytes(("nomosiki-banner-dummy-" + stance.name()).getBytes(StandardCharsets.UTF_8));
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
