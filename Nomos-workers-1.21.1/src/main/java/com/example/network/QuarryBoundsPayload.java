package com.example.network;

import com.example.ZKWorkers;
import com.example.block.work.QuarryBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record QuarryBoundsPayload(
		BlockPos pos,
		int minX,
		int maxX,
		int minY,
		int maxY,
		int minZ,
		int maxZ
) implements CustomPacketPayload {
	public static final Type<QuarryBoundsPayload> TYPE = new Type<>(ZKWorkers.id("quarry_bounds"));
	public static final StreamCodec<RegistryFriendlyByteBuf, QuarryBoundsPayload> CODEC = StreamCodec.of(
			QuarryBoundsPayload::encode,
			QuarryBoundsPayload::decode
	);

	public QuarryBounds.Box bounds() {
		return QuarryBounds.clamp(this.minX, this.maxX, this.minY, this.maxY, this.minZ, this.maxZ);
	}

	private static void encode(RegistryFriendlyByteBuf buf, QuarryBoundsPayload payload) {
		buf.writeBlockPos(payload.pos);
		buf.writeVarInt(payload.minX);
		buf.writeVarInt(payload.maxX);
		buf.writeVarInt(payload.minY);
		buf.writeVarInt(payload.maxY);
		buf.writeVarInt(payload.minZ);
		buf.writeVarInt(payload.maxZ);
	}

	private static QuarryBoundsPayload decode(RegistryFriendlyByteBuf buf) {
		return new QuarryBoundsPayload(
				buf.readBlockPos(),
				buf.readVarInt(),
				buf.readVarInt(),
				buf.readVarInt(),
				buf.readVarInt(),
				buf.readVarInt(),
				buf.readVarInt()
		);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
