package com.nomosiki.registry;

import com.nomosiki.NomosikiMod;
import com.nomosiki.screen.HireMenuData;
import com.nomosiki.screen.FactionCreateMenu;
import com.nomosiki.screen.HireScreenHandler;
import com.nomosiki.screen.NomosikMenuData;
import com.nomosiki.screen.NomosikScreenHandler;
import com.nomosiki.screen.TesterSquadShopMenuData;
import com.nomosiki.screen.TesterSquadShopScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public final class ModScreenHandlers {
	public static final ExtendedScreenHandlerType<NomosikScreenHandler, NomosikMenuData> NOMOSIK =
		Registry.register(
			BuiltInRegistries.MENU,
			NomosikiMod.id("nomosik"),
			new ExtendedScreenHandlerType<>(NomosikScreenHandler::create, NomosikMenuData.STREAM_CODEC));
	public static final ExtendedScreenHandlerType<HireScreenHandler, HireMenuData> HIRE =
		Registry.register(
			BuiltInRegistries.MENU,
			NomosikiMod.id("hire"),
			new ExtendedScreenHandlerType<>(HireScreenHandler::create, HireMenuData.STREAM_CODEC));
	public static final MenuType<FactionCreateMenu> FACTION_CREATE =
		Registry.register(
			BuiltInRegistries.MENU,
			NomosikiMod.id("faction_create"),
			new MenuType<>(FactionCreateMenu::new, FeatureFlags.DEFAULT_FLAGS));
	public static final ExtendedScreenHandlerType<TesterSquadShopScreenHandler, TesterSquadShopMenuData> TESTER_SQUAD_SHOP =
		Registry.register(
			BuiltInRegistries.MENU,
			NomosikiMod.id("tester_squad_shop"),
			new ExtendedScreenHandlerType<>(TesterSquadShopScreenHandler::create, TesterSquadShopMenuData.STREAM_CODEC));

	private ModScreenHandlers() {
	}

	public static void register() {
	}
}
