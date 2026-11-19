package com.nomosiki.combat;

import com.nomosiki.entity.NomosikCombatMode;
import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.entity.NomosikiHorseMount;
import com.nomosiki.faction.FactionDiplomacyService;
import com.nomosiki.faction.FactionRelationStatus;
import com.nomosiki.faction.FactionService;
import com.nomosiki.match.MatchService;
import com.nomosiki.match.entity.BuildingHeartEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Единая точка: можно ли номосику атаковать цель (дипломатия фракций + legacy). */
public final class CombatRelation {
	private CombatRelation() {
	}

	@Nullable
	public static UUID resolveOwnerId(LivingEntity entity) {
		if (entity instanceof Player player) {
			return player.getUUID();
		}
		if (entity instanceof NomosikEntity nomosik) {
			return nomosik.getOwnerUUID();
		}
		if (entity instanceof BuildingHeartEntity heart) {
			return heart.getOwnerUUID();
		}
		return null;
	}

	public static boolean isMatchEnemyTarget(UUID attackerOwnerId, LivingEntity candidate) {
		if (attackerOwnerId == null || candidate == null) {
			return false;
		}
		if (!(candidate.level() instanceof ServerLevel serverLevel)) {
			return false;
		}
		MinecraftServer server = serverLevel.getServer();
		if (server == null) {
			return false;
		}
		MatchService match = MatchService.get(server);
		if (candidate instanceof BuildingHeartEntity heart) {
			byte heartSide = heart.matchSide();
			byte attackerSide = match.session().sideOfOwner(attackerOwnerId);
			return match.isRunning()
				&& attackerSide != com.nomosiki.match.entity.CaptureBeaconEntity.SIDE_NONE
				&& heartSide != attackerSide;
		}
		UUID candidateOwner = resolveOwnerId(candidate);
		return match.areMatchEnemies(attackerOwnerId, candidateOwner);
	}

	public static FactionRelationStatus getRelation(MinecraftServer server, String factionA, String factionB) {
		return FactionService.diplomacy(server).getRelation(factionA, factionB);
	}

	/**
	 * Союзник: одна фракция или stance {@link FactionRelationStatus#ALLY} между разными фракциями.
	 */
	public static boolean isFactionAlly(NomosikEntity nomosik, LivingEntity other) {
		if (nomosik.level().isClientSide()) {
			return false;
		}
		UUID ownerA = nomosik.getOwnerUUID();
		UUID ownerB = resolveOwnerId(other);
		if (ownerA == null || ownerB == null || ownerA.equals(ownerB)) {
			return false;
		}
		FactionDiplomacyService diplomacy = diplomacy(nomosik);
		if (diplomacy == null) {
			return false;
		}
		String factionA = diplomacy.factionOf(ownerA);
		String factionB = diplomacy.factionOf(ownerB);
		if (factionA == null || factionB == null) {
			return false;
		}
		return diplomacy.areAllied(factionA, factionB);
	}

	/** Цель — игрок или номосик вражеской фракции при stance {@link FactionRelationStatus#AT_WAR}. */
	public static boolean isFactionEnemyTarget(UUID attackerOwnerId, LivingEntity candidate) {
		if (attackerOwnerId == null) {
			return false;
		}
		if (!(candidate instanceof Player) && !(candidate instanceof NomosikEntity)) {
			return false;
		}
		UUID candidateOwnerId = resolveOwnerId(candidate);
		if (candidateOwnerId == null || attackerOwnerId.equals(candidateOwnerId)) {
			return false;
		}
		if (!(candidate.level() instanceof ServerLevel serverLevel)) {
			return false;
		}
		MinecraftServer server = serverLevel.getServer();
		if (server == null) {
			return false;
		}
		FactionDiplomacyService diplomacy = FactionService.diplomacy(server);
		String attackerFaction = diplomacy.factionOf(attackerOwnerId);
		String candidateFaction = diplomacy.factionOf(candidateOwnerId);
		if (attackerFaction == null || candidateFaction == null) {
			return false;
		}
		if (attackerFaction.equalsIgnoreCase(candidateFaction)) {
			return false;
		}
		return diplomacy.getRelation(attackerFaction, candidateFaction) == FactionRelationStatus.AT_WAR;
	}

