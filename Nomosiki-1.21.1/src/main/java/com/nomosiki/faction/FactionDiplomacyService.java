package com.nomosiki.faction;

import com.nomosiki.advancement.NomosikiActions;
import com.nomosiki.advancement.NomosikiAdvancements;
import com.nomosiki.combat.CombatRelation;
import com.nomosiki.discord.DiplomacyDiscordEvent;
import com.nomosiki.discord.DiscordDiplomacyNotifier;
import com.nomosiki.discord.DiscordPoliticsBoard;
import com.nomosiki.entity.NomosikEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Tri-state faction diplomacy: relations, alliance requests, war/peace. */
public final class FactionDiplomacyService {
	private static final long COOLDOWN_MS = 60_000L;

	private final MinecraftServer server;
	private final FactionManager factions;
	private final FactionDiplomacySavedData data;
	private final Map<FactionWarPair, Long> cooldownUntilMs = new HashMap<>();

	public FactionDiplomacyService(MinecraftServer server, FactionManager factions, FactionDiplomacySavedData data) {
		this.server = server;
		this.factions = factions;
		this.data = data;
	}

	public FactionRelationStatus getRelation(String factionA, String factionB) {
		return this.data.getRelation(factionA, factionB);
	}

	public boolean areAtWar(String factionA, String factionB) {
		return getRelation(factionA, factionB) == FactionRelationStatus.AT_WAR;
	}

	public boolean areAllied(String factionA, String factionB) {
		if (factionA == null || factionB == null || factionA.isBlank() || factionB.isBlank()) {
			return false;
		}
		if (factionA.equalsIgnoreCase(factionB)) {
			return true;
		}
		return getRelation(factionA, factionB).isAlliance();
	}

	public boolean areRegularAllied(String factionA, String factionB) {
		if (factionA == null || factionB == null || factionA.isBlank() || factionB.isBlank()) {
			return false;
		}
		if (factionA.equalsIgnoreCase(factionB)) {
			return true;
		}
		return getRelation(factionA, factionB) == FactionRelationStatus.ALLY;
	}

	public boolean areMilitaryAllied(String factionA, String factionB) {
		if (factionA == null || factionB == null || factionA.isBlank() || factionB.isBlank()) {
			return false;
		}
		if (factionA.equalsIgnoreCase(factionB)) {
			return true;
		}
		return getRelation(factionA, factionB) == FactionRelationStatus.MILITARY_ALLY;
	}

	public boolean areAtWar(UUID playerA, UUID playerB) {
		String factionA = factionOf(playerA);
		String factionB = factionOf(playerB);
		if (factionA == null || factionB == null) {
			return false;
		}
		return areAtWar(factionA, factionB);
	}

	@Nullable
	public String factionOf(UUID playerId) {
		return this.factions.getPlayerFaction(playerId);
	}

	public boolean isFactionLeader(ServerPlayer player) {
		String factionName = factionOf(player.getUUID());
		if (factionName == null) {
			return false;
		}
		return this.factions.getFaction(factionName)
			.map(faction -> player.getUUID().equals(faction.owner()))
			.orElse(false);
	}

	public boolean hasIncomingAllianceRequest(String myFaction, String otherFaction) {
		return this.data.hasIncomingAllianceRequest(myFaction, otherFaction);
	}

	public boolean hasOutgoingAllianceRequest(String myFaction, String otherFaction) {
		return this.data.hasOutgoingAllianceRequest(myFaction, otherFaction);
	}

	@Nullable
	public FactionAlliancePending getPendingAlliance(String myFaction, String otherFaction) {
		return this.data.getPendingAlliance(myFaction, otherFaction);
	}

	/** @return empty string on success, otherwise a translation message key */
	public String declareWar(ServerPlayer leader, String enemyFactionName) {
		ResolvedPair pair = resolveLeaderPair(leader, enemyFactionName);
		if (pair.errorKey() != null) {
			return pair.errorKey();
		}
		if (isOnCooldown(pair.myFaction(), pair.otherFaction())) {
			return "message.nomosiki.faction.war_cooldown";
		}
		return declareWarBetween(pair.myFaction(), pair.otherFaction(), leaderName(leader), leader);
	}

	/** @return empty string on success, otherwise a translation message key */
	public String adminDeclareWar(String factionA, String factionB, String actorName) {
		ResolvedPair pair = resolveNamedPair(factionA, factionB);
		if (pair.errorKey() != null) {
			return pair.errorKey();
		}
		return declareWarBetween(pair.myFaction(), pair.otherFaction(), actorName, null);
	}

