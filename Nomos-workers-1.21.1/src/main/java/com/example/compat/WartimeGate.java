package com.example.compat;

import com.example.ZKWorkers;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.lang.reflect.Method;

/** Cached Nomosiki wartime flag. Reflection runs once per server tick, not per worker. */
public final class WartimeGate {
	private static final String API = "com.nomosiki.api.NomosikiFactionApi";

	private static boolean resolved;
	private static Method wartimeActive;
	private static boolean active;

	private WartimeGate() {
	}

	public static void tick(MinecraftServer server) {
		boolean now = query(server);
		WorkerWartimeSavedData data = data(server);
		long gameTime = server.overworld().getGameTime();
		if (now && !data.open()) {
			data.begin(gameTime);
		} else if (!now && data.open()) {
			data.finish(gameTime);
		}
		active = now;
	}

	public static boolean isActive() {
		return active;
	}

	public static long skippedTicks(ServerLevel level, long from, long to) {
		MinecraftServer server = level.getServer();
		if (server == null) {
			return 0L;
		}
		return data(server).skipped(from, to);
	}

	private static WorkerWartimeSavedData data(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(WorkerWartimeSavedData.factory(), WorkerWartimeSavedData.DATA_ID);
	}

	private static boolean query(MinecraftServer server) {
		resolve();
		if (wartimeActive == null) {
			return false;
		}
		try {
			return Boolean.TRUE.equals(wartimeActive.invoke(null, server));
		} catch (ReflectiveOperationException exception) {
			ZKWorkers.LOGGER.warn("Nomosiki wartime check failed", exception);
			return false;
		}
	}

	private static void resolve() {
		if (resolved) {
			return;
		}
		resolved = true;
		if (!FabricLoader.getInstance().isModLoaded("nomosiki")) {
			return;
		}
		try {
			Class<?> api = Class.forName(API);
			wartimeActive = api.getMethod("isWartimeActive", MinecraftServer.class);
		} catch (ReflectiveOperationException exception) {
			ZKWorkers.LOGGER.warn("Nomosiki wartime API is missing; workers stay on the day schedule");
			wartimeActive = null;
		}
	}
}
