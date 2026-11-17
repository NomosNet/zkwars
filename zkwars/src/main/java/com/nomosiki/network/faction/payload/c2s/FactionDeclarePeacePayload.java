package com.nomosiki.network.faction.payload.c2s;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record FactionDeclarePeacePayload(String enemyFactionName) implements CustomPacketPayload {
	public static final Type<FactionDeclarePeacePayload> TYPE = new Type<>(NomosikiMod.id("faction_declare_peace"));
	public static final StreamCodec<RegistryFriendlyByteBuf, FactionDeclarePeacePayload> CODEC = StreamCodec.of(
		FactionDeclarePeacePayload::encode,
		FactionDeclarePeacePayload::decode
	);

	private static void encode(RegistryFriendlyByteBuf buf, FactionDeclarePeacePayload payload) {
		buf.writeUtf(payload.enemyFactionName, 32);
	}

	private static FactionDeclarePeacePayload decode(RegistryFriendlyByteBuf buf) {
		return new FactionDeclarePeacePayload(buf.readUtf(32));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
