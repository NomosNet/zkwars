package com.nomosiki.advancement;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Kill and duel counters, plus advancement actions earned while the player was offline. */
public final class NomosikiAdvancementProgress extends SavedData {
	public static final String DATA_ID = "nomosiki_advancements";

	private final Map<UUID, Counts> counts = new HashMap<>();
	private final Map<UUID, Set<String>> pending = new HashMap<>();

	public static Factory<NomosikiAdvancementProgress> factory() {
		return new Factory<>(NomosikiAdvancementProgress::new, NomosikiAdvancementProgress::fromNbt, DataFixTypes.LEVEL);
	}

	public static NomosikiAdvancementProgress get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(factory(), DATA_ID);
	}

	private static NomosikiAdvancementProgress fromNbt(CompoundTag tag, HolderLookup.Provider lookup) {
		NomosikiAdvancementProgress data = new NomosikiAdvancementProgress();
		ListTag players = tag.getList("Players", Tag.TAG_COMPOUND);
		for (int i = 0; i < players.size(); i++) {
			CompoundTag row = players.getCompound(i);
			if (!row.hasUUID("Id")) {
				continue;
			}
			UUID id = row.getUUID("Id");
			int kills = row.getInt("Kills");
			int duels = row.getInt("Duels");
			if (kills > 0 || duels > 0) {
				data.counts.put(id, new Counts(kills, duels));
			}
			ListTag actions = row.getList("Pending", Tag.TAG_STRING);
			if (!actions.isEmpty()) {
				Set<String> queued = new HashSet<>();
				for (int j = 0; j < actions.size(); j++) {
					String action = actions.getString(j);
					if (!action.isEmpty()) {
						queued.add(action);
					}
				}
				if (!queued.isEmpty()) {
					data.pending.put(id, queued);
				}
			}
		}
		return data;
	}

	public int addKill(UUID playerId) {
		Counts row = this.counts.computeIfAbsent(playerId, ignored -> new Counts(0, 0));
		row.kills++;
		this.setDirty();
		return row.kills;
	}

	public int addDuelWin(UUID playerId) {
		Counts row = this.counts.computeIfAbsent(playerId, ignored -> new Counts(0, 0));
		row.duels++;
		this.setDirty();
		return row.duels;
	}

	public void queue(UUID playerId, String action) {
		if (action == null || action.isEmpty()) {
			return;
		}
		this.pending.computeIfAbsent(playerId, ignored -> new HashSet<>()).add(action);
		this.setDirty();
	}

	public void flush(ServerPlayer player, NomosikiActionTrigger trigger) {
		Set<String> actions = this.pending.remove(player.getUUID());
		if (actions == null || actions.isEmpty()) {
			return;
		}
		for (String action : actions) {
			trigger.trigger(player, action);
		}
		this.setDirty();
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		Set<UUID> ids = new HashSet<>();
		ids.addAll(this.counts.keySet());
		ids.addAll(this.pending.keySet());
		ListTag players = new ListTag();
		for (UUID id : ids) {
			CompoundTag row = new CompoundTag();
			row.putUUID("Id", id);
			Counts count = this.counts.get(id);
			if (count != null) {
				row.putInt("Kills", count.kills);
				row.putInt("Duels", count.duels);
			}
			Set<String> actions = this.pending.get(id);
			if (actions != null && !actions.isEmpty()) {
				ListTag list = new ListTag();
				for (String action : actions) {
					list.add(StringTag.valueOf(action));
				}
				row.put("Pending", list);
			}
			players.add(row);
		}
		tag.put("Players", players);
		return tag;
	}

	private static final class Counts {
		private int kills;
		private int duels;

		private Counts(int kills, int duels) {
			this.kills = kills;
			this.duels = duels;
		}
	}
}
