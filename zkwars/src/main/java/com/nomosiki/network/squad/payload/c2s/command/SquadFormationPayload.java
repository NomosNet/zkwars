package com.nomosiki.network.squad.payload.c2s.command;



import com.nomosiki.NomosikiMod;

import com.nomosiki.entity.SquadFormation;

import com.nomosiki.squad.SquadFormationLayout;

import net.minecraft.network.RegistryFriendlyByteBuf;

import net.minecraft.network.codec.StreamCodec;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;



import java.util.ArrayList;

import java.util.List;

import java.util.Optional;

import java.util.UUID;



/**

 * C2S: sets {@link com.nomosiki.entity.SquadFormation} and spacing on recruits.

 * Wire order: {@code formationId} (byte), {@code spacing} (float), entity id list, optional squad UUID.

 */

public record SquadFormationPayload(byte formationId, float spacing, List<Integer> targetEntityIds, Optional<UUID> forSquadId) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SquadFormationPayload> TYPE =

		new CustomPacketPayload.Type<>(NomosikiMod.id("squad_formation"));



	public static final StreamCodec<RegistryFriendlyByteBuf, SquadFormationPayload> CODEC = StreamCodec.of(

		SquadFormationPayload::encode,

		SquadFormationPayload::decode

	);



	private static void encode(RegistryFriendlyByteBuf buf, SquadFormationPayload p) {

		buf.writeByte(p.formationId);

		buf.writeFloat(p.spacing);

		buf.writeVarInt(p.targetEntityIds.size());

		for (int id : p.targetEntityIds) {

			buf.writeVarInt(id);

		}

		buf.writeBoolean(p.forSquadId.isPresent());

		p.forSquadId.ifPresent(buf::writeUUID);

	}



	private static SquadFormationPayload decode(RegistryFriendlyByteBuf buf) {

		byte formationId = buf.readByte();

		float spacing = buf.readFloat();

		int n = buf.readVarInt();

		List<Integer> ids = new ArrayList<>(Math.min(n, 4096));

		for (int i = 0; i < n; i++) {

			ids.add(buf.readVarInt());

		}

		Optional<UUID> squad = Optional.empty();

		if (buf.readBoolean() && buf.readableBytes() >= 16) {

			squad = Optional.of(buf.readUUID());

		}

		return new SquadFormationPayload(formationId, spacing, List.copyOf(ids), squad);

	}



	public SquadFormation formation() {

		return SquadFormation.fromId(this.formationId);

	}



	/** Spacing in blocks; server clamps via {@link com.nomosiki.entity.NomosikEntity#setSquadFormationSpacingServer}. */

	public float spacingOrDefault() {

		return Float.isFinite(this.spacing) && this.spacing > 0.0F

			? this.spacing

			: (float) SquadFormationLayout.DEFAULT_SPACING;

	}



	@Override

	public Type<? extends CustomPacketPayload> type() {

		return TYPE;

	}

}

