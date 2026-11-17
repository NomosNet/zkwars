package com.example.item;

import net.minecraft.world.item.Item;

public class HammerItem extends Item {
	public static final int DURABILITY = 1024;

	public HammerItem(Properties properties) {
		super(properties.durability(DURABILITY));
	}
}
