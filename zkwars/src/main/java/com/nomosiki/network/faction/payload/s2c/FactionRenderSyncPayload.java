package com.nomosiki.network.faction.payload.s2c;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record FactionRenderSyncPayload(
	Map<UUID, String> colorByPlayer,
	Map<UUID, Byte> stanceByPlayer
) implements CustomPacketPayload {
	public static final Type<FactionRenderSyncPayload> TYPE = new Type<>(NomosikiMod.id("faction_render_sync"));
	public static final StreamCodec<RegistryFriendlyByteBuf, FactionRenderSyncPayload> CODEC = StreamCodec.of(
		FactionRenderSyncPayload::encode,
		FactionRenderSyncPayload::decode
	);

	private static void encode(RegistryFriendlyByteBuf buf, FactionRenderSyncPayload payload) {
		buf.writeVarInt(payload.colorByPlayer.size());
		for (Map.Entry<UUID, String> entry : payload.colorByPlayer.entrySet()) {
			buf.writeUUID(entry.getKey());
			buf.writeUtf(entry.getValue(), 16);
		}
		buf.writeVarInt(payload.stanceByPlayer.size());
		for (Map.Entry<UUID, Byte> entry : payload.stanceByPlayer.entrySet()) {
			buf.writeUUID(entry.getKey());
			buf.writeByte(entry.getValue());
		}
	}

	private static FactionRenderSyncPayload decode(RegistryFriendlyByteBuf buf) {
		int size = buf.readVarInt();
		Map<UUID, String> colors = new HashMap<>(Math.max(16, size));
		for (int i = 0; i < size; i++) {
			colors.put(buf.readUUID(), buf.readUtf(16));
		}
		Map<UUID, Byte> stances = new HashMap<>();
		if (buf.readableBytes() > 0) {
			int stanceSize = buf.readVarInt();
			stances = new HashMap<>(Math.max(16, stanceSize));
			for (int i = 0; i < stanceSize; i++) {
				stances.put(buf.readUUID(), buf.readByte());
			}
		}
		return new FactionRenderSyncPayload(Map.copyOf(colors), Map.copyOf(stances));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
