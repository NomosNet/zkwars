package com.nomosiki.network.squad.payload.c2s.management;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SquadCreatePayload(String name, byte iconId, byte typeId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SquadCreatePayload> TYPE =
		new CustomPacketPayload.Type<>(NomosikiMod.id("squad_create"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SquadCreatePayload> CODEC = StreamCodec.of(
		SquadCreatePayload::encode,
		SquadCreatePayload::decode
	);

	private static void encode(RegistryFriendlyByteBuf buf, SquadCreatePayload p) {
		buf.writeUtf(p.name, 32);
		buf.writeByte(p.iconId);
		buf.writeByte(p.typeId);
	}

	private static SquadCreatePayload decode(RegistryFriendlyByteBuf buf) {
		return new SquadCreatePayload(buf.readUtf(32), buf.readByte(), buf.readByte());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
