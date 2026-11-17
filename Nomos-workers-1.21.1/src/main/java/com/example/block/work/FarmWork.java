package com.example.block.work;

import com.example.advancement.WorkerActions;
import com.example.advancement.WorkerAdvancements;
import com.example.block.WorkerKind;
import com.example.block.entity.WorkerBlockEntity;
import com.example.compat.GensokyoFoodCompat;
import com.example.entity.HumanWorkerEntity;
import com.example.inventory.WorkerSlots;
import com.example.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.util.List;

public final class FarmWork {
	private FarmWork() {
	}

	public static void tick(ServerLevel level, BlockPos origin, WorkerBlockEntity block) {
		HumanWorkerEntity farmer = WorkerSupport.readyWorker(level, origin, ModEntities.HUMAN_FARMER, block);
		if (farmer == null) {
			return;
		}
		BlockPos target = findMature(level, origin);
		if (target == null) {
			if (!sow(level, origin, block, farmer) && !growWithBoneMeal(level, origin, block, farmer)) {
				till(level, origin, block, farmer);
			}
			return;
		}
		BlockState state = level.getBlockState(target);
		List<ItemStack> drops = Block.getDrops(state, level, target, level.getBlockEntity(target), farmer, farmer.getMainHandItem());
		Block plant = state.getBlock();
		BlockState planted = plant.defaultBlockState();
		boolean replant = canReplant(plant) && planted.canSurvive(level, target);
		boolean fromDrops = replant && takeMatchingSeed(drops, plant);
		if (!WorkerSupport.canFitOutput(block, drops)) {
			return;
		}
		level.levelEvent(2001, target, Block.getId(state));
		if (!level.destroyBlock(target, false, farmer)) {
			return;
		}
		WorkerSupport.insertOutput(block, drops);
		WorkerAdvancements.grant(farmer, WorkerActions.FARM_HARVEST);
		if (replant && (fromDrops || takeInputSeed(block, plant))) {
			level.setBlock(target, planted, Block.UPDATE_ALL);
		}
		farmer.wearEquippedTool(block);
		farmer.playWorkSwing(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
	}

	private static boolean takeMatchingSeed(List<ItemStack> drops, Block plant) {
		for (ItemStack drop : drops) {
			if (drop.isEmpty() || !(drop.getItem() instanceof BlockItem blockItem)) {
				continue;
			}
			if (blockItem.getBlock() == plant && WorkerSlots.isCropSeed(drop)) {
				drop.shrink(1);
				return true;
			}
		}
		return false;
	}

	private static boolean takeInputSeed(WorkerBlockEntity block, Block plant) {
		int slot = findSeed(block, plant);
		if (slot < 0) {
			return false;
		}
		block.removeItem(slot, 1);
		return true;
	}

	private static boolean sow(ServerLevel level, BlockPos origin, WorkerBlockEntity block, HumanWorkerEntity farmer) {
		BlockPos farmland = findEmptyFarmland(level, origin);
		if (farmland != null && plantCrop(level, farmland.above(), block, farmer)) {
			return true;
		}
		return plantRice(level, origin, block, farmer);
	}

	private static void till(ServerLevel level, BlockPos origin, WorkerBlockEntity block, HumanWorkerEntity farmer) {
		if (findEmptyFarmland(level, origin) != null || riceWithoutSoilSeed(block)) {
			return;
		}
		BlockPos soil = findTillable(level, origin);
		if (soil == null) {
			return;
		}
		level.setBlock(soil, Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL);
		WorkerAdvancements.grant(farmer, WorkerActions.FARM_TILLED);
		farmer.wearEquippedTool(block);
		farmer.playWorkSwing(soil.getX() + 0.5, soil.getY() + 0.5, soil.getZ() + 0.5);
	}

	private static boolean growWithBoneMeal(ServerLevel level, BlockPos origin, WorkerBlockEntity block, HumanWorkerEntity farmer) {
		int slot = findBoneMeal(block);
		if (slot < 0) {
			return false;
		}
		BlockPos target = findNearest(level, origin, pos -> canBonemeal(level, pos));
		if (target == null) {
			return false;
		}
		BlockState state = level.getBlockState(target);
		if (!(state.getBlock() instanceof BonemealableBlock growable) || !growable.isValidBonemealTarget(level, target, state)) {
			return false;
		}
		ItemStack meal = block.getItem(slot);
		if (growable.isBonemealSuccess(level, level.random, target, state)) {
			growable.performBonemeal(level, level.random, target, state);
		}
		meal.shrink(1);
		level.levelEvent(1505, target, 0);
		if (meal.isEmpty()) {
			block.setItem(slot, ItemStack.EMPTY);
		} else {
			block.setChanged();
		}
		farmer.playWorkSwing(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
		return true;
	}

	private static int findBoneMeal(WorkerBlockEntity block) {
		for (int slot = 0; slot < block.getContainerSize(); slot++) {
			if (block.getItem(slot).is(Items.BONE_MEAL)) {
				return slot;
			}
		}
		return -1;
	}

	private static boolean canBonemeal(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		Block block = state.getBlock();
		if (!isFarmPlant(block) || !(block instanceof BonemealableBlock growable)) {
			return false;
		}
		return growable.isValidBonemealTarget(level, pos, state);
	}

	private static boolean isFarmPlant(Block block) {
		if (block instanceof CropBlock || block instanceof StemBlock || block instanceof NetherWartBlock || block instanceof SweetBerryBushBlock) {
			return true;
		}
		Block rice = GensokyoFoodCompat.riceBlock();
		return rice != null && block == rice;
	}

	private static boolean plantRice(ServerLevel level, BlockPos origin, WorkerBlockEntity block, HumanWorkerEntity farmer) {
		int slot = findRiceSeed(block);
		Block rice = GensokyoFoodCompat.riceBlock();
		if (slot < 0 || rice == null) {
			return false;
		}
		BlockPos water = findRiceWater(level, origin);
		if (water == null) {
			return false;
		}
		BlockState planted = rice.defaultBlockState();
		if (!planted.canSurvive(level, water)) {
			return false;
		}
		block.removeItem(slot, 1);
		level.setBlock(water, planted, Block.UPDATE_ALL);
		farmer.playWorkSwing(water.getX() + 0.5, water.getY() + 0.5, water.getZ() + 0.5);
		return true;
	}

	private static boolean plantCrop(ServerLevel level, BlockPos cropPos, WorkerBlockEntity block, HumanWorkerEntity farmer) {
		int slot = findSeedAt(level, cropPos, block);
		if (slot < 0 || !(block.getItem(slot).getItem() instanceof BlockItem blockItem)) {
			return false;
		}
		block.removeItem(slot, 1);
		level.setBlock(cropPos, blockItem.getBlock().defaultBlockState(), Block.UPDATE_ALL);
		farmer.playWorkSwing(cropPos.getX() + 0.5, cropPos.getY() + 0.5, cropPos.getZ() + 0.5);
		return true;
	}

	private static int findRiceSeed(WorkerBlockEntity block) {
		for (int slot = 0; slot < WorkerSlots.inputCount(block.kind()); slot++) {
			if (GensokyoFoodCompat.isRiceSeed(block.getItem(slot))) {
				return slot;
			}
		}
		return -1;
	}

	private static boolean riceWithoutSoilSeed(WorkerBlockEntity block) {
		boolean rice = false;
		boolean soil = false;
		for (int slot = 0; slot < WorkerSlots.inputCount(block.kind()); slot++) {
			ItemStack stack = block.getItem(slot);
			if (GensokyoFoodCompat.isRiceSeed(stack)) {
				rice = true;
				continue;
			}
			if (!(stack.getItem() instanceof BlockItem blockItem) || !WorkerSlots.isCropSeed(stack)) {
				continue;
			}
			Block planted = blockItem.getBlock();
			if (planted instanceof CropBlock || planted instanceof StemBlock) {
				soil = true;
			}
		}
		return rice && !soil;
	}

	private static int findSeedAt(ServerLevel level, BlockPos pos, WorkerBlockEntity block) {
		for (int slot = 0; slot < WorkerSlots.inputCount(block.kind()); slot++) {
			ItemStack stack = block.getItem(slot);
			if (GensokyoFoodCompat.isRiceSeed(stack) || !(stack.getItem() instanceof BlockItem blockItem) || !WorkerSlots.isCropSeed(stack)) {
				continue;
			}
			if (blockItem.getBlock().defaultBlockState().canSurvive(level, pos)) {
				return slot;
			}
		}
		return -1;
	}

	private static BlockPos findEmptyFarmland(ServerLevel level, BlockPos origin) {
		return findNearest(level, origin, pos -> level.getBlockState(pos).is(Blocks.FARMLAND) && level.getBlockState(pos.above()).isAir());
	}

	private static BlockPos findTillable(ServerLevel level, BlockPos origin) {
		return findNearest(level, origin, pos -> {
			BlockState state = level.getBlockState(pos);
			return (state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK)) && level.getBlockState(pos.above()).isAir();
		});
	}

	private static BlockPos findRiceWater(ServerLevel level, BlockPos origin) {
		return findNearest(level, origin, pos -> {
			BlockState state = level.getBlockState(pos);
			if (!state.is(Blocks.WATER)) {
				return false;
			}
			FluidState fluid = level.getFluidState(pos);
			if (!fluid.is(FluidTags.WATER) || !fluid.isSource()) {
				return false;
			}
			BlockState below = level.getBlockState(pos.below());
			return below.is(BlockTags.DIRT) || below.is(Blocks.FARMLAND);
		});
	}

	private static BlockPos findNearest(ServerLevel level, BlockPos origin, java.util.function.Predicate<BlockPos> test) {
		QuarryBounds.Box box = WorkArea.box(WorkerKind.FARMER, WorkArea.DEFAULT_RADIUS);
		BlockPos nearest = null;
		double best = Double.MAX_VALUE;
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int dx = box.minX(); dx <= box.maxX(); dx++) {
			for (int dy = box.minY(); dy <= box.maxY(); dy++) {
				for (int dz = box.minZ(); dz <= box.maxZ(); dz++) {
					cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
					if (!test.test(cursor)) {
						continue;
					}
					double distance = origin.distSqr(cursor);
					if (distance < best) {
						best = distance;
						nearest = cursor.immutable();
					}
				}
			}
		}
		return nearest;
	}

	private static int findSeed(WorkerBlockEntity block, Block plant) {
		for (int slot = 0; slot < WorkerSlots.inputCount(block.kind()); slot++) {
			ItemStack stack = block.getItem(slot);
			if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() == plant && WorkerSlots.isCropSeed(stack)) {
				return slot;
			}
		}
		return -1;
	}

	private static boolean canReplant(Block plant) {
		if (GensokyoFoodCompat.isRicePanicles(plant)) {
			return false;
		}
		return plant instanceof CropBlock || plant instanceof NetherWartBlock || plant instanceof SweetBerryBushBlock;
	}

	private static BlockPos findMature(ServerLevel level, BlockPos origin) {
		return findNearest(level, origin, pos -> isMature(level.getBlockState(pos)));
	}

	private static boolean isMature(BlockState state) {
		Block block = state.getBlock();
		if (block instanceof CropBlock crop) {
			return crop.isMaxAge(state);
		}
		if (block instanceof NetherWartBlock) {
			return state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE;
		}
		if (block instanceof SweetBerryBushBlock) {
			return state.getValue(SweetBerryBushBlock.AGE) >= 3;
		}
		return state.is(Blocks.PUMPKIN) || state.is(Blocks.MELON);
	}
}