	private String declareWarBetween(String myFaction, String enemyFaction, String actorName, @Nullable ServerPlayer credit) {
		if (areAtWar(myFaction, enemyFaction)) {
			return "message.nomosiki.faction.war_already";
		}
		if (areMilitaryAllied(myFaction, enemyFaction)) {
			return "message.nomosiki.faction.war_military_allied";
		}
		if (areRegularAllied(myFaction, enemyFaction)) {
			return "message.nomosiki.faction.war_allied";
		}

		Set<String> aggressorBlock = buildMilitaryBlock(myFaction);
		Set<String> defenderBlock = buildMilitaryBlock(enemyFaction);
		Set<String> conflicted = new HashSet<>(aggressorBlock);
		conflicted.retainAll(defenderBlock);
		aggressorBlock.removeAll(conflicted);
		defenderBlock.removeAll(conflicted);

		Set<String> affectedFactions = new LinkedHashSet<>();
		affectedFactions.add(myFaction);
		affectedFactions.add(enemyFaction);

		List<String> cascadePairs = new ArrayList<>();
		this.data.clearPendingAlliance(myFaction, enemyFaction);
		for (String aggressor : aggressorBlock) {
			for (String defender : defenderBlock) {
				if (aggressor.equalsIgnoreCase(defender)) {
					continue;
				}
				applyWarBetween(aggressor, defender);
				affectedFactions.add(aggressor);
				affectedFactions.add(defender);
				boolean primary = aggressor.equalsIgnoreCase(myFaction) && defender.equalsIgnoreCase(enemyFaction);
				if (!primary) {
					cascadePairs.add(aggressor + "↔" + defender);
				}
			}
		}
		recordCooldown(myFaction, enemyFaction);

		long now = System.currentTimeMillis();
		for (String conflictedFaction : conflicted) {
			this.data.addWarSideChoicePending(new FactionWarSideChoicePending(
				myFaction, enemyFaction, conflictedFaction, now));
			notifyFactionOwner(conflictedFaction,
				Component.translatable("message.nomosiki.faction.war_side_choice", myFaction, enemyFaction));
			affectedFactions.add(conflictedFaction);
		}

		Set<String> defenderRegularAllies = new HashSet<>(this.data.regularAlliesOf(enemyFaction));
		defenderRegularAllies.removeAll(defenderBlock);
		defenderRegularAllies.removeAll(conflicted);
		for (String ally : defenderRegularAllies) {
			this.data.addWarJoinPending(new FactionWarJoinPending(myFaction, enemyFaction, ally, now));
			notifyFactionOwner(ally,
				Component.translatable("message.nomosiki.faction.war_join_offer", myFaction, enemyFaction));
			affectedFactions.add(ally);
		}

		refreshDiplomacyScreens(affectedFactions);
		DiscordDiplomacyNotifier.get().notify(DiplomacyDiscordEvent.war(
			myFaction, enemyFaction, actorName, cascadePairs));
		if (credit != null) {
			NomosikiAdvancements.grant(credit, NomosikiActions.WAR);
			if (!cascadePairs.isEmpty()) {
				NomosikiAdvancements.grant(credit, NomosikiActions.CASCADE);
			}
		}
		return "";
	}

	/**
	 * Puts every pair of named factions that are not military allies at war, including each side's military allies.
	 * @return empty string on success, otherwise a translation message key
	 */
	public String openWarsForWartime(List<String> namedFactions) {
		if (namedFactions == null || namedFactions.size() < 2) {
			return "message.nomosiki.wartime.need_two";
		}
		boolean anyWar = false;
		for (int i = 0; i < namedFactions.size(); i++) {
			for (int j = i + 1; j < namedFactions.size(); j++) {
				if (!areMilitaryAllied(namedFactions.get(i), namedFactions.get(j))) {
					anyWar = true;
					break;
				}
			}
			if (anyWar) {
				break;
			}
		}
		if (!anyWar) {
			return "message.nomosiki.wartime.one_bloc";
		}
		Set<String> affected = new LinkedHashSet<>();
		for (int i = 0; i < namedFactions.size(); i++) {
			for (int j = i + 1; j < namedFactions.size(); j++) {
				String left = namedFactions.get(i);
				String right = namedFactions.get(j);
				if (areMilitaryAllied(left, right)) {
					continue;
				}
				cascadeWar(left, right, affected);
			}
		}
		refreshDiplomacyScreens(affected);
		return "";
	}

	/** @return empty string when the peace offer is sent, otherwise a translation message key */
	public String declarePeace(ServerPlayer leader, String enemyFactionName) {
		ResolvedPair pair = resolveLeaderPair(leader, enemyFactionName);
		if (pair.errorKey() != null) {
			return pair.errorKey();
		}
		String myFaction = pair.myFaction();
		String enemyFaction = pair.otherFaction();
		if (!areAtWar(myFaction, enemyFaction)) {
			return "message.nomosiki.faction.peace_not_at_war";
		}
		if (this.data.hasOutgoingPeace(myFaction, enemyFaction)) {
			return "message.nomosiki.faction.peace_pending_outgoing";
		}
		if (this.data.hasIncomingPeace(myFaction, enemyFaction)) {
			return "message.nomosiki.faction.peace_pending_incoming";
		}
		if (isOnCooldown(myFaction, enemyFaction)) {
			return "message.nomosiki.faction.war_cooldown";
		}
		this.data.setPendingPeace(myFaction, enemyFaction, new FactionPeacePending(myFaction, System.currentTimeMillis()));
		recordCooldown(myFaction, enemyFaction);
		notifyFactionOwner(enemyFaction,
			Component.translatable("message.nomosiki.faction.peace_offer_received", myFaction));
		refreshDiplomacyScreens(myFaction, enemyFaction);
		DiscordDiplomacyNotifier.get().notify(DiplomacyDiscordEvent.of(
			DiplomacyDiscordEvent.Type.PEACE_OFFERED, myFaction, enemyFaction, leaderName(leader)));
		NomosikiAdvancements.grant(leader, NomosikiActions.PEACE);
		return "";
	}

	/** @return empty string when peace is concluded, otherwise a translation message key */
	public String acceptPeace(ServerPlayer leader, String requesterFactionName) {
		ResolvedPair pair = resolveLeaderPair(leader, requesterFactionName);
		if (pair.errorKey() != null) {
			return pair.errorKey();
		}
		String myFaction = pair.myFaction();
		String requesterFaction = pair.otherFaction();
		if (!this.data.hasIncomingPeace(myFaction, requesterFaction)) {
			return "message.nomosiki.faction.peace_no_pending";
		}
		if (!areAtWar(myFaction, requesterFaction)) {
			this.data.clearPendingPeace(myFaction, requesterFaction);
			return "message.nomosiki.faction.peace_not_at_war";
		}
		this.data.clearPendingPeace(myFaction, requesterFaction);
		clearFactionEnemyTargets(myFaction, requesterFaction);
		this.data.setRelation(myFaction, requesterFaction, FactionRelationStatus.NEUTRAL);
		recordCooldown(myFaction, requesterFaction);
		broadcastToFactions(myFaction, requesterFaction,
			Component.translatable("message.nomosiki.faction.peace_declared", myFaction, requesterFaction));
		refreshDiplomacyScreens(myFaction, requesterFaction);
		DiscordDiplomacyNotifier.get().notify(DiplomacyDiscordEvent.of(
			DiplomacyDiscordEvent.Type.PEACE_DECLARED, myFaction, requesterFaction, leaderName(leader)));
		NomosikiAdvancements.grant(leader, NomosikiActions.PEACE);
		return "";
	}

