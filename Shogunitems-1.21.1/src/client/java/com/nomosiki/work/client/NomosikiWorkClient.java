package com.nomosiki.work.client;

import com.nomosiki.work.NomosikiWork;
import com.nomosiki.work.gun.BulletEntity;
import com.nomosiki.work.gun.BulletRenderer;
import com.nomosiki.work.gun.ClientUtilities;
import com.nomosiki.work.gun.SmokeEffectPacket;
import com.nomosiki.work.registry.ModItems;
import com.nomosiki.work.weapon.ThrownWeaponEntity;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import mod.azure.azurelib.common.render.armor.AzArmorRenderer;
import mod.azure.azurelib.common.render.armor.AzArmorRendererConfig;
import mod.azure.azurelib.common.render.armor.AzArmorRendererRegistry;
import mod.azure.azurelib.common.render.item.AzItemRenderer;
import mod.azure.azurelib.common.render.item.AzItemRendererConfig;
import mod.azure.azurelib.common.render.item.AzItemRendererRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

public class NomosikiWorkClient implements ClientModInitializer {
	private static final ResourceLocation STEEL_SAMURAI_GEO = NomosikiWork.id("geo/samurai_armor_new.geo.json");
	private static final ResourceLocation IRON_SAMURAI_GEO = NomosikiWork.id("geo/iron_samurai_armor.geo.json");

	@Override
	public void onInitializeClient() {
		ClientUtilities.registerItemProperties();
		EntityRendererRegistry.register(BulletEntity.ENTITY_TYPE, BulletRenderer::new);
		EntityRendererRegistry.register(ThrownWeaponEntity.ENTITY_TYPE, ThrownItemRenderer::new);
		ClientPlayNetworking.registerGlobalReceiver(SmokeEffectPacket.TYPE, (packet, context) ->
			context.client().execute(() -> ClientUtilities.handleSmokeEffectPacket(packet)));

		registerYumiPredicates();
		registerDyeableSamuraiTint(
			ModItems.STEEL_SAMURAI_HELMET,
			ModItems.STEEL_SAMURAI_CHESTPLATE,
			ModItems.STEEL_SAMURAI_LEGGINGS,
			ModItems.STEEL_SAMURAI_BOOTS,
			ModItems.IRON_SAMURAI_HELMET,
			ModItems.IRON_SAMURAI_CHESTPLATE,
			ModItems.IRON_SAMURAI_LEGGINGS,
			ModItems.IRON_SAMURAI_BOOTS);

		AzArmorRendererRegistry.register(
			() -> new AzArmorRenderer(AzArmorRendererConfig.builder(
				(entity, stack) -> STEEL_SAMURAI_GEO,
				(entity, stack) -> dyedSamuraiTexture(stack, "steel_samurai")).build()),
			ModItems.STEEL_SAMURAI_HELMET,
			ModItems.STEEL_SAMURAI_CHESTPLATE,
			ModItems.STEEL_SAMURAI_LEGGINGS,
			ModItems.STEEL_SAMURAI_BOOTS);
		AzArmorRendererRegistry.register(
			() -> new AzArmorRenderer(AzArmorRendererConfig.builder(
				(entity, stack) -> IRON_SAMURAI_GEO,
				(entity, stack) -> dyedSamuraiTexture(stack, "iron_samurai")).build()),
			ModItems.IRON_SAMURAI_HELMET,
			ModItems.IRON_SAMURAI_CHESTPLATE,
			ModItems.IRON_SAMURAI_LEGGINGS,
			ModItems.IRON_SAMURAI_BOOTS);
		registerArmor("samurai_armor_new", "models/armor/netherite_samurai.png",
			ModItems.NETHERITE_SAMURAI_HELMET,
			ModItems.NETHERITE_SAMURAI_CHESTPLATE,
			ModItems.NETHERITE_SAMURAI_LEGGINGS,
			ModItems.NETHERITE_SAMURAI_BOOTS);
		registerArmor("light_samurai_armor", "models/armor/light_samurai.png",
			ModItems.LIGHT_SAMURAI_HELMET,
			ModItems.LIGHT_SAMURAI_CHESTPLATE,
			ModItems.LIGHT_SAMURAI_LEGGINGS,
			ModItems.LIGHT_SAMURAI_BOOTS);
		registerArmor("ninja_armor", "models/armor/ninja_steel.png",
			ModItems.STEEL_NINJA_HELMET,
			ModItems.STEEL_NINJA_CHESTPLATE,
			ModItems.NINJA_LEGGINGS,
			ModItems.STEEL_NINJA_BOOTS);
		registerArmor("ninja_armor", "models/armor/ninja_netherite.png",
			ModItems.NETHERITE_NINJA_HELMET,
			ModItems.NETHERITE_NINJA_CHESTPLATE,
			ModItems.NETHERITE_NINJA_BOOTS);
		registerArmor("kimono", "models/armor/kimono.png", ModItems.KIMONO);
		registerArmor("straw_hat", "models/armor/straw_hat.png", ModItems.STRAW_HAT);

		registerWeaponGeo("katana", "weapon/katana.png", ModItems.KATANA);
		registerWeaponGeo("katana", "weapon/katana_netherite.png", ModItems.KATANA_NETHERITE);
		registerWeaponGeo("wakizashi", "weapon/katana.png", ModItems.WAKIZASHI);
		registerWeaponGeo("wakizashi", "weapon/katana_netherite.png", ModItems.WAKIZASHI_NETHERITE);
		registerWeaponGeo("odachi", "weapon/odachi.png", ModItems.ODACHI);
		registerWeaponGeo("odachi", "weapon/odachi_netherite.png", ModItems.ODACHI_NETHERITE);
		registerWeaponGeo("naginata", "weapon/naginata.png", ModItems.NAGINATA);
		registerWeaponGeo("naginata", "weapon/naginata_netherite.png", ModItems.NAGINATA_NETHERITE);
		registerWeaponGeo("nagamaki", "weapon/nagamaki.png", ModItems.NAGAMAKI);
		registerWeaponGeo("nagamaki", "weapon/nagamaki_netherite.png", ModItems.NAGAMAKI_NETHERITE);
		registerWeaponGeo("kamayari", "weapon/kamayari.png", ModItems.KAMAYARI);
		registerWeaponGeo("kamayari", "weapon/kamayari_netherite.png", ModItems.KAMAYARI_NETHERITE);
		registerWeaponGeo("tonbukiri", "weapon/tonbukiri.png", ModItems.TONBUKIRI);
		registerWeaponGeo("tonbukiri", "weapon/tonbukiri_netherite.png", ModItems.TONBUKIRI_NETHERITE);
		registerWeaponGeo("tetsubo", "weapon/tetsubo.png", ModItems.TETSUBO);
		registerWeaponGeo("tetsubo", "weapon/tetsubo_netherite.png", ModItems.TETSUBO_NETHERITE);
		registerWeaponGeo("masakari", "weapon/masakari.png", ModItems.MASAKARI);
		registerWeaponGeo("masakari", "weapon/masakari_netherite.png", ModItems.MASAKARI_NETHERITE);
		registerWeaponGeo("kabutowari", "weapon/kabutowari.png", ModItems.KABUTOWARI);
		registerWeaponGeo("kabutowari", "weapon/kabutowari_netherite.png", ModItems.KABUTOWARI_NETHERITE);
		registerWeaponGeo("kunai", "weapon/kunai.png", ModItems.KUNAI);
		registerWeaponGeo("kunai", "weapon/kunai_netherite.png", ModItems.KUNAI_NETHERITE);
		registerWeaponGeo("tekko_kagi", "weapon/tekko_kagi.png", ModItems.SHUKO);
		registerWeaponGeo("tekko_kagi", "weapon/tekko_kagi_netherite.png", ModItems.SHUKO_NETHERITE);
		registerWeaponGeo("shuriken", "item/shuriken.png", ModItems.SHURIKEN);
	}

