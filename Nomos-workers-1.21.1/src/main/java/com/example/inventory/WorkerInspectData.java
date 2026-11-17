package com.example.inventory;

import com.example.entity.HumanWorkerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record WorkerInspectData(
		int entityId,
		String statusKey,
		boolean courier,
		boolean smelter,
		boolean supply,
		Optional<BlockPos> work,
		Optional<BlockPos> bed,
		Optional<BlockPos> chest,
		List<BlockPos> route
) {
	public static final StreamCodec<RegistryFriendlyByteBuf, WorkerInspectData> STREAM_CODEC = StreamCodec.of(
			WorkerInspectData::encode,
			WorkerInspectData::decode
	);

	public static WorkerInspectData capture(HumanWorkerEntity worker) {
		return new WorkerInspectData(
				worker.getId(),
				worker.statusKey(),
				worker.isCourier(),
				worker.isSmelter(),
				worker.courierSupply(),
				Optional.ofNullable(worker.getWorkBlockPos()),
				Optional.ofNullable(worker.getBedPos()),
				Optional.ofNullable(worker.chestPos()),
				List.copyOf(worker.route())
		);
	}

	public boolean hasOrder() {
		if (this.courier) {
			return this.chest.isPresent() || this.bed.isPresent() || !this.route.isEmpty();
		}
		return this.work.isPresent() || this.bed.isPresent();
	}

	private static void encode(RegistryFriendlyByteBuf buf, WorkerInspectData data) {
		buf.writeVarInt(data.entityId);
		buf.writeUtf(data.statusKey);
		buf.writeBoolean(data.courier);
		buf.writeBoolean(data.smelter);
		buf.writeBoolean(data.supply);
		writePos(buf, data.work);
		writePos(buf, data.bed);
		writePos(buf, data.chest);
		buf.writeVarInt(data.route.size());
		for (BlockPos pos : data.route) {
			buf.writeBlockPos(pos);
		}
	}

	private static WorkerInspectData decode(RegistryFriendlyByteBuf buf) {
		int entityId = buf.readVarInt();
		String statusKey = buf.readUtf();
		boolean courier = buf.readBoolean();
		boolean smelter = buf.readBoolean();
		boolean supply = buf.readBoolean();
		Optional<BlockPos> work = readPos(buf);
		Optional<BlockPos> bed = readPos(buf);
		Optional<BlockPos> chest = readPos(buf);
		int routeSize = buf.readVarInt();
		List<BlockPos> route = new ArrayList<>(Math.max(0, routeSize));
		for (int index = 0; index < routeSize; index++) {
			route.add(buf.readBlockPos());
		}
		return new WorkerInspectData(entityId, statusKey, courier, smelter, supply, work, bed, chest, List.copyOf(route));
	}

	private static void writePos(RegistryFriendlyByteBuf buf, Optional<BlockPos> pos) {
		buf.writeBoolean(pos.isPresent());
		pos.ifPresent(buf::writeBlockPos);
	}

	private static Optional<BlockPos> readPos(RegistryFriendlyByteBuf buf) {
		return buf.readBoolean() ? Optional.of(buf.readBlockPos()) : Optional.empty();
	}
}
