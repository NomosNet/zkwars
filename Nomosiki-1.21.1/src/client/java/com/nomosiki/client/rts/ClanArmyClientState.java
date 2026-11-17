package com.nomosiki.client.rts;

import com.nomosiki.network.squad.payload.s2c.ClanArmyRosterPayload;
import com.nomosiki.network.squad.payload.s2c.ClanArmyStatePayload;
import com.nomosiki.network.squad.payload.s2c.SquadArmyStatePayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Faction roster and a watched clan member's army. Separate from the viewer's own orders. */
public final class ClanArmyClientState {
	public static final int ROSTER_H = 14;

	private static boolean admin;
	private static boolean canTransfer;
	private static List<ClanArmyRosterPayload.Member> members = List.of();
	private static Optional<UUID> watchedId = Optional.empty();
	private static String watchedName = "";
	private static List<SquadArmyStatePayload.SquadRow> watchedSquads = List.of();

	private ClanArmyClientState() {
	}

	public static void applyRoster(ClanArmyRosterPayload payload) {
		admin = payload.admin();
		canTransfer = payload.canTransfer();
		members = List.copyOf(payload.members());
		if (watchedId.isPresent() && members.stream().noneMatch(member -> member.id().equals(watchedId.get()))) {
			clearWatch();
		}
	}

	public static void applyWatched(ClanArmyStatePayload payload) {
		watchedId = Optional.of(payload.commanderId());
		watchedName = payload.commanderName();
		watchedSquads = List.copyOf(payload.army().squads());
	}

	public static void clearWatch() {
		watchedId = Optional.empty();
		watchedName = "";
		watchedSquads = List.of();
	}

	public static void clear() {
		admin = false;
		canTransfer = false;
		members = List.of();
		clearWatch();
	}

	public static boolean admin() {
		return admin;
	}

	public static boolean canTransfer() {
		return canTransfer;
	}

	public static boolean showsRoster() {
		return admin && !members.isEmpty();
	}

	public static int rosterHeight() {
		return showsRoster() ? ROSTER_H : 0;
	}

	public static List<ClanArmyRosterPayload.Member> members() {
		return members;
	}

	public static boolean watching() {
		return watchedId.isPresent();
	}

	public static Optional<UUID> watchedId() {
		return watchedId;
	}

	public static String watchedName() {
		return watchedName;
	}

	public static List<SquadArmyStatePayload.SquadRow> watchedSquads() {
		return watchedSquads;
	}

	public static List<ClanArmyRosterPayload.Member> onlineExcept(UUID commanderId) {
		List<ClanArmyRosterPayload.Member> result = new ArrayList<>();
		for (ClanArmyRosterPayload.Member member : members) {
			if (member.online() && !member.id().equals(commanderId)) {
				result.add(member);
			}
		}
		return result;
	}
}
