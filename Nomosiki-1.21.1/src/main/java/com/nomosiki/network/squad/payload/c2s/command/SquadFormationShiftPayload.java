package com.nomosiki.network.squad.payload.c2s.command;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** C2S: shift squad anchor forward/backward along the formation axis without changing order. */
public record SquadFormationShiftPayload(byte direction, List<UUID> squadIds) implements CustomPacketPayload {
	public static final byte STEP_FORWARD = 1;
	public static final byte STEP_BACKWARD = -1;

	private static final int MAX_SQUADS = 16;

	public static final CustomPacketPayload.Type<SquadFormationShiftPayload> TYPE =
		new CustomPacketPayload.Type<>(NomosikiMod.id("squad_formation_shift"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SquadFormationShiftPayload> CODEC = StreamCodec.of(
		SquadFormationShiftPayload::encode,
		SquadFormationShiftPayload::decode
	);

	private static void encode(RegistryFriendlyByteBuf buf, SquadFormationShiftPayload payload) {
		buf.writeByte(payload.direction);
		int count = Math.min(payload.squadIds.size(), MAX_SQUADS);
		buf.writeVarInt(count);
		for (int i = 0; i < count; i++) {
			buf.writeUUID(payload.squadIds.get(i));
		}
	}

	private static SquadFormationShiftPayload decode(RegistryFriendlyByteBuf buf) {
		byte direction = buf.readByte();
		int count = Math.min(buf.readVarInt(), MAX_SQUADS);
		List<UUID> squadIds = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			squadIds.add(buf.readUUID());
		}
		return new SquadFormationShiftPayload(direction, List.copyOf(squadIds));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
