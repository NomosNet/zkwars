package com.example.block.work;

import com.example.advancement.WorkerActions;
import com.example.advancement.WorkerAdvancements;
import com.example.block.WorkerKind;
import com.example.block.entity.WorkerBlockEntity;
import com.example.inventory.WorkerMenu;
import com.example.network.BuilderCaptureChunkPayload;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BuilderTasks extends SavedData {
	public static final String DATA_ID = "zk_workers_builder";
	public static final int RANGE = 128;
	public static final int MAX_BLOCKS = 200_000;
	private static final String NO_SCHEMATIC = "zkwars-workers.worker.status.no_schematic";
	private static final String NO_MATERIALS = "zkwars-workers.worker.status.no_materials";

	private final Map<Long, Task> tasks = new HashMap<>();
	private final Map<Long, Pending> pending = new HashMap<>();

	public static SavedData.Factory<BuilderTasks> factory() {
		return new SavedData.Factory<>(BuilderTasks::new, BuilderTasks::load, DataFixTypes.LEVEL);
	}

	public static BuilderTasks get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(factory(), DATA_ID);
	}

	public static String statusKey(ServerLevel level, BlockPos pos) {
		Task task = get(level).tasks.get(pos.asLong());
		if (task == null || task.entries.length == 0) {
			return NO_SCHEMATIC;
		}
		if (task.waiting) {
			return NO_MATERIALS;
		}
		return null;
	}

	public void remove(BlockPos pos) {
		this.pending.remove(pos.asLong());
		if (this.tasks.remove(pos.asLong()) != null) {
			this.setDirty();
		}
	}

	public Task task(BlockPos pos) {
		return this.tasks.get(pos.asLong());
	}

	public void changed() {
		this.setDirty();
	}

	public void accept(ServerPlayer player, BuilderCaptureChunkPayload payload) {
		if (!(player.containerMenu instanceof WorkerMenu menu) || menu.kind() != WorkerKind.BUILDER) {
			return;
		}
		if (!payload.pos().equals(menu.pos()) || !menu.stillValid(player)) {
			return;
		}
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}
		BlockEntity blockEntity = level.getBlockEntity(payload.pos());
		if (!(blockEntity instanceof WorkerBlockEntity worker) || worker.kind() != WorkerKind.BUILDER) {
			return;
		}
		if (payload.count() <= 0 || payload.count() > BuilderCaptureChunkPayload.MAX_CHUNKS) {
			tell(player, "zkwars-workers.builder.too_big");
			return;
		}
		long key = payload.pos().asLong();
		Pending current = this.pending.get(key);
		if (current == null || current.token != payload.token() || current.count != payload.count() || !current.player.equals(player.getUUID())) {
			current = new Pending(player.getUUID(), payload.token(), payload.count());
			this.pending.put(key, current);
		}
		if (current.chunks[payload.index()] == null) {
			current.received++;
		}
		current.chunks[payload.index()] = payload.cells();
		if (current.received < current.count) {
			return;
		}
		this.pending.remove(key);
		this.commit(player, level, payload.pos(), current);
	}

	private void commit(ServerPlayer player, ServerLevel level, BlockPos origin, Pending incoming) {
		int total = 0;
		for (List<BuilderCaptureChunkPayload.Cell> chunk : incoming.chunks) {
			if (chunk == null) {
				return;
			}
			total += chunk.size();
			if (total > MAX_BLOCKS) {
				tell(player, "zkwars-workers.builder.too_big");
				return;
			}
		}
		Map<String, Integer> paletteIds = new LinkedHashMap<>();
		Map<Integer, Integer> byPos = new LinkedHashMap<>();
		HolderLookup<Block> lookup = level.registryAccess().lookupOrThrow(Registries.BLOCK);
		for (List<BuilderCaptureChunkPayload.Cell> chunk : incoming.chunks) {
			for (BuilderCaptureChunkPayload.Cell cell : chunk) {
				if (outOfRange(cell.dx(), cell.dy(), cell.dz())) {
					tell(player, "zkwars-workers.builder.too_far");
					return;
				}
				BlockState state = parse(lookup, cell.state());
				if (state == null || !SchematicBlocks.placeable(state)) {
					continue;
				}
				String canonical = BlockStateText.stringify(state);
				int palette = paletteIds.computeIfAbsent(canonical, ignored -> paletteIds.size());
				byPos.put(packPos(cell.dx(), cell.dy(), cell.dz()), palette);
			}
		}
		if (byPos.isEmpty()) {
			tell(player, "zkwars-workers.builder.empty");
			return;
		}
		List<Integer> positions = new ArrayList<>(byPos.keySet());
		positions.sort(Comparator.comparingInt(BuilderTasks::posY).thenComparingInt(BuilderTasks::posX).thenComparingInt(BuilderTasks::posZ));
		List<String> palette = new ArrayList<>(paletteIds.keySet());
		long[] entries = new long[positions.size()];
		for (int i = 0; i < positions.size(); i++) {
			int packed = positions.get(i);
			entries[i] = ((long) byPos.get(packed) << 27) | (packed & ((1 << 27) - 1));
		}
		this.tasks.put(origin.asLong(), new Task(palette, entries));
		this.setDirty();
		WorkerAdvancements.grant(player, WorkerActions.SCHEMATIC);
		tell(player, "zkwars-workers.builder.accepted", entries.length);
	}

	private static BlockState parse(HolderLookup<Block> lookup, String text) {
		try {
			return BlockStateParser.parseForBlock(lookup, text, false).blockState();
		} catch (RuntimeException | com.mojang.brigadier.exceptions.CommandSyntaxException ignored) {
			return null;
		}
	}

	private static boolean outOfRange(int dx, int dy, int dz) {
		return Math.abs(dx) > RANGE || Math.abs(dy) > RANGE || Math.abs(dz) > RANGE;
	}

	private static int packPos(int dx, int dy, int dz) {
		return ((dx + RANGE) << 18) | ((dy + RANGE) << 9) | (dz + RANGE);
	}

	private static int posX(int packed) {
		return ((packed >> 18) & 511) - RANGE;
	}

	private static int posY(int packed) {
		return ((packed >> 9) & 511) - RANGE;
	}

	private static int posZ(int packed) {
		return (packed & 511) - RANGE;
	}

	private static void tell(ServerPlayer player, String key, Object... args) {
		player.displayClientMessage(Component.translatable(key, args), false);
	}

	private static BuilderTasks load(CompoundTag tag, HolderLookup.Provider lookup) {
		BuilderTasks data = new BuilderTasks();
		ListTag list = tag.getList("Tasks", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag taskTag = list.getCompound(i);
			long pos = taskTag.getLong("Pos");
			ListTag paletteTag = taskTag.getList("Palette", Tag.TAG_STRING);
			List<String> palette = new ArrayList<>(paletteTag.size());
			for (int p = 0; p < paletteTag.size(); p++) {
				palette.add(paletteTag.getString(p));
			}
			long[] entries = taskTag.getLongArray("Entries");
			if (entries.length == 0 || entries.length > MAX_BLOCKS || palette.isEmpty()) {
				continue;
			}
			Task task = new Task(palette, entries);
			task.cursor = Math.floorMod(taskTag.getInt("Cursor"), entries.length);
			task.waiting = taskTag.getBoolean("Waiting");
			task.placedAny = taskTag.getBoolean("Placed");
			data.tasks.put(pos, task);
		}
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		ListTag list = new ListTag();
		for (Map.Entry<Long, Task> entry : this.tasks.entrySet()) {
			Task task = entry.getValue();
			if (task.entries.length == 0) {
				continue;
			}
			CompoundTag taskTag = new CompoundTag();
			taskTag.putLong("Pos", entry.getKey());
			taskTag.putInt("Cursor", task.cursor);
			taskTag.putBoolean("Waiting", task.waiting);
			taskTag.putBoolean("Placed", task.placedAny);
			ListTag palette = new ListTag();
			for (String state : task.palette) {
				palette.add(StringTag.valueOf(state));
			}
			taskTag.put("Palette", palette);
			taskTag.putLongArray("Entries", task.entries);
			list.add(taskTag);
		}
		tag.put("Tasks", list);
		return tag;
	}

	public static final class Task {
		private final List<String> palette;
		private final long[] entries;
		private int cursor;
		private boolean waiting;
		private boolean placedAny;
		private int matchedRun;
		private transient BlockState[] resolved;

		private Task(List<String> palette, long[] entries) {
			this.palette = List.copyOf(palette);
			this.entries = entries;
		}

		public int size() {
			return this.entries.length;
		}

		public int cursor() {
			return this.cursor;
		}

		public void setCursor(int cursor) {
			this.cursor = cursor;
		}

		public void setWaiting(boolean waiting) {
			this.waiting = waiting;
		}

		public void notePlaced() {
			this.placedAny = true;
			this.matchedRun = 0;
		}

		public void notePresent() {
			if (this.matchedRun < this.entries.length) {
				this.matchedRun++;
			}
		}

		public void noteBreak() {
			this.matchedRun = 0;
		}

		public boolean finished() {
			return this.placedAny && this.matchedRun >= this.entries.length;
		}

		public BlockPos pos(BlockPos origin, int index) {
			long entry = this.entries[index];
			int packed = (int) (entry & ((1L << 27) - 1));
			return origin.offset(posX(packed), posY(packed), posZ(packed));
		}

		public BlockState state(ServerLevel level, int index) {
			this.resolve(level);
			int paletteIndex = (int) (this.entries[index] >> 27);
			if (paletteIndex < 0 || paletteIndex >= this.resolved.length) {
				return null;
			}
			return this.resolved[paletteIndex];
		}

		private void resolve(ServerLevel level) {
			if (this.resolved != null) {
				return;
			}
			HolderLookup<Block> lookup = level.registryAccess().lookupOrThrow(Registries.BLOCK);
			BlockState[] states = new BlockState[this.palette.size()];
			for (int i = 0; i < this.palette.size(); i++) {
				BlockState state = parse(lookup, this.palette.get(i));
				states[i] = state != null && SchematicBlocks.placeable(state) ? state : null;
			}
			this.resolved = states;
		}
	}

	private static final class Pending {
		private final UUID player;
		private final int token;
		private final int count;
		private final List<BuilderCaptureChunkPayload.Cell>[] chunks;
		private int received;

		@SuppressWarnings("unchecked")
		private Pending(UUID player, int token, int count) {
			this.player = player;
			this.token = token;
			this.count = count;
			this.chunks = new List[count];
		}
	}
}
