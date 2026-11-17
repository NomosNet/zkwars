package com.nomosiki.client.compat.emote;

import com.nomosiki.compat.emote.NomosikEmoteEligibility;
import com.nomosiki.entity.NomosikEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;

public final class NomosikEmoteApplier {
	private NomosikEmoteApplier() {
	}

	public static <T extends LivingEntity> void apply(PlayerModel<T> model, T entity, float ageInTicks) {
		if (!(entity instanceof NomosikEntity nomosik)) {
			return;
		}
		if (!NomosikEmoteEligibility.canPlayPlayerModelEmote(nomosik)) {
			return;
		}
		NomosikEmoteClientState.Playing playing = NomosikEmoteClientState.get(nomosik.getUUID());
		if (playing == null) {
			return;
		}
		NomosikEmoteDefinition def = playing.definition();
		float tick = playing.tickAt(ageInTicks);
		if (!def.loop() && tick >= def.stopTick()) {
			NomosikEmoteClientState.stop(nomosik.getUUID());
			return;
		}
		Map<NomosikEmoteDefinition.Part, NomosikEmoteDefinition.PartPose> sample = def.sample(tick);
		float toRad = def.degrees() ? Mth.DEG_TO_RAD : 1f;

		applyPart(model.head, sample.get(NomosikEmoteDefinition.Part.HEAD), toRad);
		applyPart(model.body, sample.get(NomosikEmoteDefinition.Part.TORSO), toRad);
		applyPart(model.leftArm, sample.get(NomosikEmoteDefinition.Part.LEFT_ARM), toRad);
		applyPart(model.rightArm, sample.get(NomosikEmoteDefinition.Part.RIGHT_ARM), toRad);
		applyPart(model.leftLeg, sample.get(NomosikEmoteDefinition.Part.LEFT_LEG), toRad);
		applyPart(model.rightLeg, sample.get(NomosikEmoteDefinition.Part.RIGHT_LEG), toRad);

		// Keep hat/jacket/sleeves/pants in sync with player-model outer layers.
		model.hat.copyFrom(model.head);
		model.jacket.copyFrom(model.body);
		model.leftSleeve.copyFrom(model.leftArm);
		model.rightSleeve.copyFrom(model.rightArm);
		model.leftPants.copyFrom(model.leftLeg);
		model.rightPants.copyFrom(model.rightLeg);
	}

	private static void applyPart(ModelPart part, NomosikEmoteDefinition.PartPose pose, float toRad) {
		if (pose == null) {
			return;
		}
		part.x += pose.x();
		part.y += pose.y();
		part.z += pose.z();
		part.xRot = pose.pitch() * toRad;
		part.yRot = pose.yaw() * toRad;
		part.zRot = pose.roll() * toRad;
	}
}
