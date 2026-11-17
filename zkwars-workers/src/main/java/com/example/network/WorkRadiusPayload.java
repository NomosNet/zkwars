package com.example.network;

import com.example.ZKWorkers;
import com.example.block.work.WorkArea;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record WorkRadiusPayload(BlockPos pos, int radius) implements CustomPacketPayload {
	public static final Type<WorkRadiusPayload> TYPE = new Type<>(ZKWorkers.id("work_radius"));
	public static final StreamCodec<RegistryFriendlyByteBuf, WorkRadiusPayload> CODEC = StreamCodec.of(
			WorkRadiusPayload::encode,
			WorkRadiusPayload::decode
	);

	public int clampedRadius() {
		return WorkArea.clampRadius(com.example.block.WorkerKind.LUMBERJACK, this.radius);
	}

	private static void encode(RegistryFriendlyByteBuf buf, WorkRadiusPayload payload) {
		buf.writeBlockPos(payload.pos);
		buf.writeVarInt(payload.radius);
	}

	private static WorkRadiusPayload decode(RegistryFriendlyByteBuf buf) {
		return new WorkRadiusPayload(buf.readBlockPos(), buf.readVarInt());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