	/** @return empty string on success, otherwise a translation message key */
	public String declinePeace(ServerPlayer leader, String requesterFactionName) {
		ResolvedPair pair = resolveLeaderPair(leader, requesterFactionName);
		if (pair.errorKey() != null) {
			return pair.errorKey();
		}
		String myFaction = pair.myFaction();
		String requesterFaction = pair.otherFaction();
		if (!this.data.hasIncomingPeace(myFaction, requesterFaction)) {
			return "message.nomosiki.faction.peace_no_pending";
		}
		this.data.clearPendingPeace(myFaction, requesterFaction);
		recordCooldown(myFaction, requesterFaction);
		notifyFactionOwner(requesterFaction,
			Component.translatable("message.nomosiki.faction.peace_declined", myFaction, requesterFaction));
		refreshDiplomacyScreens(myFaction, requesterFaction);
		DiscordDiplomacyNotifier.get().notify(DiplomacyDiscordEvent.of(
			DiplomacyDiscordEvent.Type.PEACE_DECLINED, myFaction, requesterFaction, leaderName(leader)));
		return "";
	}

	/** @return empty string on success, otherwise a translation message key */
	public String cancelPeace(ServerPlayer leader, String targetFactionName) {
		ResolvedPair pair = resolveLeaderPair(leader, targetFactionName);
		if (pair.errorKey() != null) {
			return pair.errorKey();
		}
		String myFaction = pair.myFaction();
		String targetFaction = pair.otherFaction();
		if (!this.data.hasOutgoingPeace(myFaction, targetFaction)) {
			return "message.nomosiki.faction.peace_no_pending";
		}
		this.data.clearPendingPeace(myFaction, targetFaction);
		refreshDiplomacyScreens(myFaction, targetFaction);
		return "";
	}

	/** @return empty string on success, otherwise a translation message key */
	public String requestAlliance(ServerPlayer leader, String targetFactionName, FactionAllianceTier tier) {
		ResolvedPair pair = resolveLeaderPair(leader, targetFactionName);
		if (pair.errorKey() != null) {
			return pair.errorKey();
		}
		String myFaction = pair.myFaction();
		String targetFaction = pair.otherFaction();
		if (areAllied(myFaction, targetFaction)) {
			return "message.nomosiki.faction.alliance_already";
		}
		if (areAtWar(myFaction, targetFaction)) {
			return "message.nomosiki.faction.alliance_at_war";
		}
		if (hasOutgoingAllianceRequest(myFaction, targetFaction)) {
			return "message.nomosiki.faction.alliance_pending_outgoing";
		}
		if (hasIncomingAllianceRequest(myFaction, targetFaction)) {
			return "message.nomosiki.faction.alliance_pending_incoming";
		}
		if (isOnCooldown(myFaction, targetFaction)) {
			return "message.nomosiki.faction.war_cooldown";
		}
		this.data.setPendingAlliance(myFaction, targetFaction,
			new FactionAlliancePending(myFaction, System.currentTimeMillis(), tier));
		Component message = tier == FactionAllianceTier.MILITARY
			? Component.translatable("message.nomosiki.faction.military_alliance_requested", myFaction, targetFaction)
			: Component.translatable("message.nomosiki.faction.alliance_requested", myFaction, targetFaction);
		notifyFactionOwner(targetFaction, message);
		leader.displayClientMessage(
			Component.translatable(tier == FactionAllianceTier.MILITARY
				? "message.nomosiki.faction.military_alliance_request_sent"
				: "message.nomosiki.faction.alliance_request_sent", targetFaction),
			true);
		refreshDiplomacyScreens(myFaction, targetFaction);
		DiscordDiplomacyNotifier.get().notify(DiplomacyDiscordEvent.alliance(
			DiplomacyDiscordEvent.Type.ALLIANCE_REQUESTED, myFaction, targetFaction, leaderName(leader),
			tier == FactionAllianceTier.MILITARY));
		return "";
	}

	/** @return empty string on success, otherwise a translation message key */
	public String requestAllianceUpgrade(ServerPlayer leader, String allyFactionName, FactionAllianceTier targetTier) {
		ResolvedPair pair = resolveLeaderPair(leader, allyFactionName);
		if (pair.errorKey() != null) {
			return pair.errorKey();
		}
		String myFaction = pair.myFaction();
		String allyFaction = pair.otherFaction();
		FactionRelationStatus current = getRelation(myFaction, allyFaction);
		if (targetTier == FactionAllianceTier.MILITARY) {
			if (current == FactionRelationStatus.MILITARY_ALLY) {
				return "message.nomosiki.faction.military_alliance_already";
			}
			if (current != FactionRelationStatus.ALLY) {
				return "message.nomosiki.faction.alliance_not_allied";
			}
		} else {
			if (current == FactionRelationStatus.ALLY) {
				return "message.nomosiki.faction.alliance_already_regular";
			}
			if (current != FactionRelationStatus.MILITARY_ALLY) {
				return "message.nomosiki.faction.military_alliance_not_allied";
			}
		}
		if (hasOutgoingAllianceRequest(myFaction, allyFaction) || hasIncomingAllianceRequest(myFaction, allyFaction)) {
			return "message.nomosiki.faction.alliance_pending_outgoing";
		}
		if (isOnCooldown(myFaction, allyFaction)) {
			return "message.nomosiki.faction.war_cooldown";
		}
		this.data.setPendingAlliance(myFaction, allyFaction,
			new FactionAlliancePending(myFaction, System.currentTimeMillis(), targetTier));
		Component message = targetTier == FactionAllianceTier.MILITARY
			? Component.translatable("message.nomosiki.faction.military_alliance_upgrade_requested", myFaction, allyFaction)
			: Component.translatable("message.nomosiki.faction.alliance_downgrade_requested", myFaction, allyFaction);
		notifyFactionOwner(allyFaction, message);
		refreshDiplomacyScreens(myFaction, allyFaction);
		return "";
	}

