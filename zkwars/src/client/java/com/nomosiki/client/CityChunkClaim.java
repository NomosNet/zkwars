package com.nomosiki.client;

import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Talks to nomos-city without a compile dependency. */
final class CityChunkClaim {
	private static final String HOOKS = "com.zkshogun.nomoscity.client.ChunkClaimHooks";
	private static Method claim;
	private static Method choices;
	private static Method clear;
	private static boolean resolved;

	private CityChunkClaim() {
	}

	static boolean available() {
		resolve();
		return claim != null && choices != null;
	}

	static void claim(String name) {
		resolve();
		if (claim == null) {
			return;
		}
		try {
			claim.invoke(null, name == null ? "" : name);
		} catch (ReflectiveOperationException ignored) {
		}
	}

	static List<String> choices() {
		resolve();
		if (choices == null) {
			return List.of();
		}
		try {
			Object value = choices.invoke(null);
			if (value instanceof List<?> list) {
				List<String> names = new ArrayList<>();
				for (Object item : list) {
					if (item instanceof String name && !name.isEmpty()) {
						names.add(name);
					}
				}
				return names;
			}
		} catch (ReflectiveOperationException ignored) {
		}
		return List.of();
	}

	static void clear() {
		resolve();
		if (clear == null) {
			return;
		}
		try {
			clear.invoke(null);
		} catch (ReflectiveOperationException ignored) {
		}
	}

	private static void resolve() {
		if (resolved) {
			return;
		}
		resolved = true;
		if (!FabricLoader.getInstance().isModLoaded("nomos-city")) {
			return;
		}
		try {
			Class<?> type = Class.forName(HOOKS);
			claim = type.getMethod("claim", String.class);
			choices = type.getMethod("choices");
			clear = type.getMethod("clear");
		} catch (ReflectiveOperationException ignored) {
			claim = null;
			choices = null;
			clear = null;
		}
	}
}
