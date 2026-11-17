package com.example.item;

import com.example.advancement.WorkerActions;
import com.example.advancement.WorkerAdvancements;
import com.example.block.WorkerBeds;
import com.example.block.WorkerBlock;
import com.example.block.WorkerKind;
import com.example.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class SmelterScrollItem extends Item {
	public static final int MAX_FURNACES = 10;
	private static final String PREFIX = "item.zk-workers.smelter_scroll";

	public SmelterScrollItem(Properties properties) {
		super(properties.stacksTo(1));
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState state = level.getBlockState(pos);
		Player player = context.getPlayer();
		if (player == null) {
			return InteractionResult.PASS;
		}
		ItemStack stack = context.getItemInHand();
		if (state.getBlock() instanceof WorkerBlock) {
			return setWorkBlock(level, player, stack, pos);
		}
		if (state.getBlock() instanceof FurnaceBlock) {
			if (!player.isShiftKeyDown()) {
				return InteractionResult.PASS;
			}
			CompoundTag tag = ScrollData.get(stack);
			if (ScrollData.containsFurnace(tag, pos)) {
				return removeFurnace(level, player, stack, pos);
			}
			return addFurnace(level, player, stack, pos);
		}
		if (ModBlocks.isBed(state)) {
			return setBed(level, player, stack, pos);
		}
		return InteractionResult.PASS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		CompoundTag tag = ScrollData.get(stack);
		if (!tag.contains(ScrollData.WORK_BLOCK_POS) && ScrollData.furnaceList(tag).isEmpty() && !tag.contains(ScrollData.BED_POS)) {
			tooltip.add(Component.translatable(PREFIX + ".empty"));
			return;
		}
		ScrollData.pos(tag, ScrollData.WORK_BLOCK_POS).ifPresentOrElse(
				pos -> tooltip.add(Component.translatable(PREFIX + ".work_block_info", pos.getX(), pos.getY(), pos.getZ())),
				() -> tooltip.add(Component.translatable(PREFIX + ".no_work_block"))
		);
		List<BlockPos> furnaces = ScrollData.furnaceList(tag);
		if (furnaces.isEmpty()) {
			tooltip.add(Component.translatable(PREFIX + ".no_furnaces"));
		} else {
			tooltip.add(Component.translatable(PREFIX + ".furnaces_info", furnaces.size(), MAX_FURNACES));
		}
		ScrollData.pos(tag, ScrollData.BED_POS).ifPresentOrElse(
				pos -> tooltip.add(Component.translatable(PREFIX + ".bed_info", pos.getX(), pos.getY(), pos.getZ())),
				() -> tooltip.add(Component.translatable(PREFIX + ".no_bed"))
		);
	}

	public static boolean isComplete(ItemStack stack) {
		CompoundTag tag = ScrollData.get(stack);
		return tag.contains(ScrollData.WORK_BLOCK_POS) && tag.contains(ScrollData.BED_POS);
	}

	private static InteractionResult setWorkBlock(Level level, Player player, ItemStack stack, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof WorkerBlock workerBlock) || workerBlock.kind() != WorkerKind.SMELTER) {
			tell(level, player, PREFIX + ".not_smelter");
			return InteractionResult.FAIL;
		}
		CompoundTag tag = ScrollData.get(stack);
		if (tag.contains(ScrollData.WORK_BLOCK_POS)) {
			tell(level, player, PREFIX + ".work_block_already_set");
			return InteractionResult.FAIL;
		}
		if (level.isClientSide) {
			return InteractionResult.SUCCESS;
		}
		tag.putLong(ScrollData.WORK_BLOCK_POS, pos.asLong());
		ScrollData.set(stack, tag);
		tell(level, player, PREFIX + ".work_block_set", pos.getX(), pos.getY(), pos.getZ());
		return InteractionResult.CONSUME;
	}

	private static InteractionResult addFurnace(Level level, Player player, ItemStack stack, BlockPos pos) {
		CompoundTag tag = ScrollData.get(stack);
		if (!tag.contains(ScrollData.WORK_BLOCK_POS)) {
			tell(level, player, PREFIX + ".work_block_not_set");
			return InteractionResult.FAIL;
		}
		if (ScrollData.containsFurnace(tag, pos)) {
			tell(level, player, PREFIX + ".furnace_already_added");
			return InteractionResult.FAIL;
		}
		if (ScrollData.furnaceList(tag).size() >= MAX_FURNACES) {
			tell(level, player, PREFIX + ".max_furnaces_reached");
			return InteractionResult.FAIL;
		}
		if (level.isClientSide) {
			return InteractionResult.SUCCESS;
		}
		ScrollData.addFurnace(tag, pos);
		int count = ScrollData.furnaceList(tag).size();
		ScrollData.set(stack, tag);
		WorkerAdvancements.grant(player, WorkerActions.FURNACE_ADDED);
		if (count >= MAX_FURNACES) {
			WorkerAdvancements.grant(player, WorkerActions.SMELTER_TEN);
		}
		tell(level, player, PREFIX + ".furnace_added", count, MAX_FURNACES);
		return InteractionResult.CONSUME;
	}

	private static InteractionResult removeFurnace(Level level, Player player, ItemStack stack, BlockPos pos) {
		CompoundTag tag = ScrollData.get(stack);
		if (!ScrollData.containsFurnace(tag, pos)) {
			tell(level, player, PREFIX + ".furnace_not_added");
			return InteractionResult.FAIL;
		}
		if (level.isClientSide) {
			return InteractionResult.SUCCESS;
		}
		ScrollData.removeFurnace(tag, pos);
		int count = ScrollData.furnaceList(tag).size();
		ScrollData.set(stack, tag);
		tell(level, player, PREFIX + ".furnace_removed", count, MAX_FURNACES);
		return InteractionResult.CONSUME;
	}

	private static InteractionResult setBed(Level level, Player player, ItemStack stack, BlockPos pos) {
		CompoundTag tag = ScrollData.get(stack);
		if (!tag.contains(ScrollData.WORK_BLOCK_POS)) {
			tell(level, player, PREFIX + ".work_block_not_set");
			return InteractionResult.FAIL;
		}
		if (tag.contains(ScrollData.BED_POS)) {
			tell(level, player, PREFIX + ".bed_already_set");
			return InteractionResult.FAIL;
		}
		BlockPos head = WorkerBeds.head(level, pos);
		if (head == null) {
			return InteractionResult.FAIL;
		}
		if (level.isClientSide) {
			return InteractionResult.SUCCESS;
		}
		tag.putLong(ScrollData.BED_POS, head.asLong());
		ScrollData.set(stack, tag);
		tell(level, player, PREFIX + ".bed_set", head.getX(), head.getY(), head.getZ());
		return InteractionResult.CONSUME;
	}

	private static void tell(Level level, Player player, String key, Object... args) {
		if (!level.isClientSide) {
			player.displayClientMessage(Component.translatable(key, args), true);
		}
	}
}
