package com.cyx.crimsoncoppergrid.init;

import com.cyx.crimsoncoppergrid.item.EnergyMeterItem;

/** 物品注册：只放没有对应方块、需要自定义行为的物品。 */
public final class ModItems {
	public static final EnergyMeterItem ENERGY_METER = ModBlocks.registerItem(
			"energy_meter",
			(key, properties) -> new EnergyMeterItem(properties.stacksTo(1)));

	private ModItems() {
	}

	public static void init() {
	}
}
