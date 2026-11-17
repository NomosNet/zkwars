package com.nomosiki.squad;

import net.minecraft.server.MinecraftServer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SquadService {
	// ConcurrentHashMap keyed by server identity (MinecraftServer uses default Object identity
	// hashing) so lookups are safe even if called off the main thread under the Async mod.
	private static final Map<MinecraftServer, SquadManager> BY_SERVER = new ConcurrentHashMap<>();

	private SquadService() {
	}

	public static SquadManager get(MinecraftServer server) {
		return BY_SERVER.computeIfAbsent(server, SquadManager::new);
	}
}
