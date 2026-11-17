package com.nomosiki.network.duel.payload.c2s;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record DuelActionPayload(byte action, UUID target) implements CustomPacketPayload {
	public static final byte CHALLENGE = 0;
	public static final byte ACCEPT = 1;
	public static final byte DENY = 2;
	public static final byte CANCEL = 3;

	public static final Type<DuelActionPayload> TYPE = new Type<>(NomosikiMod.id("duel_action"));
	public static final StreamCodec<RegistryFriendlyByteBuf, DuelActionPayload> CODEC = StreamCodec.of(
		(buf, payload) -> {
			buf.writeByte(payload.action);
			buf.writeUUID(payload.target);
		},
		buf -> new DuelActionPayload(buf.readByte(), buf.readUUID())
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
