package com.example.registry;

import com.example.ZKWorkers;
import com.example.inventory.WorkerInspectData;
import com.example.inventory.WorkerInspectMenu;
import com.example.inventory.WorkerMenu;
import com.example.inventory.WorkerMenuData;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
	public static final MenuType<WorkerMenu> WORKER = Registry.register(
			BuiltInRegistries.MENU,
			ZKWorkers.id("worker_block"),
			new ExtendedScreenHandlerType<>(WorkerMenu::create, WorkerMenuData.STREAM_CODEC)
	);
	public static final MenuType<WorkerInspectMenu> WORKER_INSPECT = Registry.register(
			BuiltInRegistries.MENU,
			ZKWorkers.id("worker_inspect"),
			new ExtendedScreenHandlerType<>(WorkerInspectMenu::create, WorkerInspectData.STREAM_CODEC)
	);

	private ModMenus() {
	}

	public static void register() {
		ZKWorkers.LOGGER.debug("Registered worker menus");
	}
}
