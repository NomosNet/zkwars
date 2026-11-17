package com.nomosiki.faction;

import com.nomosiki.advancement.NomosikiActions;
import com.nomosiki.advancement.NomosikiAdvancements;
import com.nomosiki.compat.items.FactionArmorCompat;
import com.nomosiki.discord.DiscordPoliticsBoard;
import com.nomosiki.network.faction.FactionDiplomacyRow;
import com.nomosiki.network.faction.FactionRelationEdge;
import com.nomosiki.network.faction.payload.s2c.FactionRenderSyncPayload;
import com.nomosiki.network.faction.payload.s2c.FactionScreenDataPayload;
import com.nomosiki.screen.FactionCreateMenu;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class FactionManager {
	private final MinecraftServer server;
	private final FactionSavedData data;
	private final FactionWarSavedData warData;
	private final FactionDiplomacySavedData diplomacyData;
	private final Map<UUID, Map<UUID, Byte>> testerStancesByViewer = new HashMap<>();

	public record InviteView(String factionName, String ownerName, String colorId) {
	}

	public FactionManager(MinecraftServer server) {
		this.server = server;
		this.data = server.overworld().getDataStorage().computeIfAbsent(FactionSavedData.factory(), FactionSavedData.DATA_ID);
		this.warData = server.overworld().getDataStorage().computeIfAbsent(FactionWarSavedData.factory(), FactionWarSavedData.DATA_ID);
		this.diplomacyData = server.overworld().getDataStorage().computeIfAbsent(
			FactionDiplomacySavedData.factory(), FactionDiplomacySavedData.DATA_ID);
		if (!this.diplomacyData.isMigratedFromWarData()) {
			this.diplomacyData.migrateFromWarData(this.warData);
		}
		this.migrateFactionBanners();
	}

	private void migrateFactionBanners() {
		var registries = this.server.registryAccess();
		boolean dirty = false;
		for (FactionData faction : this.data.factions.values()) {
			CompoundTag banner = faction.bannerNbt();
			if (banner == null || banner.isEmpty() || FactionBannerHelper.isPortableFormat(banner)) {
				continue;
			}
			ItemStack stack = FactionBannerHelper.stackFromStored(banner, registries);
			if (FactionBannerHelper.hasPatterns(stack)) {
				faction.bannerNbt(FactionBannerHelper.serializePortable(stack));
				dirty = true;
			}
		}
		if (dirty) {
			this.data.setDirty();
		}
	}

	public FactionDiplomacySavedData diplomacyData() {
		return this.diplomacyData;
	}

	/** @deprecated Legacy war storage; relations live in {@link #diplomacyData()}. */
	@Deprecated
	public FactionWarSavedData warData() {
		return this.warData;
	}

	public boolean areFactionsAtWar(String factionA, String factionB) {
		return this.diplomacyData.getRelation(factionA, factionB) == FactionRelationStatus.AT_WAR;
	}

	public boolean areFactionsAllied(String factionA, String factionB) {
		if (factionA == null || factionB == null || factionA.isBlank() || factionB.isBlank()) {
			return false;
		}
		if (factionA.equalsIgnoreCase(factionB)) {
			return true;
		}
		return this.diplomacyData.getRelation(factionA, factionB).isAlliance();
	}

	public void putTesterStances(UUID viewerId, Map<UUID, Byte> extras) {
		if (viewerId == null) {
			return;
		}
		if (extras == null || extras.isEmpty()) {
			this.testerStancesByViewer.remove(viewerId);
			return;
		}
		this.testerStancesByViewer.put(viewerId, new HashMap<>(extras));
	}

	public void sendRenderSyncTo(ServerPlayer player) {
		ServerPlayNetworking.send(player, new FactionRenderSyncPayload(buildRenderColorMap(), buildStanceMap(player)));
	}

	public void sendRenderSyncToAll() {
		Map<UUID, String> colors = buildRenderColorMap();
		for (ServerPlayer player : this.server.getPlayerList().getPlayers()) {
			ServerPlayNetworking.send(player, new FactionRenderSyncPayload(colors, buildStanceMap(player)));
		}
	}

	public void sendScreenData(ServerPlayer player) {
		ServerPlayNetworking.send(player, buildScreenPayload(player));
	}

	public FactionScreenDataPayload buildScreenPayload(ServerPlayer player) {
		UUID playerId = player.getUUID();
		String factionName = this.data.playerToFaction.get(playerId);
		String myColor = "";
		ItemStack myBanner = ItemStack.EMPTY;
		boolean isOwner = false;
		List<String> members = new ArrayList<>();
		if (factionName != null) {
			FactionData faction = this.data.factions.get(factionName);
			if (faction != null) {
				myColor = faction.color().id();
				myBanner = FactionBannerHelper.bannerStackForDisplay(faction.bannerNbt(), faction.color(), player.registryAccess());
				isOwner = playerId.equals(faction.owner());
				for (UUID member : faction.members()) {
					members.add(resolvePlayerName(member));
				}
				members.sort(String::compareToIgnoreCase);
			}
		}
		List<InviteView> invites = new ArrayList<>();
		for (String inviteFaction : this.data.invites.getOrDefault(playerId, Set.of())) {
			FactionData faction = this.data.factions.get(inviteFaction);
			if (faction == null) {
				continue;
			}
			invites.add(new InviteView(
				inviteFaction,
				resolvePlayerName(faction.owner()),
				faction.color().id()));
		}
		invites.sort(Comparator.comparing(InviteView::factionName, String.CASE_INSENSITIVE_ORDER));
		List<FactionDiplomacyRow> diplomacy = new ArrayList<>();
		for (String otherName : listOtherFactions(factionName)) {
			FactionData other = this.data.factions.get(otherName);
			if (other == null) {
				continue;
			}
			int otherMembers = other.members().size();
			if (factionName == null) {
				diplomacy.add(new FactionDiplomacyRow(
					otherName,
					other.color().id(),
					FactionBannerHelper.bannerStackForDisplay(other.bannerNbt(), other.color(), player.registryAccess()),
					FactionRelationStatus.NEUTRAL.id(),
					false,
					false,
					otherMembers
				));
				continue;
			}
			FactionRelationStatus relation = this.diplomacyData.getRelation(factionName, otherName);
			FactionAlliancePending pending = this.diplomacyData.getPendingAlliance(factionName, otherName);
			byte pendingTier = pending == null ? FactionAllianceTier.REGULAR.id() : pending.tier().id();
			boolean incoming = this.diplomacyData.hasIncomingAllianceRequest(factionName, otherName);
			boolean outgoing = this.diplomacyData.hasOutgoingAllianceRequest(factionName, otherName);
			boolean warJoin = this.diplomacyData.findWarJoinPending(factionName, otherName) != null;
			var sideChoice = this.diplomacyData.findWarSideChoicePending(factionName, otherName);
			boolean warSide = sideChoice != null;
			String sideDefender = sideChoice == null ? "" : sideChoice.defenderFaction();
			boolean peaceIncoming = this.diplomacyData.hasIncomingPeace(factionName, otherName);
			boolean peaceOutgoing = this.diplomacyData.hasOutgoingPeace(factionName, otherName);
			diplomacy.add(new FactionDiplomacyRow(
				otherName,
				other.color().id(),
				FactionBannerHelper.bannerStackForDisplay(other.bannerNbt(), other.color(), player.registryAccess()),
				relation.id(),
				incoming,
				outgoing,
				incoming || outgoing ? pendingTier : FactionAllianceTier.REGULAR.id(),
				warJoin,
				warSide,
				sideDefender,
				otherMembers,
				peaceIncoming,
				peaceOutgoing
			));
		}
		List<FactionRelationEdge> relationEdges = new ArrayList<>();
		for (Map.Entry<FactionWarPair, FactionRelationStatus> entry : this.diplomacyData.nonNeutralRelations().entrySet()) {
			FactionWarPair pair = entry.getKey();
			relationEdges.add(new FactionRelationEdge(pair.factionA(), pair.factionB(), entry.getValue().id()));
		}
		return new FactionScreenDataPayload(
			factionName == null ? "" : factionName,
			myColor,
			myBanner,
			isOwner,
			members,
			invites,
			diplomacy,
			relationEdges
		);
	}

	public void sendScreenDataToFactionMembers(String factionName) {
		this.getFaction(factionName).ifPresent(faction -> {
			for (UUID memberId : faction.members()) {
				ServerPlayer online = this.server.getPlayerList().getPlayer(memberId);
				if (online != null) {
					sendScreenData(online);
				}
			}
		});
	}

	public String createFaction(ServerPlayer owner, String rawName, FactionCreateMenu menu) {
		String name = sanitizeName(rawName);
		if (name.isEmpty()) {
			return "message.nomosiki.faction.invalid_name";
		}
		if (this.data.playerToFaction.containsKey(owner.getUUID())) {
			return "message.nomosiki.faction.already_in_faction";
		}
		if (this.findFactionIgnoreCase(name) != null) {
			return "message.nomosiki.faction.exists";
		}
		ItemStack banner = menu.getBannerStack();
		if (banner.isEmpty() || !(banner.getItem() instanceof BannerItem)) {
			return "message.nomosiki.faction.nobanner";
		}
		if (FactionBannerHelper.isBlank(banner)) {
			return "message.nomosiki.faction.banner_no_patterns";
		}
		CompoundTag bannerNbt = FactionBannerHelper.serialize(banner, owner.registryAccess());
		if (isBannerInUse(bannerNbt)) {
			return "message.nomosiki.faction.banner_exists";
		}
		FactionColor color = FactionColor.fromDye(FactionBannerHelper.getBaseColor(banner));
		FactionData faction = new FactionData(name, owner.getUUID(), color, bannerNbt);
		menu.consumeBanner();
		this.data.factions.put(name, faction);
		this.data.playerToFaction.put(owner.getUUID(), name);
		this.data.invites.remove(owner.getUUID());
		this.data.setDirty();
		sendRenderSyncToAll();
		broadcastTabName(owner);
		FactionArmorCompat.refreshLoaded(this.server);
		NomosikiAdvancements.grant(owner, NomosikiActions.FACTION_CREATED);
		return "";
	}

	public boolean isBannerInUse(CompoundTag bannerNbt) {
		if (bannerNbt == null || bannerNbt.isEmpty()) {
			return false;
		}
		for (FactionData faction : this.data.factions.values()) {
			if (FactionBannerHelper.isSameBanner(bannerNbt, faction.bannerNbt())) {
				return true;
			}
		}
		return false;
	}

	public String invite(ServerPlayer owner, String targetName) {
		String ownerFactionName = this.data.playerToFaction.get(owner.getUUID());
		if (ownerFactionName == null) {
			return "message.nomosiki.faction.no_faction";
		}
		FactionData faction = this.data.factions.get(ownerFactionName);
		if (faction == null) {
			return "message.nomosiki.faction.no_faction";
		}
		if (!owner.getUUID().equals(faction.owner())) {
			return "message.nomosiki.faction.not_owner";
		}
		ServerPlayer target = this.server.getPlayerList().getPlayerByName(targetName);
		if (target == null) {
			return "message.nomosiki.faction.player_not_found";
		}
		UUID targetId = target.getUUID();
		if (this.data.playerToFaction.containsKey(targetId)) {
			return "message.nomosiki.faction.target_has_faction";
		}
		if (targetId.equals(owner.getUUID())) {
			return "message.nomosiki.faction.cannot_invite_self";
		}
		Set<String> targetInvites = this.data.invites.computeIfAbsent(targetId, ignored -> new HashSet<>());
		if (!targetInvites.add(ownerFactionName)) {
			return "message.nomosiki.faction.invite_already_sent";
		}
		this.data.setDirty();
		sendInviteNotice(target, ownerFactionName, owner.getName());
		sendScreenData(target);
		return "";
	}

	public List<String> pendingInvites(UUID playerId) {
		Set<String> invites = this.data.invites.get(playerId);
		if (invites == null || invites.isEmpty()) {
			return List.of();
		}
		List<String> names = new ArrayList<>();
		for (String name : invites) {
			if (this.data.factions.containsKey(name)) {
				names.add(name);
			}
		}
		names.sort(String.CASE_INSENSITIVE_ORDER);
		return names;
	}

	public String respondInvite(ServerPlayer player, String factionName, boolean accept) {
		Set<String> invites = this.data.invites.get(player.getUUID());
		if (invites == null) {
			return "message.nomosiki.faction.invite_not_found";
		}
		String resolved = resolvePendingInvite(invites, factionName);
		if (resolved == null) {
			return "message.nomosiki.faction.invite_not_found";
		}
		if (accept) {
			if (this.data.playerToFaction.containsKey(player.getUUID())) {
				return "message.nomosiki.faction.already_in_faction";
			}
			FactionData faction = this.data.factions.get(resolved);
			if (faction == null) {
				return "message.nomosiki.faction.invite_not_found";
			}
			this.data.invites.remove(player.getUUID());
			faction.members().add(player.getUUID());
			this.data.playerToFaction.put(player.getUUID(), resolved);
			player.displayClientMessage(Component.translatable("message.nomosiki.faction.joined", resolved), false);
			NomosikiAdvancements.grant(player, NomosikiActions.FACTION_JOINED);
			this.data.setDirty();
			sendRenderSyncToAll();
			broadcastTabName(player);
			FactionArmorCompat.refreshLoaded(this.server);
			return "";
		}
		invites.remove(resolved);
		if (invites.isEmpty()) {
			this.data.invites.remove(player.getUUID());
		}
		this.data.setDirty();
		player.displayClientMessage(Component.translatable("message.nomosiki.faction.invite_declined", resolved), false);
		return "";
	}

	/** @return a translation key on failure, or an empty string on success */
	public String leaveFaction(ServerPlayer player) {
		if (WartimeService.get(this.server).isActive()) {
			return "message.nomosiki.faction.leave_wartime";
		}
		String factionName = this.data.playerToFaction.remove(player.getUUID());
		if (factionName == null) {
			return "message.nomosiki.faction.no_faction";
		}
		FactionData faction = this.data.factions.get(factionName);
		if (faction == null) {
			this.data.setDirty();
			sendRenderSyncToAll();
			broadcastTabName(player);
			FactionArmorCompat.refreshLoaded(this.server);
			return "";
		}
		faction.members().remove(player.getUUID());
		List<ServerPlayer> tabRefresh = new ArrayList<>();
		tabRefresh.add(player);
		if (faction.members().isEmpty() || faction.owner().equals(player.getUUID())) {
			Set<UUID> formerMembers = new HashSet<>(faction.members());
			formerMembers.add(player.getUUID());
			for (UUID member : List.copyOf(faction.members())) {
				this.data.playerToFaction.remove(member);
				ServerPlayer online = this.server.getPlayerList().getPlayer(member);
				if (online != null) {
					online.displayClientMessage(Component.translatable("message.nomosiki.faction.disbanded", factionName), false);
					sendScreenData(online);
					tabRefresh.add(online);
				}
			}
			this.data.factions.remove(factionName);
			dropInvitesForFaction(factionName, formerMembers);
			this.diplomacyData.removeFaction(factionName);
			this.warData.removeFaction(factionName);
			WartimeService.get(this.server).onFactionRemoved(factionName);
			DiscordPoliticsBoard.refresh(this.server);
		} else {
			String leaverName = player.getGameProfile().getName();
			for (UUID member : faction.members()) {
				ServerPlayer online = this.server.getPlayerList().getPlayer(member);
				if (online != null) {
					online.displayClientMessage(Component.translatable("message.nomosiki.faction.member_left", leaverName), false);
					sendScreenData(online);
				}
			}
		}
		this.data.setDirty();
		sendRenderSyncToAll();
		for (ServerPlayer online : tabRefresh) {
			broadcastTabName(online);
		}
		FactionArmorCompat.refreshLoaded(this.server);
		return "";
	}

	/**
	 * Online player, or a name already stored in the server profile cache.
	 * Accounts that have never joined this server are unknown.
	 */
	@Nullable
	public UUID resolvePlayerId(String name) {
		if (name == null || name.isBlank()) {
			return null;
		}
		String trimmed = name.trim();
		ServerPlayer online = this.server.getPlayerList().getPlayerByName(trimmed);
		if (online != null) {
			return online.getUUID();
		}
		var cached = this.server.getProfileCache().get(trimmed);
		return cached.map(profile -> profile.getId()).orElse(null);
	}

	public boolean isPlayerOnline(UUID playerId) {
		return this.server.getPlayerList().getPlayer(playerId) != null;
	}

	public List<FactionData> allFactions() {
		List<FactionData> list = new ArrayList<>(this.data.factions.values());
		list.sort(Comparator.comparing(FactionData::name, String.CASE_INSENSITIVE_ORDER));
		return list;
	}

	/** @return a translation key on failure, or an empty string on success */
	public String adminSetLeader(String rawFaction, UUID newLeader, String actorName) {
		String factionName = findFactionIgnoreCase(rawFaction);
		if (factionName == null || newLeader == null) {
			return factionName == null
				? "message.nomosiki.faction.war_enemy_not_found"
				: "message.nomosiki.faction.admin.player_not_found";
		}
		FactionData faction = this.data.factions.get(factionName);
		if (faction.owner().equals(newLeader)) {
			return "message.nomosiki.faction.admin.already_leader";
		}
		String current = this.data.playerToFaction.get(newLeader);
		if (current != null && !current.equalsIgnoreCase(factionName)) {
			FactionData other = this.data.factions.get(current);
			if (other != null && other.owner().equals(newLeader) && other.members().size() > 1) {
				return "message.nomosiki.faction.admin.leader_busy";
			}
			String removed = removeMemberForAdmin(newLeader, true);
			if (!removed.isEmpty()) {
				return removed;
			}
		}
		this.data.invites.remove(newLeader);
		faction.members().add(newLeader);
		this.data.playerToFaction.put(newLeader, factionName);
		faction.owner(newLeader);
		this.data.setDirty();
		notifyMembers(faction, Component.translatable(
			"message.nomosiki.faction.admin.leader_set",
			memberDisplayName(newLeader),
			factionName,
			actorName
		));
		finishMembershipChange(newLeader);
		return "";
	}

	/** @return a translation key on failure, or an empty string on success */
	public String adminMove(UUID playerId, String rawFaction, String actorName) {
		if (playerId == null) {
			return "message.nomosiki.faction.admin.player_not_found";
		}
		String targetName = findFactionIgnoreCase(rawFaction);
		if (targetName == null) {
			return "message.nomosiki.faction.war_enemy_not_found";
		}
		String current = this.data.playerToFaction.get(playerId);
		if (current == null) {
			return "message.nomosiki.faction.no_faction";
		}
		if (current.equalsIgnoreCase(targetName)) {
			return "message.nomosiki.faction.admin.already_member";
		}
		FactionData source = this.data.factions.get(current);
		if (source != null && source.owner().equals(playerId) && source.members().size() > 1) {
			return "message.nomosiki.faction.admin.leader_has_members";
		}
		String removed = removeMemberForAdmin(playerId, true);
		if (!removed.isEmpty()) {
			return removed;
		}
		return placeMember(playerId, targetName, Component.translatable(
			"message.nomosiki.faction.admin.moved",
			memberDisplayName(playerId),
			targetName,
			actorName
		));
	}

	/** @return a translation key on failure, or an empty string on success */
	public String adminAdd(UUID playerId, String rawFaction, String actorName) {
		if (playerId == null) {
			return "message.nomosiki.faction.admin.player_not_found";
		}
		if (this.data.playerToFaction.containsKey(playerId)) {
			return "message.nomosiki.faction.already_in_faction";
		}
		String targetName = findFactionIgnoreCase(rawFaction);
		if (targetName == null) {
			return "message.nomosiki.faction.war_enemy_not_found";
		}
		return placeMember(playerId, targetName, Component.translatable(
			"message.nomosiki.faction.admin.added",
			memberDisplayName(playerId),
			targetName,
			actorName
		));
	}

	/** @return a translation key on failure, or an empty string on success */
	public String adminKick(UUID playerId, String actorName) {
		if (playerId == null) {
			return "message.nomosiki.faction.admin.player_not_found";
		}
		String current = this.data.playerToFaction.get(playerId);
		if (current == null) {
			return "message.nomosiki.faction.no_faction";
		}
		FactionData faction = this.data.factions.get(current);
		if (faction != null && faction.owner().equals(playerId)) {
			return "message.nomosiki.faction.admin.cannot_kick_leader";
		}
		String label = memberDisplayName(playerId);
		if (faction != null) {
			faction.members().remove(playerId);
		}
		this.data.playerToFaction.remove(playerId);
		this.data.setDirty();
		if (faction != null) {
			notifyMembers(faction, Component.translatable("message.nomosiki.faction.admin.kicked", label, actorName));
		}
		ServerPlayer kicked = this.server.getPlayerList().getPlayer(playerId);
		if (kicked != null) {
			kicked.displayClientMessage(Component.translatable("message.nomosiki.faction.admin.kicked_you", current, actorName), false);
			sendScreenData(kicked);
		}
		finishMembershipChange(playerId);
		return "";
	}

	/** @return a translation key on failure, or an empty string on success */
	public String adminDisband(String rawFaction, String actorName) {
		String factionName = findFactionIgnoreCase(rawFaction);
		if (factionName == null) {
			return "message.nomosiki.faction.war_enemy_not_found";
		}
		FactionData faction = this.data.factions.get(factionName);
		List<UUID> members = new ArrayList<>(faction.members());
		disbandNow(faction, Component.translatable("message.nomosiki.faction.admin.disbanded", factionName, actorName));
		sendRenderSyncToAll();
		for (UUID memberId : members) {
			ServerPlayer online = this.server.getPlayerList().getPlayer(memberId);
			if (online != null) {
				broadcastTabName(online);
			}
		}
		FactionArmorCompat.refreshLoaded(this.server);
		return "";
	}

	/**
	 * @param disbandIfSoleOwner when the player is the only member, dissolve that faction
	 * @return a translation key on failure, or an empty string on success
	 */
	private String removeMemberForAdmin(UUID playerId, boolean disbandIfSoleOwner) {
		String current = this.data.playerToFaction.get(playerId);
		if (current == null) {
			return "";
		}
		FactionData faction = this.data.factions.get(current);
		if (faction == null) {
			this.data.playerToFaction.remove(playerId);
			this.data.setDirty();
			return "";
		}
		boolean owner = faction.owner().equals(playerId);
		if (owner && faction.members().size() > 1) {
			return "message.nomosiki.faction.admin.leader_has_members";
		}
		if (owner || faction.members().size() <= 1) {
			if (!disbandIfSoleOwner && !owner) {
				faction.members().remove(playerId);
				this.data.playerToFaction.remove(playerId);
				this.data.setDirty();
				return "";
			}
			disbandNow(faction, Component.translatable(
				"message.nomosiki.faction.admin.disbanded",
				faction.name(),
				memberDisplayName(playerId)
			));
			return "";
		}
		faction.members().remove(playerId);
		this.data.playerToFaction.remove(playerId);
		this.data.setDirty();
		notifyMembers(faction, Component.translatable("message.nomosiki.faction.member_left", memberDisplayName(playerId)));
		return "";
	}

	private String placeMember(UUID playerId, String factionName, Component notice) {
		FactionData faction = this.data.factions.get(factionName);
		if (faction == null) {
			return "message.nomosiki.faction.war_enemy_not_found";
		}
		this.data.invites.remove(playerId);
		faction.members().add(playerId);
		this.data.playerToFaction.put(playerId, factionName);
		this.data.setDirty();
		notifyMembers(faction, notice);
		finishMembershipChange(playerId);
		NomosikiAdvancements.grant(this.server, playerId, NomosikiActions.FACTION_JOINED);
		return "";
	}

	private void disbandNow(FactionData faction, Component notice) {
		String factionName = faction.name();
		Set<UUID> formerMembers = new HashSet<>(faction.members());
		for (UUID member : List.copyOf(faction.members())) {
			this.data.playerToFaction.remove(member);
			ServerPlayer online = this.server.getPlayerList().getPlayer(member);
			if (online != null) {
				online.displayClientMessage(notice, false);
				sendScreenData(online);
			}
		}
		this.data.factions.remove(factionName);
		dropInvitesForFaction(factionName, formerMembers);
		this.diplomacyData.removeFaction(factionName);
		this.warData.removeFaction(factionName);
		WartimeService.get(this.server).onFactionRemoved(factionName);
		DiscordPoliticsBoard.refresh(this.server);
		this.data.setDirty();
	}

	private void notifyMembers(FactionData faction, Component notice) {
		for (UUID member : faction.members()) {
			ServerPlayer online = this.server.getPlayerList().getPlayer(member);
			if (online != null) {
				online.displayClientMessage(notice, false);
				sendScreenData(online);
			}
		}
	}

	private void finishMembershipChange(UUID playerId) {
		sendRenderSyncToAll();
		ServerPlayer online = this.server.getPlayerList().getPlayer(playerId);
		if (online != null) {
			broadcastTabName(online);
		}
		FactionArmorCompat.refreshLoaded(this.server);
	}

	public Component tabListName(ServerPlayer player) {
		String name = player.getGameProfile().getName();
		FactionColor color = getFactionColorByOwner(player.getUUID());
		if (color == null) {
			return Component.literal("!").withStyle(ChatFormatting.RED).append(Component.literal(name));
		}
		return Component.literal(name).withColor(color.nameRgb());
	}

	public void broadcastTabName(ServerPlayer player) {
		this.server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(
			ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
			player
		));
	}

	public FactionColor getFactionColorByOwner(UUID ownerUuid) {
		String factionName = this.data.playerToFaction.get(ownerUuid);
		if (factionName == null) {
			return null;
		}
		FactionData faction = this.data.factions.get(factionName);
		return faction == null ? null : faction.color();
	}

	@Nullable
	public String getPlayerFaction(UUID playerId) {
		return this.data.playerToFaction.get(playerId);
	}

	/**
	 * Nickname from the online list or the server profile cache.
	 * Falls back to the full UUID when the name is unknown, so an operator can still identify the account.
	 */
	public String memberDisplayName(UUID playerId) {
		ServerPlayer online = this.server.getPlayerList().getPlayer(playerId);
		if (online != null) {
			return online.getGameProfile().getName();
		}
		var profile = this.server.getProfileCache().get(playerId);
		if (profile.isPresent()) {
			return profile.get().getName();
		}
		return playerId.toString();
	}

	@Nullable
	public String resolveFactionName(String name) {
		return findFactionIgnoreCase(name);
	}

	public Optional<FactionData> getFaction(String name) {
		String resolved = findFactionIgnoreCase(name);
		if (resolved == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(this.data.factions.get(resolved));
	}

	public List<String> listOtherFactions(String myFaction) {
		List<String> result = new ArrayList<>();
		for (String name : this.data.factions.keySet()) {
			if (myFaction == null || !name.equalsIgnoreCase(myFaction)) {
				result.add(name);
			}
		}
		result.sort(String::compareToIgnoreCase);
		return result;
	}

	private Map<UUID, String> buildRenderColorMap() {
		Map<UUID, String> map = new HashMap<>();
		for (Map.Entry<UUID, String> entry : this.data.playerToFaction.entrySet()) {
			FactionData faction = this.data.factions.get(entry.getValue());
			if (faction != null) {
				map.put(entry.getKey(), faction.color().id());
			}
		}
		return map;
	}

	private Map<UUID, Byte> buildStanceMap(ServerPlayer viewer) {
		Map<UUID, Byte> map = new HashMap<>();
		UUID viewerId = viewer.getUUID();
		String myFaction = this.data.playerToFaction.get(viewerId);
		for (Map.Entry<UUID, String> entry : this.data.playerToFaction.entrySet()) {
			UUID otherId = entry.getKey();
			if (otherId.equals(viewerId)) {
				continue;
			}
			String theirFaction = entry.getValue();
			SquadBannerStance stance;
			if (myFaction != null && myFaction.equalsIgnoreCase(theirFaction)) {
				stance = SquadBannerStance.FACTION;
			} else if (myFaction == null || theirFaction == null || theirFaction.isBlank()) {
				stance = SquadBannerStance.NEUTRAL;
			} else {
				stance = SquadBannerStance.fromRelation(this.diplomacyData.getRelation(myFaction, theirFaction));
			}
			map.put(otherId, stance.id());
		}
		Map<UUID, Byte> extras = this.testerStancesByViewer.get(viewerId);
		if (extras != null) {
			map.putAll(extras);
		}
		return map;
	}

	private void sendInviteNotice(ServerPlayer target, String factionName, Component ownerName) {
		target.displayClientMessage(
			Component.translatable("message.nomosiki.faction.invited", factionName, ownerName),
			false
		);
		String joinCommand = "/faction join " + factionName;
		String declineCommand = "/faction decline " + factionName;
		Component join = Component.translatable("message.nomosiki.faction.invite_join").withStyle(style -> style
			.withColor(ChatFormatting.GREEN)
			.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, joinCommand))
			.withHoverEvent(new HoverEvent(
				HoverEvent.Action.SHOW_TEXT,
				Component.translatable("message.nomosiki.faction.invite_join_hover")
			)));
		Component decline = Component.translatable("message.nomosiki.faction.invite_decline").withStyle(style -> style
			.withColor(ChatFormatting.RED)
			.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, declineCommand))
			.withHoverEvent(new HoverEvent(
				HoverEvent.Action.SHOW_TEXT,
				Component.translatable("message.nomosiki.faction.invite_decline_hover")
			)));
		target.displayClientMessage(Component.translatable("message.nomosiki.faction.invite_actions", join, decline), false);
		target.displayClientMessage(Component.translatable("message.nomosiki.faction.invite_open_hint"), false);
	}

	@Nullable
	private static String resolvePendingInvite(@Nullable Set<String> invites, String factionName) {
		if (invites == null || factionName == null) {
			return null;
		}
		String trimmed = factionName.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		for (String name : invites) {
			if (name.equalsIgnoreCase(trimmed)) {
				return name;
			}
		}
		return null;
	}

	private void dropInvitesForFaction(String factionName, Set<UUID> formerMembers) {
		List<UUID> notify = new ArrayList<>();
		var iterator = this.data.invites.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, Set<String>> entry = iterator.next();
			if (!entry.getValue().remove(factionName)) {
				continue;
			}
			if (entry.getValue().isEmpty()) {
				iterator.remove();
			}
			if (!formerMembers.contains(entry.getKey())) {
				notify.add(entry.getKey());
			}
		}
		for (UUID playerId : notify) {
			if (this.data.playerToFaction.containsKey(playerId)) {
				continue;
			}
			ServerPlayer online = this.server.getPlayerList().getPlayer(playerId);
			if (online == null) {
				continue;
			}
			online.displayClientMessage(Component.translatable("message.nomosiki.faction.invite_expired", factionName), false);
			sendScreenData(online);
		}
	}

	private String resolvePlayerName(UUID playerId) {
		ServerPlayer online = this.server.getPlayerList().getPlayer(playerId);
		if (online != null) {
			return online.getGameProfile().getName();
		}
		var profile = this.server.getProfileCache().get(playerId);
		if (profile.isPresent()) {
			return profile.get().getName();
		}
		String raw = playerId.toString();
		return raw.substring(0, Math.min(8, raw.length()));
	}

	private String sanitizeName(String raw) {
		if (raw == null) {
			return "";
		}
		String result = raw.replace("\u00A7", "").trim();
		if (result.length() > 20) {
			result = result.substring(0, 20);
		}
		return result;
	}

	private String findFactionIgnoreCase(String name) {
		if (name == null || name.isBlank()) {
			return null;
		}
		String trimmed = name.trim();
		for (String existing : this.data.factions.keySet()) {
			if (existing.equalsIgnoreCase(trimmed)) {
				return existing;
			}
		}
		return null;
	}
}
