package com.nomosiki;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Helpers for code that must run on the dedicated server thread (entity sync, animate packets,
 * {@code startUsingItem}, etc.) when Async or other mods tick mobs on worker threads.
 */
public final class NomosikMainThread {
	private NomosikMainThread() {
	}

	public static boolean isServerMainThread(Level level) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return true;
		}
		MinecraftServer server = serverLevel.getServer();
		return server == null || server.isSameThread();
	}

	public static void run(ServerLevel level, Runnable action) {
		MinecraftServer server = level.getServer();
		if (server != null && !server.isSameThread()) {
			server.execute(action);
			return;
		}
		action.run();
	}
}
