package com.nomosiki.discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nomosiki.config.NomosikiServerConfig;
import com.nomosiki.faction.FactionRelationStatus;
import com.nomosiki.faction.FactionService;
import com.nomosiki.faction.FactionWarPair;
import com.nomosiki.faction.WartimeService;
import net.minecraft.server.MinecraftServer;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** One editable Discord message with the current wars, alliances, and wartime. */
public final class DiscordPoliticsBoard {
	private static final DateTimeFormatter TIME_FMT =
		DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(ZoneId.systemDefault());
	private static final int COLOR_WARTIME = 0xE67E22;
	private static final Object LOCK = new Object();
	private static boolean inFlight;
	private static boolean dirty;
	private static MinecraftServer pendingServer;

	private DiscordPoliticsBoard() {
	}

	public static void refresh(MinecraftServer server) {
		if (server == null) {
			return;
		}
		synchronized (LOCK) {
			pendingServer = server;
			if (inFlight) {
				dirty = true;
				return;
			}
			inFlight = true;
		}
		dispatch(server);
	}

	private static void dispatch(MinecraftServer server) {
		JsonObject embed = build(server);
		String existingId = WartimeService.get(server).statusMessageId();
		DiscordDiplomacyNotifier.get().upsertStatus(embed, existingId, newId -> server.execute(() -> {
			if (newId != null && !newId.isBlank()) {
				WartimeService.get(server).setStatusMessageId(newId);
			}
			MinecraftServer again = null;
			synchronized (LOCK) {
				inFlight = false;
				if (dirty) {
					dirty = false;
					inFlight = true;
					again = pendingServer;
				}
			}
			if (again != null) {
				dispatch(again);
			}
		}));
	}

	static JsonObject build(MinecraftServer server) {
		boolean en = "en".equalsIgnoreCase(NomosikiServerConfig.INSTANCE.discord.language);
		Map<FactionWarPair, FactionRelationStatus> relations = FactionService.get(server).diplomacyData().nonNeutralRelations();
		List<String> wars = new ArrayList<>();
		List<String> military = new ArrayList<>();
		List<String> regular = new ArrayList<>();
		for (Map.Entry<FactionWarPair, FactionRelationStatus> entry : relations.entrySet()) {
			String line = DiscordDiplomacyNotifier.escapeMarkdown(entry.getKey().factionA())
				+ " ↔ "
				+ DiscordDiplomacyNotifier.escapeMarkdown(entry.getKey().factionB());
			switch (entry.getValue()) {
				case AT_WAR -> wars.add(line);
				case MILITARY_ALLY -> military.add(line);
				case ALLY -> regular.add(line);
				default -> {
				}
			}
		}
		WartimeService wartime = WartimeService.get(server);
		boolean active = wartime.isActive();
		JsonObject embed = new JsonObject();
		embed.addProperty("title", en ? "Server politics" : "Политика сервера");
		embed.addProperty("color", active ? COLOR_WARTIME : DiscordDiplomacyNotifier.COLOR_POLITICS_IDLE);
		JsonArray fields = new JsonArray();
		DiscordDiplomacyNotifier.addField(fields, en ? "Wars" : "Войны", joinOrNone(wars, en), false);
		DiscordDiplomacyNotifier.addField(fields, en ? "Military alliances" : "Военные союзы", joinOrNone(military, en), false);
		DiscordDiplomacyNotifier.addField(fields, en ? "Alliances" : "Союзы", joinOrNone(regular, en), false);
		DiscordDiplomacyNotifier.addField(fields, en ? "Wartime" : "Вартайм", wartimeText(wartime, en), false);
		embed.add("fields", fields);
		return embed;
	}

	private static String wartimeText(WartimeService wartime, boolean en) {
		if (!wartime.isActive()) {
			return en ? "none" : "нет";
		}
		String until = TIME_FMT.format(Instant.ofEpochMilli(wartime.endsAtMs()));
		String names = wartime.namedFactions().stream()
			.map(DiscordDiplomacyNotifier::escapeMarkdown)
			.reduce((left, right) -> left + ", " + right)
			.orElse(en ? "none" : "нет");
		return (en ? "Until " : "До ") + until + (en ? ". Factions: " : ". Участвуют: ") + names;
	}

	private static String joinOrNone(List<String> lines, boolean en) {
		if (lines.isEmpty()) {
			return en ? "none" : "нет";
		}
		return String.join(", ", lines);
	}
}
