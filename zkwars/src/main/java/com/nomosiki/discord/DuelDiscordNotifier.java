package com.nomosiki.discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
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

/**
 * Refusal notices for a webhook other than the politics channel.
 * Failures are logged only and never thrown back onto the server thread.
 */
public final class DuelDiscordNotifier {
	private static final int COLOR_DECLINED = 0x7F8C8D;
	private static final DuelDiscordNotifier INSTANCE = new DuelDiscordNotifier();

	private final HttpClient httpClient;
	private final ExecutorService executor;

	private DuelDiscordNotifier() {
		this.httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(8))
			.build();
		this.executor = Executors.newSingleThreadExecutor(r -> {
			Thread thread = new Thread(r, "nomosiki-duel-webhook");
			thread.setDaemon(true);
			return thread;
		});
	}

	public static DuelDiscordNotifier get() {
		return INSTANCE;
	}

	public void notifyRefusal(String challengerName, String challengerFaction, String targetName, String targetFaction) {
		try {
			NomosikiServerConfig.DuelDiscord cfg = NomosikiServerConfig.INSTANCE.duelDiscord;
			if (!cfg.enabled || cfg.webhookUrl == null || cfg.webhookUrl.isBlank()) {
				return;
			}
			JsonObject embed = new JsonObject();
			embed.addProperty("title", "Отказ от дуэли");
			embed.addProperty("color", COLOR_DECLINED);
			JsonArray fields = new JsonArray();
			DiscordDiplomacyNotifier.addField(fields, "Вызывающий", DiscordDiplomacyNotifier.escapeMarkdown(challengerName), true);
			DiscordDiplomacyNotifier.addField(fields, "Фракция", DiscordDiplomacyNotifier.escapeMarkdown(challengerFaction), true);
			DiscordDiplomacyNotifier.addField(fields, "Отказавший", DiscordDiplomacyNotifier.escapeMarkdown(targetName), true);
			DiscordDiplomacyNotifier.addField(fields, "Фракция", DiscordDiplomacyNotifier.escapeMarkdown(targetFaction), true);
			embed.add("fields", fields);
			embed.addProperty("timestamp", Instant.now().toString());
			sendAsync(cfg.webhookUrl.trim(), embed);
		} catch (Exception ex) {
			NomosikiMod.LOGGER.warn("zkwars: duel discord notify failed (ignored)", ex);
		}
	}

	private void sendAsync(String webhookUrl, JsonObject embed) {
		JsonObject body = new JsonObject();
		body.add("content", JsonNull.INSTANCE);
		JsonArray embeds = new JsonArray();
		embeds.add(embed);
		body.add("embeds", embeds);
		String json = body.toString();
		this.executor.execute(() -> {
			try {
				HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(webhookUrl))
					.timeout(Duration.ofSeconds(10))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(json))
					.build();
				HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
				int code = response.statusCode();
				if (code < 200 || code >= 300) {
					NomosikiMod.LOGGER.warn("zkwars: duel discord webhook HTTP {}", code);
				}
			} catch (Exception ex) {
				NomosikiMod.LOGGER.warn("zkwars: duel discord webhook send failed", ex);
			}
		});
	}
}
