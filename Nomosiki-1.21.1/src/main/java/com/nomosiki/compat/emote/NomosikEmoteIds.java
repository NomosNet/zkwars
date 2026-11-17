package com.nomosiki.compat.emote;

import com.nomosiki.NomosikiMod;
import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Stable ids for builtin nomosiki emotes. */
public final class NomosikEmoteIds {
	public static final ResourceLocation WAVE = NomosikiMod.id("emote/wave");
	public static final ResourceLocation BOW = NomosikiMod.id("emote/bow");
	public static final ResourceLocation CHEER = NomosikiMod.id("emote/cheer");
	public static final ResourceLocation CLAP = NomosikiMod.id("emote/clap");
	public static final ResourceLocation POINT = NomosikiMod.id("emote/point");
	public static final ResourceLocation SHRUG = NomosikiMod.id("emote/shrug");

	/** Canonical builtin pack — server allowlist and client catalog source. */
	public static final List<ResourceLocation> BUILTIN = List.of(WAVE, BOW, CHEER, CLAP, POINT, SHRUG);

	public record Resolved(UUID id, String key) {
	}

	private NomosikEmoteIds() {
	}

	public static UUID uuidOf(ResourceLocation key) {
		return UUID.nameUUIDFromBytes(("nomosiki-emote:" + key).getBytes(StandardCharsets.UTF_8));
	}

	public static String keyString(ResourceLocation key) {
		return key.toString();
	}

	/**
	 * Resolves a client-requested emote to a canonical builtin entry.
	 * Prefers {@code emoteKey}, then {@code emoteId}. Unknown requests are rejected.
	 */
	public static Optional<Resolved> resolveAllowed(UUID emoteId, String emoteKey) {
		if (emoteKey != null && !emoteKey.isEmpty()) {
			for (ResourceLocation key : BUILTIN) {
				if (keyString(key).equals(emoteKey)) {
					return Optional.of(new Resolved(uuidOf(key), keyString(key)));
				}
			}
		}
		if (emoteId != null) {
			for (ResourceLocation key : BUILTIN) {
				if (uuidOf(key).equals(emoteId)) {
					return Optional.of(new Resolved(uuidOf(key), keyString(key)));
				}
			}
		}
		return Optional.empty();
	}
}
