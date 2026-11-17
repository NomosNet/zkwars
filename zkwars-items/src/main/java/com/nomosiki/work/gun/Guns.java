package com.nomosiki.work.gun;

import com.nomosiki.work.NomosikiWork;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.item.v1.EnchantmentEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.util.TriState;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Path;

public final class Guns {
	public static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("zkwars-items-guns.txt");

	private Guns() {
	}

	public static void register() {
		Config.load();

		GunItems.registerDataComponentTypes((path, component) ->
			Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, NomosikiWork.id(path), component));
		GunItems.register((path, item) ->
			Registry.register(BuiltInRegistries.ITEM, NomosikiWork.id(path), item));
		GunSounds.register(sound ->
			Registry.register(BuiltInRegistries.SOUND_EVENT, sound.getLocation(), sound));
		BulletEntity.register((path, entityType) ->
			Registry.register(BuiltInRegistries.ENTITY_TYPE, NomosikiWork.id(path), entityType));

		EnchantmentEvents.ALLOW_ENCHANTING.register((enchantment, stack, context) ->
			VanillaHelper.canEnchant(enchantment, stack) ? TriState.TRUE : TriState.DEFAULT);

		ServerTickEvents.END_WORLD_TICK.register(world -> DeferredDamage.apply());
		PayloadTypeRegistry.playS2C().register(SmokeEffectPacket.TYPE, SmokeEffectPacket.CODEC);
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> Config.load());
	}

	public static void disableVelocityUpdate(EntityType.Builder<?> builder) {
		builder.alwaysUpdateVelocity(false);
	}

	public static void sendSmokeEffect(ServerLevel level, Vec3 origin, Vec3 direction) {
		SmokeEffectPacket packet = SmokeEffectPacket.fromVec3(origin, direction);
		BlockPos blockPos = BlockPos.containing(origin.x, origin.y, origin.z);
		for (ServerPlayer serverPlayer : PlayerLookup.tracking(level, blockPos)) {
			ServerPlayNetworking.send(serverPlayer, packet);
		}
	}
}