	/** @return empty string on success, otherwise a translation message key */
	public String acceptAlliance(ServerPlayer leader, String requesterFactionName) {
		ResolvedPair pair = resolveLeaderPair(leader, requesterFactionName);
		if (pair.errorKey() != null) {
			return pair.errorKey();
		}
		String myFaction = pair.myFaction();
		String requesterFaction = pair.otherFaction();
		FactionAlliancePending pending = this.data.getPendingAlliance(myFaction, requesterFaction);
		if (pending == null || !pending.requesterFaction().equalsIgnoreCase(requesterFaction)) {
			return "message.nomosiki.faction.alliance_no_pending";
		}
		this.data.clearPendingAlliance(myFaction, requesterFaction);
		FactionRelationStatus newStatus = pending.tier().toRelationStatus();
		this.data.setRelation(myFaction, requesterFaction, newStatus);
		recordCooldown(myFaction, requesterFaction);
		Component message = newStatus == FactionRelationStatus.MILITARY_ALLY
			? Component.translatable("message.nomosiki.faction.military_alliance_formed", myFaction, requesterFaction)
			: Component.translatable("message.nomosiki.faction.alliance_formed", myFaction, requesterFaction);
		broadcastToFactions(myFaction, requesterFaction, message);
		refreshDiplomacyScreens(myFaction, requesterFaction);
		DiscordDiplomacyNotifier.get().notify(DiplomacyDiscordEvent.alliance(
			DiplomacyDiscordEvent.Type.ALLIANCE_FORMED, myFaction, requesterFaction, leaderName(leader),
			newStatus == FactionRelationStatus.MILITARY_ALLY));
		NomosikiAdvancements.grant(leader, NomosikiActions.ALLIANCE);
		return "";
	}

	/** @return empty string on success, otherwise a translation message key */
	public String declineAlliance(ServerPlayer leader, String requesterFactionName) {
		ResolvedPair pair = resolveLeaderPair(leader, requesterFactionName);
		if (pair.errorKey() != null) {
			return pair.errorKey();
		}
		String myFaction = pair.myFaction();
		String requesterFaction = pair.otherFaction();
		if (!hasIncomingAllianceRequest(myFaction, requesterFaction)) {
			return "message.nomosiki.faction.alliance_no_pending";
		}
		this.data.clearPendingAlliance(myFaction, requesterFaction);
		recordCooldown(myFaction, requesterFaction);
		notifyFactionOwner(requesterFaction,
			Component.translatable("message.nomosiki.faction.alliance_declined", myFaction, requesterFaction));
		refreshDiplomacyScreens(myFaction, requesterFaction);
		DiscordDiplomacyNotifier.get().notify(DiplomacyDiscordEvent.of(
			DiplomacyDiscordEvent.Type.ALLIANCE_DECLINED, myFaction, requesterFaction, leaderName(leader)));
		return "";
	}

	/** @return empty string on success, otherwise a translation message key */
	public String cancelAllianceRequest(ServerPlayer leader, String targetFactionName) {
		ResolvedPair pair = resolveLeaderPair(leader, targetFactionName);
		if (pair.errorKey() != null) {
			return pair.errorKey();
		}
		String myFaction = pair.myFaction();
		String targetFaction = pair.otherFaction();
		if (!hasOutgoingAllianceRequest(myFaction, targetFaction)) {
			return "message.nomosiki.faction.alliance_no_pending";
		}
		this.data.clearPendingAlliance(myFaction, targetFaction);
		refreshDiplomacyScreens(myFaction, targetFaction);
		return "";
	}

	/** @return empty string on success, otherwise a translation message key */
	public String breakAlliance(ServerPlayer leader, String allyFactionName) {
		ResolvedPair pair = resolveLeaderPair(leader, allyFactionName);
		if (pair.errorKey() != null) {
			return pair.errorKey();
		}
		String myFaction = pair.myFaction();
		String allyFaction = pair.otherFaction();
		if (!areAllied(myFaction, allyFaction)) {
			return "message.nomosiki.faction.alliance_not_allied";
		}
		if (isOnCooldown(myFaction, allyFaction)) {
			return "message.nomosiki.faction.war_cooldown";
		}
		boolean wasMilitary = areMilitaryAllied(myFaction, allyFaction);
		this.data.setRelation(myFaction, allyFaction, FactionRelationStatus.NEUTRAL);
		recordCooldown(myFaction, allyFaction);
		Component message = wasMilitary
			? Component.translatable("message.nomosiki.faction.military_alliance_broken", myFaction, allyFaction)
			: Component.translatable("message.nomosiki.faction.alliance_broken", myFaction, allyFaction);
		broadcastToFactions(myFaction, allyFaction, message);
		refreshDiplomacyScreens(myFaction, allyFaction);
		DiscordDiplomacyNotifier.get().notify(DiplomacyDiscordEvent.alliance(
			DiplomacyDiscordEvent.Type.ALLIANCE_BROKEN, myFaction, allyFaction, leaderName(leader), wasMilitary));
		return "";
	}

