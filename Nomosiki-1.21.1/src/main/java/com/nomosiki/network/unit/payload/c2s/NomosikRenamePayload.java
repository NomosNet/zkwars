package com.nomosiki.network.unit.payload.c2s;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record NomosikRenamePayload(int entityId, String name) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<NomosikRenamePayload> TYPE =
		new CustomPacketPayload.Type<>(NomosikiMod.id("nomosik_rename"));
	public static final StreamCodec<RegistryFriendlyByteBuf, NomosikRenamePayload> CODEC = StreamCodec.of(
		NomosikRenamePayload::encode,
		NomosikRenamePayload::decode
	);

	private static void encode(RegistryFriendlyByteBuf buf, NomosikRenamePayload p) {
		buf.writeVarInt(p.entityId);
		buf.writeUtf(p.name, 64);
	}

	private static NomosikRenamePayload decode(RegistryFriendlyByteBuf buf) {
		return new NomosikRenamePayload(buf.readVarInt(), buf.readUtf(64));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	/**
	 * Strip chat control, limit length, trim.
	 */
	public static String sanitize(String raw) {
		if (raw == null) {
			return "";
		}
		String s = raw.replace("\u00A7", "").trim();
		if (s.length() > 32) {
			s = s.substring(0, 32);
		}
		return s;
	}
}
