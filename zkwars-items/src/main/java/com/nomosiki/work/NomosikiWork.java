package com.nomosiki.work;

import com.nomosiki.work.armor.FactionArmorTint;
import com.nomosiki.work.gun.Guns;
import com.nomosiki.work.registry.ModArmorMaterials;
import com.nomosiki.work.registry.ModItemGroups;
import com.nomosiki.work.registry.ModItems;
import com.nomosiki.work.weapon.ThrownWeaponEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NomosikiWork implements ModInitializer {
	public static final String MOD_ID = "zkwars-items";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModArmorMaterials.register();
		ModItems.register();
		ThrownWeaponEntity.register();
		Guns.register();
		ModItemGroups.register();
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (!handler.player.hasDisconnected()) {
				FactionArmorTint.refreshWorn(handler.player);
			}
		});
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (entity instanceof LivingEntity living) {
				FactionArmorTint.refreshWorn(living);
			}
		});
		LOGGER.info("ZK Nomosiki Items RP gear loaded");
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
