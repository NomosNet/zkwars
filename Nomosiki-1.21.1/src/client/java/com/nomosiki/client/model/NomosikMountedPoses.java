package com.nomosiki.client.model;



import net.minecraft.client.model.HumanoidModel;

import net.minecraft.util.Mth;

import net.minecraft.world.InteractionHand;

import net.minecraft.world.entity.HumanoidArm;

import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.item.BowItem;

import net.minecraft.world.item.ItemStack;



/**

 * Bow draw pose that vanilla {@link HumanoidModel#setupAnim} skips on nomosiki ({@link net.minecraft.client.model.PlayerModel} mobs and {@code riding} passengers).

 */

public final class NomosikMountedPoses {

	private NomosikMountedPoses() {

	}



	public static <T extends LivingEntity> void applyBow(HumanoidModel<T> model, T entity, ItemStack bow) {

		HumanoidArm bowArm = entity.getUsedItemHand() == InteractionHand.MAIN_HAND

			? entity.getMainArm()

			: entity.getMainArm().getOpposite();

		float pull = bowPullProgress(entity, bow);

		float headX = model.head.xRot;

		float headY = model.head.yRot;

		float draw = pull * 0.15F;



		if (bowArm == HumanoidArm.RIGHT) {

			model.rightArmPose = HumanoidModel.ArmPose.ITEM;

			model.leftArmPose = HumanoidModel.ArmPose.BOW_AND_ARROW;

			model.rightArm.yRot = -0.1F + headY;

			model.rightArm.xRot = -Mth.HALF_PI + headX - draw;

			model.rightArm.zRot = 0.0F;

			model.leftArm.xRot = -Mth.HALF_PI + headX - draw;

			model.leftArm.yRot = 0.1F + headY + 0.4F;

			model.leftArm.zRot = -Mth.DEG_TO_RAD * 20.0F;

		} else {

			model.leftArmPose = HumanoidModel.ArmPose.ITEM;

			model.rightArmPose = HumanoidModel.ArmPose.BOW_AND_ARROW;

			model.leftArm.yRot = 0.1F + headY;

			model.leftArm.xRot = -Mth.HALF_PI + headX - draw;

			model.leftArm.zRot = 0.0F;

			model.rightArm.xRot = -Mth.HALF_PI + headX - draw;

			model.rightArm.yRot = -0.1F + headY - 0.4F;

			model.rightArm.zRot = Mth.DEG_TO_RAD * 20.0F;

		}

	}



	private static float bowPullProgress(LivingEntity entity, ItemStack bow) {

		int ticksUsing = entity.getTicksUsingItem();

		if (ticksUsing <= 0) {

			return 0.0F;

		}

		return Mth.clamp(BowItem.getPowerForTime(ticksUsing), 0.0F, 1.0F);

	}

}

