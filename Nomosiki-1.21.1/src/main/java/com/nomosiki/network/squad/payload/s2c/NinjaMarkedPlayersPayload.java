package com.nomosiki.network.squad.payload.s2c;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Enemy players currently marked by the viewer's ninja squads.
 */
public record NinjaMarkedPlayersPayload(List<NinjaMarkedPlayersPayload.MarkedPlayerRow> players)
	implements CustomPacketPayload {

	public record MarkedPlayerRow(
		UUID playerId,
		String name,
		double x,
		double y,
		double z,
		int remainingTicks,
		byte stanceId
	) {
	}

	private static final int MAX_MARKS = 16;

	public static final Type<NinjaMarkedPlayersPayload> TYPE =
		new Type<>(NomosikiMod.id("ninja_marked_players"));

	public static final StreamCodec<RegistryFriendlyByteBuf, NinjaMarkedPlayersPayload> CODEC = StreamCodec.of(
		NinjaMarkedPlayersPayload::encode,
		NinjaMarkedPlayersPayload::decode
	);

	private static void encode(RegistryFriendlyByteBuf buf, NinjaMarkedPlayersPayload p) {
		int count = Math.min(p.players.size(), MAX_MARKS);
		buf.writeVarInt(count);
		for (int i = 0; i < count; i++) {
			MarkedPlayerRow row = p.players.get(i);
			buf.writeUUID(row.playerId());
			buf.writeUtf(row.name() == null ? "" : row.name(), 32);
			buf.writeDouble(row.x());
			buf.writeDouble(row.y());
			buf.writeDouble(row.z());
			buf.writeVarInt(row.remainingTicks());
			buf.writeByte(row.stanceId());
		}
	}

	private static NinjaMarkedPlayersPayload decode(RegistryFriendlyByteBuf buf) {
		int count = Math.min(buf.readVarInt(), MAX_MARKS);
		List<MarkedPlayerRow> players = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			players.add(new MarkedPlayerRow(
				buf.readUUID(),
				buf.readUtf(32),
				buf.readDouble(),
				buf.readDouble(),
				buf.readDouble(),
				buf.readVarInt(),
				buf.readByte()
			));
		}
		return new NinjaMarkedPlayersPayload(List.copyOf(players));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
