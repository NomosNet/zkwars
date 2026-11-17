package com.example.inventory;

import com.example.block.WorkerKind;
import com.example.compat.GensokyoFoodCompat;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

import org.jetbrains.annotations.Nullable;

public final class WorkerSlots {
	public static final int SLOT_COUNT = 27;
	public static final int SMELTER_SECTION = 18;
	public static final int CRAFTER_PATTERN = 9;
	public static final int CRAFTER_RESOURCES = 18;
	public static final int CRAFTER_OUTPUT = 9;

	private WorkerSlots() {
	}

	public static int slotCount(WorkerKind kind) {
		if (kind == WorkerKind.SMELTER) {
			return SMELTER_SECTION * 3;
		}
		if (kind == WorkerKind.CRAFTER) {
			return CRAFTER_PATTERN + CRAFTER_RESOURCES + CRAFTER_OUTPUT;
		}
		return SLOT_COUNT;
	}

	public static int inputCount(WorkerKind kind) {
		if (kind == WorkerKind.FARMER) {
			return 3;
		}
		if (kind == WorkerKind.SMELTER) {
			return SMELTER_SECTION;
		}
		return 0;
	}

	public static int patternCount(WorkerKind kind) {
		return kind == WorkerKind.CRAFTER ? CRAFTER_PATTERN : 0;
	}

	public static int resourceStart(WorkerKind kind) {
		return patternCount(kind);
	}

	public static int resourceCount(WorkerKind kind) {
		return kind == WorkerKind.CRAFTER ? CRAFTER_RESOURCES : 0;
	}

	public static int fuelStart(WorkerKind kind) {
		return kind == WorkerKind.SMELTER ? SMELTER_SECTION : 0;
	}

	public static int fuelCount(WorkerKind kind) {
		return kind == WorkerKind.SMELTER ? SMELTER_SECTION : 0;
	}

	public static int outputStart(WorkerKind kind) {
		if (kind == WorkerKind.SMELTER) {
			return SMELTER_SECTION * 2;
		}
		if (kind == WorkerKind.CRAFTER) {
			return CRAFTER_PATTERN + CRAFTER_RESOURCES;
		}
		return inputCount(kind);
	}

	public static boolean isPatternSlot(WorkerKind kind, int index) {
		return index >= 0 && index < patternCount(kind);
	}

	public static boolean isResourceSlot(WorkerKind kind, int index) {
		int start = resourceStart(kind);
		int count = resourceCount(kind);
		return count > 0 && index >= start && index < start + count;
	}

	public static boolean isInputSlot(WorkerKind kind, int index) {
		if (kind == WorkerKind.CRAFTER) {
			return isResourceSlot(kind, index);
		}
		return index >= 0 && index < inputCount(kind);
	}

	public static boolean isFuelSlot(WorkerKind kind, int index) {
		int start = fuelStart(kind);
		int count = fuelCount(kind);
		return count > 0 && index >= start && index < start + count;
	}

	public static boolean isOutputSlot(WorkerKind kind, int index) {
		return index >= outputStart(kind) && index < slotCount(kind);
	}

	public static boolean mayPlace(WorkerKind kind, int index, ItemStack stack) {
		return mayPlace(kind, index, stack, null);
	}

	public static boolean mayPlace(WorkerKind kind, int index, ItemStack stack, @Nullable Level level) {
		if (kind == WorkerKind.SMELTER) {
			if (stack.isEmpty() || isOutputSlot(kind, index)) {
				return false;
			}
			if (isFuelSlot(kind, index)) {
				return isFuel(stack);
			}
			if (isInputSlot(kind, index)) {
				return level == null || isSmeltable(level, stack);
			}
			return false;
		}
		if (kind == WorkerKind.CRAFTER) {
			if (stack.isEmpty() || isOutputSlot(kind, index)) {
				return false;
			}
			return isPatternSlot(kind, index) || isResourceSlot(kind, index);
		}
		if (!kind.restrictsInput() || !isInputSlot(kind, index)) {
			return true;
		}
		return switch (kind) {
			case FARMER -> isCropSeed(stack) || stack.is(Items.BONE_MEAL);
			case CHICKEN -> stack.is(ItemTags.CHICKEN_FOOD);
			case COW -> stack.is(ItemTags.COW_FOOD);
			case SHEEP -> stack.is(ItemTags.SHEEP_FOOD);
			case PIG -> stack.is(ItemTags.PIG_FOOD);
			case QUARRY, LUMBERJACK, BUILDER, SMELTER, FISHER, CRAFTER -> true;
		};
	}

	public static boolean matchesPattern(Container container, ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		for (int slot = 0; slot < CRAFTER_PATTERN && slot < container.getContainerSize(); slot++) {
			ItemStack pattern = container.getItem(slot);
			if (!pattern.isEmpty() && ItemStack.isSameItemSameComponents(pattern, stack)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isSmeltable(@Nullable Level level, ItemStack stack) {
		if (level == null || stack.isEmpty()) {
			return false;
		}
		return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level).isPresent();
	}

	public static boolean isFuel(ItemStack stack) {
		return !stack.isEmpty() && AbstractFurnaceBlockEntity.isFuel(stack);
	}

	public static boolean isCropSeed(ItemStack stack) {
		if (!(stack.getItem() instanceof BlockItem blockItem)) {
			return false;
		}
		Block block = blockItem.getBlock();
		return block instanceof CropBlock
				|| block instanceof StemBlock
				|| block instanceof NetherWartBlock
				|| block instanceof SweetBerryBushBlock
				|| GensokyoFoodCompat.isRiceSeed(stack);
	}
}
