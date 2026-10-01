package com.cyx.crimsoncoppergrid.registry;

import com.cyx.crimsoncoppergrid.item.EnergyMeterItem;

/**
 * 物品注册。
 *
 * <p>方块的 BlockItem 在 {@link ModBlocks} 里随方块一起注册；这里放的是
 * 需要自定义行为、没有对应方块的物品。
 */
public final class ModItems {
	public static final EnergyMeterItem ENERGY_METER = ModBlocks.registerItem(
			"energy_meter",
			(key, properties) -> new EnergyMeterItem(properties.stacksTo(1)));

	private ModItems() {
	}

	public static void init() {
	}
}
