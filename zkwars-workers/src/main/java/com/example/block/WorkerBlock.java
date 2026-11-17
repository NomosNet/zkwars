package com.example.block;

import com.example.block.entity.WorkerBlockEntity;
import com.example.block.work.BuilderTasks;
import com.example.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

public class WorkerBlock extends BaseEntityBlock {
	private final WorkerKind kind;
	private final MapCodec<WorkerBlock> codec;

	public WorkerBlock(WorkerKind kind, Properties properties) {
		super(properties);
		this.kind = kind;
		this.codec = simpleCodec(next -> new WorkerBlock(kind, next));
	}

	public WorkerKind kind() {
		return this.kind;
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return this.codec;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new WorkerBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.WORKER, WorkerBlockEntity::serverTick);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		return this.openMenu(level, pos, player);
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		InteractionResult result = this.openMenu(level, pos, player);
		return result.consumesAction() ? ItemInteractionResult.CONSUME : ItemInteractionResult.SUCCESS;
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!state.is(newState.getBlock())) {
			BlockEntity blockEntity = level.getBlockEntity(pos);
			if (blockEntity instanceof WorkerBlockEntity worker) {
				Containers.dropContents(level, pos, worker);
			}
			if (this.kind == WorkerKind.BUILDER && level instanceof ServerLevel serverLevel) {
				BuilderTasks.get(serverLevel).remove(pos);
			}
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	private InteractionResult openMenu(Level level, BlockPos pos, Player player) {
		if (level.isClientSide) {
			return InteractionResult.SUCCESS;
		}
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity instanceof WorkerBlockEntity worker && player instanceof ServerPlayer serverPlayer) {
			serverPlayer.openMenu(worker);
			return InteractionResult.CONSUME;
		}
		return InteractionResult.PASS;
	}
}
