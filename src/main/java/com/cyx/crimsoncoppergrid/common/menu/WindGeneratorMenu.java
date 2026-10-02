package com.cyx.crimsoncoppergrid.common.menu;

import com.cyx.crimsoncoppergrid.blockentity.WindGeneratorBlockEntity;

import net.minecraft.world.entity.player.Inventory;

/**
 * 风力发电机的读数面板。字段与绘制都在 {@link GeneratorMenu} 里。
 *
 * <p>风力没有额外字段，界面数据就是父类的存量 + 容量。
 */
public class WindGeneratorMenu extends GeneratorMenu {

	public WindGeneratorMenu(int containerId, Inventory playerInventory) {
		super(ModMenuTypes.WIND_GENERATOR, containerId, playerInventory);
	}

	public WindGeneratorMenu(int containerId, Inventory playerInventory, WindGeneratorBlockEntity generator) {
		super(ModMenuTypes.WIND_GENERATOR, containerId, playerInventory, generator);
	}
}
