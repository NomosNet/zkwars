package com.example.compat;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

/** Latest wartime window in overworld game time. A chunk unloaded across two wars only skips the latest one. */
public final class WorkerWartimeSavedData extends SavedData {
	public static final String DATA_ID = "zk_workers_wartime";

	private boolean open;
	private long start = -1L;
	private long end = -1L;

	public static Factory<WorkerWartimeSavedData> factory() {
		return new Factory<>(WorkerWartimeSavedData::new, WorkerWartimeSavedData::fromNbt, DataFixTypes.LEVEL);
	}

	private static WorkerWartimeSavedData fromNbt(CompoundTag tag, HolderLookup.Provider lookup) {
		WorkerWartimeSavedData data = new WorkerWartimeSavedData();
		data.open = tag.getBoolean("Open");
		data.start = tag.contains("Start") ? tag.getLong("Start") : -1L;
		data.end = tag.contains("End") ? tag.getLong("End") : -1L;
		return data;
	}

	public boolean open() {
		return this.open;
	}

	public void begin(long gameTime) {
		this.open = true;
		this.start = gameTime;
		this.setDirty();
	}

	public void finish(long gameTime) {
		this.open = false;
		this.end = gameTime;
		this.setDirty();
	}

	/** Ticks of {@code [from, to)} that fall inside the latest wartime window. */
	public long skipped(long from, long to) {
		if (this.start < 0L || to <= from) {
			return 0L;
		}
		long windowEnd = this.open ? to : this.end;
		if (windowEnd < 0L) {
			return 0L;
		}
		long left = Math.max(from, this.start);
		long right = Math.min(to, windowEnd);
		return Math.max(0L, right - left);
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		tag.putBoolean("Open", this.open);
		tag.putLong("Start", this.start);
		tag.putLong("End", this.end);
		return tag;
	}
}
