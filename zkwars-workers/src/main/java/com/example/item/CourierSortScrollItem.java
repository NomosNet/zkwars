package com.example.item;

import com.example.block.WorkerBeds;
import com.example.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CourierSortScrollItem extends CourierScrollItem {
	public static final int MAX_CONTAINERS = 32;

	private final String prefix;

	public CourierSortScrollItem(Properties properties, String prefix) {
		super(properties, prefix);
		this.prefix = prefix;
	}

	public static boolean isStorage(BlockState state) {
		return state.getBlock() instanceof ChestBlock
				|| state.getBlock() instanceof BarrelBlock
				|| state.getBlock() instanceof ShulkerBoxBlock;
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
		if (isStorage(state)) {
			return addContainer(level, player, stack, pos);
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
			tooltip.add(Component.translatable(this.prefix + ".empty"));
			return;
		}
		ScrollData.pos(tag, ScrollData.CHEST_POS).ifPresentOrElse(
				pos -> tooltip.add(Component.translatable(this.prefix + ".chest_info", pos.getX(), pos.getY(), pos.getZ())),
				() -> tooltip.add(Component.translatable(this.prefix + ".no_chest"))
		);
		List<BlockPos> blocks = ScrollData.blockList(tag);
		if (blocks.isEmpty()) {
			tooltip.add(Component.translatable(this.prefix + ".no_blocks"));
		} else {
			tooltip.add(Component.translatable(this.prefix + ".blocks_info", blocks.size(), MAX_CONTAINERS));
		}
		ScrollData.pos(tag, ScrollData.BED_POS).ifPresentOrElse(
				pos -> tooltip.add(Component.translatable(this.prefix + ".bed_info", pos.getX(), pos.getY(), pos.getZ())),
				() -> tooltip.add(Component.translatable(this.prefix + ".no_bed"))
		);
	}

	private InteractionResult addContainer(Level level, Player player, ItemStack stack, BlockPos pos) {
		CompoundTag tag = ScrollData.get(stack);
		if (!tag.contains(ScrollData.CHEST_POS)) {
			if (level.isClientSide) {
				return InteractionResult.SUCCESS;
			}
			tag.putLong(ScrollData.CHEST_POS, pos.asLong());
			ScrollData.set(stack, tag);
			tell(level, player, this.prefix + ".chest_set", pos.getX(), pos.getY(), pos.getZ());
			return InteractionResult.CONSUME;
		}
		BlockPos source = ScrollData.pos(tag, ScrollData.CHEST_POS).orElse(null);
		if (sameContainer(level, source, pos) || ScrollData.containsBlock(tag, pos) || listedTwin(level, tag, pos)) {
			tell(level, player, sameContainer(level, source, pos) ? this.prefix + ".same_container" : this.prefix + ".block_already_added");
			return InteractionResult.FAIL;
		}
		if (ScrollData.blockList(tag).size() >= MAX_CONTAINERS) {
			tell(level, player, this.prefix + ".max_blocks_reached");
			return InteractionResult.FAIL;
		}
		if (level.isClientSide) {
			return InteractionResult.SUCCESS;
		}
		ScrollData.addBlock(tag, pos);
		int count = ScrollData.blockList(tag).size();
		ScrollData.set(stack, tag);
		tell(level, player, this.prefix + ".block_added", count, MAX_CONTAINERS);
		return InteractionResult.CONSUME;
	}

	private InteractionResult setBed(Level level, Player player, ItemStack stack, BlockPos pos) {
		CompoundTag tag = ScrollData.get(stack);
		if (!tag.contains(ScrollData.CHEST_POS)) {
			tell(level, player, this.prefix + ".chest_not_set");
			return InteractionResult.FAIL;
		}
		if (tag.contains(ScrollData.BED_POS)) {
			tell(level, player, this.prefix + ".bed_already_set");
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
		tell(level, player, this.prefix + ".bed_set", head.getX(), head.getY(), head.getZ());
		return InteractionResult.CONSUME;
	}

	private static boolean listedTwin(Level level, CompoundTag tag, BlockPos pos) {
		BlockPos twin = connectedChest(level, pos);
		return twin != null && ScrollData.containsBlock(tag, twin);
	}

	private static boolean sameContainer(Level level, @Nullable BlockPos source, BlockPos pos) {
		if (source == null) {
			return false;
		}
		if (source.equals(pos)) {
			return true;
		}
		BlockPos twin = connectedChest(level, source);
		return twin != null && twin.equals(pos);
	}

	private static @Nullable BlockPos connectedChest(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof ChestBlock) || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
			return null;
		}
		return pos.relative(ChestBlock.getConnectedDirection(state));
	}

	private static void tell(Level level, Player player, String key, Object... args) {
		if (!level.isClientSide) {
			player.displayClientMessage(Component.translatable(key, args), true);
		}
	}
}
