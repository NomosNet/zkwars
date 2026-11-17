package com.nomosiki.advancement;

import com.nomosiki.NomosikiMod;
import com.nomosiki.entity.NomosikCombatMode;
import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.entity.SquadOrder;
import com.nomosiki.squad.NomosikSquad;
import com.nomosiki.squad.SquadManager;
import com.nomosiki.squad.SquadType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public final class NomosikiAdvancements {
	public static final NomosikiActionTrigger ACTION = new NomosikiActionTrigger();

	private NomosikiAdvancements() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.TRIGGER_TYPES, NomosikiMod.id("action"), ACTION);
	}

	public static void onJoin(ServerPlayer player) {
		NomosikiAdvancementProgress.get(player.server).flush(player, ACTION);
	}

	public static void grant(@Nullable ServerPlayer player, String action) {
		if (player == null || action == null || action.isEmpty()) {
			return;
		}
		ACTION.trigger(player, action);
	}

	public static void grant(MinecraftServer server, UUID playerId, String action) {
		if (server == null || playerId == null || action == null || action.isEmpty()) {
			return;
		}
		ServerPlayer player = server.getPlayerList().getPlayer(playerId);
		if (player != null) {
			ACTION.trigger(player, action);
			return;
		}
		NomosikiAdvancementProgress.get(server).queue(playerId, action);
	}

	/** A finished province hire. Credits the buyer even if they are offline. */
	public static void onGarrison(MinecraftServer server, UUID commanderId, SquadType branch, NomosikSquad squad, SquadManager squads) {
		grant(server, commanderId, NomosikiActions.HIRED);
		if (branch != null) {
			grant(server, commanderId, NomosikiActions.hired(branch));
		}
		inspectSquad(server, commanderId, squad, squads);
	}

	public static void onSquadRoster(MinecraftServer server, UUID commanderId, NomosikSquad squad, SquadManager squads) {
		inspectSquad(server, commanderId, squad, squads);
	}

	public static void onOrder(ServerPlayer player, SquadOrder order, int squadsHit) {
		grant(player, NomosikiActions.ORDER);
		if (order != null && order.usesAttackVolume()) {
			grant(player, NomosikiActions.ATTACK_ORDER);
		}
		if (squadsHit >= NomosikiActions.SIXTEEN_SQUADS) {
			grant(player, NomosikiActions.SIXTEEN);
		}
	}

	public static void onAttackOrder(ServerPlayer player, int squadsHit) {
		grant(player, NomosikiActions.ATTACK_ORDER);
		if (squadsHit >= NomosikiActions.SIXTEEN_SQUADS) {
			grant(player, NomosikiActions.SIXTEEN);
		}
	}

	public static void onKill(LivingEntity victim, DamageSource source) {
		NomosikEntity killer = killerOf(source);
		if (killer == null || killer == victim || killer.isAllyOrSelf(victim)) {
			return;
		}
		UUID owner = killer.getOwnerUUID();
		if (owner == null || !(killer.level() instanceof ServerLevel level)) {
			return;
		}
		MinecraftServer server = level.getServer();
		int kills = NomosikiAdvancementProgress.get(server).addKill(owner);
		grant(server, owner, NomosikiActions.FIRST_BLOOD);
		if (killer.getCombatMode() == NomosikCombatMode.RAID) {
			grant(server, owner, NomosikiActions.RAID);
		}
		if (kills >= NomosikiActions.KILL_GOAL) {
			grant(server, owner, NomosikiActions.KILLS_25);
		}
		if (kills >= NomosikiActions.KILL_CHALLENGE) {
			grant(server, owner, NomosikiActions.KILLS_100);
		}
	}

	public static void onDuelWin(MinecraftServer server, UUID winnerId) {
		int wins = NomosikiAdvancementProgress.get(server).addDuelWin(winnerId);
		grant(server, winnerId, NomosikiActions.DUEL);
		if (wins >= NomosikiActions.DUEL_CHALLENGE) {
			grant(server, winnerId, NomosikiActions.DUEL_FIVE);
		}
	}

	private static void inspectSquad(MinecraftServer server, UUID commanderId, NomosikSquad squad, SquadManager squads) {
		if (squad == null || commanderId == null) {
			return;
		}
		if (squad.memberCount() >= NomosikiActions.COMPANY_SIZE) {
			grant(server, commanderId, NomosikiActions.COMPANY);
		}
		if (squad.memberCount() >= NomosikSquad.MAX_MEMBERS) {
			grant(server, commanderId, NomosikiActions.ARMY);
		}
		if (squads == null) {
			return;
		}
		Set<SquadType> branches = EnumSet.noneOf(SquadType.class);
		for (UUID memberId : squad.memberEntityIdsView()) {
			NomosikEntity member = squads.findNomosikByUuidAnyLevel(memberId);
			if (member != null) {
				branches.add(member.getSquadBranchType());
			}
		}
		if (branches.contains(SquadType.INFANTRY)
			&& branches.contains(SquadType.ARCHER)
			&& branches.contains(SquadType.KNIGHT)
			&& branches.contains(SquadType.NOMAD)) {
			grant(server, commanderId, NomosikiActions.FOUR_ROLES);
		}
	}

	private static @Nullable NomosikEntity killerOf(DamageSource source) {
		NomosikEntity killer = asNomosik(source.getEntity());
		if (killer != null) {
			return killer;
		}
		Entity direct = source.getDirectEntity();
		killer = asNomosik(direct);
		if (killer != null) {
			return killer;
		}
		if (direct instanceof Projectile projectile) {
			return asNomosik(projectile.getOwner());
		}
		return null;
	}

	private static @Nullable NomosikEntity asNomosik(@Nullable Entity entity) {
		return entity instanceof NomosikEntity nomosik ? nomosik : null;
	}
}
