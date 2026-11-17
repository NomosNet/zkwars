package com.nomosiki.duel;

import com.nomosiki.advancement.NomosikiAdvancements;
import com.nomosiki.discord.DuelDiscordNotifier;
import com.nomosiki.faction.FactionService;
import com.nomosiki.faction.WartimeService;
import com.nomosiki.network.duel.payload.s2c.DuelScreenPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Challenges, accepted fights, and the 24 hour cooldown. Pending fights live only in memory. */
public final class DuelService {
	public static final long COOLDOWN_MS = 24L * 60L * 60L * 1000L;
	public static final long CHALLENGE_MS = 2L * 60L * 1000L;
	private static final long BLOCKED_NOTICE_MS = 2_000L;
	private static final UUID NIL = new UUID(0L, 0L);
	private static final Map<MinecraftServer, DuelService> BY_SERVER = new ConcurrentHashMap<>();

	private final MinecraftServer server;
	private final DuelSavedData data;
	private final Map<UUID, Pending> byChallenger = new HashMap<>();
	private final Map<UUID, Pending> byTarget = new HashMap<>();
	private final Map<UUID, UUID> active = new HashMap<>();
	private final Map<UUID, Long> blockedNoticeUntil = new HashMap<>();

	private DuelService(MinecraftServer server) {
		this.server = server;
		this.data = server.overworld().getDataStorage().computeIfAbsent(DuelSavedData.factory(), DuelSavedData.DATA_ID);
	}

	public static DuelService get(MinecraftServer server) {
		return BY_SERVER.computeIfAbsent(server, DuelService::new);
	}

	public static void forget(MinecraftServer server) {
		BY_SERVER.remove(server);
	}

	public void tick() {
		if (this.server.getTickCount() % 20 != 0) {
			return;
		}
		long now = System.currentTimeMillis();
		this.blockedNoticeUntil.entrySet().removeIf(entry -> entry.getValue() <= now);
		if (this.server.getTickCount() % 1200 == 0) {
			this.data.pruneExpired();
		}
		List<Pending> expired = new ArrayList<>();
		for (Pending pending : this.byChallenger.values()) {
			if (now >= pending.expiresAtMs) {
				expired.add(pending);
			}
		}
		for (Pending pending : expired) {
			drop(pending);
			Component message = Component.translatable("message.nomosiki.duel.expired");
			tell(pending.challenger, message);
			tell(pending.target, message);
			sync(pending.challenger, false, true);
			sync(pending.target, false, true);
		}
	}

	@Nullable
	public Component challenge(ServerPlayer from, ServerPlayer to) {
		Component error = challengeError(from, to);
		if (error != null) {
			return error;
		}
		Pending pending = new Pending(from.getUUID(), to.getUUID(), System.currentTimeMillis() + CHALLENGE_MS);
		this.byChallenger.put(from.getUUID(), pending);
		this.byTarget.put(to.getUUID(), pending);
		from.displayClientMessage(Component.translatable("message.nomosiki.duel.outgoing", name(to)), false);
		to.displayClientMessage(Component.translatable(
			"message.nomosiki.duel.incoming", name(from), factionComponent(from.getUUID())), false);
		sync(from, false, false);
		sync(to, true, false);
		return null;
	}

