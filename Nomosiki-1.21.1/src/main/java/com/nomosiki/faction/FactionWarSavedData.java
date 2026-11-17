package com.nomosiki.faction;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Persisted symmetric war relations between faction names.
 */
public final class FactionWarSavedData extends SavedData {
	public static final String DATA_ID = "nomosiki_faction_wars";

	private final Set<FactionWarPair> pairs = new HashSet<>();

	public static Factory<FactionWarSavedData> factory() {
		return new Factory<>(FactionWarSavedData::new, FactionWarSavedData::fromNbt, DataFixTypes.LEVEL);
	}

	private static FactionWarSavedData fromNbt(CompoundTag tag, HolderLookup.Provider lookup) {
		FactionWarSavedData data = new FactionWarSavedData();
		ListTag list = tag.getList("Pairs", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag pairTag = list.getCompound(i);
			String a = pairTag.getString("A");
			String b = pairTag.getString("B");
			if (a.isBlank() || b.isBlank() || a.equalsIgnoreCase(b)) {
				continue;
			}
			try {
				data.pairs.add(FactionWarPair.of(a, b));
			} catch (IllegalArgumentException ignored) {
			}
		}
		return data;
	}

	public boolean isAtWar(String factionA, String factionB) {
		if (factionA == null || factionB == null || factionA.isBlank() || factionB.isBlank()) {
			return false;
		}
		if (factionA.equalsIgnoreCase(factionB)) {
			return false;
		}
		for (FactionWarPair pair : this.pairs) {
			if (pair.matches(factionA, factionB)) {
				return true;
			}
		}
		return false;
	}

	public void setAtWar(String factionA, String factionB, boolean atWar) {
		if (factionA == null || factionB == null || factionA.isBlank() || factionB.isBlank()) {
			return;
		}
		if (factionA.equalsIgnoreCase(factionB)) {
			return;
		}
		FactionWarPair canonical = FactionWarPair.of(factionA, factionB);
		if (atWar) {
			this.pairs.add(canonical);
		} else {
			this.pairs.removeIf(pair -> pair.matches(factionA, factionB));
		}
		this.setDirty();
	}

	public void removeFaction(String factionName) {
		if (factionName == null || factionName.isBlank()) {
			return;
		}
		if (this.pairs.removeIf(pair -> pair.involves(factionName))) {
			this.setDirty();
		}
	}

	public List<FactionWarPair> allPairs() {
		return List.copyOf(this.pairs);
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		ListTag list = new ListTag();
		for (FactionWarPair pair : this.pairs) {
			CompoundTag pairTag = new CompoundTag();
			pairTag.putString("A", pair.factionA());
			pairTag.putString("B", pair.factionB());
			list.add(pairTag);
		}
		tag.put("Pairs", list);
		return tag;
	}
}
