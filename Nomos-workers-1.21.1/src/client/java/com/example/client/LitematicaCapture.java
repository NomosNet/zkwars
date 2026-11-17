package com.example.client;

import com.example.ZKWorkers;
import com.example.block.work.BlockStateText;
import com.example.block.work.BuilderTasks;
import com.example.block.work.SchematicBlocks;
import com.example.network.BuilderCaptureChunkPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class LitematicaCapture {
	private static Method cachedBlockState;

	private LitematicaCapture() {
	}

	public static void capture(BlockPos origin) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}
		if (!FabricLoader.getInstance().isModLoaded("litematica")) {
			tell(player, "zk-workers.builder.no_litematica");
			return;
		}
		try {
			List<BuilderCaptureChunkPayload.Cell> cells = read(player, origin);
			if (cells == null) {
				return;
			}
			if (cells.isEmpty()) {
				tell(player, "zk-workers.builder.empty");
				return;
			}
			send(origin, player, cells);
		} catch (ReflectiveOperationException | ClassCastException | IllegalArgumentException exception) {
			ZKWorkers.LOGGER.warn("Could not read the selected Litematica schematic", exception);
			tell(player, "zk-workers.builder.api");
		}
	}

	private static List<BuilderCaptureChunkPayload.Cell> read(LocalPlayer player, BlockPos origin) throws ReflectiveOperationException {
		Object placement = selectedPlacement();
		if (placement == null) {
			tell(player, "zk-workers.builder.no_selection");
			return null;
		}
		Object world = schematicWorld();
		if (world == null) {
			tell(player, "zk-workers.builder.no_selection");
			return null;
		}
		Method boxesMethod = boxesMethod(placement.getClass());
		Object required = enumConstant(boxesMethod.getParameterTypes()[0], "PLACEMENT_ENABLED");
		Object boxes = boxesMethod.invoke(placement, required);
		if (!(boxes instanceof Map<?, ?> regions)) {
			throw new IllegalStateException("subregion boxes");
		}
		Method blockState = blockStateMethod(world.getClass());
		List<BuilderCaptureChunkPayload.Cell> cells = new ArrayList<>();
		for (Object box : regions.values()) {
			BlockPos first = (BlockPos) box.getClass().getMethod("getPos1").invoke(box);
			BlockPos second = (BlockPos) box.getClass().getMethod("getPos2").invoke(box);
			int minX = Math.min(first.getX(), second.getX());
			int maxX = Math.max(first.getX(), second.getX());
			int minY = Math.min(first.getY(), second.getY());
			int maxY = Math.max(first.getY(), second.getY());
			int minZ = Math.min(first.getZ(), second.getZ());
			int maxZ = Math.max(first.getZ(), second.getZ());
			if (fartherThan(origin, minX, maxX, minY, maxY, minZ, maxZ)) {
				tell(player, "zk-workers.builder.too_far");
				return null;
			}
			BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
			for (int y = minY; y <= maxY; y++) {
				for (int x = minX; x <= maxX; x++) {
					for (int z = minZ; z <= maxZ; z++) {
						cursor.set(x, y, z);
						Object stateObject = blockState.invoke(world, cursor.immutable());
						if (!(stateObject instanceof BlockState state) || !SchematicBlocks.placeable(state)) {
							continue;
						}
						cells.add(new BuilderCaptureChunkPayload.Cell(
								BlockStateText.stringify(state),
								x - origin.getX(),
								y - origin.getY(),
								z - origin.getZ()
						));
						if (cells.size() > BuilderTasks.MAX_BLOCKS) {
							tell(player, "zk-workers.builder.too_big");
							return null;
						}
					}
				}
			}
		}
		cells.sort(Comparator.comparingInt(BuilderCaptureChunkPayload.Cell::dy)
				.thenComparingInt(BuilderCaptureChunkPayload.Cell::dx)
				.thenComparingInt(BuilderCaptureChunkPayload.Cell::dz));
		return cells;
	}

	private static void send(BlockPos origin, LocalPlayer player, List<BuilderCaptureChunkPayload.Cell> cells) {
		int token = player.getRandom().nextInt();
		int count = (cells.size() + BuilderCaptureChunkPayload.MAX_CELLS - 1) / BuilderCaptureChunkPayload.MAX_CELLS;
		for (int index = 0; index < count; index++) {
			int from = index * BuilderCaptureChunkPayload.MAX_CELLS;
			int to = Math.min(cells.size(), from + BuilderCaptureChunkPayload.MAX_CELLS);
			ClientPlayNetworking.send(new BuilderCaptureChunkPayload(origin, token, index, count, List.copyOf(cells.subList(from, to))));
		}
	}

	private static boolean fartherThan(BlockPos origin, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
		int dx = Math.max(Math.abs(minX - origin.getX()), Math.abs(maxX - origin.getX()));
		int dy = Math.max(Math.abs(minY - origin.getY()), Math.abs(maxY - origin.getY()));
		int dz = Math.max(Math.abs(minZ - origin.getZ()), Math.abs(maxZ - origin.getZ()));
		return dx > BuilderTasks.RANGE || dy > BuilderTasks.RANGE || dz > BuilderTasks.RANGE;
	}

	private static Object selectedPlacement() throws ReflectiveOperationException {
		Object manager = invokeStatic(new String[] {
				"fi.dy.masa.litematica.data.DataManager",
				"litematica.data.DataManager"
		}, "getSchematicPlacementManager");
		if (manager == null) {
			throw new IllegalStateException("placement manager");
		}
		return manager.getClass().getMethod("getSelectedSchematicPlacement").invoke(manager);
	}

	private static Object schematicWorld() throws ReflectiveOperationException {
		return invokeStatic(new String[] {
				"fi.dy.masa.litematica.world.SchematicWorldHandler",
				"litematica.world.SchematicWorldHandler"
		}, "getSchematicWorld");
	}

	private static Object invokeStatic(String[] classNames, String method) throws ReflectiveOperationException {
		ReflectiveOperationException last = null;
		for (String className : classNames) {
			try {
				Class<?> type = Class.forName(className);
				return type.getMethod(method).invoke(null);
			} catch (ClassNotFoundException | NoSuchMethodException exception) {
				last = exception;
			}
		}
		if (last != null) {
			throw last;
		}
		throw new ClassNotFoundException(method);
	}

	private static Method blockStateMethod(Class<?> type) throws NoSuchMethodException {
		Method cached = cachedBlockState;
		if (cached != null && cached.getDeclaringClass().isAssignableFrom(type)) {
			return cached;
		}
		Method bridge = null;
		for (Class<?> cursor = type; cursor != null; cursor = cursor.getSuperclass()) {
			for (Method method : cursor.getDeclaredMethods()) {
				if (method.getParameterCount() != 1 || method.getParameterTypes()[0] != BlockPos.class || method.getReturnType() != BlockState.class) {
					continue;
				}
				if (method.isSynthetic() || method.isBridge()) {
					if (bridge == null) {
						bridge = method;
					}
					continue;
				}
				return cacheBlockState(method);
			}
		}
		if (bridge != null) {
			return cacheBlockState(bridge);
		}
		throw new NoSuchMethodException("BlockPos -> BlockState");
	}

	private static Method cacheBlockState(Method method) {
		method.trySetAccessible();
		cachedBlockState = method;
		return method;
	}

	private static Method boxesMethod(Class<?> type) throws NoSuchMethodException {
		for (Method method : type.getMethods()) {
			if (method.getName().equals("getSubRegionBoxes") && method.getParameterCount() == 1 && method.getParameterTypes()[0].isEnum()) {
				return method;
			}
		}
		throw new NoSuchMethodException("getSubRegionBoxes");
	}

	private static Object enumConstant(Class<?> type, String name) {
		Object[] constants = type.getEnumConstants();
		if (constants == null) {
			throw new IllegalArgumentException(name);
		}
		for (Object constant : constants) {
			if (name.equals(((Enum<?>) constant).name())) {
				return constant;
			}
		}
		throw new IllegalArgumentException(name);
	}

	private static void tell(LocalPlayer player, String key) {
		player.displayClientMessage(Component.translatable(key), false);
	}
}
