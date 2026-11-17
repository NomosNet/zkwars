package com.example.item;

import com.example.block.WorkerBeds;
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
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class CourierScrollItem extends Item {
	public static final int MAX_WORKER_BLOCKS = 5;

	private final String prefix;

	public CourierScrollItem(Properties properties, String prefix) {
		super(properties.stacksTo(1));
		this.prefix = prefix;
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
		if (state.getBlock() instanceof ChestBlock) {
			return setChest(level, player, stack, pos);
		}
		if (ModBlocks.isWorkerBlock(state)) {
			return addWorkerBlock(level, player, stack, pos);
		}
		if (ModBlocks.isBed(state)) {
			return setBed(level, player, stack, pos);
		}
		return InteractionResult.PASS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		CompoundTag tag = ScrollData.get(stack);
		if (!tag.contains(ScrollData.CHEST_POS) && ScrollData.blockList(tag).isEmpty() && !tag.contains(ScrollData.BED_POS)) {
			tooltip.add(Component.translatable(prefix + ".empty"));
			return;
		}
		ScrollData.pos(tag, ScrollData.CHEST_POS).ifPresentOrElse(
				pos -> tooltip.add(Component.translatable(prefix + ".chest_info", pos.getX(), pos.getY(), pos.getZ())),
				() -> tooltip.add(Component.translatable(prefix + ".no_chest"))
		);
		List<BlockPos> blocks = ScrollData.blockList(tag);
		if (blocks.isEmpty()) {
			tooltip.add(Component.translatable(prefix + ".no_blocks"));
		} else {
			tooltip.add(Component.translatable(prefix + ".blocks_info", blocks.size(), MAX_WORKER_BLOCKS));
		}
		ScrollData.pos(tag, ScrollData.BED_POS).ifPresentOrElse(
				pos -> tooltip.add(Component.translatable(prefix + ".bed_info", pos.getX(), pos.getY(), pos.getZ())),
				() -> tooltip.add(Component.translatable(prefix + ".no_bed"))
		);
	}

	public static boolean isComplete(ItemStack stack) {
		CompoundTag tag = ScrollData.get(stack);
		return tag.contains(ScrollData.CHEST_POS) && !ScrollData.blockList(tag).isEmpty() && tag.contains(ScrollData.BED_POS);
	}

	private InteractionResult setChest(Level level, Player player, ItemStack stack, BlockPos pos) {
		CompoundTag tag = ScrollData.get(stack);
		if (tag.contains(ScrollData.CHEST_POS)) {
			tell(level, player, prefix + ".chest_already_set");
			return InteractionResult.FAIL;
		}
		if (level.isClientSide) {
			return InteractionResult.SUCCESS;
		}
		tag.putLong(ScrollData.CHEST_POS, pos.asLong());
		ScrollData.set(stack, tag);
		tell(level, player, prefix + ".chest_set", pos.getX(), pos.getY(), pos.getZ());
		return InteractionResult.CONSUME;
	}

	private InteractionResult addWorkerBlock(Level level, Player player, ItemStack stack, BlockPos pos) {
		CompoundTag tag = ScrollData.get(stack);
		if (!tag.contains(ScrollData.CHEST_POS)) {
			tell(level, player, prefix + ".chest_not_set");
			return InteractionResult.FAIL;
		}
		if (ScrollData.containsBlock(tag, pos)) {
			tell(level, player, prefix + ".block_already_added");
			return InteractionResult.FAIL;
		}
		if (ScrollData.blockList(tag).size() >= MAX_WORKER_BLOCKS) {
			tell(level, player, prefix + ".max_blocks_reached");
			return InteractionResult.FAIL;
		}
		if (level.isClientSide) {
			return InteractionResult.SUCCESS;
		}
		ScrollData.addBlock(tag, pos);
		int count = ScrollData.blockList(tag).size();
		ScrollData.set(stack, tag);
		tell(level, player, prefix + ".block_added", count, MAX_WORKER_BLOCKS);
		return InteractionResult.CONSUME;
	}

	private InteractionResult setBed(Level level, Player player, ItemStack stack, BlockPos pos) {
		CompoundTag tag = ScrollData.get(stack);
		if (!tag.contains(ScrollData.CHEST_POS)) {
			tell(level, player, prefix + ".chest_not_set");
			return InteractionResult.FAIL;
		}
		if (tag.contains(ScrollData.BED_POS)) {
			tell(level, player, prefix + ".bed_already_set");
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
		tell(level, player, prefix + ".bed_set", head.getX(), head.getY(), head.getZ());
		return InteractionResult.CONSUME;
	}

	private static void tell(Level level, Player player, String key, Object... args) {
		if (!level.isClientSide) {
			player.displayClientMessage(Component.translatable(key, args), true);
		}
	}
}