	@Nullable
	public Component accept(ServerPlayer player) {
		Pending pending = this.byTarget.get(player.getUUID());
		if (pending == null || System.currentTimeMillis() >= pending.expiresAtMs) {
			return Component.translatable("message.nomosiki.duel.none");
		}
		ServerPlayer challenger = this.server.getPlayerList().getPlayer(pending.challenger);
		if (challenger == null) {
			drop(pending);
			sync(player, false, false);
			return Component.translatable("message.nomosiki.duel.offline");
		}
		if (WartimeService.get(this.server).isActive()) {
			drop(pending);
			Component blocked = Component.translatable("message.nomosiki.duel.wartime_blocked");
			challenger.displayClientMessage(blocked, false);
			sync(player, false, true);
			sync(challenger, false, true);
			return blocked;
		}
		if (sameFaction(pending.challenger, pending.target)) {
			drop(pending);
			Component blocked = Component.translatable("message.nomosiki.duel.same_faction");
			challenger.displayClientMessage(blocked, false);
			sync(player, false, false);
			sync(challenger, false, false);
			return blocked;
		}
		long fromCd = this.data.cooldownUntil(pending.challenger);
		long toCd = this.data.cooldownUntil(pending.target);
		long now = System.currentTimeMillis();
		if (fromCd > now || toCd > now) {
			drop(pending);
			sync(player, false, false);
			sync(challenger, false, false);
			return fromCd > now
				? cooldownOther(name(challenger), fromCd)
				: cooldownSelf(toCd);
		}
		drop(pending);
		long until = now + COOLDOWN_MS;
		this.data.setCooldown(pending.challenger, until);
		this.data.setCooldown(pending.target, until);
		this.active.put(pending.challenger, pending.target);
		this.active.put(pending.target, pending.challenger);
		challenger.displayClientMessage(Component.translatable("message.nomosiki.duel.accepted", name(player)), false);
		player.displayClientMessage(Component.translatable("message.nomosiki.duel.accepted", name(challenger)), false);
		sync(player, false, false);
		sync(challenger, false, false);
		return null;
	}

	@Nullable
	public Component deny(ServerPlayer player) {
		Pending pending = this.byTarget.get(player.getUUID());
		if (pending == null || System.currentTimeMillis() >= pending.expiresAtMs) {
			return Component.translatable("message.nomosiki.duel.none");
		}
		ServerPlayer challenger = this.server.getPlayerList().getPlayer(pending.challenger);
		String challengerName = challenger != null ? name(challenger) : resolveName(pending.challenger);
		drop(pending);
		player.displayClientMessage(Component.translatable("message.nomosiki.duel.denied_you", challengerName), false);
		if (challenger != null) {
			challenger.displayClientMessage(Component.translatable("message.nomosiki.duel.denied_them", name(player)), false);
			sync(challenger, false, false);
		}
		notifyRefusal(pending.challenger, challengerName, player);
		sync(player, false, false);
		return null;
	}

	@Nullable
	public Component cancel(ServerPlayer player) {
		Pending pending = this.byChallenger.get(player.getUUID());
		if (pending == null) {
			return Component.translatable("message.nomosiki.duel.no_outgoing");
		}
		ServerPlayer target = this.server.getPlayerList().getPlayer(pending.target);
		String targetName = target != null ? name(target) : resolveName(pending.target);
		drop(pending);
		player.displayClientMessage(Component.translatable("message.nomosiki.duel.cancelled_you", targetName), false);
		if (target != null) {
			target.displayClientMessage(Component.translatable("message.nomosiki.duel.cancelled_them", name(player)), false);
			sync(target, false, false);
		}
		sync(player, false, false);
		return null;
	}

	public boolean mayDamage(ServerPlayer attacker, ServerPlayer victim) {
		if (attacker.getUUID().equals(victim.getUUID())) {
			return true;
		}
		if (WartimeService.get(this.server).isActive()
			&& FactionService.diplomacy(this.server).areAtWar(attacker.getUUID(), victim.getUUID())) {
			return true;
		}
		return victim.getUUID().equals(this.active.get(attacker.getUUID()));
	}

	public void notifyBlocked(ServerPlayer attacker, ServerPlayer victim) {
		long now = System.currentTimeMillis();
		Long until = this.blockedNoticeUntil.get(attacker.getUUID());
		if (until != null && now < until) {
			return;
		}
		this.blockedNoticeUntil.put(attacker.getUUID(), now + BLOCKED_NOTICE_MS);
		Component message = Component.translatable("message.nomosiki.duel.pvp_blocked");
		attacker.displayClientMessage(message, false);
		if (!attacker.getUUID().equals(victim.getUUID())) {
			Long victimUntil = this.blockedNoticeUntil.get(victim.getUUID());
			if (victimUntil == null || now >= victimUntil) {
				this.blockedNoticeUntil.put(victim.getUUID(), now + BLOCKED_NOTICE_MS);
				victim.displayClientMessage(message, false);
			}
		}
	}