	/** @return empty string on success, otherwise a translation message key */
	public String acceptWarJoin(ServerPlayer leader, String aggressorFactionName) {
		ResolvedPair pair = resolveLeaderPair(leader, aggressorFactionName);
		if (pair.errorKey() != null) {
			return pair.errorKey();
		}
		String myFaction = pair.myFaction();
		String aggressorFaction = pair.otherFaction();
		FactionWarJoinPending pending = this.data.findWarJoinPending(myFaction, aggressorFaction);
		if (pending == null) {
			return "message.nomosiki.faction.war_join_no_pending";
		}
		this.data.clearWarJoinPending(myFaction, aggressorFaction);
		this.data.setRelation(myFaction, aggressorFaction, FactionRelationStatus.AT_WAR);
		recordCooldown(myFaction, aggressorFaction);
		broadcastToFactions(myFaction, aggressorFaction,
			Component.translatable("message.nomosiki.faction.war_join_accepted", myFaction, aggressorFaction));
		refreshDiplomacyScreens(Set.of(myFaction, aggressorFaction, pending.defenderFaction()));
		return "";
	}

	/** @return empty string on success, otherwise a translation message key */
	public String declineWarJoin(ServerPlayer leader, String aggressorFactionName) {
		ResolvedPair pair = resolveLeaderPair(leader, aggressorFactionName);
		if (pair.errorKey() != null) {
			return pair.errorKey();
		}
		String myFaction = pair.myFaction();
		String aggressorFaction = pair.otherFaction();
		if (this.data.findWarJoinPending(myFaction, aggressorFaction) == null) {
			return "message.nomosiki.faction.war_join_no_pending";
		}
		this.data.clearWarJoinPending(myFaction, aggressorFaction);
		refreshDiplomacyScreens(myFaction, aggressorFaction);
		return "";
	}

	/** @return empty string on success, otherwise a translation message key */
	public String resolveWarSideChoice(ServerPlayer leader, String aggressorFactionName, byte choice) {
		ResolvedPair pair = resolveLeaderPair(leader, aggressorFactionName);
		if (pair.errorKey() != null) {
			return pair.errorKey();
		}
		String myFaction = pair.myFaction();
		String aggressorFaction = pair.otherFaction();
		FactionWarSideChoicePending pending = this.data.findWarSideChoicePending(myFaction, aggressorFaction);
		if (pending == null) {
			return "message.nomosiki.faction.war_side_no_pending";
		}
		String defenderFaction = pending.defenderFaction();
		this.data.clearWarSideChoicePending(myFaction, aggressorFaction);
		Set<String> affected = new LinkedHashSet<>(List.of(myFaction, aggressorFaction, defenderFaction));
		switch (choice) {
			case FactionWarSideChoice.SUPPORT_AGGRESSOR -> {
				this.data.setRelation(myFaction, defenderFaction, FactionRelationStatus.AT_WAR);
				recordCooldown(myFaction, defenderFaction);
				clearFactionEnemyTargets(myFaction, defenderFaction);
				broadcastToFactions(myFaction, defenderFaction,
					Component.translatable("message.nomosiki.faction.war_side_support", myFaction, aggressorFaction));
			}
			case FactionWarSideChoice.SUPPORT_DEFENDER -> {
				this.data.setRelation(myFaction, aggressorFaction, FactionRelationStatus.AT_WAR);
				recordCooldown(myFaction, aggressorFaction);
				clearFactionEnemyTargets(myFaction, aggressorFaction);
				broadcastToFactions(myFaction, aggressorFaction,
					Component.translatable("message.nomosiki.faction.war_side_support", myFaction, defenderFaction));
			}
			case FactionWarSideChoice.STAY_NEUTRAL -> {
				notifyFactionOwner(myFaction,
					Component.translatable("message.nomosiki.faction.war_side_stay_neutral", aggressorFaction, defenderFaction));
			}
			default -> {
				return "message.nomosiki.faction.war_side_no_pending";
			}
		}
		refreshDiplomacyScreens(affected);
		return "";
	}

	/** Immediate peace. Skips the offer and the cooldown. */
	public String adminForcePeace(String factionA, String factionB, String actorName) {
		ResolvedPair pair = resolveNamedPair(factionA, factionB);
		if (pair.errorKey() != null) {
			return pair.errorKey();
		}
		String left = pair.myFaction();
		String right = pair.otherFaction();
		if (!areAtWar(left, right)) {
			return "message.nomosiki.faction.peace_not_at_war";
		}
		this.data.clearPendingPeace(left, right);
		clearFactionEnemyTargets(left, right);
		this.data.setRelation(left, right, FactionRelationStatus.NEUTRAL);
		broadcastToFactions(left, right,
			Component.translatable("message.nomosiki.faction.peace_declared", left, right));
		refreshDiplomacyScreens(left, right);
		DiscordDiplomacyNotifier.get().notify(DiplomacyDiscordEvent.of(
			DiplomacyDiscordEvent.Type.PEACE_DECLARED, left, right, actorName));
		return "";
	}

