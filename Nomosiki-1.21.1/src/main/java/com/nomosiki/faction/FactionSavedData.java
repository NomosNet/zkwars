package com.nomosiki.faction;

import net.minecraft.core.HolderLookup;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class FactionSavedData extends SavedData {
	public static final String DATA_ID = "nomosiki_factions";
	public final Map<String, FactionData> factions = new HashMap<>();
	public final Map<UUID, String> playerToFaction = new HashMap<>();
	public final Map<UUID, Set<String>> invites = new HashMap<>();

	public static Factory<FactionSavedData> factory() {
		return new Factory<>(FactionSavedData::new, FactionSavedData::fromNbt, DataFixTypes.LEVEL);
	}

	private static FactionSavedData fromNbt(CompoundTag tag, HolderLookup.Provider lookup) {
		FactionSavedData data = new FactionSavedData();
		ListTag factionsList = tag.getList("Factions", Tag.TAG_COMPOUND);
		for (int i = 0; i < factionsList.size(); i++) {
			CompoundTag factionTag = factionsList.getCompound(i);
			FactionData faction = FactionData.fromNbt(factionTag);
			if (faction.name().isBlank()) {
				continue;
			}
			data.factions.put(faction.name(), faction);
			for (UUID member : faction.members()) {
				data.playerToFaction.put(member, faction.name());
			}
		}
		ListTag invitesList = tag.getList("Invites", Tag.TAG_COMPOUND);
		for (int i = 0; i < invitesList.size(); i++) {
			CompoundTag inviteTag = invitesList.getCompound(i);
			if (!inviteTag.hasUUID("Player")) {
				continue;
			}
			UUID playerId = inviteTag.getUUID("Player");
			ListTag list = inviteTag.getList("Factions", Tag.TAG_STRING);
			Set<String> pending = new HashSet<>();
			for (int j = 0; j < list.size(); j++) {
				String factionName = list.getString(j);
				if (!factionName.isBlank()) {
					pending.add(factionName);
				}
			}
			if (!pending.isEmpty()) {
				data.invites.put(playerId, pending);
			}
		}
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		ListTag factionsList = new ListTag();
		for (FactionData faction : this.factions.values()) {
			CompoundTag factionTag = new CompoundTag();
			faction.writeNbt(factionTag);
			factionsList.add(factionTag);
		}
		tag.put("Factions", factionsList);

		ListTag invitesList = new ListTag();
		for (Map.Entry<UUID, Set<String>> entry : this.invites.entrySet()) {
			if (entry.getValue().isEmpty()) {
				continue;
			}
			CompoundTag inviteTag = new CompoundTag();
			inviteTag.putUUID("Player", entry.getKey());
			ListTag list = new ListTag();
			for (String faction : entry.getValue()) {
				list.add(StringTag.valueOf(faction));
			}
			inviteTag.put("Factions", list);
			invitesList.add(inviteTag);
		}
		tag.put("Invites", invitesList);
		return tag;
	}
}
