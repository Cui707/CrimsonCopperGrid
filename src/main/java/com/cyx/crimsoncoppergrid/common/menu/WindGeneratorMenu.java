package com.cyx.crimsoncoppergrid.common.menu;

import com.cyx.crimsoncoppergrid.blockentity.WindGeneratorBlockEntity;

import net.minecraft.world.entity.player.Inventory;

/**
 * 风力发电机的读数面板。
 *
 * <p>比父类多三个字段：当前输出、当前塔高、状态。前两个告诉玩家「现在拿到多少」，
 * 状态告诉玩家「没拿到的话卡在哪一步」—— 塔高是向下扫方块数出来的，客户端不能凭空知道，
 * 得由服务端算好同步过来。
 */
public class WindGeneratorMenu extends GeneratorMenu {

	public WindGeneratorMenu(int containerId, Inventory playerInventory) {
		super(ModMenuTypes.WIND_GENERATOR, containerId, playerInventory, WindGeneratorBlockEntity.DATA_COUNT);
	}

	public WindGeneratorMenu(int containerId, Inventory playerInventory, WindGeneratorBlockEntity generator) {
		super(ModMenuTypes.WIND_GENERATOR, containerId, playerInventory, generator);
	}

	/** 当前发电量；0 表示没在发电。 */
	public long getOutput() {
		return readLong(WindGeneratorBlockEntity.DATA_OUTPUT);
	}

	/** 当前塔高（底座与发电机之间隔着的方块数）；{@code -1} 表示没有底座。 */
	public int getTowerHeight() {
		return readData(WindGeneratorBlockEntity.DATA_TOWER_HEIGHT);
	}

	/** 当前状态，取值见 {@code WindGeneratorBlockEntity} 的 {@code STATUS_*} 常量。 */
	public int getStatus() {
		return readData(WindGeneratorBlockEntity.DATA_STATUS);
	}
}