	/** Forms or upgrades an alliance immediately. Skips the request and the cooldown. */
	public String adminForceAlliance(String factionA, String factionB, FactionAllianceTier tier, String actorName) {
		ResolvedPair pair = resolveNamedPair(factionA, factionB);
		if (pair.errorKey() != null) {
			return pair.errorKey();
		}
		String left = pair.myFaction();
		String right = pair.otherFaction();
		if (areAtWar(left, right)) {
			return "message.nomosiki.faction.alliance_at_war";
		}
		FactionRelationStatus desired = tier == null ? FactionRelationStatus.ALLY : tier.toRelationStatus();
		if (getRelation(left, right) == desired) {
			return desired == FactionRelationStatus.MILITARY_ALLY
				? "message.nomosiki.faction.military_alliance_already"
				: "message.nomosiki.faction.alliance_already";
		}
		this.data.clearPendingAlliance(left, right);
		this.data.setRelation(left, right, desired);
		Component message = desired == FactionRelationStatus.MILITARY_ALLY
			? Component.translatable("message.nomosiki.faction.military_alliance_formed", left, right)
			: Component.translatable("message.nomosiki.faction.alliance_formed", left, right);
		broadcastToFactions(left, right, message);
		refreshDiplomacyScreens(left, right);
		DiscordDiplomacyNotifier.get().notify(DiplomacyDiscordEvent.alliance(
			DiplomacyDiscordEvent.Type.ALLIANCE_FORMED, left, right, actorName,
			desired == FactionRelationStatus.MILITARY_ALLY));
		return "";
	}

	/** Breaks an alliance immediately. Skips the cooldown. */
	public String adminBreakAlliance(String factionA, String factionB, String actorName) {
		ResolvedPair pair = resolveNamedPair(factionA, factionB);
		if (pair.errorKey() != null) {
			return pair.errorKey();
		}
		String left = pair.myFaction();
		String right = pair.otherFaction();
		if (!areAllied(left, right)) {
			return "message.nomosiki.faction.alliance_not_allied";
		}
		boolean wasMilitary = areMilitaryAllied(left, right);
		this.data.setRelation(left, right, FactionRelationStatus.NEUTRAL);
		Component message = wasMilitary
			? Component.translatable("message.nomosiki.faction.military_alliance_broken", left, right)
			: Component.translatable("message.nomosiki.faction.alliance_broken", left, right);
		broadcastToFactions(left, right, message);
		refreshDiplomacyScreens(left, right);
		DiscordDiplomacyNotifier.get().notify(DiplomacyDiscordEvent.alliance(
			DiplomacyDiscordEvent.Type.ALLIANCE_BROKEN, left, right, actorName, wasMilitary));
		return "";
	}

	public String adminAcceptPeace(String acceptingFaction, String requesterFaction, String actorName) {
		ResolvedPair pair = resolveNamedPair(acceptingFaction, requesterFaction);
		if (pair.errorKey() != null) {
			return pair.errorKey();
		}
		String mine = pair.myFaction();
		String requester = pair.otherFaction();
		if (!this.data.hasIncomingPeace(mine, requester)) {
			return "message.nomosiki.faction.peace_no_pending";
		}
		if (!areAtWar(mine, requester)) {
			this.data.clearPendingPeace(mine, requester);
			return "message.nomosiki.faction.peace_not_at_war";
		}
		this.data.clearPendingPeace(mine, requester);
		clearFactionEnemyTargets(mine, requester);
		this.data.setRelation(mine, requester, FactionRelationStatus.NEUTRAL);
		broadcastToFactions(mine, requester,
			Component.translatable("message.nomosiki.faction.peace_declared", mine, requester));
		refreshDiplomacyScreens(mine, requester);
		DiscordDiplomacyNotifier.get().notify(DiplomacyDiscordEvent.of(
			DiplomacyDiscordEvent.Type.PEACE_DECLARED, mine, requester, actorName));
		return "";
	}

	public String adminDeclinePeace(String decliningFaction, String requesterFaction, String actorName) {
		ResolvedPair pair = resolveNamedPair(decliningFaction, requesterFaction);
		if (pair.errorKey() != null) {
			return pair.errorKey();
		}
		String mine = pair.myFaction();
		String requester = pair.otherFaction();
		if (!this.data.hasIncomingPeace(mine, requester)) {
			return "message.nomosiki.faction.peace_no_pending";
		}
		this.data.clearPendingPeace(mine, requester);
		notifyFactionOwner(requester,
			Component.translatable("message.nomosiki.faction.peace_declined", mine, requester));
		refreshDiplomacyScreens(mine, requester);
		DiscordDiplomacyNotifier.get().notify(DiplomacyDiscordEvent.of(
			DiplomacyDiscordEvent.Type.PEACE_DECLINED, mine, requester, actorName));
		return "";
	}

	public String adminAcceptAlliance(String acceptingFaction, String requesterFaction, String actorName) {
		ResolvedPair pair = resolveNamedPair(acceptingFaction, requesterFaction);
		if (pair.errorKey() != null) {
			return pair.errorKey();
		}
		String mine = pair.myFaction();
		String requester = pair.otherFaction();
		FactionAlliancePending pending = this.data.getPendingAlliance(mine, requester);
		if (pending == null || !pending.requesterFaction().equalsIgnoreCase(requester)) {
			return "message.nomosiki.faction.alliance_no_pending";
		}
		this.data.clearPendingAlliance(mine, requester);
		FactionRelationStatus newStatus = pending.tier().toRelationStatus();
		this.data.setRelation(mine, requester, newStatus);
		Component message = newStatus == FactionRelationStatus.MILITARY_ALLY
			? Component.translatable("message.nomosiki.faction.military_alliance_formed", mine, requester)
			: Component.translatable("message.nomosiki.faction.alliance_formed", mine, requester);
		broadcastToFactions(mine, requester, message);
		refreshDiplomacyScreens(mine, requester);
		DiscordDiplomacyNotifier.get().notify(DiplomacyDiscordEvent.alliance(
			DiplomacyDiscordEvent.Type.ALLIANCE_FORMED, mine, requester, actorName,
			newStatus == FactionRelationStatus.MILITARY_ALLY));
		return "";
	}

