package com.nomosiki.squad;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Marks squads whose anchor moved via forward/backward shift so {@link SquadBrain}
 * preserves {@link SquadBrain.LayoutOrientationSnap} centroid instead of re-capturing it.
 */
public final class SquadFormationShiftService {
	private static final Set<UUID> PENDING_ORIENTATION_PRESERVE = ConcurrentHashMap.newKeySet();

	private SquadFormationShiftService() {
	}

	/** Call immediately before applying a shifted anchor to squad members. */
	public static void markOrientationPreserve(UUID squadId) {
		PENDING_ORIENTATION_PRESERVE.add(squadId);
	}

	/**
	 * @return {@code true} once per shift; consumed on the next {@link SquadBrain} layout tick.
	 */
	public static boolean consumeOrientationPreserve(UUID squadId) {
		return PENDING_ORIENTATION_PRESERVE.remove(squadId);
	}
}
