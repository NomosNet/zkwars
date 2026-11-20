package com.nomosiki.match.schematic;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * One decoded .litematic. Block coordinates are relative to the schematic's minimum corner.
 * Palette ids are packed the way Litematica packs them: bits may cross a long boundary,
 * and index {@code (y * sizeZ + z) * sizeX + x} starts at each region's minimum corner.
 */
public final class LitematicaSchematic {
	public static final int MAX_AXIS = 64;
	public static final int MAX_BLOCKS = 8000;

	public record Cell(int x, int y, int z, BlockState state) {
	}

	private final int sizeX;
	private final int sizeY;
	private final int sizeZ;
	private final List<Cell> blocks;

	private LitematicaSchematic(int sizeX, int sizeY, int sizeZ, List<Cell> blocks) {
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.sizeZ = sizeZ;
		this.blocks = List.copyOf(blocks);
	}

	public int sizeX() {
		return this.sizeX;
	}

	public int sizeY() {
		return this.sizeY;
	}

	public int sizeZ() {
		return this.sizeZ;
	}

	public List<Cell> blocks() {
		return this.blocks;
	}

	public static LitematicaSchematic parse(CompoundTag root, HolderLookup.Provider registries) throws SchematicException {
		CompoundTag regions = root.getCompound("Regions");
		if (regions.isEmpty()) {
			throw new SchematicException(false);
		}
		HolderLookup<Block> blocks = registries.lookupOrThrow(Registries.BLOCK);
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int minZ = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;
		int maxZ = Integer.MIN_VALUE;
		List<RawRegion> parsed = new ArrayList<>();
		for (String name : regions.getAllKeys()) {
			Tag child = regions.get(name);
			if (!(child instanceof CompoundTag region)) {
				continue;
			}
			RawRegion raw = readRegion(region, blocks);
			parsed.add(raw);
			minX = Math.min(minX, raw.minX);
			minY = Math.min(minY, raw.minY);
			minZ = Math.min(minZ, raw.minZ);
			maxX = Math.max(maxX, raw.minX + raw.sizeX - 1);
			maxY = Math.max(maxY, raw.minY + raw.sizeY - 1);
			maxZ = Math.max(maxZ, raw.minZ + raw.sizeZ - 1);
		}
		if (parsed.isEmpty()) {
			throw new SchematicException(false);
		}
		int sizeX = maxX - minX + 1;
		int sizeY = maxY - minY + 1;
		int sizeZ = maxZ - minZ + 1;
		if (sizeX > MAX_AXIS || sizeY > MAX_AXIS || sizeZ > MAX_AXIS || sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
			throw new SchematicException(true);
		}
		List<Cell> cells = new ArrayList<>();
		for (RawRegion region : parsed) {
			int bits = bitsPerBlock(region.palette.length);
			for (int y = 0; y < region.sizeY; y++) {
				for (int z = 0; z < region.sizeZ; z++) {
					for (int x = 0; x < region.sizeX; x++) {
						int index = (y * region.sizeZ + z) * region.sizeX + x;
						int id = getAt(region.states, index, bits);
						if (id < 0 || id >= region.palette.length) {
							throw new SchematicException(false);
						}
						BlockState state = region.palette[id];
						if (!SchematicBlocks.placeable(state)) {
							continue;
						}
						cells.add(new Cell(region.minX - minX + x, region.minY - minY + y, region.minZ - minZ + z, state));
						if (cells.size() > MAX_BLOCKS) {
							throw new SchematicException(true);
						}
					}
				}
			}
		}
		if (cells.isEmpty()) {
			throw new SchematicException(false);
		}
		cells.sort(Comparator.comparingInt(Cell::y).thenComparingInt(Cell::x).thenComparingInt(Cell::z));
		return new LitematicaSchematic(sizeX, sizeY, sizeZ, cells);
	}