	public void onDeath(ServerPlayer dead) {
		UUID opponentId = this.active.remove(dead.getUUID());
		if (opponentId == null) {
			return;
		}
		this.active.remove(opponentId);
		String winner = resolveName(opponentId);
		this.server.getPlayerList().broadcastSystemMessage(
			Component.translatable("message.nomosiki.duel.victory", winner, name(dead)), false);
		NomosikiAdvancements.onDuelWin(this.server, opponentId);
		sync(dead, false, false);
		sync(opponentId, false, false);
	}

	public void onDisconnect(ServerPlayer player) {
		UUID id = player.getUUID();
		UUID opponentId = this.active.remove(id);
		if (opponentId != null) {
			this.active.remove(opponentId);
			ServerPlayer opponent = this.server.getPlayerList().getPlayer(opponentId);
			if (opponent != null) {
				opponent.displayClientMessage(Component.translatable("message.nomosiki.duel.opponent_left", name(player)), false);
				sync(opponent, false, false);
			}
		}
		Pending outgoing = this.byChallenger.get(id);
		if (outgoing != null) {
			drop(outgoing);
			tell(outgoing.target, Component.translatable("message.nomosiki.duel.left_pending", name(player)));
			sync(outgoing.target, false, true);
		}
		Pending incoming = this.byTarget.get(id);
		if (incoming != null) {
			drop(incoming);
			tell(incoming.challenger, Component.translatable("message.nomosiki.duel.left_pending", name(player)));
			sync(incoming.challenger, false, true);
		}
	}

	public void onWartimeStarted() {
		Set<UUID> involved = new HashSet<>();
		involved.addAll(this.byChallenger.keySet());
		involved.addAll(this.byTarget.keySet());
		involved.addAll(this.active.keySet());
		this.byChallenger.clear();
		this.byTarget.clear();
		this.active.clear();
		Component message = Component.translatable("message.nomosiki.duel.wartime_cleared");
		for (ServerPlayer player : this.server.getPlayerList().getPlayers()) {
			boolean wasInvolved = involved.contains(player.getUUID());
			if (wasInvolved) {
				player.displayClientMessage(message, false);
			}
			sync(player, false, wasInvolved);
		}
	}

	public void sendScreen(ServerPlayer player) {
		sync(player, false, false);
	}

	@Nullable
	private Component challengeError(ServerPlayer from, ServerPlayer to) {
		if (from.getUUID().equals(to.getUUID())) {
			return Component.translatable("message.nomosiki.duel.self");
		}
		if (WartimeService.get(this.server).isActive()) {
			return Component.translatable("message.nomosiki.duel.wartime_blocked");
		}
		if (sameFaction(from.getUUID(), to.getUUID())) {
			return Component.translatable("message.nomosiki.duel.same_faction");
		}
		if (isBusy(from.getUUID())) {
			return Component.translatable("message.nomosiki.duel.busy_self");
		}
		if (isBusy(to.getUUID())) {
			return Component.translatable("message.nomosiki.duel.busy_other", name(to));
		}
		long now = System.currentTimeMillis();
		long fromCd = this.data.cooldownUntil(from.getUUID());
		if (fromCd > now) {
			return cooldownSelf(fromCd);
		}
		long toCd = this.data.cooldownUntil(to.getUUID());
		if (toCd > now) {
			return cooldownOther(name(to), toCd);
		}
		return null;
	}

	private void notifyRefusal(UUID challengerId, String challengerName, ServerPlayer target) {
		String challengerFaction = factionName(challengerId);
		String targetFaction = factionName(target.getUUID());
		if (challengerFaction == null || targetFaction == null || challengerFaction.equalsIgnoreCase(targetFaction)) {
			return;
		}
		DuelDiscordNotifier.get().notifyRefusal(challengerName, challengerFaction, name(target), targetFaction);
	}

	private void sync(UUID playerId, boolean requestOpen, boolean closeScreen) {
		ServerPlayer player = this.server.getPlayerList().getPlayer(playerId);
		if (player != null) {
			sync(player, requestOpen, closeScreen);
		}
	}

