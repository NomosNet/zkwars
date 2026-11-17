package com.nomosiki.network.shop.payload.c2s;

import com.nomosiki.NomosikiMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record TesterSquadShopBuyPayload(BlockPos pos, String name, byte typeId, int count) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<TesterSquadShopBuyPayload> TYPE =
		new CustomPacketPayload.Type<>(NomosikiMod.id("tester_squad_shop_buy"));
	public static final StreamCodec<RegistryFriendlyByteBuf, TesterSquadShopBuyPayload> CODEC = StreamCodec.of(
		TesterSquadShopBuyPayload::encode,
		TesterSquadShopBuyPayload::decode
	);

	private static void encode(RegistryFriendlyByteBuf buf, TesterSquadShopBuyPayload p) {
		BlockPos.STREAM_CODEC.encode(buf, p.pos);
		buf.writeUtf(p.name, 32);
		buf.writeByte(p.typeId);
		buf.writeVarInt(p.count);
	}

	private static TesterSquadShopBuyPayload decode(RegistryFriendlyByteBuf buf) {
		return new TesterSquadShopBuyPayload(
			BlockPos.STREAM_CODEC.decode(buf),
			buf.readUtf(32),
			buf.readByte(),
			buf.readVarInt()
		);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
