package com.nomosiki.faction;

import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * @deprecated Thin delegate over {@link FactionDiplomacyService}; use {@link FactionService#diplomacy}.
 */
@Deprecated
public final class FactionWarService {
	private final FactionDiplomacyService diplomacy;

	FactionWarService(FactionDiplomacyService diplomacy) {
		this.diplomacy = diplomacy;
	}

	public boolean areAtWar(String factionA, String factionB) {
		return this.diplomacy.areAtWar(factionA, factionB);
	}

	public boolean areAtWar(UUID playerA, UUID playerB) {
		return this.diplomacy.areAtWar(playerA, playerB);
	}

	public String factionOf(UUID playerId) {
		return this.diplomacy.factionOf(playerId);
	}

	public boolean isFactionLeader(ServerPlayer player) {
		return this.diplomacy.isFactionLeader(player);
	}

	public String declareWar(ServerPlayer leader, String enemyFactionName) {
		return this.diplomacy.declareWar(leader, enemyFactionName);
	}

	public String declarePeace(ServerPlayer leader, String enemyFactionName) {
		return this.diplomacy.declarePeace(leader, enemyFactionName);
	}
}
