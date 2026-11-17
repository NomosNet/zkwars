package com.nomosiki.network.squad.payload.c2s.command;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record SquadMountCommandPayload(List<UUID> squadIds, boolean mount) implements CustomPacketPayload {
	private static final int MAX_SQUADS = 16;

	public static final CustomPacketPayload.Type<SquadMountCommandPayload> TYPE =
		new CustomPacketPayload.Type<>(NomosikiMod.id("squad_mount_command"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SquadMountCommandPayload> CODEC = StreamCodec.of(
		SquadMountCommandPayload::encode,
		SquadMountCommandPayload::decode
	);

	private static void encode(RegistryFriendlyByteBuf buf, SquadMountCommandPayload p) {
		int squadCount = Math.min(p.squadIds.size(), MAX_SQUADS);
		buf.writeVarInt(squadCount);
		for (int i = 0; i < squadCount; i++) {
			buf.writeUUID(p.squadIds.get(i));
		}
		buf.writeBoolean(p.mount);
	}

	private static SquadMountCommandPayload decode(RegistryFriendlyByteBuf buf) {
		int squadCount = Math.min(buf.readVarInt(), MAX_SQUADS);
		List<UUID> squadIds = new ArrayList<>(squadCount);
		for (int i = 0; i < squadCount; i++) {
			squadIds.add(buf.readUUID());
		}
		boolean mount = buf.readBoolean();
		return new SquadMountCommandPayload(List.copyOf(squadIds), mount);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
