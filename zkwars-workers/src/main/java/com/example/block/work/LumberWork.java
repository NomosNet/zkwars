package com.example.block.work;

import com.example.block.WorkerKind;
import com.example.advancement.WorkerActions;
import com.example.advancement.WorkerAdvancements;
import com.example.block.entity.WorkerBlockEntity;
import com.example.entity.HumanWorkerEntity;
import com.example.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FungusBlock;
import net.minecraft.world.level.block.MangrovePropaguleBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class LumberWork {
	private LumberWork() {
	}

	public static void tick(ServerLevel level, BlockPos origin, WorkerBlockEntity block) {
		HumanWorkerEntity lumberjack = WorkerSupport.readyWorker(level, origin, ModEntities.HUMAN_LUMBERJACK, block);
		if (lumberjack == null) {
			return;
		}
		pickupSaplings(level, origin, block);
		if (block.hasLumberQueue()) {
			if (chopHead(level, block, lumberjack)) {
				return;
			}
		}
		if (block.lumberBase() != null && !block.lumberFoliage() && hasShears(lumberjack)) {
			List<BlockPos> leaves = collectLeaves(level, origin, block);
			if (!leaves.isEmpty()) {
				block.beginLumberFoliage(leaves);
				chopHead(level, block, lumberjack);
				return;
			}
		}
		if (block.lumberBase() != null) {
			plant(level, block, lumberjack);
			return;
		}
		BlockPos seed = block.takeLumberSeed();
		if (seed != null) {
			begin(level, origin, block, lumberjack, seed);
			return;
		}
		BlockPos nearest = findNearestLog(level, origin, block);
		if (nearest != null) {
			begin(level, origin, block, lumberjack, nearest);
		}
	}

	private static void begin(ServerLevel level, BlockPos origin, WorkerBlockEntity block, HumanWorkerEntity lumberjack, BlockPos seed) {
		BlockState state = level.getBlockState(seed);
		if (!state.is(BlockTags.LOGS)) {
			return;
		}
		Block wood = state.getBlock();
		BlockPos base = trunkBase(level, seed, wood);
		BlockPos corner = base;
		int span = 1;
		BlockPos square = squareCorner(level, base, wood);
		if (square != null) {
			corner = square;
			span = 2;
		}
		List<BlockPos> logs = collect(level, origin, block, corner, span, wood, seed);
		if (logs.isEmpty()) {
			return;
		}
		block.setLumberJob(corner, span, wood, logs);
		chopHead(level, block, lumberjack);
	}

	private static boolean chopHead(ServerLevel level, WorkerBlockEntity block, HumanWorkerEntity lumberjack) {
		if (block.lumberFoliage() && !hasShears(lumberjack)) {
			block.beginLumberFoliage(List.of());
			return false;
		}
		Block wood = block.lumberWood();
		for (int attempt = 0; attempt < WorkArea.MAX_TREE_LOGS && block.hasLumberQueue(); attempt++) {
			BlockPos target = block.lumberQueueHead();
			if (target == null) {
				return false;
			}
			BlockState state = level.getBlockState(target);
			boolean expected = block.lumberFoliage() ? isFoliage(state) : isTrunk(state, wood);
			if (!expected) {
				block.popLumberQueue();
				continue;
			}
			if (!chop(level, target, block, lumberjack)) {
				return true;
			}
			block.popLumberQueue();
			return true;
		}
		return false;
	}

	private static void plant(ServerLevel level, WorkerBlockEntity block, HumanWorkerEntity lumberjack) {
		BlockPos base = block.lumberBase();
		if (base == null) {
			block.clearLumberJob();
			return;
		}
		if (block.lumberSpan() >= 2) {
			plantSquare(level, block, lumberjack, base);
			return;
		}
		plantOne(level, block, lumberjack, base);
		block.clearLumberJob();
	}

	private static void plantOne(ServerLevel level, WorkerBlockEntity block, HumanWorkerEntity lumberjack, BlockPos base) {
		if (!level.getBlockState(base).isAir()) {
			return;
		}
		Block sapling = saplingFor(block.lumberWood());
		if (sapling == null) {
			return;
		}
		int slot = findSapling(level, base, block, sapling);
		if (slot < 0) {
			return;
		}
		ItemStack stack = block.getItem(slot);
		if (!(stack.getItem() instanceof BlockItem blockItem)) {
			return;
		}
		BlockState planted = blockItem.getBlock().defaultBlockState();
		if (!planted.canSurvive(level, base)) {
			return;
		}
		block.removeItem(slot, 1);
		level.setBlock(base, planted, Block.UPDATE_ALL);
		WorkerAdvancements.grant(lumberjack, WorkerActions.SAPLING_PLANTED);
		lumberjack.playWorkSwing(base.getX() + 0.5, base.getY() + 0.5, base.getZ() + 0.5);
	}

	private static void plantSquare(ServerLevel level, WorkerBlockEntity block, HumanWorkerEntity lumberjack, BlockPos base) {
		Block sapling = saplingFor(block.lumberWood());
		if (sapling == null) {
			block.clearLumberJob();
			return;
		}
		List<BlockPos> open = new ArrayList<>(4);
		for (int dx = 0; dx < 2; dx++) {
			for (int dz = 0; dz < 2; dz++) {
				BlockPos pos = base.offset(dx, 0, dz);
				BlockState state = level.getBlockState(pos);
				if (state.getBlock() == sapling) {
					continue;
				}
				if (!state.isAir() || !sapling.defaultBlockState().canSurvive(level, pos)) {
					block.clearLumberJob();
					return;
				}
				open.add(pos);
			}
		}
		if (open.isEmpty()) {
			block.clearLumberJob();
			return;
		}
		if (countSapling(block, sapling) < open.size()) {
			return;
		}
		for (BlockPos pos : open) {
			int slot = findSapling(level, pos, block, sapling);
			if (slot < 0) {
				return;
			}
			block.removeItem(slot, 1);
			level.setBlock(pos, sapling.defaultBlockState(), Block.UPDATE_ALL);
			WorkerAdvancements.grant(lumberjack, WorkerActions.SAPLING_PLANTED);
		}
		BlockPos swing = open.get(0);
		lumberjack.playWorkSwing(swing.getX() + 0.5, swing.getY() + 0.5, swing.getZ() + 0.5);
		block.clearLumberJob();
	}

	private static int countSapling(WorkerBlockEntity block, Block sapling) {
		int count = 0;
		for (int slot = 0; slot < block.getContainerSize(); slot++) {
			ItemStack stack = block.getItem(slot);
			if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() == sapling) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private static int findSapling(ServerLevel level, BlockPos pos, WorkerBlockEntity block, @Nullable Block wanted) {
		for (int slot = 0; slot < block.getContainerSize(); slot++) {
			ItemStack stack = block.getItem(slot);
			if (!(stack.getItem() instanceof BlockItem blockItem)) {
				continue;
			}
			Block plant = blockItem.getBlock();
			if (!(plant instanceof SaplingBlock || plant instanceof MangrovePropaguleBlock || plant instanceof FungusBlock)) {
				continue;
			}
			if (wanted != null && plant != wanted) {
				continue;
			}
			if (plant.defaultBlockState().canSurvive(level, pos)) {
				return slot;
			}
		}
		return -1;
	}

	private static @Nullable Block saplingFor(@Nullable Block wood) {
		if (wood == Blocks.OAK_LOG || wood == Blocks.OAK_WOOD) {
			return Blocks.OAK_SAPLING;
		}
		if (wood == Blocks.SPRUCE_LOG || wood == Blocks.SPRUCE_WOOD) {
			return Blocks.SPRUCE_SAPLING;
		}
		if (wood == Blocks.BIRCH_LOG || wood == Blocks.BIRCH_WOOD) {
			return Blocks.BIRCH_SAPLING;
		}
		if (wood == Blocks.JUNGLE_LOG || wood == Blocks.JUNGLE_WOOD) {
			return Blocks.JUNGLE_SAPLING;
		}
		if (wood == Blocks.ACACIA_LOG || wood == Blocks.ACACIA_WOOD) {
			return Blocks.ACACIA_SAPLING;
		}
		if (wood == Blocks.DARK_OAK_LOG || wood == Blocks.DARK_OAK_WOOD) {
			return Blocks.DARK_OAK_SAPLING;
		}
		if (wood == Blocks.CHERRY_LOG || wood == Blocks.CHERRY_WOOD) {
			return Blocks.CHERRY_SAPLING;
		}
		if (wood == Blocks.MANGROVE_LOG || wood == Blocks.MANGROVE_WOOD) {
			return Blocks.MANGROVE_PROPAGULE;
		}
		if (wood == Blocks.CRIMSON_STEM || wood == Blocks.CRIMSON_HYPHAE) {
			return Blocks.CRIMSON_FUNGUS;
		}
		if (wood == Blocks.WARPED_STEM || wood == Blocks.WARPED_HYPHAE) {
			return Blocks.WARPED_FUNGUS;
		}
		return null;
	}

	private static BlockPos trunkBase(ServerLevel level, BlockPos log, Block wood) {
		BlockPos base = log;
		while (level.getBlockState(base.below()).getBlock() == wood) {
			base = base.below();
		}
		return base;
	}

	private static @Nullable BlockPos squareCorner(ServerLevel level, BlockPos base, Block wood) {
		int[][] offsets = {{0, 0}, {-1, 0}, {0, -1}, {-1, -1}};
		for (int[] offset : offsets) {
			int x = base.getX() + offset[0];
			int z = base.getZ() + offset[1];
			if (isSquare(level, x, base.getY(), z, wood)) {
				return new BlockPos(x, base.getY(), z);
			}
		}
		return null;
	}

	private static boolean isSquare(ServerLevel level, int x, int y, int z, Block wood) {
		for (int dy = 0; dy < WorkArea.TRUNK_LAYERS; dy++) {
			for (int dx = 0; dx < 2; dx++) {
				for (int dz = 0; dz < 2; dz++) {
					if (level.getBlockState(new BlockPos(x + dx, y + dy, z + dz)).getBlock() != wood) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private static List<BlockPos> collect(ServerLevel level, BlockPos origin, WorkerBlockEntity block, BlockPos corner, int span, Block wood, BlockPos seed) {
		QuarryBounds.Box zone = WorkArea.box(WorkerKind.LUMBERJACK, block.workRadius());
		int minY = origin.getY() + zone.minY();
		int maxY = origin.getY() + zone.maxY();
		int columns = span * span;
		int[] columnX = new int[columns];
		int[] columnZ = new int[columns];
		int index = 0;
		for (int dx = 0; dx < span; dx++) {
			for (int dz = 0; dz < span; dz++) {
				columnX[index] = corner.getX() + dx;
				columnZ[index] = corner.getZ() + dz;
				index++;
			}
		}
		List<BlockPos> logs = new ArrayList<>();
		ArrayDeque<BlockPos> pending = new ArrayDeque<>();
		Set<Long> seen = new HashSet<>();
		for (int column = 0; column < columns; column++) {
			for (int y = minY; y <= maxY; y++) {
				offer(level, new BlockPos(columnX[column], y, columnZ[column]), wood, minY, maxY, columnX, columnZ, seen, pending);
			}
		}
		offer(level, seed, wood, minY, maxY, columnX, columnZ, seen, pending);
		while (!pending.isEmpty() && logs.size() < WorkArea.MAX_TREE_LOGS) {
			BlockPos pos = pending.poll();
			logs.add(pos);
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						if (dx == 0 && dy == 0 && dz == 0) {
							continue;
						}
						BlockPos next = pos.offset(dx, dy, dz);
						if (next.getY() < minY || next.getY() > maxY || !nearTrunk(next, columnX, columnZ)) {
							continue;
						}
						BlockState state = level.getBlockState(next);
						if (!isTrunk(state, wood)) {
							continue;
						}
						if (!inColumn(next, columnX, columnZ) && !isMangroveRoot(state) && !touchesFoliage(level, next)) {
							continue;
						}
						if (seen.add(next.asLong())) {
							pending.add(next);
						}
					}
				}
			}
		}
		logs.sort((left, right) -> {
			int y = Integer.compare(right.getY(), left.getY());
			if (y != 0) {
				return y;
			}
			int x = Integer.compare(left.getX(), right.getX());
			if (x != 0) {
				return x;
			}
			return Integer.compare(left.getZ(), right.getZ());
		});
		return logs;
	}

	private static void offer(ServerLevel level, BlockPos pos, Block wood, int minY, int maxY, int[] columnX, int[] columnZ, Set<Long> seen, ArrayDeque<BlockPos> pending) {
		if (pos.getY() < minY || pos.getY() > maxY) {
			return;
		}
		BlockState state = level.getBlockState(pos);
		if (!isTrunk(state, wood) || !nearTrunk(pos, columnX, columnZ)) {
			return;
		}
		if (!inColumn(pos, columnX, columnZ) && !isMangroveRoot(state) && !touchesFoliage(level, pos)) {
			return;
		}
		if (seen.add(pos.asLong())) {
			pending.add(pos);
		}
	}

	private static boolean inColumn(BlockPos pos, int[] columnX, int[] columnZ) {
		for (int index = 0; index < columnX.length; index++) {
			if (columnX[index] == pos.getX() && columnZ[index] == pos.getZ()) {
				return true;
			}
		}
		return false;
	}

	private static boolean nearTrunk(BlockPos pos, int[] columnX, int[] columnZ) {
		int limit = WorkArea.CANOPY * WorkArea.CANOPY;
		for (int index = 0; index < columnX.length; index++) {
			int dx = pos.getX() - columnX[index];
			int dz = pos.getZ() - columnZ[index];
			if (dx * dx + dz * dz <= limit) {
				return true;
			}
		}
		return false;
	}

	private static boolean touchesFoliage(ServerLevel level, BlockPos pos) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (dx == 0 && dy == 0 && dz == 0) {
						continue;
					}
					BlockState state = level.getBlockState(pos.offset(dx, dy, dz));
					if (state.is(BlockTags.LEAVES) || state.is(BlockTags.WART_BLOCKS)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private static boolean chop(ServerLevel level, BlockPos target, WorkerBlockEntity block, HumanWorkerEntity lumberjack) {
		BlockState state = level.getBlockState(target);
		ItemStack tool = block.lumberFoliage() ? ItemStack.EMPTY : lumberjack.getMainHandItem();
		List<ItemStack> drops = Block.getDrops(state, level, target, level.getBlockEntity(target), lumberjack, tool);
		if (!WorkerSupport.canFitOutput(block, drops)) {
			return false;
		}
		level.levelEvent(2001, target, Block.getId(state));
		if (!level.destroyBlock(target, false, lumberjack)) {
			return false;
		}
		WorkerSupport.insertOutput(block, drops);
		if (block.lumberFoliage()) {
			wearShears(lumberjack);
		} else {
			lumberjack.wearEquippedTool(block);
			WorkerAdvancements.grant(lumberjack, WorkerActions.TREE_FELLED);
		}
		lumberjack.playWorkSwing(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
		return true;
	}

	private static @Nullable BlockPos findNearestLog(ServerLevel level, BlockPos origin, WorkerBlockEntity block) {
		QuarryBounds.Box box = WorkArea.box(WorkerKind.LUMBERJACK, block.workRadius());
		int width = box.maxX() - box.minX() + 1;
		int columns = width * width;
		if (columns <= 0) {
			return null;
		}
		int start = Math.floorMod(block.lumberScanCursor(), columns);
		int budget = Math.min(WorkArea.LUMBER_SCAN_COLUMNS, columns);
		BlockPos nearest = null;
		double best = Double.MAX_VALUE;
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int step = 0; step < budget; step++) {
			int index = (start + step) % columns;
			int dx = box.minX() + index % width;
			int dz = box.minZ() + index / width;
			for (int dy = box.maxY(); dy >= box.minY(); dy--) {
				cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
				if (!level.getBlockState(cursor).is(BlockTags.LOGS)) {
					continue;
				}
				double distance = origin.distSqr(cursor);
				if (distance < best) {
					best = distance;
					nearest = cursor.immutable();
				}
				break;
			}
		}
		block.setLumberScanCursor((start + budget) % columns);
		return nearest;
	}

	private static boolean isTrunk(BlockState state, @Nullable Block wood) {
		if (wood == null) {
			return state.is(BlockTags.LOGS);
		}
		return state.getBlock() == wood || isMangroveRoot(state) && isMangroveWood(wood);
	}

	private static boolean isMangroveWood(Block wood) {
		return wood == Blocks.MANGROVE_LOG || wood == Blocks.MANGROVE_WOOD;
	}

	private static boolean isMangroveRoot(BlockState state) {
		Block block = state.getBlock();
		return block == Blocks.MANGROVE_ROOTS || block == Blocks.MUDDY_MANGROVE_ROOTS;
	}

	private static boolean isFoliage(BlockState state) {
		return state.is(BlockTags.LEAVES) || state.is(BlockTags.WART_BLOCKS);
	}

	private static boolean hasShears(HumanWorkerEntity lumberjack) {
		ItemStack stack = lumberjack.getOffhandItem();
		return stack.getItem() instanceof ShearsItem && HumanWorkerEntity.hasRemainingDurability(stack);
	}

	private static void wearShears(HumanWorkerEntity lumberjack) {
		ItemStack stack = lumberjack.getOffhandItem();
		if (stack.getItem() instanceof ShearsItem) {
			stack.hurtAndBreak(1, lumberjack, EquipmentSlot.OFFHAND);
		}
	}

	private static List<BlockPos> collectLeaves(ServerLevel level, BlockPos origin, WorkerBlockEntity block) {
		BlockPos base = block.lumberBase();
		if (base == null) {
			return List.of();
		}
		QuarryBounds.Box zone = WorkArea.box(WorkerKind.LUMBERJACK, block.workRadius());
		int minY = origin.getY() + zone.minY();
		int maxY = origin.getY() + zone.maxY();
		int span = Math.max(1, block.lumberSpan());
		int[] columnX = new int[span * span];
		int[] columnZ = new int[span * span];
		int index = 0;
		for (int dx = 0; dx < span; dx++) {
			for (int dz = 0; dz < span; dz++) {
				columnX[index] = base.getX() + dx;
				columnZ[index] = base.getZ() + dz;
				index++;
			}
		}
		List<BlockPos> leaves = new ArrayList<>();
		ArrayDeque<BlockPos> pending = new ArrayDeque<>();
		Set<Long> seen = new HashSet<>();
		for (int column = 0; column < columnX.length; column++) {
			for (int y = minY; y <= maxY; y++) {
				BlockPos columnPos = new BlockPos(columnX[column], y, columnZ[column]);
				offerLeaf(level, columnPos, minY, maxY, columnX, columnZ, seen, pending);
				for (int dx = -1; dx <= 1; dx++) {
					for (int dy = -1; dy <= 1; dy++) {
						for (int dz = -1; dz <= 1; dz++) {
							offerLeaf(level, columnPos.offset(dx, dy, dz), minY, maxY, columnX, columnZ, seen, pending);
						}
					}
				}
			}
		}
		while (!pending.isEmpty() && leaves.size() < WorkArea.MAX_TREE_LOGS) {
			BlockPos pos = pending.poll();
			leaves.add(pos);
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						if (dx == 0 && dy == 0 && dz == 0) {
							continue;
						}
						offerLeaf(level, pos.offset(dx, dy, dz), minY, maxY, columnX, columnZ, seen, pending);
					}
				}
			}
		}
		leaves.sort((left, right) -> Integer.compare(right.getY(), left.getY()));
		return leaves;
	}

	private static void offerLeaf(ServerLevel level, BlockPos pos, int minY, int maxY, int[] columnX, int[] columnZ, Set<Long> seen, ArrayDeque<BlockPos> pending) {
		if (pos.getY() < minY || pos.getY() > maxY || !nearTrunk(pos, columnX, columnZ)) {
			return;
		}
		if (!isFoliage(level.getBlockState(pos))) {
			return;
		}
		if (seen.add(pos.asLong())) {
			pending.add(pos);
		}
	}

	private static void pickupSaplings(ServerLevel level, BlockPos origin, WorkerBlockEntity block) {
		AABB box = WorkArea.volume(origin, WorkArea.box(WorkerKind.LUMBERJACK, block.workRadius()));
		for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, box, item -> isSapling(item.getItem()))) {
			ItemStack stack = entity.getItem().copy();
			if (!WorkerSupport.canFitOutput(block, List.of(stack))) {
				return;
			}
			WorkerSupport.insertOutput(block, List.of(stack));
			entity.discard();
		}
	}

	private static boolean isSapling(ItemStack stack) {
		if (!(stack.getItem() instanceof BlockItem blockItem)) {
			return false;
		}
		Block plant = blockItem.getBlock();
		return plant instanceof SaplingBlock || plant instanceof MangrovePropaguleBlock || plant instanceof FungusBlock;
	}
}
