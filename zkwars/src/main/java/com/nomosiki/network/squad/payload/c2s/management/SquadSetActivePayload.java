package com.nomosiki.network.squad.payload.c2s.management;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Optional;
import java.util.UUID;

public record SquadSetActivePayload(Optional<UUID> squadId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SquadSetActivePayload> TYPE =
		new CustomPacketPayload.Type<>(NomosikiMod.id("squad_set_active"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SquadSetActivePayload> CODEC = StreamCodec.of(
		SquadSetActivePayload::encode,
		SquadSetActivePayload::decode
	);

	private static void encode(RegistryFriendlyByteBuf buf, SquadSetActivePayload p) {
		buf.writeBoolean(p.squadId.isPresent());
		p.squadId.ifPresent(buf::writeUUID);
	}

	private static SquadSetActivePayload decode(RegistryFriendlyByteBuf buf) {
		Optional<UUID> id = Optional.empty();
		if (buf.readBoolean() && buf.readableBytes() >= 16) {
			id = Optional.of(buf.readUUID());
		}
		return new SquadSetActivePayload(id);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
