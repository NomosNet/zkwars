package com.example.block.work;

import com.example.block.WorkerKind;
import com.example.advancement.WorkerActions;
import com.example.advancement.WorkerAdvancements;
import com.example.block.entity.WorkerBlockEntity;
import com.example.compat.GensokyoFoodCompat;
import com.example.entity.HumanWorkerEntity;
import com.example.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.FluidState;

import java.util.ArrayList;
import java.util.List;

public final class FisherWork {
	private FisherWork() {
	}

	public static void tick(ServerLevel level, BlockPos origin, WorkerBlockEntity block) {
		HumanWorkerEntity fisher = WorkerSupport.readyWorker(level, origin, ModEntities.HUMAN_FISHER, block);
		if (fisher == null || !hasWater(level, origin, block)) {
			return;
		}
		ItemStack caught = new ItemStack(roll(level));
		if (!WorkerSupport.canFitOutput(block, List.of(caught))) {
			return;
		}
		WorkerSupport.insertOutput(block, List.of(caught));
		WorkerAdvancements.grant(fisher, WorkerActions.FISH_CAUGHT);
		fisher.wearEquippedTool(block);
		fisher.playWorkSwing(origin.getX() + 0.5, origin.getY() + 1.0, origin.getZ() + 0.5);
	}

	private static boolean hasWater(ServerLevel level, BlockPos origin, WorkerBlockEntity block) {
		QuarryBounds.Box box = WorkArea.box(WorkerKind.FISHER, block.workRadius());
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int dx = box.minX(); dx <= box.maxX(); dx++) {
			for (int dy = box.minY(); dy <= box.maxY(); dy++) {
				for (int dz = box.minZ(); dz <= box.maxZ(); dz++) {
					cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
					FluidState fluid = level.getFluidState(cursor);
					if (fluid.is(FluidTags.WATER) && fluid.isSource()) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private static Item roll(ServerLevel level) {
		List<Item> fish = new ArrayList<>();
		fish.add(Items.COD);
		fish.add(Items.SALMON);
		fish.add(Items.TROPICAL_FISH);
		fish.add(Items.PUFFERFISH);
		GensokyoFoodCompat.addCaughtFish(fish);
		return fish.get(level.getRandom().nextInt(fish.size()));
	}
}
