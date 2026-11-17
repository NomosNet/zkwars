package com.example.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

final class CourierLoad {
	static final int LIMIT = 4;
	private static final String CARGO = "Cargo";
	private static final String ITEM = "Item";
	private static final String SOURCE = "Source";
	private static final String DEST = "Dest";

	private final List<Piece> pieces = new ArrayList<>();

	boolean isEmpty() {
		return this.pieces.isEmpty();
	}

	boolean hasRoom() {
		return this.pieces.size() < LIMIT;
	}

	int size() {
		return this.pieces.size();
	}

	ItemStack piece(int index) {
		if (index < 0 || index >= this.pieces.size()) {
			return ItemStack.EMPTY;
		}
		return this.pieces.get(index).stack;
	}

	void setPiece(int index, ItemStack stack) {
		if (index < 0 || index >= this.pieces.size() || stack.isEmpty()) {
			return;
		}
		Piece old = this.pieces.get(index);
		if (old.stack != stack) {
			this.pieces.set(index, new Piece(stack.copy(), old.source, old.dest));
		}
	}

	ItemStack removePiece(int index) {
		if (index < 0 || index >= this.pieces.size()) {
			return ItemStack.EMPTY;
		}
		return this.pieces.remove(index).stack;
	}

	ItemStack head() {
		return this.pieces.isEmpty() ? ItemStack.EMPTY : this.pieces.get(0).stack;
	}

	@Nullable BlockPos source() {
		return this.pieces.isEmpty() ? null : this.pieces.get(0).source;
	}

	@Nullable BlockPos dest() {
		return this.pieces.isEmpty() ? null : this.pieces.get(0).dest;
	}

	void add(ItemStack stack, @Nullable BlockPos source, @Nullable BlockPos dest) {
		if (stack.isEmpty() || !this.hasRoom()) {
			return;
		}
		this.pieces.add(new Piece(stack.copy(), source, dest));
	}

	void replaceHead(ItemStack stack) {
		if (this.pieces.isEmpty()) {
			if (!stack.isEmpty()) {
				this.add(stack, null, null);
			}
			return;
		}
		if (stack.isEmpty()) {
			this.pieces.remove(0);
			return;
		}
		Piece piece = this.pieces.get(0);
		this.pieces.set(0, new Piece(stack.copy(), piece.source, piece.dest));
	}

	void removeHead() {
		if (!this.pieces.isEmpty()) {
			this.pieces.remove(0);
		}
	}

	List<ItemStack> stacks() {
		List<ItemStack> stacks = new ArrayList<>();
		for (Piece piece : this.pieces) {
			stacks.add(piece.stack.copy());
		}
		return stacks;
	}

	void clear() {
		this.pieces.clear();
	}

	void save(CompoundTag tag, HolderLookup.Provider registries) {
		if (this.pieces.isEmpty()) {
			return;
		}
		ListTag list = new ListTag();
		for (Piece piece : this.pieces) {
			CompoundTag entry = new CompoundTag();
			entry.put(ITEM, piece.stack.save(registries));
			if (piece.source != null) {
				entry.putLong(SOURCE, piece.source.asLong());
			}
			if (piece.dest != null) {
				entry.putLong(DEST, piece.dest.asLong());
			}
			list.add(entry);
		}
		tag.put(CARGO, list);
	}

	void load(CompoundTag tag, HolderLookup.Provider registries) {
		this.pieces.clear();
		if (!tag.contains(CARGO, Tag.TAG_LIST)) {
			return;
		}
		ListTag list = tag.getList(CARGO, Tag.TAG_COMPOUND);
		for (int index = 0; index < list.size() && this.pieces.size() < LIMIT; index++) {
			CompoundTag entry = list.getCompound(index);
			ItemStack stack = ItemStack.parse(registries, entry.getCompound(ITEM)).orElse(ItemStack.EMPTY);
			if (stack.isEmpty()) {
				continue;
			}
			BlockPos source = entry.contains(SOURCE) ? BlockPos.of(entry.getLong(SOURCE)) : null;
			BlockPos dest = entry.contains(DEST) ? BlockPos.of(entry.getLong(DEST)) : null;
			this.pieces.add(new Piece(stack, source, dest));
		}
	}

	private record Piece(ItemStack stack, @Nullable BlockPos source, @Nullable BlockPos dest) {
	}
}