	public String adminDeclineAlliance(String decliningFaction, String requesterFaction, String actorName) {
		ResolvedPair pair = resolveNamedPair(decliningFaction, requesterFaction);
		if (pair.errorKey() != null) {
			return pair.errorKey();
		}
		String mine = pair.myFaction();
		String requester = pair.otherFaction();
		if (!hasIncomingAllianceRequest(mine, requester)) {
			return "message.nomosiki.faction.alliance_no_pending";
		}
		this.data.clearPendingAlliance(mine, requester);
		notifyFactionOwner(requester,
			Component.translatable("message.nomosiki.faction.alliance_declined", mine, requester));
		refreshDiplomacyScreens(mine, requester);
		DiscordDiplomacyNotifier.get().notify(DiplomacyDiscordEvent.of(
			DiplomacyDiscordEvent.Type.ALLIANCE_DECLINED, mine, requester, actorName));
		return "";
	}

	public String adminAcceptWarJoin(String allyFaction, String aggressorFaction, String actorName) {
		ResolvedPair pair = resolveNamedPair(allyFaction, aggressorFaction);
		if (pair.errorKey() != null) {
			return pair.errorKey();
		}
		String ally = pair.myFaction();
		String aggressor = pair.otherFaction();
		FactionWarJoinPending pending = this.data.findWarJoinPending(ally, aggressor);
		if (pending == null) {
			return "message.nomosiki.faction.war_join_no_pending";
		}
		this.data.clearWarJoinPending(ally, aggressor);
		this.data.setRelation(ally, aggressor, FactionRelationStatus.AT_WAR);
		broadcastToFactions(ally, aggressor,
			Component.translatable("message.nomosiki.faction.war_join_accepted", ally, aggressor));
		refreshDiplomacyScreens(Set.of(ally, aggressor, pending.defenderFaction()));
		return "";
	}

	public String adminDeclineWarJoin(String allyFaction, String aggressorFaction, String actorName) {
		ResolvedPair pair = resolveNamedPair(allyFaction, aggressorFaction);
		if (pair.errorKey() != null) {
			return pair.errorKey();
		}
		String ally = pair.myFaction();
		String aggressor = pair.otherFaction();
		if (this.data.findWarJoinPending(ally, aggressor) == null) {
			return "message.nomosiki.faction.war_join_no_pending";
		}
		this.data.clearWarJoinPending(ally, aggressor);
		refreshDiplomacyScreens(ally, aggressor);
		return "";
	}

	public String adminResolveSide(String conflictedFaction, String aggressorFaction, byte choice, String actorName) {
		ResolvedPair pair = resolveNamedPair(conflictedFaction, aggressorFaction);
		if (pair.errorKey() != null) {
			return pair.errorKey();
		}
		String mine = pair.myFaction();
		String aggressor = pair.otherFaction();
		FactionWarSideChoicePending pending = this.data.findWarSideChoicePending(mine, aggressor);
		if (pending == null) {
			return "message.nomosiki.faction.war_side_no_pending";
		}
		String defender = pending.defenderFaction();
		this.data.clearWarSideChoicePending(mine, aggressor);
		Set<String> affected = new LinkedHashSet<>(List.of(mine, aggressor, defender));
		switch (choice) {
			case FactionWarSideChoice.SUPPORT_AGGRESSOR -> {
				this.data.setRelation(mine, defender, FactionRelationStatus.AT_WAR);
				clearFactionEnemyTargets(mine, defender);
				broadcastToFactions(mine, defender,
					Component.translatable("message.nomosiki.faction.war_side_support", mine, aggressor));
			}
			case FactionWarSideChoice.SUPPORT_DEFENDER -> {
				this.data.setRelation(mine, aggressor, FactionRelationStatus.AT_WAR);
				clearFactionEnemyTargets(mine, aggressor);
				broadcastToFactions(mine, aggressor,
					Component.translatable("message.nomosiki.faction.war_side_support", mine, defender));
			}
			case FactionWarSideChoice.STAY_NEUTRAL -> notifyFactionOwner(mine,
				Component.translatable("message.nomosiki.faction.admin.side_neutral", actorName, aggressor, defender));
			default -> {
				return "message.nomosiki.faction.war_side_no_pending";
			}
		}
		refreshDiplomacyScreens(affected);
		return "";
	}

	public void clearFactionEnemyTargets(String factionA, String factionB) {
		for (ServerLevel level : this.server.getAllLevels()) {
			for (Entity entity : level.getAllEntities()) {
				if (!(entity instanceof NomosikEntity nomosik)) {
					continue;
				}
				LivingEntity target = nomosik.getTarget();
				if (target == null) {
					continue;
				}
				UUID ownerId = nomosik.getOwnerUUID();
				if (ownerId == null) {
					continue;
				}
				String ownerFaction = factionOf(ownerId);
				UUID targetOwnerId = CombatRelation.resolveOwnerId(target);
				if (ownerFaction == null || targetOwnerId == null) {
					continue;
				}
				String targetFaction = factionOf(targetOwnerId);
				if (targetFaction == null) {
					continue;
				}
				boolean ownerInPair = ownerFaction.equalsIgnoreCase(factionA) || ownerFaction.equalsIgnoreCase(factionB);
				boolean targetInPair = targetFaction.equalsIgnoreCase(factionA) || targetFaction.equalsIgnoreCase(factionB);
				if (ownerInPair && targetInPair && !ownerFaction.equalsIgnoreCase(targetFaction)) {
					nomosik.setTarget(null);
				}
			}
		}
	}

	private Set<String> buildMilitaryBlock(String faction) {
		Set<String> block = new LinkedHashSet<>();
		block.add(faction);
		block.addAll(this.data.militaryAlliesOf(faction));
		return block;
	}