	private static void registerYumiPredicates() {
		ItemProperties.register(
			ModItems.YUMI,
			ResourceLocation.withDefaultNamespace("pull"),
			(stack, level, entity, seed) -> {
				if (entity == null || entity.getUseItem() != stack) {
					return 0.0F;
				}
				return (float) (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20.0F;
			});
		ItemProperties.register(
			ModItems.YUMI,
			ResourceLocation.withDefaultNamespace("pulling"),
			(stack, level, entity, seed) ->
				entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
	}

	private static void registerDyeableSamuraiTint(Item... items) {
		int fallback = DyeColor.RED.getTextureDiffuseColor();
		ColorProviderRegistry.ITEM.register(
			(stack, tintIndex) -> tintIndex > 0 ? -1 : DyedItemColor.getOrDefault(stack, fallback),
			items);
	}

	private static ResourceLocation dyedSamuraiTexture(ItemStack stack, String folder) {
		int rgb = DyedItemColor.getOrDefault(stack, DyeColor.RED.getTextureDiffuseColor());
		return NomosikiWork.id("textures/models/armor/" + folder + "/" + nearestDye(rgb).getName() + "_samurai_armor.png");
	}

	private static DyeColor nearestDye(int rgb) {
		DyeColor nearest = DyeColor.RED;
		int best = Integer.MAX_VALUE;
		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = rgb & 0xFF;
		for (DyeColor color : DyeColor.values()) {
			int c = color.getTextureDiffuseColor();
			int dr = r - ((c >> 16) & 0xFF);
			int dg = g - ((c >> 8) & 0xFF);
			int db = b - (c & 0xFF);
			int dist = dr * dr + dg * dg + db * db;
			if (dist < best) {
				best = dist;
				nearest = color;
			}
		}
		return nearest;
	}

	private static void registerArmor(String geoName, String texturePath, Item first, Item... rest) {
		ResourceLocation geo = NomosikiWork.id("geo/" + geoName + ".geo.json");
		ResourceLocation texture = NomosikiWork.id("textures/" + texturePath);
		AzArmorRendererRegistry.register(
			() -> new AzArmorRenderer(AzArmorRendererConfig.builder(geo, texture).build()),
			first,
			rest);
	}

	private static void registerWeaponGeo(String geoName, String texturePath, Item item) {
		ResourceLocation geo = NomosikiWork.id("geo/" + geoName + ".geo.json");
		ResourceLocation texture = NomosikiWork.id("textures/" + texturePath);
		AzItemRendererRegistry.register(
			() -> new WorkItemRenderer(AzItemRendererConfig.builder(geo, texture).build()),
			item);
	}

	private static final class WorkItemRenderer extends AzItemRenderer {
		private WorkItemRenderer(AzItemRendererConfig config) {
			super(config);
		}
	}
}
