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
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class WorkerScrollItem extends Item {
	public WorkerScrollItem(Properties properties) {
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
		if (ModBlocks.isWorkerBlock(state)) {
			return setWorkBlock(level, player, stack, pos);
		}
		if (ModBlocks.isBed(state)) {
			return setBed(level, player, stack, pos);
		}
		return InteractionResult.PASS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		CompoundTag tag = ScrollData.get(stack);
		if (!tag.contains(ScrollData.WORK_BLOCK_POS) && !tag.contains(ScrollData.BED_POS)) {
			tooltip.add(Component.translatable("item.zkwars-workers.worker_scroll.empty"));
			return;
		}
		ScrollData.pos(tag, ScrollData.WORK_BLOCK_POS).ifPresentOrElse(
				pos -> tooltip.add(Component.translatable("item.zkwars-workers.worker_scroll.work_block_info", pos.getX(), pos.getY(), pos.getZ())),
				() -> tooltip.add(Component.translatable("item.zkwars-workers.worker_scroll.no_work_block"))
		);
		ScrollData.pos(tag, ScrollData.BED_POS).ifPresentOrElse(
				pos -> tooltip.add(Component.translatable("item.zkwars-workers.worker_scroll.bed_info", pos.getX(), pos.getY(), pos.getZ())),
				() -> tooltip.add(Component.translatable("item.zkwars-workers.worker_scroll.no_bed"))
		);
	}

	public static boolean isComplete(ItemStack stack) {
		CompoundTag tag = ScrollData.get(stack);
		return tag.contains(ScrollData.WORK_BLOCK_POS) && tag.contains(ScrollData.BED_POS);
	}

	private static InteractionResult setWorkBlock(Level level, Player player, ItemStack stack, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof WorkerBlock workerBlock && workerBlock.kind() == WorkerKind.SMELTER) {
			tell(level, player, "item.zkwars-workers.worker_scroll.smelter");
			return InteractionResult.FAIL;
		}
		CompoundTag tag = ScrollData.get(stack);
		if (tag.contains(ScrollData.WORK_BLOCK_POS)) {
			tell(level, player, "item.zkwars-workers.worker_scroll.work_block_already_set");
			return InteractionResult.FAIL;
		}
		if (level.isClientSide) {
			return InteractionResult.SUCCESS;
		}
		tag.putLong(ScrollData.WORK_BLOCK_POS, pos.asLong());
		ScrollData.set(stack, tag);
		WorkerAdvancements.grant(player, WorkerActions.WORK_BLOCK_SET);
		tell(level, player, "item.zkwars-workers.worker_scroll.work_block_set", pos.getX(), pos.getY(), pos.getZ());
		return InteractionResult.CONSUME;
	}

	private static InteractionResult setBed(Level level, Player player, ItemStack stack, BlockPos pos) {
		CompoundTag tag = ScrollData.get(stack);
		if (!tag.contains(ScrollData.WORK_BLOCK_POS)) {
			tell(level, player, "item.zkwars-workers.worker_scroll.work_block_not_set");
			return InteractionResult.FAIL;
		}
		if (tag.contains(ScrollData.BED_POS)) {
			tell(level, player, "item.zkwars-workers.worker_scroll.bed_already_set");
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
		WorkerAdvancements.grant(player, WorkerActions.BED_SET);
		tell(level, player, "item.zkwars-workers.worker_scroll.bed_set", head.getX(), head.getY(), head.getZ());
		return InteractionResult.CONSUME;
	}

	private static void tell(Level level, Player player, String key, Object... args) {
		if (!level.isClientSide) {
			player.displayClientMessage(Component.translatable(key, args), true);
		}
	}
}
