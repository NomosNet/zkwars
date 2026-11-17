package com.nomosiki.faction;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class FactionData {
	private final String name;
	private UUID owner;
	private FactionColor color;
	private @Nullable CompoundTag bannerNbt;
	private final Set<UUID> members = new HashSet<>();

	public FactionData(String name, UUID owner, FactionColor color) {
		this(name, owner, color, null);
	}

	public FactionData(String name, UUID owner, FactionColor color, @Nullable CompoundTag bannerNbt) {
		this.name = name;
		this.owner = owner;
		this.color = color;
		this.bannerNbt = bannerNbt == null || bannerNbt.isEmpty() ? null : bannerNbt.copy();
		this.members.add(owner);
	}

	public String name() {
		return name;
	}

	public UUID owner() {
		return owner;
	}

	public void owner(UUID value) {
		this.owner = value;
	}

	public FactionColor color() {
		return color;
	}

	public void color(FactionColor value) {
		this.color = value;
	}

	@Nullable
	public CompoundTag bannerNbt() {
		return this.bannerNbt == null ? null : this.bannerNbt.copy();
	}

	public void bannerNbt(@Nullable CompoundTag value) {
		this.bannerNbt = value == null || value.isEmpty() ? null : value.copy();
	}

	public boolean hasBanner() {
		return this.bannerNbt != null && !this.bannerNbt.isEmpty();
	}

	public Set<UUID> members() {
		return members;
	}

	public void writeNbt(CompoundTag tag) {
		tag.putString("Name", this.name);
		tag.putUUID("Owner", this.owner);
		tag.putString("Color", this.color.id());
		if (this.bannerNbt != null && !this.bannerNbt.isEmpty()) {
			tag.put("Banner", this.bannerNbt.copy());
		}
		ListTag list = new ListTag();
		for (UUID member : this.members) {
			list.add(StringTag.valueOf(member.toString()));
		}
		tag.put("Members", list);
	}

	public static FactionData fromNbt(CompoundTag tag) {
		String name = tag.getString("Name");
		UUID owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : UUID.randomUUID();
		FactionColor color = FactionColor.fromId(tag.getString("Color"));
		CompoundTag banner = tag.contains("Banner", Tag.TAG_COMPOUND) ? tag.getCompound("Banner") : null;
		FactionData data = new FactionData(name, owner, color, banner);
		data.members.clear();
		ListTag list = tag.getList("Members", Tag.TAG_STRING);
		for (int i = 0; i < list.size(); i++) {
			String raw = list.getString(i);
			try {
				data.members.add(UUID.fromString(raw));
			} catch (IllegalArgumentException ignored) {
			}
		}
		if (data.members.isEmpty()) {
			data.members.add(owner);
		}
		return data;
	}
}