	private void cascadeWar(String myFaction, String enemyFaction, Set<String> affectedFactions) {
		Set<String> aggressorBlock = buildMilitaryBlock(myFaction);
		Set<String> defenderBlock = buildMilitaryBlock(enemyFaction);
		Set<String> conflicted = new HashSet<>(aggressorBlock);
		conflicted.retainAll(defenderBlock);
		aggressorBlock.removeAll(conflicted);
		defenderBlock.removeAll(conflicted);
		affectedFactions.add(myFaction);
		affectedFactions.add(enemyFaction);
		this.data.clearPendingAlliance(myFaction, enemyFaction);
		for (String aggressor : aggressorBlock) {
			for (String defender : defenderBlock) {
				if (aggressor.equalsIgnoreCase(defender)) {
					continue;
				}
				applyWarBetween(aggressor, defender);
				affectedFactions.add(aggressor);
				affectedFactions.add(defender);
			}
		}
		long now = System.currentTimeMillis();
		for (String conflictedFaction : conflicted) {
			this.data.addWarSideChoicePending(new FactionWarSideChoicePending(
				myFaction, enemyFaction, conflictedFaction, now));
			notifyFactionOwner(conflictedFaction,
				Component.translatable("message.nomosiki.faction.war_side_choice", myFaction, enemyFaction));
			affectedFactions.add(conflictedFaction);
		}
		Set<String> defenderRegularAllies = new HashSet<>(this.data.regularAlliesOf(enemyFaction));
		defenderRegularAllies.removeAll(defenderBlock);
		defenderRegularAllies.removeAll(conflicted);
		for (String ally : defenderRegularAllies) {
			this.data.addWarJoinPending(new FactionWarJoinPending(myFaction, enemyFaction, ally, now));
			notifyFactionOwner(ally,
				Component.translatable("message.nomosiki.faction.war_join_offer", myFaction, enemyFaction));
			affectedFactions.add(ally);
		}
	}

	private void applyWarBetween(String factionA, String factionB) {
		if (areAtWar(factionA, factionB)) {
			return;
		}
		this.data.setRelation(factionA, factionB, FactionRelationStatus.AT_WAR);
		clearFactionEnemyTargets(factionA, factionB);
		broadcastToFactions(factionA, factionB,
			Component.translatable("message.nomosiki.faction.war_declared", factionA, factionB));
	}

	private record ResolvedPair(@Nullable String errorKey, @Nullable String myFaction, @Nullable String otherFaction) {
	}

	private ResolvedPair resolveNamedPair(String rawA, String rawB) {
		String factionA = this.factions.resolveFactionName(rawA == null ? "" : rawA.trim());
		String factionB = this.factions.resolveFactionName(rawB == null ? "" : rawB.trim());
		if (factionA == null || factionB == null) {
			return new ResolvedPair("message.nomosiki.faction.war_enemy_not_found", null, null);
		}
		if (factionA.equalsIgnoreCase(factionB)) {
			return new ResolvedPair("message.nomosiki.faction.war_self", null, null);
		}
		return new ResolvedPair(null, factionA, factionB);
	}

	private ResolvedPair resolveLeaderPair(ServerPlayer leader, String otherFactionName) {
		String myFaction = factionOf(leader.getUUID());
		if (myFaction == null) {
			return new ResolvedPair("message.nomosiki.faction.no_faction", null, null);
		}
		if (!isFactionLeader(leader)) {
			return new ResolvedPair("message.nomosiki.faction.war_not_owner", null, null);
		}
		String otherFaction = this.factions.resolveFactionName(otherFactionName);
		if (otherFaction == null) {
			return new ResolvedPair("message.nomosiki.faction.war_enemy_not_found", null, null);
		}
		if (otherFaction.equalsIgnoreCase(myFaction)) {
			return new ResolvedPair("message.nomosiki.faction.war_self", null, null);
		}
		return new ResolvedPair(null, myFaction, otherFaction);
	}

	private boolean isOnCooldown(String factionA, String factionB) {
		Long until = this.cooldownUntilMs.get(FactionWarPair.of(factionA, factionB));
		return until != null && until > System.currentTimeMillis();
	}

	private void recordCooldown(String factionA, String factionB) {
		this.cooldownUntilMs.put(FactionWarPair.of(factionA, factionB), System.currentTimeMillis() + COOLDOWN_MS);
	}

	private void notifyFactionOwner(String factionName, Component message) {
		this.factions.getFaction(factionName).ifPresent(faction -> {
			ServerPlayer owner = this.server.getPlayerList().getPlayer(faction.owner());
			if (owner != null) {
				owner.displayClientMessage(message, false);
			}
		});
	}

	private static String leaderName(ServerPlayer leader) {
		return leader.getGameProfile().getName();
	}

	private void broadcastToFactions(String factionA, String factionB, Component message) {
		Set<UUID> notified = new HashSet<>();
		for (String factionName : List.of(factionA, factionB)) {
			this.factions.getFaction(factionName).ifPresent(faction -> {
				for (UUID memberId : faction.members()) {
					if (!notified.add(memberId)) {
						continue;
					}
					ServerPlayer online = this.server.getPlayerList().getPlayer(memberId);
					if (online != null) {
						online.displayClientMessage(message, false);
					}
				}
			});
		}
	}

	private void refreshDiplomacyScreens(String factionA, String factionB) {
		refreshDiplomacyScreens(Set.of(factionA, factionB));
	}

	private void refreshDiplomacyScreens(Set<String> factionNames) {
		for (String factionName : factionNames) {
			this.factions.sendScreenDataToFactionMembers(factionName);
		}
		this.factions.sendRenderSyncToAll();
		DiscordPoliticsBoard.refresh(this.server);
	}
}
