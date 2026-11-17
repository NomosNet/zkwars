package com.nomosiki.client;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

/**
 * Squad names may be a stored translation key (city garrisons) or a literal the player typed.
 */
public final class SquadLabels {
	private SquadLabels() {
	}

	public static Component component(String stored) {
		if (stored == null || stored.isEmpty()) {
			return Component.empty();
		}
		if (I18n.exists(stored)) {
			return Component.translatable(stored);
		}
		return Component.literal(stored);
	}

	public static String text(String stored) {
		return component(stored).getString();
	}
}