	private static RawRegion readRegion(CompoundTag region, HolderLookup<Block> blocks) throws SchematicException {
		CompoundTag position = region.getCompound("Position");
		CompoundTag size = region.getCompound("Size");
		if (position.isEmpty() || size.isEmpty()) {
			throw new SchematicException(false);
		}
		int posX = coord(position, "x");
		int posY = coord(position, "y");
		int posZ = coord(position, "z");
		int rawX = coord(size, "x");
		int rawY = coord(size, "y");
		int rawZ = coord(size, "z");
		if (rawX == 0 || rawY == 0 || rawZ == 0 || rawX == Integer.MIN_VALUE || rawY == Integer.MIN_VALUE || rawZ == Integer.MIN_VALUE) {
			throw new SchematicException(false);
		}
		int sizeX = Math.abs(rawX);
		int sizeY = Math.abs(rawY);
		int sizeZ = Math.abs(rawZ);
		if (sizeX > MAX_AXIS || sizeY > MAX_AXIS || sizeZ > MAX_AXIS) {
			throw new SchematicException(true);
		}
		int minX = rawX >= 0 ? posX : posX + rawX + 1;
		int minY = rawY >= 0 ? posY : posY + rawY + 1;
		int minZ = rawZ >= 0 ? posZ : posZ + rawZ + 1;
		BlockState[] palette = readPalette(region, blocks);
		if (palette.length == 0) {
			throw new SchematicException(false);
		}
		long[] states = region.contains("BlockStates", Tag.TAG_LONG_ARRAY) ? region.getLongArray("BlockStates") : new long[0];
		long volume = (long) sizeX * sizeY * sizeZ;
		int bits = bitsPerBlock(palette.length);
		long need = (volume * bits + 63L) / 64L;
		if (states.length == 0) {
			if (palette.length != 1) {
				throw new SchematicException(false);
			}
		} else if (states.length < need) {
			throw new SchematicException(false);
		}
		return new RawRegion(minX, minY, minZ, sizeX, sizeY, sizeZ, palette, states);
	}

	private static BlockState[] readPalette(CompoundTag region, HolderLookup<Block> blocks) throws SchematicException {
		Tag raw = region.get("BlockStatePalette");
		if (!(raw instanceof ListTag list) || list.isEmpty()) {
			throw new SchematicException(false);
		}
		BlockState[] palette = new BlockState[list.size()];
		for (int i = 0; i < list.size(); i++) {
			Tag entry = list.get(i);
			String text;
			if (entry instanceof StringTag stringTag) {
				text = stringTag.getAsString();
			} else if (entry instanceof CompoundTag compound) {
				text = paletteText(compound);
			} else {
				throw new SchematicException(false);
			}
			if (text.isBlank()) {
				throw new SchematicException(false);
			}
			try {
				palette[i] = BlockStateParser.parseForBlock(blocks, text, false).blockState();
			} catch (CommandSyntaxException | RuntimeException exception) {
				palette[i] = null;
			}
		}
		return palette;
	}

	private static String paletteText(CompoundTag compound) {
		String name = compound.getString("Name");
		if (name.isBlank() || !compound.contains("Properties", Tag.TAG_COMPOUND)) {
			return name;
		}
		CompoundTag properties = compound.getCompound("Properties");
		if (properties.isEmpty()) {
			return name;
		}
		StringBuilder builder = new StringBuilder(name).append('[');
		boolean first = true;
		for (String key : properties.getAllKeys()) {
			if (!first) {
				builder.append(',');
			}
			first = false;
			builder.append(key).append('=').append(properties.getString(key));
		}
		builder.append(']');
		return builder.toString();
	}

	private static int coord(CompoundTag tag, String name) {
		if (tag.contains(name, Tag.TAG_INT)) {
			return tag.getInt(name);
		}
		String upper = name.toUpperCase();
		return tag.contains(upper, Tag.TAG_INT) ? tag.getInt(upper) : 0;
	}

	static int bitsPerBlock(int paletteSize) {
		if (paletteSize <= 1) {
			return 2;
		}
		return Math.max(2, Integer.SIZE - Integer.numberOfLeadingZeros(paletteSize - 1));
	}

	static int getAt(long[] data, int index, int bits) {
		if (data.length == 0) {
			return 0;
		}
		long mask = (1L << bits) - 1L;
		long startOffset = (long) index * bits;
		int startLong = (int) (startOffset >> 6);
		int endLong = (int) ((((long) index + 1L) * bits - 1L) >> 6);
		int startBit = (int) (startOffset & 63L);
		if (startLong < 0 || startLong >= data.length) {
			return -1;
		}
		if (startLong == endLong) {
			return (int) ((data[startLong] >>> startBit) & mask);
		}
		if (endLong >= data.length) {
			return -1;
		}
		int endOffset = 64 - startBit;
		return (int) (((data[startLong] >>> startBit) | (data[endLong] << endOffset)) & mask);
	}

	private record RawRegion(int minX, int minY, int minZ, int sizeX, int sizeY, int sizeZ, BlockState[] palette, long[] states) {
	}

	public static final class SchematicException extends Exception {
		private final boolean tooBig;

		public SchematicException(boolean tooBig) {
			this.tooBig = tooBig;
		}

		public boolean tooBig() {
			return this.tooBig;
		}
	}
}
