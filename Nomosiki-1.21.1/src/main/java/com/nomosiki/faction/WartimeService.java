package com.nomosiki.faction;

import com.nomosiki.discord.DiplomacyDiscordEvent;
import com.nomosiki.discord.DiscordDiplomacyNotifier;
import com.nomosiki.discord.DiscordPoliticsBoard;
import com.nomosiki.duel.DuelService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Admin wartime: named factions and their military allies stay; everyone else is removed. */
public final class WartimeService {
	private static final long WARNING_MS = 10_000L;
	private static final DateTimeFormatter TIME_FMT =
		DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(ZoneId.systemDefault());
	private static final Map<MinecraftServer, WartimeService> BY_SERVER = new ConcurrentHashMap<>();

	private final MinecraftServer server;
	private final WartimeSavedData data;
	private long kickAtMs;

	private WartimeService(MinecraftServer server) {
		this.server = server;
		this.data = server.overworld().getDataStorage().computeIfAbsent(WartimeSavedData.factory(), WartimeSavedData.DATA_ID);
	}

	public static WartimeService get(MinecraftServer server) {
		return BY_SERVER.computeIfAbsent(server, WartimeService::new);
	}

	public boolean isActive() {
		return this.data.active() && System.currentTimeMillis() < this.data.endsAtMs();
	}

	public long endsAtMs() {
		return this.data.endsAtMs();
	}

	public List<String> namedFactions() {
		return this.data.factions();
	}

	public String statusMessageId() {
		return this.data.statusMessageId();
	}

	public void setStatusMessageId(String id) {
		this.data.setStatusMessageId(id);
	}

	public void onFactionRemoved(String factionName) {
		this.data.removeFaction(factionName);
	}

	/** @return a failure message, or null when wartime has started */
	@Nullable
	public Component start(int minutes, String rawList, String actorName) {
		if (this.data.active() && System.currentTimeMillis() < this.data.endsAtMs()) {
			return Component.translatable("message.nomosiki.wartime.already");
		}
		List<String> named = new ArrayList<>();
		Component parseError = resolveFactions(rawList, named);
		if (parseError != null) {
			return parseError;
		}
		FactionDiplomacyService diplomacy = FactionService.diplomacy(this.server);
		String warError = diplomacy.openWarsForWartime(named);
		if (!warError.isEmpty()) {
			return Component.translatable(warError);
		}
		long endsAt = System.currentTimeMillis() + minutes * 60_000L;
		this.data.begin(named, endsAt);
		this.kickAtMs = System.currentTimeMillis() + WARNING_MS;
		String until = formatTime(endsAt);
		String names = String.join(", ", named);
		Component started = Component.translatable("message.nomosiki.wartime.started", until, names);
		this.server.getPlayerList().broadcastSystemMessage(started, false);
		Component warning = Component.translatable("message.nomosiki.wartime.warning", until);
		for (ServerPlayer player : this.server.getPlayerList().getPlayers()) {
			if (!isAllowed(player)) {
				player.displayClientMessage(warning, false);
			}
		}
		DiscordDiplomacyNotifier.get().notify(DiplomacyDiscordEvent.notice(
			DiplomacyDiscordEvent.Type.WARTIME_STARTED, names, "", actorName, until));
		DiscordPoliticsBoard.refresh(this.server);
		DuelService.get(this.server).onWartimeStarted();
		return null;
	}

	/** @return a failure message, or null when wartime was stopped */
	@Nullable
	public Component stop(String actorName) {
		if (!this.data.active()) {
			return Component.translatable("message.nomosiki.wartime.not_active");
		}
		String names = String.join(", ", this.data.factions());
		endSession(false, names, actorName);
		return null;
	}

	public void tick() {
		if (!this.data.active()) {
			return;
		}
		long now = System.currentTimeMillis();
		if (now >= this.data.endsAtMs()) {
			String names = String.join(", ", this.data.factions());
			endSession(true, names, null);
			return;
		}
		if (this.kickAtMs > 0) {
			if (now >= this.kickAtMs) {
				this.kickAtMs = 0L;
				kickIllegal();
			}
		} else if (this.server.getTickCount() % 100 == 0) {
			kickIllegal();
		}
		if (this.server.getTickCount() % 1200 == 0) {
			DiscordPoliticsBoard.refresh(this.server);
		}
	}

	public void onJoin(ServerPlayer player) {
		if (!isActive() || isAllowed(player)) {
			return;
		}
		player.connection.disconnect(kickMessage());
	}

	public boolean isAllowed(ServerPlayer player) {
		if (!this.data.active() || System.currentTimeMillis() >= this.data.endsAtMs()) {
			return true;
		}
		if (player.hasPermissions(2)) {
			return true;
		}
		String faction = FactionService.get(this.server).getPlayerFaction(player.getUUID());
		if (faction == null || faction.isBlank()) {
			return false;
		}
		FactionDiplomacyService diplomacy = FactionService.diplomacy(this.server);
		for (String named : this.data.factions()) {
			if (faction.equalsIgnoreCase(named) || diplomacy.areMilitaryAllied(faction, named)) {
				return true;
			}
		}
		return false;
	}

	private void endSession(boolean expired, String names, @Nullable String actorName) {
		this.kickAtMs = 0L;
		this.data.end();
		Component message = Component.translatable(expired
			? "message.nomosiki.wartime.expired"
			: "message.nomosiki.wartime.stopped");
		this.server.getPlayerList().broadcastSystemMessage(message, false);
		DiscordDiplomacyNotifier.get().notify(DiplomacyDiscordEvent.notice(
			expired ? DiplomacyDiscordEvent.Type.WARTIME_EXPIRED : DiplomacyDiscordEvent.Type.WARTIME_STOPPED,
			names,
			"",
			actorName,
			""));
		DiscordPoliticsBoard.refresh(this.server);
	}

	private void kickIllegal() {
		if (!this.data.active()) {
			return;
		}
		Component message = kickMessage();
		for (ServerPlayer player : List.copyOf(this.server.getPlayerList().getPlayers())) {
			if (!isAllowed(player)) {
				player.connection.disconnect(message);
			}
		}
	}

	private Component kickMessage() {
		return Component.translatable("message.nomosiki.wartime.kick", formatTime(this.data.endsAtMs()));
	}

	private Component resolveFactions(String rawList, List<String> named) {
		if (rawList == null || rawList.isBlank()) {
			return Component.translatable("message.nomosiki.wartime.bad_list");
		}
		FactionManager factions = FactionService.get(this.server);
		LinkedHashSet<String> unique = new LinkedHashSet<>();
		for (String part : rawList.split(",")) {
			String trimmed = part.trim();
			if (trimmed.isEmpty()) {
				continue;
			}
			String resolved = factions.resolveFactionName(trimmed);
			if (resolved == null) {
				String known = String.join(", ", factions.listOtherFactions(null));
				if (known.isEmpty()) {
					known = "—";
				}
				return Component.translatable("message.nomosiki.wartime.unknown", trimmed, known);
			}
			unique.add(resolved);
		}
		if (unique.size() < 2) {
			return Component.translatable("message.nomosiki.wartime.need_two");
		}
		named.addAll(unique);
		return null;
	}

	public static String formatTime(long epochMs) {
		return TIME_FMT.format(Instant.ofEpochMilli(epochMs));
	}
}
