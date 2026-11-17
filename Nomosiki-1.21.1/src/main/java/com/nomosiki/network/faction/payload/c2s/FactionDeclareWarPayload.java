package com.nomosiki.network.faction.payload.c2s;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record FactionDeclareWarPayload(String enemyFactionName) implements CustomPacketPayload {
	public static final Type<FactionDeclareWarPayload> TYPE = new Type<>(NomosikiMod.id("faction_declare_war"));
	public static final StreamCodec<RegistryFriendlyByteBuf, FactionDeclareWarPayload> CODEC = StreamCodec.of(
		FactionDeclareWarPayload::encode,
		FactionDeclareWarPayload::decode
	);

	private static void encode(RegistryFriendlyByteBuf buf, FactionDeclareWarPayload payload) {
		buf.writeUtf(payload.enemyFactionName, 32);
	}

	private static FactionDeclareWarPayload decode(RegistryFriendlyByteBuf buf) {
		return new FactionDeclareWarPayload(buf.readUtf(32));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
