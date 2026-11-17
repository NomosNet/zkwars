package com.nomosiki.client.compat.emote;

import com.nomosiki.compat.emote.NomosikEmoteIds;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class NomosikEmoteCatalog {
	public record Entry(UUID id, ResourceLocation key, String name, NomosikEmoteDefinition definition) {
	}

	private static final Map<UUID, Entry> BY_ID = new LinkedHashMap<>();
	private static final Map<String, Entry> BY_KEY = new LinkedHashMap<>();
	private static boolean loaded;

	private NomosikEmoteCatalog() {
	}

	public static synchronized void ensureLoaded() {
		if (loaded) {
			return;
		}
		loaded = true;
		for (ResourceLocation key : NomosikEmoteIds.BUILTIN) {
			registerBuiltin(key);
		}
	}

	private static void registerBuiltin(ResourceLocation key) {
		NomosikEmoteDefinition def = NomosikEmoteJson.loadBuiltin(key);
		Entry entry = new Entry(def.id(), key, def.name(), def);
		BY_ID.put(entry.id(), entry);
		BY_KEY.put(key.toString(), entry);
	}

	public static List<Entry> wheelEntries() {
		ensureLoaded();
		List<Entry> playable = new ArrayList<>();
		for (Entry e : BY_ID.values()) {
			if (e.definition() != null && !e.definition().keyframes().isEmpty()) {
				playable.add(e);
			}
		}
		return playable;
	}

	public static Optional<Entry> find(UUID id, String emoteKey) {
		ensureLoaded();
		if (emoteKey != null && !emoteKey.isEmpty()) {
			Entry byKey = BY_KEY.get(emoteKey);
			if (byKey != null && byKey.definition() != null) {
				return Optional.of(byKey);
			}
		}
		Entry byId = BY_ID.get(id);
		if (byId != null && byId.definition() != null) {
			return Optional.of(byId);
		}
		return Optional.empty();
	}
}
