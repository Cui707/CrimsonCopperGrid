package com.cyx.crimsoncoppergrid.common.menu;

import com.cyx.crimsoncoppergrid.blockentity.LavaGeneratorBlockEntity;

import net.minecraft.world.entity.player.Inventory;

/**
 * 电力岩浆机的读数面板，比另外两台多一行罐内岩浆量。
 *
 * <p>多了这一格，客户端空壳数据也必须跟着多一格 —— 否则服务端下发它时
 * 客户端会数组越界。所以这里直接引用方块实体的 {@code DATA_COUNT}。
 */
public class LavaGeneratorMenu extends GeneratorMenu {

	public LavaGeneratorMenu(int containerId, Inventory playerInventory) {
		super(ModMenuTypes.LAVA_GENERATOR, containerId, playerInventory, LavaGeneratorBlockEntity.DATA_COUNT);
	}

	public LavaGeneratorMenu(int containerId, Inventory playerInventory, LavaGeneratorBlockEntity generator) {
		super(ModMenuTypes.LAVA_GENERATOR, containerId, playerInventory, generator);
	}
}
