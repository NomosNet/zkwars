package com.example.block.work;

import com.example.advancement.WorkerActions;
import com.example.advancement.WorkerAdvancements;
import com.example.block.WorkerKind;
import com.example.block.entity.WorkerBlockEntity;
import com.example.entity.HumanWorkerEntity;
import com.example.inventory.WorkerSlots;
import com.example.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CraftWork {
	private CraftWork() {
	}

	public static void tick(ServerLevel level, BlockPos origin, WorkerBlockEntity block) {
		HumanWorkerEntity crafter = WorkerSupport.findWorker(level, origin, ModEntities.HUMAN_CRAFTER, false);
		if (crafter == null) {
			return;
		}
		List<ItemStack> grid = pattern(block);
		if (grid.isEmpty()) {
			return;
		}
		CraftingInput input = CraftingInput.of(3, 3, grid);
		Optional<RecipeHolder<CraftingRecipe>> found = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
		if (found.isEmpty()) {
			return;
		}
		CraftingRecipe recipe = found.get().value();
		ItemStack result = recipe.assemble(input, level.registryAccess());
		if (result.isEmpty()) {
			return;
		}
		List<ItemStack> drops = new ArrayList<>();
		drops.add(result.copy());
		for (ItemStack leftover : recipe.getRemainingItems(input)) {
			if (!leftover.isEmpty()) {
				drops.add(leftover.copy());
			}
		}
		if (!WorkerSupport.canFitOutput(block, drops) || !consume(block, grid)) {
			return;
		}
		WorkerSupport.insertOutput(block, drops);
		WorkerAdvancements.grant(crafter, WorkerActions.CRAFTED);
		crafter.playWorkSwing(origin.getX() + 0.5, origin.getY() + 1.0, origin.getZ() + 0.5);
	}

	private static List<ItemStack> pattern(WorkerBlockEntity block) {
		List<ItemStack> grid = new ArrayList<>(WorkerSlots.patternCount(WorkerKind.CRAFTER));
		boolean any = false;
		for (int slot = 0; slot < WorkerSlots.patternCount(WorkerKind.CRAFTER); slot++) {
			ItemStack pattern = block.getItem(slot);
			if (pattern.isEmpty()) {
				grid.add(ItemStack.EMPTY);
				continue;
			}
			any = true;
			ItemStack one = pattern.copy();
			one.setCount(1);
			grid.add(one);
		}
		return any ? grid : List.of();
	}

	private static boolean consume(WorkerBlockEntity block, List<ItemStack> grid) {
		int start = WorkerSlots.resourceStart(WorkerKind.CRAFTER);
		int count = WorkerSlots.resourceCount(WorkerKind.CRAFTER);
		int[] take = new int[count];
		int[] left = new int[count];
		ItemStack[] stacks = new ItemStack[count];
		for (int index = 0; index < count; index++) {
			stacks[index] = block.getItem(start + index);
			left[index] = stacks[index].getCount();
		}
		for (ItemStack ingredient : grid) {
			if (ingredient.isEmpty()) {
				continue;
			}
			int found = -1;
			for (int index = 0; index < count; index++) {
				if (left[index] <= 0 || !ItemStack.isSameItemSameComponents(stacks[index], ingredient)) {
					continue;
				}
				found = index;
				break;
			}
			if (found < 0) {
				return false;
			}
			left[found]--;
			take[found]++;
		}
		for (int index = 0; index < count; index++) {
			if (take[index] > 0) {
				block.removeItem(start + index, take[index]);
			}
		}
		return true;
	}
}