	public static boolean isFactionEnemyTarget(NomosikEntity attacker, LivingEntity candidate) {
		return isFactionEnemyTarget(attacker.getOwnerUUID(), candidate);
	}

	/** @deprecated Use {@link #isFactionEnemyTarget(UUID, LivingEntity)}. */
	@Deprecated
	public static boolean isFactionWarTarget(UUID attackerOwnerId, LivingEntity candidate) {
		return isFactionEnemyTarget(attackerOwnerId, candidate);
	}

	/** @deprecated Use {@link #isFactionEnemyTarget(NomosikEntity, LivingEntity)}. */
	@Deprecated
	public static boolean isFactionWarTarget(NomosikEntity attacker, LivingEntity candidate) {
		return isFactionEnemyTarget(attacker, candidate);
	}

	/** Чужой игрок/номосик с дипломатией {@link FactionRelationStatus#NEUTRAL} (не союзник). */
	public static boolean isFactionNeutralTarget(UUID attackerOwnerId, LivingEntity candidate) {
		if (attackerOwnerId == null) {
			return false;
		}
		if (!(candidate instanceof Player) && !(candidate instanceof NomosikEntity)) {
			return false;
		}
		UUID candidateOwnerId = resolveOwnerId(candidate);
		if (candidateOwnerId == null || attackerOwnerId.equals(candidateOwnerId)) {
			return false;
		}
		if (!(candidate.level() instanceof ServerLevel serverLevel)) {
			return false;
		}
		MinecraftServer server = serverLevel.getServer();
		if (server == null) {
			return false;
		}
		FactionDiplomacyService diplomacy = FactionService.diplomacy(server);
		String attackerFaction = diplomacy.factionOf(attackerOwnerId);
		String candidateFaction = diplomacy.factionOf(candidateOwnerId);
		if (attackerFaction == null || candidateFaction == null) {
			return false;
		}
		if (diplomacy.areAllied(attackerFaction, candidateFaction)) {
			return false;
		}
		return diplomacy.getRelation(attackerFaction, candidateFaction) == FactionRelationStatus.NEUTRAL;
	}

	/** RAID: любая живая цель, кроме союзников и владельца (см. {@link NomosikEntity#isAllyOrSelf}). */
	public static boolean mayRaidAttack(NomosikEntity attacker, LivingEntity target) {
		if (target == null || !target.isAlive() || target.isRemoved() || target == attacker) {
			return false;
		}
		return !attacker.isAllyOrSelf(target);
	}

	public static boolean mayAttack(NomosikEntity attacker, LivingEntity target) {
		if (target == null || !target.isAlive() || target.isRemoved() || target == attacker) {
			return false;
		}
		if (attacker.getCombatMode() == NomosikCombatMode.RAID) {
			return mayRaidAttack(attacker, target);
		}
		if (attacker.isAllyOrSelf(target)) {
			return false;
		}
		UUID ownerId = attacker.getOwnerUUID();
		if (ownerId == null) {
			return false;
		}
		if (isMatchEnemyTarget(ownerId, target)) {
			return true;
		}
		if (isFactionEnemyTarget(ownerId, target)) {
			return true;
		}
		if (attacker.getCombatMode() == NomosikCombatMode.AGGRESSIVE && isFactionNeutralTarget(ownerId, target)) {
			return true;
		}
		return isLegacyAutoTarget(ownerId, target);
	}

	private static boolean isLegacyAutoTarget(UUID ownerId, LivingEntity candidate) {
		if (NomosikiHorseMount.isNomosikiMountEntity(candidate)) {
			return false;
		}
		if (candidate instanceof Enemy) {
			return true;
		}
		return candidate instanceof Mob mob
			&& mob.getTarget() instanceof Player targetPlayer
			&& ownerId.equals(targetPlayer.getUUID());
	}

	@Nullable
	private static FactionDiplomacyService diplomacy(NomosikEntity nomosik) {
		if (!(nomosik.level() instanceof ServerLevel serverLevel)) {
			return null;
		}
		MinecraftServer server = serverLevel.getServer();
		return server == null ? null : FactionService.diplomacy(server);
	}
}
