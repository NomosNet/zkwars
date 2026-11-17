package com.nomosiki.faction;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** One server-wide wartime session. Peace relations are stored separately and are not cleared here. */
public final class WartimeSavedData extends SavedData {
	public static final String DATA_ID = "nomosiki_wartime";

	private boolean active;
	private long endsAtMs;
	private final List<String> factions = new ArrayList<>();
	private String statusMessageId = "";

	public static Factory<WartimeSavedData> factory() {
		return new Factory<>(WartimeSavedData::new, WartimeSavedData::fromNbt, DataFixTypes.LEVEL);
	}

	private static WartimeSavedData fromNbt(CompoundTag tag, HolderLookup.Provider lookup) {
		WartimeSavedData data = new WartimeSavedData();
		data.active = tag.getBoolean("Active");
		data.endsAtMs = tag.getLong("EndsAt");
		data.statusMessageId = tag.getString("StatusMessageId");
		ListTag factions = tag.getList("Factions", Tag.TAG_STRING);
		for (int i = 0; i < factions.size(); i++) {
			String name = factions.getString(i);
			if (!name.isBlank()) {
				data.factions.add(name);
			}
		}
		return data;
	}

	public boolean active() {
		return this.active;
	}

	public long endsAtMs() {
		return this.endsAtMs;
	}

	public List<String> factions() {
		return List.copyOf(this.factions);
	}

	public String statusMessageId() {
		return this.statusMessageId == null ? "" : this.statusMessageId;
	}

	public void setStatusMessageId(String id) {
		String next = id == null ? "" : id;
		if (!next.equals(this.statusMessageId)) {
			this.statusMessageId = next;
			this.setDirty();
		}
	}

	public void begin(List<String> factionNames, long endsAtMs) {
		this.active = true;
		this.endsAtMs = endsAtMs;
		this.factions.clear();
		this.factions.addAll(factionNames);
		this.setDirty();
	}

	public void end() {
		if (!this.active && this.factions.isEmpty()) {
			return;
		}
		this.active = false;
		this.endsAtMs = 0L;
		this.factions.clear();
		this.setDirty();
	}

	public void removeFaction(String factionName) {
		if (factionName == null || factionName.isBlank()) {
			return;
		}
		if (this.factions.removeIf(name -> name.equalsIgnoreCase(factionName))) {
			this.setDirty();
		}
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		tag.putBoolean("Active", this.active);
		tag.putLong("EndsAt", this.endsAtMs);
		tag.putString("StatusMessageId", this.statusMessageId == null ? "" : this.statusMessageId);
		ListTag factionsTag = new ListTag();
		for (String faction : this.factions) {
			factionsTag.add(StringTag.valueOf(faction));
		}
		tag.put("Factions", factionsTag);
		return tag;
	}
}
