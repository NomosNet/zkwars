package com.nomosiki.compat.emote;

import com.nomosiki.config.NomosikiServerConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Gate for server emote toggle and client UI readiness.
 * Playback is self-contained (builtin JSON + model applier).
 */
public final class NomosikEmoteCompat {
	private NomosikEmoteCompat() {
	}

	/** True when server config allows squad emotes. */
	public static boolean isServerEnabled() {
		return NomosikiServerConfig.INSTANCE.emotesEnabled;
	}

	/** Client UI / applier ready — builtin pack always works on client. */
	public static boolean isClientRuntimeReady() {
		return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
	}
}
