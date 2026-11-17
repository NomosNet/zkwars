package com.nomosiki.network.squad.payload.c2s.command;

import com.nomosiki.NomosikiMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * C2S: place one or more squads along a dragged front (Total War-style deploy).
 */
public record SquadDeployFormationPayload(List<Entry> entries) implements CustomPacketPayload {
	public static final int MAX_ENTRIES = 16;

	public record Entry(
		UUID squadId,
		BlockPos anchor,
		float facingYaw,
		float spacing,
		int maxInRow,
		byte orderId,
		int zoneRadius
	) {
	}

	public static final CustomPacketPayload.Type<SquadDeployFormationPayload> TYPE =
		new CustomPacketPayload.Type<>(NomosikiMod.id("squad_deploy_formation"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SquadDeployFormationPayload> CODEC = StreamCodec.of(
		SquadDeployFormationPayload::encode,
		SquadDeployFormationPayload::decode
	);

	private static void encode(RegistryFriendlyByteBuf buf, SquadDeployFormationPayload payload) {
		int count = Math.min(payload.entries.size(), MAX_ENTRIES);
		buf.writeVarInt(count);
		for (int i = 0; i < count; i++) {
			Entry e = payload.entries.get(i);
			buf.writeUUID(e.squadId());
			BlockPos.STREAM_CODEC.encode(buf, e.anchor());
			buf.writeFloat(e.facingYaw());
			buf.writeFloat(e.spacing());
			buf.writeVarInt(e.maxInRow());
			buf.writeByte(e.orderId());
			buf.writeVarInt(e.zoneRadius());
		}
	}

	private static SquadDeployFormationPayload decode(RegistryFriendlyByteBuf buf) {
		int count = Math.min(buf.readVarInt(), MAX_ENTRIES);
		List<Entry> entries = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			UUID squadId = buf.readUUID();
			BlockPos anchor = BlockPos.STREAM_CODEC.decode(buf);
			float facingYaw = buf.readFloat();
			float spacing = buf.readFloat();
			int maxInRow = buf.readVarInt();
			byte orderId = buf.readByte();
			int zoneRadius = buf.readVarInt();
			entries.add(new Entry(squadId, anchor, facingYaw, spacing, maxInRow, orderId, zoneRadius));
		}
		return new SquadDeployFormationPayload(List.copyOf(entries));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
