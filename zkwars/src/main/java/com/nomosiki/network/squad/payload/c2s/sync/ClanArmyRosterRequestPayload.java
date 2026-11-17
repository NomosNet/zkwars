package com.nomosiki.network.squad.payload.c2s.sync;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ClanArmyRosterRequestPayload() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ClanArmyRosterRequestPayload> TYPE =
		new CustomPacketPayload.Type<>(NomosikiMod.id("clan_army_roster_request"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ClanArmyRosterRequestPayload> CODEC =
		StreamCodec.unit(new ClanArmyRosterRequestPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
