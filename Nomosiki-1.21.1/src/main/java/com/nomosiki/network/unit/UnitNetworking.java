package com.nomosiki.network.unit;

import com.nomosiki.advancement.NomosikiActions;
import com.nomosiki.advancement.NomosikiAdvancements;
import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.hire.NomosikHire;
import com.nomosiki.network.squad.SquadCommandNetworking;
import com.nomosiki.network.unit.payload.c2s.NomosikHirePayload;
import com.nomosiki.network.unit.payload.c2s.NomosikOpenMenuPayload;
import com.nomosiki.network.unit.payload.c2s.NomosikRecruitActionPayload;
import com.nomosiki.network.unit.payload.c2s.NomosikRenamePayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class UnitNetworking {
	private UnitNetworking() {
	}

	public static void register() {
		PayloadTypeRegistry.playC2S().register(NomosikRenamePayload.TYPE, NomosikRenamePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(NomosikRecruitActionPayload.TYPE, NomosikRecruitActionPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(NomosikOpenMenuPayload.TYPE, NomosikOpenMenuPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(NomosikHirePayload.TYPE, NomosikHirePayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(NomosikRenamePayload.TYPE, (payload, context) -> {
			NomosikEntity nomosik = resolveOwnedNomosik(context.player(), payload.entityId());
			if (nomosik == null) {
				return;
			}
			String name = NomosikRenamePayload.sanitize(payload.name());
			if (name.isEmpty()) {
				nomosik.setCustomName(null);
				nomosik.setCustomNameVisible(false);
				return;
			}
			nomosik.setCustomName(Component.literal(name));
			nomosik.setCustomNameVisible(true);
		});

		ServerPlayNetworking.registerGlobalReceiver(NomosikRecruitActionPayload.TYPE, (payload, context) -> {
			NomosikEntity nomosik = resolveOwnedNomosik(context.player(), payload.entityId());
			if (nomosik == null) {
				return;
			}
			switch (payload.actionId()) {
				case NomosikRecruitActionPayload.ACTION_TOGGLE_FOLLOW -> {
					nomosik.setShouldFollowOwner(!nomosik.shouldFollowOwner());
					NomosikiAdvancements.grant(context.player(), NomosikiActions.FOLLOW);
				}
				case NomosikRecruitActionPayload.ACTION_CLEAR_TARGET -> nomosik.setTarget(null);
				default -> {
				}
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(NomosikHirePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			ServerLevel level = player.serverLevel();
			Entity entity = level.getEntity(payload.entityId());
			if (!(entity instanceof NomosikEntity nomosik)) {
				return;
			}
			if (nomosik.getOwnerUUID() != null) {
				return;
			}
			if (player.distanceToSqr(nomosik) > SquadCommandNetworking.recruitGuiRangeSqr()) {
				return;
			}
			NomosikHire.tryHire(player, nomosik);
		});

		ServerPlayNetworking.registerGlobalReceiver(NomosikOpenMenuPayload.TYPE, (payload, context) -> {
			NomosikEntity nomosik = resolveOwnedNomosik(context.player(), payload.entityId());
			if (nomosik == null) {
				return;
			}
			context.player().openMenu(nomosik);
		});
	}

	private static NomosikEntity resolveOwnedNomosik(ServerPlayer player, int entityId) {
		ServerLevel level = player.serverLevel();
		Entity entity = level.getEntity(entityId);
		if (!(entity instanceof NomosikEntity nomosik)) {
			return null;
		}
		if (!player.getUUID().equals(nomosik.getOwnerUUID())) {
			return null;
		}
		if (player.distanceToSqr(nomosik) > SquadCommandNetworking.recruitGuiRangeSqr()) {
			return null;
		}
		return nomosik;
	}
}
