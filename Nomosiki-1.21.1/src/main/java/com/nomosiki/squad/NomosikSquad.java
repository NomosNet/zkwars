package com.nomosiki.squad;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Commander-owned squad roster. {@link #type()} is the squad’s default <em>branch / inclination</em>
 * (reserve pools, default naming or icons, and similar). It does <strong>not</strong> restrict which unit
 * classes may belong to the squad when members are assigned by the player.
 */
public final class NomosikSquad {
	public static final int MAX_MEMBERS = 128;

	private final UUID id;
	private UUID commanderId;
	private final SquadType type;
	private final List<UUID> memberEntityIds;
	/** Auto-created pool for new hires; not set on squads created via {@code /nomosik squad create}. */
	private final boolean reservePool;
	/** Empty means display / persistence layer may derive an auto-name from {@link #type()}. */
	private String name;
	private byte iconId;

	public NomosikSquad(UUID id, UUID commanderId, SquadType type) {
		this(id, commanderId, type, new ArrayList<>(), false, "", (byte) 0);
	}

	public NomosikSquad(UUID id, UUID commanderId, SquadType type, List<UUID> members) {
		this(id, commanderId, type, members, false, "", (byte) 0);
	}

	public NomosikSquad(UUID id, UUID commanderId, SquadType type, List<UUID> members, boolean reservePool) {
		this(id, commanderId, type, members, reservePool, "", (byte) 0);
	}

	public NomosikSquad(UUID id, UUID commanderId, SquadType type, List<UUID> members, boolean reservePool, String name, byte iconId) {
		this.id = id;
		this.commanderId = commanderId;
		this.type = type;
		this.memberEntityIds = new ArrayList<>(members);
		this.reservePool = reservePool;
		this.name = name == null ? "" : name;
		this.iconId = iconId;
	}

	public UUID id() {
		return this.id;
	}

	public UUID commanderId() {
		return this.commanderId;
	}

	public void setCommanderId(UUID commanderId) {
		this.commanderId = commanderId;
	}

	/** Squad branch used as default “inclination” (e.g. reserve squads), not a hard composition rule. */
	public SquadType type() {
		return this.type;
	}

	public boolean reservePool() {
		return this.reservePool;
	}

	public String name() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name == null ? "" : name;
	}

	public byte iconId() {
		return this.iconId;
	}

	public void setIconId(byte iconId) {
		this.iconId = iconId;
	}

	public List<UUID> memberEntityIdsView() {
		return Collections.unmodifiableList(this.memberEntityIds);
	}

	public int memberCount() {
		return this.memberEntityIds.size();
	}

	public boolean containsMember(UUID entityUuid) {
		return this.memberEntityIds.contains(entityUuid);
	}

	public boolean addMember(UUID entityUuid) {
		if (this.memberEntityIds.contains(entityUuid) || this.memberEntityIds.size() >= MAX_MEMBERS) {
			return false;
		}
		this.memberEntityIds.add(entityUuid);
		return true;
	}

	public void removeMember(UUID entityUuid) {
		this.memberEntityIds.remove(entityUuid);
	}
}
