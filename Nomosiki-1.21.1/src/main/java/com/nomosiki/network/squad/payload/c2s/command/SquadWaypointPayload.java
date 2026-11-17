package com.nomosiki.network.squad.payload.c2s.command;

import com.nomosiki.NomosikiMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record SquadWaypointPayload(byte actionId, List<UUID> squadIds, List<BlockPos> points) implements CustomPacketPayload {
	public static final byte ACTION_ADD = 0;
	public static final byte ACTION_CLEAR = 1;
	public static final byte ACTION_REPLACE_ALL = 2;
	private static final int MAX_SQUADS = 16;
	private static final int MAX_POINTS = 8;

	public static final CustomPacketPayload.Type<SquadWaypointPayload> TYPE =
		new CustomPacketPayload.Type<>(NomosikiMod.id("squad_waypoint"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SquadWaypointPayload> CODEC = StreamCodec.of(
		SquadWaypointPayload::encode,
		SquadWaypointPayload::decode
	);

	private static void encode(RegistryFriendlyByteBuf buf, SquadWaypointPayload p) {
		buf.writeByte(p.actionId);
		int squadCount = Math.min(p.squadIds.size(), MAX_SQUADS);
		buf.writeVarInt(squadCount);
		for (int i = 0; i < squadCount; i++) {
			buf.writeUUID(p.squadIds.get(i));
		}
		int pointCount = Math.min(p.points.size(), MAX_POINTS);
		buf.writeVarInt(pointCount);
		for (int i = 0; i < pointCount; i++) {
			BlockPos.STREAM_CODEC.encode(buf, p.points.get(i));
		}
	}

	private static SquadWaypointPayload decode(RegistryFriendlyByteBuf buf) {
		byte actionId = buf.readByte();
		int squadCount = Math.min(buf.readVarInt(), MAX_SQUADS);
		List<UUID> squadIds = new ArrayList<>(squadCount);
		for (int i = 0; i < squadCount; i++) {
			squadIds.add(buf.readUUID());
		}
		int pointCount = Math.min(buf.readVarInt(), MAX_POINTS);
		List<BlockPos> points = new ArrayList<>(pointCount);
		for (int i = 0; i < pointCount; i++) {
			points.add(BlockPos.STREAM_CODEC.decode(buf));
		}
		return new SquadWaypointPayload(actionId, List.copyOf(squadIds), List.copyOf(points));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