	private void sync(ServerPlayer player, boolean requestOpen, boolean closeScreen) {
		UUID id = player.getUUID();
		byte phase = DuelScreenPayload.PHASE_IDLE;
		String otherName = "";
		UUID otherId = NIL;
		long expires = 0L;
		Pending outgoing = this.byChallenger.get(id);
		Pending incoming = this.byTarget.get(id);
		UUID opponent = this.active.get(id);
		if (opponent != null) {
			phase = DuelScreenPayload.PHASE_FIGHTING;
			otherId = opponent;
			otherName = resolveName(opponent);
		} else if (outgoing != null) {
			phase = DuelScreenPayload.PHASE_OUTGOING;
			otherId = outgoing.target;
			otherName = resolveName(outgoing.target);
			expires = outgoing.expiresAtMs;
		} else if (incoming != null) {
			phase = DuelScreenPayload.PHASE_INCOMING;
			otherId = incoming.challenger;
			otherName = resolveName(incoming.challenger);
			expires = incoming.expiresAtMs;
		}
		List<DuelScreenPayload.Row> rows = List.of();
		if (phase == DuelScreenPayload.PHASE_IDLE || phase == DuelScreenPayload.PHASE_INCOMING) {
			rows = roster(player);
		}
		ServerPlayNetworking.send(player, new DuelScreenPayload(
			this.data.cooldownUntil(id),
			phase,
			otherName,
			otherId,
			expires,
			WartimeService.get(this.server).isActive(),
			requestOpen,
			closeScreen,
			rows
		));
	}

	private List<DuelScreenPayload.Row> roster(ServerPlayer viewer) {
		List<DuelScreenPayload.Row> rows = new ArrayList<>();
		for (ServerPlayer other : this.server.getPlayerList().getPlayers()) {
			if (other == viewer || sameFaction(viewer.getUUID(), other.getUUID())) {
				continue;
			}
			boolean can = challengeError(viewer, other) == null;
			String faction = factionName(other.getUUID());
			rows.add(new DuelScreenPayload.Row(other.getUUID(), name(other), faction == null ? "" : faction, can));
		}
		rows.sort(Comparator.comparing(DuelScreenPayload.Row::name, String.CASE_INSENSITIVE_ORDER));
		return rows;
	}

	private boolean isBusy(UUID playerId) {
		return this.active.containsKey(playerId)
			|| this.byChallenger.containsKey(playerId)
			|| this.byTarget.containsKey(playerId);
	}

	private boolean sameFaction(UUID a, UUID b) {
		String factionA = factionName(a);
		String factionB = factionName(b);
		return factionA != null && factionB != null && factionA.equalsIgnoreCase(factionB);
	}

	@Nullable
	private String factionName(UUID playerId) {
		String faction = FactionService.get(this.server).getPlayerFaction(playerId);
		if (faction == null || faction.isBlank()) {
			return null;
		}
		return faction;
	}

	private Component factionComponent(UUID playerId) {
		String faction = factionName(playerId);
		if (faction == null) {
			return Component.translatable("screen.nomosiki.duel.no_faction");
		}
		return Component.literal(faction);
	}

	private void drop(Pending pending) {
		this.byChallenger.remove(pending.challenger, pending);
		this.byTarget.remove(pending.target, pending);
	}

	private void tell(UUID playerId, Component message) {
		ServerPlayer player = this.server.getPlayerList().getPlayer(playerId);
		if (player != null) {
			player.displayClientMessage(message, false);
		}
	}

	private static Component cooldownSelf(long untilMs) {
		long[] parts = remaining(untilMs);
		return Component.translatable("message.nomosiki.duel.cooldown_self", parts[0], parts[1]);
	}

	private static Component cooldownOther(String name, long untilMs) {
		long[] parts = remaining(untilMs);
		return Component.translatable("message.nomosiki.duel.cooldown_other", name, parts[0], parts[1]);
	}

	private static long[] remaining(long untilMs) {
		long left = Math.max(0L, untilMs - System.currentTimeMillis());
		long hours = left / 3_600_000L;
		long minutes = (left % 3_600_000L) / 60_000L;
		if (left > 0L && hours == 0L && minutes == 0L) {
			minutes = 1L;
		}
		return new long[] {hours, minutes};
	}

	private static String name(ServerPlayer player) {
		return player.getGameProfile().getName();
	}

	private String resolveName(UUID playerId) {
		ServerPlayer online = this.server.getPlayerList().getPlayer(playerId);
		if (online != null) {
			return name(online);
		}
		return playerId.toString().substring(0, 8);
	}

	private record Pending(UUID challenger, UUID target, long expiresAtMs) {
	}
}
