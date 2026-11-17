package com.nomosiki.network.faction.payload.c2s;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record FactionCreatePayload(String factionName, String colorId) implements CustomPacketPayload {
	public static final Type<FactionCreatePayload> TYPE = new Type<>(NomosikiMod.id("faction_create"));
	public static final StreamCodec<RegistryFriendlyByteBuf, FactionCreatePayload> CODEC = StreamCodec.of(
		FactionCreatePayload::encode,
		FactionCreatePayload::decode
	);

	private static void encode(RegistryFriendlyByteBuf buf, FactionCreatePayload payload) {
		buf.writeUtf(payload.factionName, 32);
		buf.writeUtf(payload.colorId, 16);
	}

	private static FactionCreatePayload decode(RegistryFriendlyByteBuf buf) {
		return new FactionCreatePayload(buf.readUtf(32), buf.readUtf(16));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
