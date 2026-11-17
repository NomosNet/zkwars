package com.nomosiki.discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nomosiki.NomosikiMod;
import com.nomosiki.config.NomosikiServerConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Fire-and-forget Discord Incoming Webhook posts for diplomacy events.
 * Failures are logged only; never throws on the server thread.
 */
public final class DiscordDiplomacyNotifier {
	private static final int FIELD_VALUE_LIMIT = 1024;
	private static final int FIELD_NAME_LIMIT = 256;
	private static final int COLOR_WAR = 0xC0392B;
	private static final int COLOR_PEACE = 0x27AE60;
	private static final int COLOR_DECLINED = 0x7F8C8D;
	private static final int COLOR_ALLIANCE = 0x2980B9;
	private static final int COLOR_BROKEN = 0x922B21;
	private static final int COLOR_WARTIME = 0xE67E22;
	private static final int COLOR_CAPTURE = 0xF1C40F;
	static final int COLOR_POLITICS_IDLE = 0x5D6D7E;

	private static final DiscordDiplomacyNotifier INSTANCE = new DiscordDiplomacyNotifier();

	private final HttpClient httpClient;
	private final ExecutorService executor;

	private DiscordDiplomacyNotifier() {
		this.httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(8))
			.build();
		this.executor = Executors.newSingleThreadExecutor(r -> {
			Thread t = new Thread(r, "nomosiki-discord-webhook");
			t.setDaemon(true);
			return t;
		});
	}

	public static DiscordDiplomacyNotifier get() {
		return INSTANCE;
	}

	public void notify(DiplomacyDiscordEvent event) {
		try {
			NomosikiServerConfig.Discord cfg = NomosikiServerConfig.INSTANCE.discord;
			if (!cfg.enabled || cfg.webhookUrl == null || cfg.webhookUrl.isBlank()) {
				return;
			}
			if (!isEventEnabled(cfg, event.type())) {
				return;
			}
			sendWebhookAsync(cfg.webhookUrl.trim(), formatEmbed(cfg, event));
		} catch (Exception ex) {
			NomosikiMod.LOGGER.warn("nomosiki: discord notify failed (ignored)", ex);
		}
	}

	private static boolean isEventEnabled(NomosikiServerConfig.Discord cfg, DiplomacyDiscordEvent.Type type) {
		return switch (type) {
			case WAR_DECLARED -> cfg.events.warDeclared;
			case PEACE_DECLARED -> cfg.events.peaceDeclared;
			case PEACE_OFFERED -> cfg.events.peaceOffered;
			case PEACE_DECLINED -> cfg.events.peaceDeclined;
			case ALLIANCE_REQUESTED -> cfg.events.allianceRequested;
			case ALLIANCE_FORMED -> cfg.events.allianceFormed;
			case ALLIANCE_DECLINED -> cfg.events.allianceDeclined;
			case ALLIANCE_BROKEN -> cfg.events.allianceBroken;
			case WARTIME_STARTED, WARTIME_STOPPED, WARTIME_EXPIRED -> cfg.events.wartime;
			case CAPTURE_STARTED, CAPTURE_COMPLETED -> cfg.events.capture;
		};
	}

	JsonObject formatEmbed(NomosikiServerConfig.Discord cfg, DiplomacyDiscordEvent event) {
		boolean en = "en".equalsIgnoreCase(cfg.language);
		JsonObject embed = new JsonObject();
		embed.addProperty("title", title(en, event));
		embed.addProperty("color", color(event.type()));
		JsonArray fields = new JsonArray();
		addEventFields(fields, en, event);
		if (!fields.isEmpty()) {
			embed.add("fields", fields);
		}
		if (cfg.includeLeaderName && event.leaderName() != null && !event.leaderName().isBlank()) {
			JsonObject footer = new JsonObject();
			footer.addProperty("text", clip((en ? "Leader: " : "Лидер: ") + plain(event.leaderName()), 2048));
			embed.add("footer", footer);
		}
		if (cfg.includeServerTime) {
			embed.addProperty("timestamp", Instant.ofEpochMilli(event.epochMs()).toString());
		}
		return embed;
	}

	private static void addEventFields(JsonArray fields, boolean en, DiplomacyDiscordEvent event) {
		String a = escapeMarkdown(event.factionA());
		String b = escapeMarkdown(event.factionB());
		switch (event.type()) {
			case WAR_DECLARED -> {
				addField(fields, en ? "Aggressor" : "Агрессор", a, true);
				addField(fields, en ? "Target" : "Цель", b, true);
				if (!event.cascadePairs().isEmpty()) {
					String joined = String.join(", ", event.cascadePairs().stream().map(DiscordDiplomacyNotifier::escapeMarkdown).toList());
					addField(fields, en ? "Pulled in" : "Втянуты", joined, false);
				}
			}
			case PEACE_OFFERED, ALLIANCE_REQUESTED -> {
				addField(fields, en ? "From" : "От", a, true);
				addField(fields, en ? "To" : "Кому", b, true);
			}
			case PEACE_DECLINED, ALLIANCE_DECLINED -> {
				addField(fields, en ? "Who" : "Кто", a, true);
				addField(fields, en ? "With" : "С кем", b, true);
			}
			case PEACE_DECLARED, ALLIANCE_FORMED, ALLIANCE_BROKEN -> {
				addField(fields, en ? "Side" : "Сторона", a, true);
				addField(fields, en ? "Side" : "Сторона", b, true);
			}
			case WARTIME_STARTED -> {
				addField(fields, en ? "Until" : "До", escapeMarkdown(event.detail()), true);
				addField(fields, en ? "Fighting" : "Воюют", a, false);
			}
			case WARTIME_STOPPED, WARTIME_EXPIRED ->
				addField(fields, en ? "Factions" : "Фракции", a, false);
			case CAPTURE_STARTED, CAPTURE_COMPLETED -> {
				addField(fields, en ? "Attacker" : "Атакующий", a, true);
				addField(fields, en ? "City" : "Город", escapeMarkdown(event.detail()), true);
				addField(fields, en ? "Defender" : "Владелец", b, true);
			}
		}
	}

	private static String title(boolean en, DiplomacyDiscordEvent event) {
		return switch (event.type()) {
			case WAR_DECLARED -> en ? "War declared" : "Объявлена война";
			case PEACE_OFFERED -> en ? "Peace offer" : "Предложение мира";
			case PEACE_DECLINED -> en ? "Peace declined" : "Мир отклонён";
			case PEACE_DECLARED -> en ? "Peace made" : "Заключён мир";
			case WARTIME_STARTED -> en ? "Wartime started" : "Вартайм начался";
			case WARTIME_STOPPED -> en ? "Wartime stopped" : "Вартайм остановлен";
			case WARTIME_EXPIRED -> en ? "Wartime ended" : "Вартайм закончился";
			case CAPTURE_STARTED -> en ? "Capture started" : "Захват начат";
			case CAPTURE_COMPLETED -> en ? "City captured" : "Город захвачен";
			case ALLIANCE_REQUESTED -> event.militaryAlliance()
				? (en ? "Military alliance offer" : "Предложение военного союза")
				: (en ? "Alliance offer" : "Предложение союза");
			case ALLIANCE_FORMED -> event.militaryAlliance()
				? (en ? "Military alliance" : "Военный союз")
				: (en ? "Alliance formed" : "Союз заключён");
			case ALLIANCE_DECLINED -> en ? "Alliance declined" : "Союз отклонён";
			case ALLIANCE_BROKEN -> event.militaryAlliance()
				? (en ? "Military alliance broken" : "Военный союз разорван")
				: (en ? "Alliance broken" : "Союз разорван");
		};
	}

	private static int color(DiplomacyDiscordEvent.Type type) {
		return switch (type) {
			case WAR_DECLARED -> COLOR_WAR;
			case PEACE_OFFERED, PEACE_DECLARED -> COLOR_PEACE;
			case PEACE_DECLINED, ALLIANCE_DECLINED -> COLOR_DECLINED;
			case ALLIANCE_REQUESTED, ALLIANCE_FORMED -> COLOR_ALLIANCE;
			case ALLIANCE_BROKEN -> COLOR_BROKEN;
			case WARTIME_STARTED, WARTIME_STOPPED, WARTIME_EXPIRED -> COLOR_WARTIME;
			case CAPTURE_STARTED, CAPTURE_COMPLETED -> COLOR_CAPTURE;
		};
	}

	static void addField(JsonArray fields, String name, String value, boolean inline) {
		JsonObject field = new JsonObject();
		field.addProperty("name", clip(name, FIELD_NAME_LIMIT));
		String safe = value == null || value.isBlank() ? "—" : value;
		field.addProperty("value", clip(safe, FIELD_VALUE_LIMIT));
		field.addProperty("inline", inline);
		fields.add(field);
	}

	static String escapeMarkdown(String raw) {
		if (raw == null || raw.isEmpty()) {
			return "";
		}
		StringBuilder out = new StringBuilder(raw.length() + 8);
		for (int i = 0; i < raw.length(); i++) {
			char c = raw.charAt(i);
			if (c == '\\' || c == '*' || c == '_' || c == '`' || c == '|' || c == '~') {
				out.append('\\');
			}
			out.append(c);
		}
		return out.toString();
	}

	/**
	 * Creates or edits the single politics summary. {@code callback} receives the stored message id,
	 * or null when the stored id should stay as it is. Always invoked, including when Discord is off.
	 */
	public void upsertStatus(JsonObject embed, String existingId, Consumer<String> callback) {
		NomosikiServerConfig.Discord cfg = NomosikiServerConfig.INSTANCE.discord;
		if (!cfg.enabled || cfg.webhookUrl == null || cfg.webhookUrl.isBlank() || embed == null) {
			callback.accept(null);
			return;
		}
		String webhookUrl = cfg.webhookUrl.trim();
		this.executor.execute(() -> {
			try {
				String id = existingId == null ? "" : existingId.trim();
				String stored = id.isEmpty() ? postStatus(webhookUrl, embed) : patchStatus(webhookUrl, id, embed);
				callback.accept(stored);
			} catch (Exception ex) {
				NomosikiMod.LOGGER.warn("nomosiki: discord status update failed", ex);
				callback.accept(null);
			}
		});
	}

	private String postStatus(String webhookUrl, JsonObject embed) throws Exception {
		HttpResponse<String> response = sendJson(webhookUri(webhookUrl, null), "POST", messageBody(embed));
		int code = response.statusCode();
		if (code < 200 || code >= 300) {
			NomosikiMod.LOGGER.warn("nomosiki: discord status HTTP {} — {}", code, truncate(response.body(), 200));
			return null;
		}
		return readMessageId(response.body());
	}

	private String patchStatus(String webhookUrl, String messageId, JsonObject embed) throws Exception {
		HttpResponse<String> response = sendJson(webhookUri(webhookUrl, messageId), "PATCH", messageBody(embed));
		int code = response.statusCode();
		if (code == 404) {
			return postStatus(webhookUrl, embed);
		}
		if (code < 200 || code >= 300) {
			NomosikiMod.LOGGER.warn("nomosiki: discord status edit HTTP {} — {}", code, truncate(response.body(), 200));
			return null;
		}
		String id = readMessageId(response.body());
		return id == null || id.isBlank() ? messageId : id;
	}

	private static JsonObject messageBody(JsonObject embed) {
		JsonObject body = new JsonObject();
		body.add("content", JsonNull.INSTANCE);
		JsonArray embeds = new JsonArray();
		embeds.add(embed);
		body.add("embeds", embeds);
		return body;
	}

	private HttpResponse<String> sendJson(URI uri, String method, JsonObject body) throws Exception {
		HttpRequest request = HttpRequest.newBuilder()
			.uri(uri)
			.timeout(Duration.ofSeconds(10))
			.header("Content-Type", "application/json")
			.method(method, HttpRequest.BodyPublishers.ofString(body.toString()))
			.build();
		return this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private static URI webhookUri(String webhookUrl, String messageId) {
		String base = webhookUrl.trim();
		int query = base.indexOf('?');
		String root = query >= 0 ? base.substring(0, query) : base;
		if (root.endsWith("/")) {
			root = root.substring(0, root.length() - 1);
		}
		if (messageId == null || messageId.isBlank()) {
			return URI.create(root + "?wait=true");
		}
		return URI.create(root + "/messages/" + messageId);
	}

	private static String readMessageId(String body) {
		if (body == null || body.isBlank()) {
			return null;
		}
		try {
			var parsed = JsonParser.parseString(body);
			if (!parsed.isJsonObject() || !parsed.getAsJsonObject().has("id")) {
				return null;
			}
			return parsed.getAsJsonObject().get("id").getAsString();
		} catch (Exception ignored) {
			return null;
		}
	}

	private void sendWebhookAsync(String webhookUrl, JsonObject embed) {
		JsonObject body = messageBody(embed);
		this.executor.execute(() -> {
			try {
				HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(webhookUrl))
					.timeout(Duration.ofSeconds(10))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(body.toString()))
					.build();
				HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
				int code = response.statusCode();
				if (code < 200 || code >= 300) {
					NomosikiMod.LOGGER.warn(
						"nomosiki: discord webhook HTTP {} — {}",
						code,
						truncate(response.body(), 200));
				}
			} catch (Exception ex) {
				NomosikiMod.LOGGER.warn("nomosiki: discord webhook send failed", ex);
			}
		});
	}

	private static String plain(String raw) {
		return raw.replace('\r', ' ').replace('\n', ' ');
	}

	private static String clip(String raw, int max) {
		if (raw == null) {
			return "";
		}
		if (raw.length() <= max) {
			return raw;
		}
		if (max <= 3) {
			return raw.substring(0, max);
		}
		return raw.substring(0, max - 3) + "...";
	}

	private static String truncate(String s, int max) {
		if (s == null) {
			return "";
		}
		return s.length() <= max ? s : s.substring(0, max) + "...";
	}
}
