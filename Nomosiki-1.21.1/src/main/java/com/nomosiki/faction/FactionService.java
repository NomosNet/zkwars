package com.nomosiki.faction;

import net.minecraft.server.MinecraftServer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class FactionService {
	// ConcurrentHashMap keyed by server identity (MinecraftServer uses default Object identity
	// hashing) so lookups are safe even if called off the main thread under the Async mod.
	private static final Map<MinecraftServer, FactionManager> BY_SERVER = new ConcurrentHashMap<>();
	private static final Map<MinecraftServer, FactionDiplomacyService> DIPLOMACY_BY_SERVER = new ConcurrentHashMap<>();
	private static final Map<MinecraftServer, FactionWarService> WAR_BY_SERVER = new ConcurrentHashMap<>();

	private FactionService() {
	}

	public static FactionManager get(MinecraftServer server) {
		return BY_SERVER.computeIfAbsent(server, FactionManager::new);
	}

	public static FactionDiplomacyService diplomacy(MinecraftServer server) {
		return DIPLOMACY_BY_SERVER.computeIfAbsent(server, s -> {
			FactionManager manager = get(s);
			return new FactionDiplomacyService(s, manager, manager.diplomacyData());
		});
	}

	/** @deprecated Prefer {@link #diplomacy(MinecraftServer)}; kept for existing call sites. */
	@Deprecated
	public static FactionWarService war(MinecraftServer server) {
		return WAR_BY_SERVER.computeIfAbsent(server, s -> new FactionWarService(diplomacy(s)));
	}
}
