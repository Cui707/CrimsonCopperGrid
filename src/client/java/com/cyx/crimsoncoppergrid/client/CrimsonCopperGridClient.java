package com.cyx.crimsoncoppergrid.client;

import com.cyx.crimsoncoppergrid.client.screen.BatteryScreen;
import com.cyx.crimsoncoppergrid.client.screen.CoalSynthesizerScreen;
import com.cyx.crimsoncoppergrid.client.screen.ElectricFurnaceScreen;
import com.cyx.crimsoncoppergrid.client.screen.FuelGeneratorScreen;
import com.cyx.crimsoncoppergrid.client.screen.LavaGeneratorScreen;
import com.cyx.crimsoncoppergrid.client.screen.PowerControllerScreen;
import com.cyx.crimsoncoppergrid.client.screen.SolarGeneratorScreen;
import com.cyx.crimsoncoppergrid.client.screen.WindGeneratorScreen;
import com.cyx.crimsoncoppergrid.common.menu.ModMenuTypes;

import net.fabricmc.api.ClientModInitializer;

import net.minecraft.client.gui.screens.MenuScreens;

/**
 * 客户端入口：把菜单类型接到屏幕上。
 *
 * <p>26.3 里 {@code MenuScreens.register} 是私有的，Fabric API 通过传递性访问拓宽
 * （{@code fabric-transitive-access-wideners-v1}）把它开放给依赖它的模组 ——
 * TechReborn 走的是同一条路。模组侧不需要做任何额外声明。
 */
public class CrimsonCopperGridClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MenuScreens.register(ModMenuTypes.BATTERY, BatteryScreen::new);
		MenuScreens.register(ModMenuTypes.ELECTRIC_FURNACE, ElectricFurnaceScreen::new);
		MenuScreens.register(ModMenuTypes.FUEL_GENERATOR, FuelGeneratorScreen::new);
		MenuScreens.register(ModMenuTypes.COAL_SYNTHESIZER, CoalSynthesizerScreen::new);
		MenuScreens.register(ModMenuTypes.SOLAR_GENERATOR, SolarGeneratorScreen::new);
		MenuScreens.register(ModMenuTypes.WIND_GENERATOR, WindGeneratorScreen::new);
		MenuScreens.register(ModMenuTypes.LAVA_GENERATOR, LavaGeneratorScreen::new);
		MenuScreens.register(ModMenuTypes.POWER_CONTROLLER, PowerControllerScreen::new);
	}
}
