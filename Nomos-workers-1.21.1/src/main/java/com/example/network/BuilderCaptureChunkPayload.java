package com.example.network;

import com.example.ZKWorkers;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

public record BuilderCaptureChunkPayload(BlockPos pos, int token, int index, int count, List<Cell> cells) implements CustomPacketPayload {
	public static final int MAX_CELLS = 200;
	public static final int MAX_CHUNKS = 1000;
	public static final Type<BuilderCaptureChunkPayload> TYPE = new Type<>(ZKWorkers.id("builder_capture"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BuilderCaptureChunkPayload> CODEC = StreamCodec.of(
			BuilderCaptureChunkPayload::encode,
			BuilderCaptureChunkPayload::decode
	);

	public record Cell(String state, int dx, int dy, int dz) {
	}

	private static void encode(RegistryFriendlyByteBuf buf, BuilderCaptureChunkPayload payload) {
		buf.writeBlockPos(payload.pos);
		buf.writeVarInt(payload.token);
		buf.writeVarInt(payload.index);
		buf.writeVarInt(payload.count);
		buf.writeVarInt(payload.cells.size());
		for (Cell cell : payload.cells) {
			buf.writeUtf(cell.state, 256);
			buf.writeVarInt(cell.dx);
			buf.writeVarInt(cell.dy);
			buf.writeVarInt(cell.dz);
		}
	}

	private static BuilderCaptureChunkPayload decode(RegistryFriendlyByteBuf buf) {
		BlockPos pos = buf.readBlockPos();
		int token = buf.readVarInt();
		int index = buf.readVarInt();
		int count = buf.readVarInt();
		int size = buf.readVarInt();
		if (count <= 0 || count > MAX_CHUNKS || index < 0 || index >= count || size < 0 || size > MAX_CELLS) {
			throw new DecoderException("Invalid builder capture");
		}
		List<Cell> cells = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			cells.add(new Cell(buf.readUtf(256), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
		}
		return new BuilderCaptureChunkPayload(pos, token, index, count, List.copyOf(cells));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
