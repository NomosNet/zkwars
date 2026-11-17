package com.example.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ScrollData {
	public static final String WORK_BLOCK_POS = "WorkBlockPos";
	public static final String BED_POS = "BedPos";
	public static final String CHEST_POS = "ChestPos";
	public static final String WORKER_BLOCKS = "WorkerBlocks";
	public static final String FURNACES = "Furnaces";

	private ScrollData() {
	}

	public static CompoundTag get(ItemStack stack) {
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
	}

	public static void set(ItemStack stack, CompoundTag tag) {
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
	}

	public static Optional<BlockPos> pos(CompoundTag tag, String key) {
		if (!tag.contains(key)) {
			return Optional.empty();
		}
		return Optional.of(BlockPos.of(tag.getLong(key)));
	}

	public static List<BlockPos> blockList(CompoundTag tag) {
		ListTag list = tag.getList(WORKER_BLOCKS, Tag.TAG_LONG);
		List<BlockPos> positions = new ArrayList<>(list.size());
		for (Tag entry : list) {
			if (entry instanceof LongTag longTag) {
				positions.add(BlockPos.of(longTag.getAsLong()));
			}
		}
		return positions;
	}

	public static boolean containsBlock(CompoundTag tag, BlockPos pos) {
		long packed = pos.asLong();
		ListTag list = tag.getList(WORKER_BLOCKS, Tag.TAG_LONG);
		for (Tag entry : list) {
			if (entry instanceof LongTag longTag && longTag.getAsLong() == packed) {
				return true;
			}
		}
		return false;
	}

	public static void addBlock(CompoundTag tag, BlockPos pos) {
		ListTag list = tag.getList(WORKER_BLOCKS, Tag.TAG_LONG);
		list.add(LongTag.valueOf(pos.asLong()));
		tag.put(WORKER_BLOCKS, list);
	}

	public static List<BlockPos> furnaceList(CompoundTag tag) {
		return longList(tag, FURNACES);
	}

	public static boolean containsFurnace(CompoundTag tag, BlockPos pos) {
		return containsLong(tag, FURNACES, pos);
	}

	public static void addFurnace(CompoundTag tag, BlockPos pos) {
		addLong(tag, FURNACES, pos);
	}

	public static void removeFurnace(CompoundTag tag, BlockPos pos) {
		long packed = pos.asLong();
		ListTag list = tag.getList(FURNACES, Tag.TAG_LONG);
		for (int index = 0; index < list.size(); index++) {
			Tag entry = list.get(index);
			if (entry instanceof LongTag longTag && longTag.getAsLong() == packed) {
				list.remove(index);
				tag.put(FURNACES, list);
				return;
			}
		}
	}

	private static List<BlockPos> longList(CompoundTag tag, String key) {
		ListTag list = tag.getList(key, Tag.TAG_LONG);
		List<BlockPos> positions = new ArrayList<>(list.size());
		for (Tag entry : list) {
			if (entry instanceof LongTag longTag) {
				positions.add(BlockPos.of(longTag.getAsLong()));
			}
		}
		return positions;
	}

	private static boolean containsLong(CompoundTag tag, String key, BlockPos pos) {
		long packed = pos.asLong();
		ListTag list = tag.getList(key, Tag.TAG_LONG);
		for (Tag entry : list) {
			if (entry instanceof LongTag longTag && longTag.getAsLong() == packed) {
				return true;
			}
		}
		return false;
	}

	private static void addLong(CompoundTag tag, String key, BlockPos pos) {
		ListTag list = tag.getList(key, Tag.TAG_LONG);
		list.add(LongTag.valueOf(pos.asLong()));
		tag.put(key, list);
	}
}
