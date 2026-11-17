package com.nomosiki.network.duel.payload.s2c;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record DuelScreenPayload(
	long cooldownUntilMs,
	byte phase,
	String otherName,
	UUID otherId,
	long expiresAtMs,
	boolean wartime,
	boolean requestOpen,
	boolean closeScreen,
	List<Row> rows
) implements CustomPacketPayload {
	public static final byte PHASE_IDLE = 0;
	public static final byte PHASE_OUTGOING = 1;
	public static final byte PHASE_INCOMING = 2;
	public static final byte PHASE_FIGHTING = 3;

	public static final Type<DuelScreenPayload> TYPE = new Type<>(NomosikiMod.id("duel_screen"));
	public static final StreamCodec<RegistryFriendlyByteBuf, DuelScreenPayload> CODEC = StreamCodec.of(
		DuelScreenPayload::encode,
		DuelScreenPayload::decode
	);

	public record Row(UUID id, String name, String faction, boolean canChallenge) {
	}

	private static void encode(RegistryFriendlyByteBuf buf, DuelScreenPayload payload) {
		buf.writeLong(payload.cooldownUntilMs);
		buf.writeByte(payload.phase);
		buf.writeUtf(payload.otherName, 32);
		buf.writeUUID(payload.otherId);
		buf.writeLong(payload.expiresAtMs);
		buf.writeBoolean(payload.wartime);
		buf.writeBoolean(payload.requestOpen);
		buf.writeBoolean(payload.closeScreen);
		buf.writeVarInt(payload.rows.size());
		for (Row row : payload.rows) {
			buf.writeUUID(row.id);
			buf.writeUtf(row.name, 32);
			buf.writeUtf(row.faction, 64);
			buf.writeBoolean(row.canChallenge);
		}
	}

	private static DuelScreenPayload decode(RegistryFriendlyByteBuf buf) {
		long cooldownUntilMs = buf.readLong();
		byte phase = buf.readByte();
		String otherName = buf.readUtf(32);
		UUID otherId = buf.readUUID();
		long expiresAtMs = buf.readLong();
		boolean wartime = buf.readBoolean();
		boolean requestOpen = buf.readBoolean();
		boolean closeScreen = buf.readBoolean();
		int size = buf.readVarInt();
		int capped = Math.min(size, 256);
		List<Row> rows = new ArrayList<>(capped);
		for (int i = 0; i < size; i++) {
			UUID id = buf.readUUID();
			String name = buf.readUtf(32);
			String faction = buf.readUtf(64);
			boolean canChallenge = buf.readBoolean();
			if (i < capped) {
				rows.add(new Row(id, name, faction, canChallenge));
			}
		}
		return new DuelScreenPayload(cooldownUntilMs, phase, otherName, otherId, expiresAtMs, wartime, requestOpen, closeScreen, rows);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
