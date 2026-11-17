package com.nomosiki.work.gun;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;

public class BlunderbussItem extends GunItem {
	public BlunderbussItem(Properties properties) {
		super(properties);
	}

	@Override
	public float bulletStdDev() {
		return Config.blunderbussBulletStdDev;
	}

	@Override
	public float bulletSpeed() {
		return Config.blunderbussBulletSpeed;
	}

	@Override
	public int pelletCount() {
		return Config.blunderbussPelletCount;
	}

	@Override
	public float damage() {
		return Config.blunderbussDamage;
	}

	@Override
	public SoundEvent fireSound(ItemStack stack) {
		return hasFlame(stack) ? GunSounds.BLUNDERBUSS_FIRE_FLAME : GunSounds.BLUNDERBUSS_FIRE;
	}
}
