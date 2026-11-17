package com.nomosiki.squad;

import com.nomosiki.advancement.NomosikiActions;
import com.nomosiki.advancement.NomosikiAdvancements;
import com.nomosiki.compat.items.FactionArmorCompat;
import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.entity.SquadOrder;
import com.nomosiki.faction.FactionService;
import com.nomosiki.network.squad.SquadNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class SquadManager {
	/** Matches {@code SquadIcons.IMAGES} size on the client; server clamps stored {@link NomosikSquad#iconId()} to this range. */
	public static final int SQUAD_ICON_COUNT = 16;

	private final MinecraftServer server;
	private final SquadSavedData data;

	public SquadManager(MinecraftServer server) {
		this.server = server;
		this.data = server.overworld().getDataStorage().computeIfAbsent(SquadSavedData.factory(), SquadSavedData.DATA_ID);
	}

	public SquadSavedData data() {
		return this.data;
	}

	public boolean isCommanderOf(ServerPlayer player, UUID squadId) {
		Optional<NomosikSquad> s = this.data.getSquad(squadId);
		return s.filter(nomosikSquad -> nomosikSquad.commanderId().equals(player.getUUID())).isPresent();
	}

	public UUID createSquad(ServerPlayer commander, SquadType type, String name, byte iconId) {
		UUID id = UUID.randomUUID();
		NomosikSquad squad = new NomosikSquad(id, commander.getUUID(), type, new ArrayList<>(), false, name, clampSquadIcon(iconId));
		this.data.putSquad(squad);
		this.data.setActiveSquad(commander.getUUID(), id);
		return id;
	}

	/**
	 * Creates a new squad with optional display name. Empty or whitespace-only {@code rawName} yields
	 * {@code <EnumName>-<first UUID segment>}. Validates non-empty names (length, allowed code points).
	 *
	 * @return empty on success, or a translation key such as {@code message.nomosiki.squad.bad_name}
	 */
	public Optional<String> createNamedSquad(ServerPlayer commander, String rawName, byte iconId, SquadType type) {
		String trimmed = rawName == null ? "" : rawName.trim();
		if (!trimmed.isEmpty() && (trimmed.length() > 32 || !isAllowedSquadName(trimmed))) {
			return Optional.of("message.nomosiki.squad.bad_name");
		}
		UUID id = UUID.randomUUID();
		String name = trimmed.isEmpty() ? type.name() + "-" + id.toString().substring(0, 8) : trimmed;
		NomosikSquad squad = new NomosikSquad(id, commander.getUUID(), type, new ArrayList<>(), false, name, clampSquadIcon(iconId));
		this.data.putSquad(squad);
		this.data.setActiveSquad(commander.getUUID(), id);
		return Optional.empty();
	}

	/**
	 * Updates icon always; updates stored name only when {@code rawName} trims to a non-empty string.
	 *
	 * @return empty on success, or a translation key on failure
	 */
	public Optional<String> tryEditSquad(ServerPlayer commander, UUID squadId, String rawName, byte iconId) {
		Optional<NomosikSquad> opt = this.data.getSquad(squadId);
		if (opt.isEmpty()) {
			return Optional.of("message.nomosiki.squad.not_found");
		}
		NomosikSquad squad = opt.get();
		if (!squad.commanderId().equals(commander.getUUID())) {
			return Optional.of("message.nomosiki.squad.not_commander");
		}
		String trimmed = rawName == null ? "" : rawName.trim();
		if (!trimmed.isEmpty()) {
			if (trimmed.length() > 32 || !isAllowedSquadName(trimmed)) {
				return Optional.of("message.nomosiki.squad.bad_name");
			}
			squad.setName(trimmed);
		}
		squad.setIconId(clampSquadIcon(iconId));
		this.data.setDirty();
		return Optional.empty();
	}

	/**
	 * Adds a recruit to a squad, or moves them from another squad they already belong to (same commander/owner rules as {@link #tryAddMember}).
	 *
	 * @return empty string on success, otherwise a translation key
	 */
	public String tryAddOrTransferMember(ServerPlayer commander, UUID squadId, NomosikEntity recruit) {
		Optional<UUID> current = recruit.getSquadId();
		if (current.isPresent() && current.get().equals(squadId)) {
			return "";
		}
		if (current.isPresent()) {
			this.removeNomosikFromSquad(recruit);
		}
		return this.tryAddMember(commander, squadId, recruit);
	}

	/**
	 * Adds every commander-owned nomosik within {@code radius} blocks that has no squad.
	 */
	public void assignNearbyOwnedToSquad(ServerPlayer commander, UUID squadId, int radius) {
		Optional<NomosikSquad> opt = this.data.getSquad(squadId);
		if (opt.isEmpty() || !opt.get().commanderId().equals(commander.getUUID())) {
			return;
		}
		int r = Mth.clamp(radius, 4, 64);
		if (!(commander.level() instanceof ServerLevel level)) {
			return;
		}
		AABB box = commander.getBoundingBox().inflate(r);
		List<NomosikEntity> candidates = level.getEntitiesOfClass(NomosikEntity.class, box,
			w -> commander.getUUID().equals(w.getOwnerUUID()) && w.getSquadId().isEmpty());
		for (NomosikEntity w : candidates) {
			this.tryAddMember(commander, squadId, w);
		}
	}

	public static byte clampSquadIcon(byte raw) {
		int v = raw;
		if (v < 0) {
			return 0;
		}
		if (v >= SQUAD_ICON_COUNT) {
			return (byte) (SQUAD_ICON_COUNT - 1);
		}
		return raw;
	}

	private static boolean isAllowedSquadName(String s) {
		for (int i = 0; i < s.length(); ) {
			int cp = s.codePointAt(i);
			if (cp < 32 || cp == 127) {
				return false;
			}
			if (cp == 0xA7) {
				return false;
			}
			i += Character.charCount(cp);
		}
		return true;
	}

	/**
	 * Puts a freshly hired nomosik into an auto-created “reserve” squad whose {@link NomosikSquad#type()}
	 * matches {@link NomosikEntity#getSquadBranchType()} — the squad type is a <em>default inclination</em> for
	 * auto-pools only. Manual adds via {@link #tryAddMember} do not enforce branch equality.
	 */
	public void autoAssignNewHire(ServerPlayer commander, NomosikEntity recruit) {
		if (recruit.level().isClientSide()) {
			return;
		}
		if (!commander.getUUID().equals(recruit.getOwnerUUID())) {
			return;
		}
		if (recruit.getSquadId().isPresent()) {
			return;
		}
		SquadType branch = recruit.getSquadBranchType();
		NomosikSquad squad = this.findReservePoolWithSpace(commander.getUUID(), branch)
			.orElseGet(() -> this.createReservePoolSquad(commander, branch, branch.translationKey(), branch.id()));
		if (!squad.addMember(recruit.getUUID())) {
			squad = this.createReservePoolSquad(commander, branch, branch.translationKey(), branch.id());
			if (!squad.addMember(recruit.getUUID())) {
				return;
			}
		}
		recruit.joinSquadServer(squad.id());
		this.data.setDirty();
	}

	/**
	 * Visible (not reserve) squad for one city hire. Members hold at {@code holdAt} and do not follow
	 * the commander. {@code name} may be a translation key; it is not limited like a player-typed name.
	 * The commander may be offline; army state is pushed when they are online.
	 *
	 * @return the new squad id, or null when nobody could be enlisted
	 */
	public @Nullable UUID formHoldingSquad(UUID commanderId, List<UUID> memberIds, BlockPos holdAt, String name) {
		if (commanderId == null || holdAt == null || memberIds == null || memberIds.isEmpty()) {
			return null;
		}
		List<NomosikEntity> recruits = new ArrayList<>();
		for (UUID memberId : memberIds) {
			if (memberId == null) {
				continue;
			}
			NomosikEntity recruit = this.findNomosikByUuidAnyLevel(memberId);
			if (recruit == null || !recruit.isAlive() || !commanderId.equals(recruit.getOwnerUUID())) {
				continue;
			}
			recruits.add(recruit);
		}
		if (recruits.isEmpty()) {
			return null;
		}
		SquadType branch = recruits.get(0).getSquadBranchType();
		UUID id = UUID.randomUUID();
		String stored = garrisonSquadName(name, branch, id);
		NomosikSquad squad = new NomosikSquad(id, commanderId, branch, new ArrayList<>(), false, stored, (byte) 0);
		this.data.putSquad(squad);
		BlockPos anchor = holdAt.immutable();
		for (NomosikEntity recruit : recruits) {
			if (recruit.getSquadId().isPresent()) {
				this.removeNomosikFromSquad(recruit);
			}
			if (!squad.addMember(recruit.getUUID())) {
				break;
			}
			recruit.joinSquadServer(squad.id());
			recruit.applySquadOrder(SquadOrder.HOLD, anchor, 8);
		}
		if (squad.memberCount() == 0) {
			this.data.removeSquad(id);
			return null;
		}
		this.data.setDirty();
		this.syncCommander(commanderId);
		NomosikiAdvancements.onGarrison(this.server, commanderId, branch, squad, this);
		return id;
	}

	private static String garrisonSquadName(String rawName, SquadType branch, UUID id) {
		String trimmed = rawName == null ? "" : rawName.trim();
		if (trimmed.isEmpty() || trimmed.length() > 64 || !isAllowedSquadName(trimmed)) {
			return branch.name() + "-" + id.toString().substring(0, 8);
		}
		return trimmed;
	}

	private Optional<NomosikSquad> findReservePoolWithSpace(UUID commanderId, SquadType branch) {
		for (NomosikSquad s : this.data.squadsOfCommander(commanderId)) {
			if (s.reservePool() && s.type() == branch && s.memberCount() < NomosikSquad.MAX_MEMBERS) {
				return Optional.of(s);
			}
		}
		return Optional.empty();
	}

	private NomosikSquad createReservePoolSquad(ServerPlayer commander, SquadType branch, String name, byte iconId) {
		UUID id = UUID.randomUUID();
		NomosikSquad squad = new NomosikSquad(id, commander.getUUID(), branch, new ArrayList<>(), true, name, iconId);
		this.data.putSquad(squad);
		return squad;
	}

	/**
	 * Gives the squad to another player in the same faction.
	 * Loaded fighters change owner immediately. Unloaded ones catch up in {@link #reconcileLoadedOwner}.
	 *
	 * @return empty string on success, otherwise a translation key
	 */
	public String transferSquad(UUID squadId, UUID toPlayerId) {
		Optional<NomosikSquad> opt = this.data.getSquad(squadId);
		if (opt.isEmpty() || toPlayerId == null) {
			return "message.nomosiki.squad.not_found";
		}
		NomosikSquad squad = opt.get();
		UUID from = squad.commanderId();
		if (from.equals(toPlayerId)) {
			return "message.nomosiki.squad.transfer_self";
		}
		var factions = FactionService.get(this.server);
		String fromFaction = factions.getPlayerFaction(from);
		String toFaction = factions.getPlayerFaction(toPlayerId);
		if (fromFaction == null || toFaction == null || !fromFaction.equalsIgnoreCase(toFaction)) {
			return "message.nomosiki.squad.transfer_faction";
		}
		squad.setCommanderId(toPlayerId);
		this.data.getActiveSquadId(from).filter(squadId::equals).ifPresent(ignored -> this.data.clearActiveSquad(from));
		for (UUID memberId : squad.memberEntityIdsView()) {
			NomosikEntity member = findNomosikByUuidAnyLevel(memberId);
			if (member != null) {
				member.setOwnerUUID(toPlayerId);
			}
		}
		this.data.setDirty();
		NomosikiAdvancements.grant(this.server, from, NomosikiActions.GIVE_SQUAD);
		syncCommander(from);
		syncCommander(toPlayerId);
		FactionArmorCompat.refreshLoaded(this.server);
		return "";
	}

	/** If a loaded fighter's squad already belongs to someone else, adopt that commander. */
	public void reconcileLoadedOwner(NomosikEntity entity) {
		Optional<UUID> squadId = entity.getSquadId();
		if (squadId.isEmpty()) {
			return;
		}
		Optional<NomosikSquad> squad = this.data.getSquad(squadId.get());
		if (squad.isEmpty()) {
			return;
		}
		UUID commander = squad.get().commanderId();
		if (!commander.equals(entity.getOwnerUUID())) {
			entity.setOwnerUUID(commander);
		}
	}

	private void syncCommander(UUID commanderId) {
		ServerPlayer online = this.server.getPlayerList().getPlayer(commanderId);
		if (online == null) {
			return;
		}
		SquadNetworking.sendSquadState(online, this);
		SquadNetworking.sendArmyState(online, this, true);
	}

	public boolean disbandSquad(ServerPlayer commander, UUID squadId) {
		Optional<NomosikSquad> opt = this.data.getSquad(squadId);
		if (opt.isEmpty() || !opt.get().commanderId().equals(commander.getUUID())) {
			return false;
		}
		for (UUID memberId : new ArrayList<>(opt.get().memberEntityIdsView())) {
			NomosikEntity w = findNomosikByUuidAnyLevel(memberId);
			if (w != null) {
				w.clearSquadIdServer();
			}
		}
		this.data.removeSquad(squadId);
		SquadAutoTargetService.clearSquad(squadId);
		return true;
	}

	public void removeNomosikFromSquad(NomosikEntity nomosik) {
		Optional<UUID> sid = nomosik.getSquadId();
		if (sid.isEmpty()) {
			return;
		}
		UUID squadUuid = sid.get();
		nomosik.clearSquadIdServer();
		Optional<NomosikSquad> opt = this.data.getSquad(squadUuid);
		if (opt.isEmpty()) {
			return;
		}
		opt.get().removeMember(nomosik.getUUID());
		if (opt.get().memberCount() == 0) {
			this.data.removeSquad(squadUuid);
			SquadAutoTargetService.clearSquad(squadUuid);
		} else {
			this.data.setDirty();
		}
	}

	/**
	 * Adds an owned, squadless nomosik to a squad. {@link NomosikSquad#type()} is not validated against
	 * {@link NomosikEntity#getSquadBranchType()} — mixed squads are allowed.
	 */
	public String tryAddMember(ServerPlayer commander, UUID squadId, NomosikEntity recruit) {
		Optional<NomosikSquad> opt = this.data.getSquad(squadId);
		if (opt.isEmpty()) {
			return "message.nomosiki.squad.not_found";
		}
		NomosikSquad squad = opt.get();
		if (!squad.commanderId().equals(commander.getUUID())) {
			return "message.nomosiki.squad.not_commander";
		}
		if (!commander.getUUID().equals(recruit.getOwnerUUID())) {
			return "message.nomosiki.squad.not_owner";
		}
		if (recruit.getSquadId().isPresent()) {
			return "message.nomosiki.squad.already_in_squad";
		}
		if (!squad.addMember(recruit.getUUID())) {
			return squad.memberCount() >= NomosikSquad.MAX_MEMBERS
				? "message.nomosiki.squad.full"
				: "message.nomosiki.squad.add_failed";
		}
		recruit.joinSquadServer(squadId);
		this.data.setDirty();
		NomosikiAdvancements.onSquadRoster(this.server, commander.getUUID(), squad, this);
		return "";
	}

	public void onNomosikRemoved(NomosikEntity nomosik) {
		removeNomosikFromSquad(nomosik);
	}

	public void setActiveSquad(ServerPlayer commander, @Nullable UUID squadId) {
		if (squadId == null) {
			this.data.clearActiveSquad(commander.getUUID());
			return;
		}
		this.data.setActiveSquad(commander.getUUID(), squadId);
	}

	public Optional<UUID> getActiveSquadId(UUID commanderId) {
		return this.data.getActiveSquadId(commanderId);
	}

	public List<NomosikSquad> listCommanderSquads(UUID commanderId) {
		return this.data.squadsOfCommander(commanderId);
	}

	/**
	 * Members that exist in {@code level}, in roster order.
	 */
	public List<NomosikEntity> resolveMembersInLevel(ServerLevel level, UUID squadId) {
		Optional<NomosikSquad> opt = this.data.getSquad(squadId);
		if (opt.isEmpty()) {
			return List.of();
		}
		Map<UUID, NomosikEntity> byUuid = new HashMap<>();
		for (Entity e : level.getAllEntities()) {
			if (e instanceof NomosikEntity w) {
				byUuid.put(w.getUUID(), w);
			}
		}
		List<NomosikEntity> ordered = new ArrayList<>();
		for (UUID id : opt.get().memberEntityIdsView()) {
			NomosikEntity w = byUuid.get(id);
			if (w != null) {
				ordered.add(w);
			}
		}
		return ordered;
	}

	public @Nullable NomosikEntity findNomosikByUuidAnyLevel(UUID entityUuid) {
		for (ServerLevel sl : this.server.getAllLevels()) {
			for (Entity e : sl.getAllEntities()) {
				if (e instanceof NomosikEntity w && w.getUUID().equals(entityUuid)) {
					return w;
				}
			}
		}
		return null;
	}

	public static @Nullable NomosikEntity findOwnedNomosikInLook(ServerPlayer player, double reach) {
		Vec3 eye = player.getEyePosition(1f);
		Vec3 view = player.getViewVector(1f);
		double best = reach * reach;
		NomosikEntity hit = null;
		AABB box = player.getBoundingBox().inflate(reach);
		List<NomosikEntity> candidates = player.level().getEntitiesOfClass(NomosikEntity.class, box,
			w -> player.getUUID().equals(w.getOwnerUUID()) && player.hasLineOfSight(w));
		for (NomosikEntity w : candidates) {
			Vec3 to = w.getEyePosition(1f).subtract(eye);
			double lenSqr = to.lengthSqr();
			if (lenSqr > best) {
				continue;
			}
			Vec3 dir = to.normalize();
			double dot = dir.dot(view);
			if (dot < 0.92) {
				continue;
			}
			if (lenSqr < best) {
				best = lenSqr;
				hit = w;
			}
		}
		return hit;
	}

}
