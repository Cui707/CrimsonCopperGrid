package com.cyx.crimsoncoppergrid.client;

import com.cyx.crimsoncoppergrid.client.render.WindTurbineRenderer;
import com.cyx.crimsoncoppergrid.client.screen.BatteryScreen;
import com.cyx.crimsoncoppergrid.client.screen.CoalSynthesizerScreen;
import com.cyx.crimsoncoppergrid.client.screen.ElectricFurnaceScreen;
import com.cyx.crimsoncoppergrid.client.screen.FuelGeneratorScreen;
import com.cyx.crimsoncoppergrid.client.screen.LavaGeneratorScreen;
import com.cyx.crimsoncoppergrid.client.screen.SolarGeneratorScreen;
import com.cyx.crimsoncoppergrid.client.screen.WindGeneratorScreen;
import com.cyx.crimsoncoppergrid.common.menu.ModMenuTypes;
import com.cyx.crimsoncoppergrid.init.ModBlockEntities;

import net.fabricmc.api.ClientModInitializer;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

/**
 * 客户端入口：把菜单类型接到屏幕上、把方块实体接到渲染器上。
 *
 * <p>26.3 里 {@code MenuScreens.register} 与 {@code BlockEntityRenderers.register} 都是私有的，
 * Fabric API 通过传递性访问拓宽（{@code fabric-transitive-access-wideners-v1}）把它们开放给
 * 依赖它的模组 —— TechReborn 走的是同一条路。模组侧不需要做任何额外声明。
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

		// 风力发电机的叶轮：方块模型只画机壳，转的部分靠方块实体渲染器
		BlockEntityRenderers.register(ModBlockEntities.WIND_GENERATOR, WindTurbineRenderer::new);
	}
}
