package com.example.client.compat;

import com.example.ZKWorkers;
import com.example.client.HumanWorkerRenderer;
import com.example.entity.HumanWorkerEntity;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Soft client hook for Female Gender Mod 3.2.1. The mod draws breasts for any living renderer, but
 * {@code BreastPhysics} is ticked only for players and armor stands, so a worker's bust stays at 0.
 */
public final class FemaleGenderCompat {
	private static final String MOD_ID = "wildfire_gender";
	private static final boolean LOADED = FabricLoader.getInstance().isModLoaded(MOD_ID);
	private static final float VISIBLE_BUST = 0.6F;
	private static final Map<HumanWorkerEntity, Integer> PHYSICS_TICK = new WeakHashMap<>();
	private static Method getEntity;
	private static Method getBustSize;
	private static Method tickPhysics;
	private static Field genderField;
	private static Field bustField;
	private static Object female;
	private static boolean ready;
	private static boolean failed;

	private FemaleGenderCompat() {
	}

	public static void attach(HumanWorkerRenderer renderer) {
		if (!LOADED) {
			return;
		}
		try {
			Class<?> layerClass = Class.forName("com.wildfire.render.GenderLayer");
			Constructor<?> constructor = layerClass.getConstructor(RenderLayerParent.class);
			RenderLayer<?, ?> layer = (RenderLayer<?, ?>) constructor.newInstance(renderer);
			renderer.addExternalLayer(layer);
		} catch (ReflectiveOperationException exception) {
			warn(exception);
		}
	}

	public static void prepare(HumanWorkerEntity entity) {
		if (!LOADED || failed || !entity.isFemale() || !bind()) {
			return;
		}
		try {
			Object config = getEntity.invoke(null, entity);
			if (config == null) {
				return;
			}
			if (genderField.get(config) != female) {
				genderField.set(config, female);
			}
			float size = ((Number) getBustSize.invoke(config)).floatValue();
			if (size < 0.02F) {
				if (bustField.getType() == float.class) {
					bustField.setFloat(config, VISIBLE_BUST);
				} else {
					bustField.set(config, VISIBLE_BUST);
				}
			}
			int tick = entity.tickCount;
			Integer seen = PHYSICS_TICK.get(entity);
			if (seen == null || seen != tick) {
				PHYSICS_TICK.put(entity, tick);
				tickPhysics.invoke(config, entity);
			}
		} catch (ReflectiveOperationException exception) {
			failed = true;
			warn(exception);
		}
	}

	private static boolean bind() {
		if (ready) {
			return true;
		}
		try {
			Class<?> configClass = Class.forName("com.wildfire.main.entitydata.EntityConfig");
			getEntity = configClass.getMethod("getEntity", LivingEntity.class);
			getBustSize = configClass.getMethod("getBustSize");
			tickPhysics = configClass.getMethod("tickBreastPhysics", LivingEntity.class);
			genderField = configClass.getDeclaredField("gender");
			genderField.setAccessible(true);
			bustField = configClass.getDeclaredField("pBustSize");
			bustField.setAccessible(true);
			Class<?> genderClass = Class.forName("com.wildfire.main.Gender");
			female = femaleValue(genderClass);
			ready = true;
			return true;
		} catch (ReflectiveOperationException exception) {
			failed = true;
			warn(exception);
			return false;
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static Object femaleValue(Class<?> genderClass) {
		return Enum.valueOf((Class) genderClass, "FEMALE");
	}

	private static void warn(ReflectiveOperationException exception) {
		ZKWorkers.LOGGER.warn("Female Gender Mod is loaded, but worker breast rendering could not attach", exception);
	}
}
