package com.nomosiki.network.squad.payload.c2s.management;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record SquadEditPayload(UUID squadId, String name, byte iconId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SquadEditPayload> TYPE =
		new CustomPacketPayload.Type<>(NomosikiMod.id("squad_edit"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SquadEditPayload> CODEC = StreamCodec.of(
		SquadEditPayload::encode,
		SquadEditPayload::decode
	);

	private static void encode(RegistryFriendlyByteBuf buf, SquadEditPayload p) {
		buf.writeUUID(p.squadId);
		buf.writeUtf(p.name, 32);
		buf.writeByte(p.iconId);
	}

	private static SquadEditPayload decode(RegistryFriendlyByteBuf buf) {
		return new SquadEditPayload(buf.readUUID(), buf.readUtf(32), buf.readByte());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
