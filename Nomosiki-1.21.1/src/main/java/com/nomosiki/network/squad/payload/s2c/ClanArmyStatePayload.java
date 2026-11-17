package com.nomosiki.network.squad.payload.s2c;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/** Another faction member's army. Kept off the viewer's own command state. */
public record ClanArmyStatePayload(UUID commanderId, String commanderName, SquadArmyStatePayload army)
	implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ClanArmyStatePayload> TYPE =
		new CustomPacketPayload.Type<>(NomosikiMod.id("clan_army_state"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ClanArmyStatePayload> CODEC = StreamCodec.of(
		ClanArmyStatePayload::encode,
		ClanArmyStatePayload::decode
	);

	private static void encode(RegistryFriendlyByteBuf buf, ClanArmyStatePayload payload) {
		buf.writeUUID(payload.commanderId);
		buf.writeUtf(payload.commanderName, 64);
		SquadArmyStatePayload.CODEC.encode(buf, payload.army);
	}

	private static ClanArmyStatePayload decode(RegistryFriendlyByteBuf buf) {
		UUID commanderId = buf.readUUID();
		String commanderName = buf.readUtf(64);
		SquadArmyStatePayload army = SquadArmyStatePayload.CODEC.decode(buf);
		return new ClanArmyStatePayload(commanderId, commanderName, army);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
