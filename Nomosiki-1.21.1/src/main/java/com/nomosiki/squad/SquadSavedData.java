package com.nomosiki.squad;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SquadSavedData extends SavedData {
	public static final String DATA_ID = "nomosiki_squads";

	// ConcurrentHashMap (not HashMap) so reads stay safe if a NomosikEntity ever touches squad
	// state while the Async mod (com.axalotl.async) ticks entities on worker threads. All current
	// mutations happen on the main server thread; this is defensive insurance for parallel ticking.
	private final Map<UUID, NomosikSquad> squads = new ConcurrentHashMap<>();
	private final Map<UUID, UUID> activeSquadByCommander = new ConcurrentHashMap<>();

	public static Factory<SquadSavedData> factory() {
		return new Factory<>(SquadSavedData::new, SquadSavedData::fromNbt, DataFixTypes.LEVEL);
	}

	private static SquadSavedData fromNbt(CompoundTag root, HolderLookup.Provider lookup) {
		SquadSavedData data = new SquadSavedData();
		boolean migratedLegacyNameOrIcon = false;
		ListTag list = root.getList("Squads", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag st = list.getCompound(i);
			if (!st.hasUUID("Id") || !st.hasUUID("Commander")) {
				continue;
			}
			UUID id = st.getUUID("Id");
			UUID commander = st.getUUID("Commander");
			SquadType type = SquadType.fromId(st.getByte("Type"));
			List<UUID> members = new ArrayList<>();
			ListTag mem = st.getList("Members", Tag.TAG_COMPOUND);
			for (int j = 0; j < mem.size(); j++) {
				CompoundTag mt = mem.getCompound(j);
				if (mt.hasUUID("U")) {
					members.add(mt.getUUID("U"));
				}
			}
			boolean reservePool = st.getBoolean("Reserve");
			boolean missingName = !st.contains("Name", Tag.TAG_STRING);
			boolean missingIcon = !st.contains("Icon", Tag.TAG_BYTE);
			if (missingName || missingIcon) {
				migratedLegacyNameOrIcon = true;
			}
			String name = missingName ? type.name() + " #" + id.toString().substring(0, 4) : st.getString("Name");
			byte iconId = missingIcon ? SquadManager.clampSquadIcon(type.id()) : SquadManager.clampSquadIcon(st.getByte("Icon"));
			data.squads.put(id, new NomosikSquad(id, commander, type, members, reservePool, name, iconId));
		}
		ListTag active = root.getList("Active", Tag.TAG_COMPOUND);
		for (int i = 0; i < active.size(); i++) {
			CompoundTag at = active.getCompound(i);
			if (at.hasUUID("Player") && at.hasUUID("Squad")) {
				data.activeSquadByCommander.put(at.getUUID("Player"), at.getUUID("Squad"));
			}
		}
		if (migratedLegacyNameOrIcon) {
			data.setDirty();
		}
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag root, HolderLookup.Provider registries) {
		ListTag list = new ListTag();
		for (NomosikSquad s : this.squads.values()) {
			CompoundTag st = new CompoundTag();
			st.putUUID("Id", s.id());
			st.putUUID("Commander", s.commanderId());
			st.putByte("Type", s.type().id());
			st.putString("Name", s.name());
			st.putByte("Icon", s.iconId());
			st.putBoolean("Reserve", s.reservePool());
			ListTag mem = new ListTag();
			for (UUID m : s.memberEntityIdsView()) {
				CompoundTag mt = new CompoundTag();
				mt.putUUID("U", m);
				mem.add(mt);
			}
			st.put("Members", mem);
			list.add(st);
		}
		root.put("Squads", list);
		ListTag active = new ListTag();
		for (Map.Entry<UUID, UUID> e : this.activeSquadByCommander.entrySet()) {
			CompoundTag at = new CompoundTag();
			at.putUUID("Player", e.getKey());
			at.putUUID("Squad", e.getValue());
			active.add(at);
		}
		root.put("Active", active);
		return root;
	}

	public Map<UUID, NomosikSquad> squadsView() {
		return Map.copyOf(this.squads);
	}

	public Optional<NomosikSquad> getSquad(UUID squadId) {
		return Optional.ofNullable(this.squads.get(squadId));
	}

	public void putSquad(NomosikSquad squad) {
		this.squads.put(squad.id(), squad);
		this.setDirty();
	}

	public void removeSquad(UUID squadId) {
		this.squads.remove(squadId);
		this.activeSquadByCommander.entrySet().removeIf(e -> e.getValue().equals(squadId));
		this.setDirty();
	}

	public List<NomosikSquad> squadsOfCommander(UUID commanderId) {
		List<NomosikSquad> out = new ArrayList<>();
		for (NomosikSquad s : this.squads.values()) {
			if (s.commanderId().equals(commanderId)) {
				out.add(s);
			}
		}
		out.sort((a, b) -> a.id().toString().compareTo(b.id().toString()));
		return out;
	}

	public Optional<UUID> getActiveSquadId(UUID commanderId) {
		UUID sid = this.activeSquadByCommander.get(commanderId);
		if (sid == null) {
			return Optional.empty();
		}
		if (!this.squads.containsKey(sid)) {
			this.activeSquadByCommander.remove(commanderId);
			this.setDirty();
			return Optional.empty();
		}
		return Optional.of(sid);
	}

	public void setActiveSquad(UUID commanderId, UUID squadId) {
		if (!this.squads.containsKey(squadId)) {
			return;
		}
		NomosikSquad s = this.squads.get(squadId);
		if (!s.commanderId().equals(commanderId)) {
			return;
		}
		this.activeSquadByCommander.put(commanderId, squadId);
		this.setDirty();
	}

	public void clearActiveIfSquadRemoved(UUID commanderId, UUID removedSquadId) {
		if (removedSquadId.equals(this.activeSquadByCommander.get(commanderId))) {
			this.activeSquadByCommander.remove(commanderId);
			this.setDirty();
		}
	}

	public void clearActiveSquad(UUID commanderId) {
		if (this.activeSquadByCommander.remove(commanderId) != null) {
			this.setDirty();
		}
	}
}
