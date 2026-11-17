package com.nomosiki.network.squad.payload.c2s.sync;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Optional;
import java.util.UUID;

/** Empty commander means the admin stopped looking at someone else's army. */
public record ClanArmyWatchPayload(Optional<UUID> commanderId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ClanArmyWatchPayload> TYPE =
		new CustomPacketPayload.Type<>(NomosikiMod.id("clan_army_watch"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ClanArmyWatchPayload> CODEC = StreamCodec.of(
		ClanArmyWatchPayload::encode,
		ClanArmyWatchPayload::decode
	);

	private static void encode(RegistryFriendlyByteBuf buf, ClanArmyWatchPayload payload) {
		buf.writeBoolean(payload.commanderId.isPresent());
		payload.commanderId.ifPresent(buf::writeUUID);
	}

	private static ClanArmyWatchPayload decode(RegistryFriendlyByteBuf buf) {
		if (!buf.readBoolean()) {
			return new ClanArmyWatchPayload(Optional.empty());
		}
		return new ClanArmyWatchPayload(Optional.of(buf.readUUID()));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
