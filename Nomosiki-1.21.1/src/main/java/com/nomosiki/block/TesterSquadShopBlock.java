package com.nomosiki.block;

import com.nomosiki.screen.TesterSquadShopMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public class TesterSquadShopBlock extends Block {
	public TesterSquadShopBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected @NotNull InteractionResult useWithoutItem(
		@NotNull BlockState state,
		Level level,
		@NotNull BlockPos pos,
		@NotNull Player player,
		@NotNull BlockHitResult hitResult
	) {
		if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
			serverPlayer.openMenu(new TesterSquadShopMenuProvider(pos));
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}
}
