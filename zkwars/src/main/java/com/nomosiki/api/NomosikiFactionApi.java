package com.nomosiki.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nomosiki.discord.DiplomacyDiscordEvent;
import com.nomosiki.discord.DiscordDiplomacyNotifier;
import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.faction.FactionBannerHelper;
import com.nomosiki.faction.FactionColor;
import com.nomosiki.faction.FactionData;
import com.nomosiki.faction.FactionSavedData;
import com.nomosiki.faction.FactionService;
import com.nomosiki.faction.WartimeService;
import com.nomosiki.squad.NomosikSquad;
import com.nomosiki.squad.SquadSavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Stable entry points for other mods. Nomos-city calls these by reflection.
 */
public final class NomosikiFactionApi {
	private NomosikiFactionApi() {
	}

	public static boolean isWartimeActive(MinecraftServer server) {
		return WartimeService.get(server).isActive();
	}

	public static boolean areAtWar(MinecraftServer server, String factionA, String factionB) {
		return FactionService.diplomacy(server).areAtWar(factionA, factionB);
	}

	/**
	 * Vanilla dye name for steel and iron samurai armor, or an empty string when this entity has no faction color.
	 * Players use their own faction. A nomosik uses its own color override, then its owner's faction.
	 */
	public static String armorDyeName(MinecraftServer server, Entity entity) {
		FactionColor color = armorColor(server, entity);
		return color == null ? "" : color.toVanillaDye().getName();
	}

	/**
	 * Dye RGB for steel and iron samurai armor, or {@code -1} when this entity has no faction color.
	 * Players use their own faction. A nomosik uses its own color override, then its owner's faction.
	 */
	public static int armorDyeRgb(MinecraftServer server, Entity entity) {
		FactionColor color = armorColor(server, entity);
		return color == null ? -1 : color.toVanillaDye().getTextureDiffuseColor();
	}

	private static FactionColor armorColor(MinecraftServer server, Entity entity) {
		if (server == null || entity == null) {
			return null;
		}
		if (entity instanceof ServerPlayer player) {
			return FactionService.get(server).getFactionColorByOwner(player.getUUID());
		}
		if (entity instanceof NomosikEntity nomosik) {
			FactionColor color = nomosik.getFactionColorOverride();
			if (color != null) {
				return color;
			}
			UUID wearer = nomosik.getOwnerUUID();
			return wearer == null ? null : FactionService.get(server).getFactionColorByOwner(wearer);
		}
		return null;
	}

	/** Banner color id of a faction, or an empty string when the faction is missing. */
	public static String factionColorId(MinecraftServer server, String factionName) {
		if (server == null || factionName == null || factionName.isEmpty()) {
			return "";
		}
		return FactionService.get(server).getFaction(factionName)
			.map(faction -> faction.color().id())
			.orElse("");
	}

	/** Nametag RGB of a faction, or {@code -1} when the faction is missing. */
	public static int factionColorRgb(MinecraftServer server, String factionName) {
		if (server == null || factionName == null || factionName.isEmpty()) {
			return -1;
		}
		return FactionService.get(server).getFaction(factionName)
			.map(faction -> faction.color().nameRgb())
			.orElse(-1);
	}

	/** One dye of the faction banner color, or an empty stack when the faction is missing. */
	public static ItemStack factionDye(MinecraftServer server, String factionName) {
		if (server == null || factionName == null || factionName.isEmpty()) {
			return ItemStack.EMPTY;
		}
		return FactionService.get(server).getFaction(factionName)
			.map(faction -> new ItemStack(DyeItem.byColor(faction.color().toVanillaDye())))
			.orElse(ItemStack.EMPTY);
	}

	/** A copy of the faction banner, including its patterns, or an empty stack when the faction is missing. */
	public static ItemStack factionBanner(MinecraftServer server, String factionName) {
		if (server == null || factionName == null || factionName.isEmpty()) {
			return ItemStack.EMPTY;
		}
		return FactionService.get(server).getFaction(factionName)
			.map(faction -> FactionBannerHelper.bannerStackForDisplay(
				faction.bannerNbt(),
				faction.color(),
				server.registryAccess()
			))
			.orElse(ItemStack.EMPTY);
	}

	/**
	 * Current faction membership and squad sizes for the stats collector.
	 * NPC count is the number of fighters listed in that player's squads, not loose units.
	 * Call on the server thread.
	 */
	public static String membershipSnapshot(MinecraftServer server) {
		JsonObject root = new JsonObject();
		JsonArray factions = new JsonArray();
		JsonArray unaffiliated = new JsonArray();
		root.add("factions", factions);
		root.add("unaffiliated", unaffiliated);
		if (server == null || server.overworld() == null) {
			return root.toString();
		}

		FactionSavedData factionData = server.overworld().getDataStorage()
			.computeIfAbsent(FactionSavedData.factory(), FactionSavedData.DATA_ID);
		SquadSavedData squadData = server.overworld().getDataStorage()
			.computeIfAbsent(SquadSavedData.factory(), SquadSavedData.DATA_ID);

		Map<UUID, Integer> npcByCommander = new HashMap<>();
		for (NomosikSquad squad : squadData.squadsView().values()) {
			int members = squad.memberCount();
			if (members <= 0 || squad.commanderId() == null) {
				continue;
			}
			npcByCommander.merge(squad.commanderId(), members, NomosikiFactionApi::addSaturated);
		}

		Set<UUID> inFaction = new HashSet<>();
		for (FactionData faction : factionData.factions.values()) {
			if (faction.name() == null || faction.name().isBlank() || faction.owner() == null) {
				continue;
			}
			JsonObject row = new JsonObject();
			row.addProperty("name", faction.name());
			row.addProperty("leaderUuid", faction.owner().toString());
			JsonArray members = new JsonArray();
			for (UUID member : faction.members()) {
				if (member == null) {
					continue;
				}
				inFaction.add(member);
				JsonObject memberRow = new JsonObject();
				memberRow.addProperty("uuid", member.toString());
				memberRow.addProperty("npcCount", npcByCommander.getOrDefault(member, 0));
				members.add(memberRow);
			}
			row.add("members", members);
			factions.add(row);
		}

		for (Map.Entry<UUID, Integer> entry : npcByCommander.entrySet()) {
			if (inFaction.contains(entry.getKey()) || entry.getValue() <= 0) {
				continue;
			}
			JsonObject row = new JsonObject();
			row.addProperty("uuid", entry.getKey().toString());
			row.addProperty("npcCount", entry.getValue());
			unaffiliated.add(row);
		}
		return root.toString();
	}

	private static int addSaturated(int left, int right) {
		long sum = (long) left + right;
		if (sum > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		return (int) sum;
	}

	public static void notifyCapture(
		MinecraftServer server,
		String attackerFaction,
		String defenderFaction,
		String cityName,
		boolean started
	) {
		DiscordDiplomacyNotifier.get().notify(DiplomacyDiscordEvent.notice(
			started ? DiplomacyDiscordEvent.Type.CAPTURE_STARTED : DiplomacyDiscordEvent.Type.CAPTURE_COMPLETED,
			attackerFaction,
			defenderFaction,
			null,
			cityName == null ? "" : cityName
		));
	}
}
