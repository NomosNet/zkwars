package com.nomosiki.client.match;

import org.jetbrains.annotations.Nullable;

public final class MatchClientState {
	private static boolean buildPlaceMode;
	@Nullable
	private static String pendingBuildingId;

	private MatchClientState() {
	}

	public static void beginBuildPlace(String buildingId) {
		buildPlaceMode = true;
		pendingBuildingId = buildingId;
	}

	public static void clearBuildPlace() {
		buildPlaceMode = false;
		pendingBuildingId = null;
	}

	public static boolean buildPlaceMode() {
		return buildPlaceMode;
	}

	@Nullable
	public static String pendingBuildingId() {
		return pendingBuildingId;
	}
}
