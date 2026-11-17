package com.nomosiki.network.squad.payload.c2s.command;



import com.nomosiki.NomosikiMod;

import com.nomosiki.entity.SquadOrder;

import net.minecraft.core.BlockPos;

import net.minecraft.network.RegistryFriendlyByteBuf;

import net.minecraft.network.codec.StreamCodec;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;



import java.util.ArrayList;
import java.util.List;
import java.util.UUID;



/**

 * @param targetEntityIds Recruits that should receive the order. When empty, every owned nomosik in command range on the server gets it.

 */

public record SquadCommandPayload(byte orderId, BlockPos anchor, int zoneRadius, List<Integer> targetEntityIds, List<UUID> orderForSquadIds) implements CustomPacketPayload {
	private static final int MAX_SQUAD_TARGETS = 16;

	public static final CustomPacketPayload.Type<SquadCommandPayload> TYPE =

		new CustomPacketPayload.Type<>(NomosikiMod.id("squad_command"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SquadCommandPayload> CODEC = StreamCodec.of(

		SquadCommandPayload::encode,

		SquadCommandPayload::decode

	);



	private static void encode(RegistryFriendlyByteBuf buf, SquadCommandPayload p) {

		buf.writeByte(p.orderId);

		BlockPos.STREAM_CODEC.encode(buf, p.anchor);

		buf.writeVarInt(p.zoneRadius);

		buf.writeVarInt(p.targetEntityIds.size());

		for (int id : p.targetEntityIds) {
			buf.writeVarInt(id);
		}
		int squadCount = Math.min(p.orderForSquadIds.size(), MAX_SQUAD_TARGETS);
		buf.writeVarInt(squadCount);
		for (int i = 0; i < squadCount; i++) {
			buf.writeUUID(p.orderForSquadIds.get(i));
		}
	}



	private static SquadCommandPayload decode(RegistryFriendlyByteBuf buf) {

		byte orderId = buf.readByte();

		BlockPos anchor = BlockPos.STREAM_CODEC.decode(buf);

		int zoneRadius = buf.readVarInt();

		int n = buf.readVarInt();

		List<Integer> ids = new ArrayList<>(Math.min(n, 4096));

		for (int i = 0; i < n; i++) {

			ids.add(buf.readVarInt());

		}

		List<UUID> squadIds = new ArrayList<>();
		if (buf.isReadable()) {
			int squadCount = Math.min(buf.readVarInt(), MAX_SQUAD_TARGETS);
			for (int i = 0; i < squadCount; i++) {
				squadIds.add(buf.readUUID());
			}
		}
		return new SquadCommandPayload(orderId, anchor, zoneRadius, List.copyOf(ids), List.copyOf(squadIds));
	}



	public SquadOrder order() {

		return SquadOrder.fromId(this.orderId);

	}



	@Override

	public Type<? extends CustomPacketPayload> type() {

		return TYPE;

	}

}

