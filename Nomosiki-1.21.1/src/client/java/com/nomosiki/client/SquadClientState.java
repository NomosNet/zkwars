package com.nomosiki.client;

import com.nomosiki.network.squad.payload.s2c.SquadStatePayload;
import com.nomosiki.squad.SquadType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class SquadClientState {
	private static volatile Optional<UUID> activeSquadId = Optional.empty();
	private static volatile List<SquadStatePayload.Brief> squads = List.of();

	private SquadClientState() {
	}

	public static void apply(SquadStatePayload payload) {
		activeSquadId = payload.activeSquadId();
		squads = List.copyOf(payload.squads());
	}

	public static Optional<UUID> activeSquadId() {
		return activeSquadId;
	}

	public static List<SquadStatePayload.Brief> squads() {
		return squads;
	}

	public static int memberCountForActiveSquad() {
		return activeSquadId.flatMap(SquadClientState::memberCountFor).orElse(0);
	}

	public static Optional<Integer> memberCountFor(UUID squadId) {
		return squads.stream()
			.filter(b -> b.squadId().equals(squadId))
			.findFirst()
			.map(SquadStatePayload.Brief::memberCount);
	}

	public static Optional<SquadType> squadTypeForActiveSquad() {
		return activeSquadId.flatMap(SquadClientState::squadTypeFor);
	}

	public static Optional<SquadType> squadTypeFor(UUID squadId) {
		return squads.stream()
			.filter(b -> b.squadId().equals(squadId))
			.findFirst()
			.map(b -> SquadType.fromId(b.typeId()));
	}

	/** Display name when the server sent a non-empty one; empty otherwise (caller may fall back to type / UUID). */
	public static Optional<String> squadNameFor(UUID squadId) {
		return squads.stream()
			.filter(b -> b.squadId().equals(squadId))
			.findFirst()
			.map(SquadStatePayload.Brief::name)
			.filter(s -> s != null && !s.isEmpty());
	}

	/** Persisted icon index, or {@code 0} when the squad is unknown. */
	public static byte iconIdFor(UUID squadId) {
		return squads.stream()
			.filter(b -> b.squadId().equals(squadId))
			.findFirst()
			.map(SquadStatePayload.Brief::iconId)
			.orElse((byte) 0);
	}
}
