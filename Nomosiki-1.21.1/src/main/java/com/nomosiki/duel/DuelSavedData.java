package com.nomosiki.duel;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Per-player duel cooldown deadlines. Pending challenges and active fights are not stored here. */
public final class DuelSavedData extends SavedData {
	public static final String DATA_ID = "nomosiki_duels";

	private final Map<UUID, Long> cooldownUntilMs = new HashMap<>();

	public static Factory<DuelSavedData> factory() {
		return new Factory<>(DuelSavedData::new, DuelSavedData::fromNbt, DataFixTypes.LEVEL);
	}

	private static DuelSavedData fromNbt(CompoundTag tag, HolderLookup.Provider lookup) {
		DuelSavedData data = new DuelSavedData();
		long now = System.currentTimeMillis();
		ListTag list = tag.getList("Cooldowns", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag entry = list.getCompound(i);
			if (!entry.hasUUID("Id")) {
				continue;
			}
			long until = entry.getLong("Until");
			if (until > now) {
				data.cooldownUntilMs.put(entry.getUUID("Id"), until);
			}
		}
		return data;
	}

	public long cooldownUntil(UUID playerId) {
		Long until = this.cooldownUntilMs.get(playerId);
		if (until == null) {
			return 0L;
		}
		if (until <= System.currentTimeMillis()) {
			this.cooldownUntilMs.remove(playerId);
			this.setDirty();
			return 0L;
		}
		return until;
	}

	public void setCooldown(UUID playerId, long untilMs) {
		this.cooldownUntilMs.put(playerId, untilMs);
		this.setDirty();
	}

	public void pruneExpired() {
		long now = System.currentTimeMillis();
		boolean changed = false;
		Iterator<Long> it = this.cooldownUntilMs.values().iterator();
		while (it.hasNext()) {
			if (it.next() <= now) {
				it.remove();
				changed = true;
			}
		}
		if (changed) {
			this.setDirty();
		}
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		ListTag list = new ListTag();
		long now = System.currentTimeMillis();
		for (Map.Entry<UUID, Long> entry : this.cooldownUntilMs.entrySet()) {
			if (entry.getValue() <= now) {
				continue;
			}
			CompoundTag row = new CompoundTag();
			row.putUUID("Id", entry.getKey());
			row.putLong("Until", entry.getValue());
			list.add(row);
		}
		tag.put("Cooldowns", list);
		return tag;
	}
}
