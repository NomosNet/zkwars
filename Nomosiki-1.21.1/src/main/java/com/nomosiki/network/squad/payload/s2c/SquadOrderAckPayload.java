package com.nomosiki.network.squad.payload.s2c;

import com.nomosiki.NomosikiMod;
import com.nomosiki.entity.SquadOrder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Optional;
import java.util.UUID;

/** Server → client feedback after a squad order is applied (RTS HUD / markers). */
public record SquadOrderAckPayload(
	byte orderId,
	BlockPos anchor,
	int zoneRadius,
	int membersAffected,
	Optional<UUID> squadId
) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SquadOrderAckPayload> TYPE =
		new CustomPacketPayload.Type<>(NomosikiMod.id("squad_order_ack"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SquadOrderAckPayload> CODEC = StreamCodec.of(
		SquadOrderAckPayload::encode,
		SquadOrderAckPayload::decode
	);

	private static void encode(RegistryFriendlyByteBuf buf, SquadOrderAckPayload p) {
		buf.writeByte(p.orderId);
		BlockPos.STREAM_CODEC.encode(buf, p.anchor);
		buf.writeVarInt(p.zoneRadius);
		buf.writeVarInt(p.membersAffected);
		buf.writeBoolean(p.squadId.isPresent());
		p.squadId.ifPresent(buf::writeUUID);
	}

	private static SquadOrderAckPayload decode(RegistryFriendlyByteBuf buf) {
		byte orderId = buf.readByte();
		BlockPos anchor = BlockPos.STREAM_CODEC.decode(buf);
		int zoneRadius = buf.readVarInt();
		int membersAffected = buf.readVarInt();
		Optional<UUID> squadId = buf.readBoolean() ? Optional.of(buf.readUUID()) : Optional.empty();
		return new SquadOrderAckPayload(orderId, anchor, zoneRadius, membersAffected, squadId);
	}

	public SquadOrder order() {
		return SquadOrder.fromId(this.orderId);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
