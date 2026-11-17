package com.nomosiki.work.gun;

import com.nomosiki.work.NomosikiWork;
import net.minecraft.sounds.SoundEvent;

import java.util.function.Consumer;

public final class GunSounds {
	public static final SoundEvent MUSKET_LOAD_0 = SoundEvent.createVariableRangeEvent(NomosikiWork.id("musket_load0"));
	public static final SoundEvent MUSKET_LOAD_1 = SoundEvent.createVariableRangeEvent(NomosikiWork.id("musket_load1"));
	public static final SoundEvent MUSKET_LOAD_2 = SoundEvent.createVariableRangeEvent(NomosikiWork.id("musket_load2"));
	public static final SoundEvent MUSKET_READY = SoundEvent.createVariableRangeEvent(NomosikiWork.id("musket_ready"));
	public static final SoundEvent MUSKET_FIRE = SoundEvent.createVariableRangeEvent(NomosikiWork.id("musket_fire"));
	public static final SoundEvent BLUNDERBUSS_FIRE = SoundEvent.createVariableRangeEvent(NomosikiWork.id("blunderbuss_fire"));
	public static final SoundEvent BLUNDERBUSS_FIRE_FLAME = SoundEvent.createVariableRangeEvent(NomosikiWork.id("blunderbuss_fire_flame"));
	public static final SoundEvent PISTOL_FIRE = SoundEvent.createVariableRangeEvent(NomosikiWork.id("pistol_fire"));
	public static final SoundEvent DISPENSER_FIRE = SoundEvent.createVariableRangeEvent(NomosikiWork.id("dispenser_fire"));
	public static final SoundEvent BULLET_FLY_BY = SoundEvent.createVariableRangeEvent(NomosikiWork.id("bullet_fly_by"));
	public static final SoundEvent BULLET_WATER_HIT = SoundEvent.createVariableRangeEvent(NomosikiWork.id("bullet_water_hit"));

	private GunSounds() {
	}

	public static void register(Consumer<SoundEvent> helper) {
		helper.accept(MUSKET_LOAD_0);
		helper.accept(MUSKET_LOAD_1);
		helper.accept(MUSKET_LOAD_2);
		helper.accept(MUSKET_READY);
		helper.accept(MUSKET_FIRE);
		helper.accept(BLUNDERBUSS_FIRE);
		helper.accept(BLUNDERBUSS_FIRE_FLAME);
		helper.accept(PISTOL_FIRE);
		helper.accept(DISPENSER_FIRE);
		helper.accept(BULLET_FLY_BY);
		helper.accept(BULLET_WATER_HIT);
	}
}
