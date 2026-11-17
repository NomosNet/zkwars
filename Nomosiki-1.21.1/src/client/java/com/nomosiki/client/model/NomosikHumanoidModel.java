package com.nomosiki.client.model;

import com.nomosiki.compat.melodies.NomosikImmersiveMelodiesCompat;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

/**
 * Vanilla {@link PlayerModel} for nomosiki units.
 * <ul>
 *   <li>Sets {@link ArmPose} like {@code PlayerRenderer} — mobs otherwise never get {@link ArmPose#BLOCK}.</li>
 *   <li>{@link NomosikMountedPoses} — bow draw (vanilla is incomplete on PlayerModel mobs / riding).</li>
 *   <li>{@link #setupAttackAnimation} — re-run after {@code riding} arm pose with {@code riding=false} for melee.</li>
 * </ul>
 */
public class NomosikHumanoidModel<T extends LivingEntity> extends PlayerModel<T> {
	public NomosikHumanoidModel(ModelPart root, boolean slim) {
		super(root, slim);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.prepareArmPoses(entity);
		super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

		if (NomosikImmersiveMelodiesCompat.isHoldingInstrument(entity)) {
			return;
		}

		if (entity.isUsingItem()) {
			UseAnim useAnim = entity.getUseItem().getUseAnimation();
			if (useAnim == UseAnim.BOW) {
				NomosikMountedPoses.applyBow(this, entity, entity.getUseItem());
				com.nomosiki.client.compat.emote.NomosikEmoteApplier.apply(this, entity, ageInTicks);
				return;
			}
			if (useAnim == UseAnim.BLOCK || useAnim == UseAnim.CROSSBOW) {
				// ArmPose.BLOCK / CROSSBOW_CHARGE already applied in super; do not overwrite with melee swing.
				com.nomosiki.client.compat.emote.NomosikEmoteApplier.apply(this, entity, ageInTicks);
				return;
			}
		}

		// attackTime is set by LivingEntityRenderer from entity.getAttackAnim(); reapply swing after riding pose.
		if (this.attackTime > 0.0F) {
			boolean wasRiding = this.riding;
			this.riding = false;
			this.setupAttackAnimation(entity, ageInTicks);
			this.riding = wasRiding;
		}

		com.nomosiki.client.compat.emote.NomosikEmoteApplier.apply(this, entity, ageInTicks);
	}

	/**
	 * Mirror {@code PlayerRenderer#setModelProperties} arm poses.
	 * Shared wide/slim model instances must be reset every frame or BLOCK leaks between entities.
	 */
	private void prepareArmPoses(T entity) {
		boolean rightHanded = entity.getMainArm() == HumanoidArm.RIGHT;
		ItemStack mainHand = entity.getMainHandItem();
		ItemStack offHand = entity.getOffhandItem();

		HumanoidModel.ArmPose mainPose = mainHand.isEmpty() ? ArmPose.EMPTY : ArmPose.ITEM;
		HumanoidModel.ArmPose offPose = offHand.isEmpty() ? ArmPose.EMPTY : ArmPose.ITEM;

		if (entity.isUsingItem() && entity.getUseItemRemainingTicks() > 0) {
			UseAnim useAnim = entity.getUseItem().getUseAnimation();
			if (useAnim == UseAnim.BLOCK) {
				if (entity.getUsedItemHand() == InteractionHand.MAIN_HAND) {
					mainPose = ArmPose.BLOCK;
				} else {
					offPose = ArmPose.BLOCK;
				}
			} else if (useAnim == UseAnim.CROSSBOW) {
				if (entity.getUsedItemHand() == InteractionHand.MAIN_HAND) {
					mainPose = ArmPose.CROSSBOW_CHARGE;
				} else {
					offPose = ArmPose.CROSSBOW_CHARGE;
				}
			}
		}

		if (rightHanded) {
			this.rightArmPose = mainPose;
			this.leftArmPose = offPose;
		} else {
			this.rightArmPose = offPose;
			this.leftArmPose = mainPose;
		}
	}
}
