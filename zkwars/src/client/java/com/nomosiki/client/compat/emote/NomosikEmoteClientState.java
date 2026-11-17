package com.nomosiki.client.compat.emote;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NomosikEmoteClientState {
	public static final class Playing {
		private final NomosikEmoteDefinition definition;
		private final float startAge;
		private final int startTickOffset;

		Playing(NomosikEmoteDefinition definition, float startAge, int startTickOffset) {
			this.definition = definition;
			this.startAge = startAge;
			this.startTickOffset = startTickOffset;
		}

		public NomosikEmoteDefinition definition() {
			return definition;
		}

		public float tickAt(float ageInTicks) {
			return startTickOffset + Math.max(0f, ageInTicks - startAge);
		}
	}

	private static final Map<UUID, Playing> PLAYING = new ConcurrentHashMap<>();

	private NomosikEmoteClientState() {
	}

	public static void start(UUID entityUuid, NomosikEmoteDefinition definition, int startTick, float ageInTicks) {
		PLAYING.put(entityUuid, new Playing(definition, ageInTicks, Math.max(0, startTick)));
	}

	public static void stop(UUID entityUuid) {
		PLAYING.remove(entityUuid);
	}

	public static Playing get(UUID entityUuid) {
		return PLAYING.get(entityUuid);
	}

	public static void clearAll() {
		PLAYING.clear();
	}

	public static void purgeMissing(java.util.function.Predicate<UUID> stillPresent) {
		Iterator<Map.Entry<UUID, Playing>> it = PLAYING.entrySet().iterator();
		while (it.hasNext()) {
			if (!stillPresent.test(it.next().getKey())) {
				it.remove();
			}
		}
	}
}
