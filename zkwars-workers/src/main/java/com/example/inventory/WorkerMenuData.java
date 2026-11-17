package com.example.inventory;

import com.example.block.WorkerKind;
import com.example.block.entity.WorkerBlockEntity;
import com.example.block.work.PenWork;
import com.example.block.work.QuarryBounds;
import com.example.block.work.WorkArea;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record WorkerMenuData(
		WorkerKind kind,
		BlockPos pos,
		int minX,
		int maxX,
		int minY,
		int maxY,
		int minZ,
		int maxZ,
		int radius,
		int adultLimit
) {
	public static final StreamCodec<RegistryFriendlyByteBuf, WorkerMenuData> STREAM_CODEC = StreamCodec.of(
			WorkerMenuData::encode,
			WorkerMenuData::decode
	);

	public QuarryBounds.Box bounds() {
		return new QuarryBounds.Box(this.minX, this.maxX, this.minY, this.maxY, this.minZ, this.maxZ);
	}

	public static WorkerMenuData of(WorkerBlockEntity block) {
		QuarryBounds.Box box = block.quarryBounds();
		return new WorkerMenuData(
				block.kind(),
				block.getBlockPos(),
				box.minX(),
				box.maxX(),
				box.minY(),
				box.maxY(),
				box.minZ(),
				box.maxZ(),
				block.workRadius(),
				block.adultLimit()
		);
	}

	private static void encode(RegistryFriendlyByteBuf buf, WorkerMenuData data) {
		buf.writeEnum(data.kind);
		buf.writeBlockPos(data.pos);
		buf.writeVarInt(data.minX);
		buf.writeVarInt(data.maxX);
		buf.writeVarInt(data.minY);
		buf.writeVarInt(data.maxY);
		buf.writeVarInt(data.minZ);
		buf.writeVarInt(data.maxZ);
		buf.writeVarInt(data.radius);
		buf.writeVarInt(data.adultLimit);
	}

	private static WorkerMenuData decode(RegistryFriendlyByteBuf buf) {
		WorkerKind kind = buf.readEnum(WorkerKind.class);
		BlockPos pos = buf.readBlockPos();
		int minX = buf.readVarInt();
		int maxX = buf.readVarInt();
		int minY = buf.readVarInt();
		int maxY = buf.readVarInt();
		int minZ = buf.readVarInt();
		int maxZ = buf.readVarInt();
		int radius = WorkArea.clampRadius(kind, buf.readVarInt());
		int adultLimit = PenWork.clampAdults(buf.readVarInt());
		return new WorkerMenuData(kind, pos, minX, maxX, minY, maxY, minZ, maxZ, radius, adultLimit);
	}
}
