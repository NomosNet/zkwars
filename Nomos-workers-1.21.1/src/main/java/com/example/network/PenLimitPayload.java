package com.example.network;

import com.example.ZKWorkers;
import com.example.block.work.PenWork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PenLimitPayload(BlockPos pos, int limit) implements CustomPacketPayload {
	public static final Type<PenLimitPayload> TYPE = new Type<>(ZKWorkers.id("pen_limit"));
	public static final StreamCodec<RegistryFriendlyByteBuf, PenLimitPayload> CODEC = StreamCodec.of(
			PenLimitPayload::encode,
			PenLimitPayload::decode
	);

	public int clampedLimit() {
		return PenWork.clampAdults(this.limit);
	}

	private static void encode(RegistryFriendlyByteBuf buf, PenLimitPayload payload) {
		buf.writeBlockPos(payload.pos);
		buf.writeVarInt(payload.limit);
	}

	private static PenLimitPayload decode(RegistryFriendlyByteBuf buf) {
		return new PenLimitPayload(buf.readBlockPos(), buf.readVarInt());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
