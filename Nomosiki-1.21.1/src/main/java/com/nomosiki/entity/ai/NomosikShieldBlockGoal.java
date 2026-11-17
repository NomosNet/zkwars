package com.nomosiki.entity.ai;

import com.nomosiki.NomosikiMod;
import com.nomosiki.compat.melodies.NomosikImmersiveMelodiesCompat;
import com.nomosiki.entity.NomosikEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Forced shield block while {@link NomosikEntity#shouldBlockWithShieldNow()} (Recruits {@code UseShield} forced path).
 * No {@link Flag#MOVE} — Follow / formation goals keep navigating; speed is cut via attribute modifier.
 */
public class NomosikShieldBlockGoal extends Goal {
	/** Until {@code NomosikiServerConfig.shield.movementMultiplier} (step 10). */
	private static final double MOVEMENT_MULTIPLIER = 0.5;
	private static final ResourceLocation SLOW_ID = NomosikiMod.id("shield_block_slow");

	private final NomosikEntity nomosik;

	public NomosikShieldBlockGoal(NomosikEntity nomosik) {
		this.nomosik = nomosik;
		// Intentionally no flags — do not mutex MOVE/LOOK with formation / follow.
	}

	@Override
	public boolean canUse() {
		return !NomosikImmersiveMelodiesCompat.isHoldingInstrument(this.nomosik)
			&& this.nomosik.shouldBlockWithShieldNow()
			&& !this.nomosik.swinging
			&& NomosikShieldItems.isShieldBlockItem(this.nomosik.getOffhandItem());
	}

	@Override
	public boolean canContinueToUse() {
		return this.canUse();
	}

	@Override
	public void start() {
		this.nomosik.startUsingItem(InteractionHand.OFF_HAND);
		this.applySlowModifier();
	}

	@Override
	public void stop() {
		this.nomosik.stopUsingItem();
		this.removeSlowModifier();
	}

	@Override
	public void tick() {
		if (!this.nomosik.isUsingItem() || this.nomosik.getUsedItemHand() != InteractionHand.OFF_HAND) {
			this.nomosik.startUsingItem(InteractionHand.OFF_HAND);
		}
	}

	private void applySlowModifier() {
		AttributeInstance speed = this.nomosik.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed == null) {
			return;
		}
		speed.addOrUpdateTransientModifier(new AttributeModifier(
			SLOW_ID,
			MOVEMENT_MULTIPLIER - 1.0,
			AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
		));
	}

	private void removeSlowModifier() {
		AttributeInstance speed = this.nomosik.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.removeModifier(SLOW_ID);
		}
	}
}
