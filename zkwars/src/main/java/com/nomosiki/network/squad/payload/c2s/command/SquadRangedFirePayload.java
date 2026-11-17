package com.nomosiki.network.squad.payload.c2s.command;

import com.nomosiki.NomosikiMod;
import com.nomosiki.entity.NomosikRangedFireMode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record SquadRangedFirePayload(List<UUID> squadIds, byte modeId, boolean hasZone, BlockPos zoneAnchor, int zoneRadius)
	implements CustomPacketPayload {
	private static final int MAX_SQUADS = 16;

	public static final CustomPacketPayload.Type<SquadRangedFirePayload> TYPE =
		new CustomPacketPayload.Type<>(NomosikiMod.id("squad_ranged_fire"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SquadRangedFirePayload> CODEC = StreamCodec.of(
		SquadRangedFirePayload::encode,
		SquadRangedFirePayload::decode
	);

	private static void encode(RegistryFriendlyByteBuf buf, SquadRangedFirePayload p) {
		int squadCount = Math.min(p.squadIds.size(), MAX_SQUADS);
		buf.writeVarInt(squadCount);
		for (int i = 0; i < squadCount; i++) {
			buf.writeUUID(p.squadIds.get(i));
		}
		buf.writeByte(p.modeId);
		buf.writeBoolean(p.hasZone);
		if (p.hasZone) {
			BlockPos.STREAM_CODEC.encode(buf, p.zoneAnchor);
			buf.writeVarInt(p.zoneRadius);
		}
	}

	private static SquadRangedFirePayload decode(RegistryFriendlyByteBuf buf) {
		int squadCount = Math.min(buf.readVarInt(), MAX_SQUADS);
		List<UUID> squadIds = new ArrayList<>(squadCount);
		for (int i = 0; i < squadCount; i++) {
			squadIds.add(buf.readUUID());
		}
		byte modeId = buf.readByte();
		boolean hasZone = buf.readBoolean();
		BlockPos anchor = BlockPos.ZERO;
		int radius = 8;
		if (hasZone) {
			anchor = BlockPos.STREAM_CODEC.decode(buf);
			radius = buf.readVarInt();
		}
		return new SquadRangedFirePayload(List.copyOf(squadIds), modeId, hasZone, anchor, radius);
	}

	public NomosikRangedFireMode mode() {
		return NomosikRangedFireMode.fromId(this.modeId);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
