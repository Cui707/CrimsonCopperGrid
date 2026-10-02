package com.cyx.crimsoncoppergrid.common.menu;

import com.cyx.crimsoncoppergrid.blockentity.SolarGeneratorBlockEntity;

import net.minecraft.world.entity.player.Inventory;

/**
 * 太阳能发电机的读数面板。字段与绘制都在 {@link GeneratorMenu} 里。
 *
 * <p>太阳能没有额外字段，界面数据就是父类的存量 + 容量。
 */
public class SolarGeneratorMenu extends GeneratorMenu {

	public SolarGeneratorMenu(int containerId, Inventory playerInventory) {
		super(ModMenuTypes.SOLAR_GENERATOR, containerId, playerInventory);
	}

	public SolarGeneratorMenu(int containerId, Inventory playerInventory, SolarGeneratorBlockEntity generator) {
		super(ModMenuTypes.SOLAR_GENERATOR, containerId, playerInventory, generator);
	}
}
