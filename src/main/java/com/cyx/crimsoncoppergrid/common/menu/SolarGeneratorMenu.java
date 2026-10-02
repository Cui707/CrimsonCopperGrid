package com.cyx.crimsoncoppergrid.common.menu;

import com.cyx.crimsoncoppergrid.blockentity.SolarGeneratorBlockEntity;

import net.minecraft.world.entity.player.Inventory;

/**
 * 太阳能发电机的读数面板。
 *
 * <p>比父类多一个「状态」字段：太阳能面板不画电量条，改为一句话告诉玩家
 * 「现在到底在不在发电、不在的话是因为什么」。服务端在 tick 里判定，
 * 通过 {@link net.minecraft.world.inventory.ContainerData} 同步过来。
 */
public class SolarGeneratorMenu extends GeneratorMenu {

	public SolarGeneratorMenu(int containerId, Inventory playerInventory) {
		super(ModMenuTypes.SOLAR_GENERATOR, containerId, playerInventory, SolarGeneratorBlockEntity.DATA_COUNT);
	}

	public SolarGeneratorMenu(int containerId, Inventory playerInventory, SolarGeneratorBlockEntity generator) {
		super(ModMenuTypes.SOLAR_GENERATOR, containerId, playerInventory, generator);
	}

	/** 取值见 {@code SolarGeneratorBlockEntity} 的 {@code STATUS_*} 常量。 */
	public int getStatus() {
		return data.get(SolarGeneratorBlockEntity.DATA_STATUS);
	}
}
