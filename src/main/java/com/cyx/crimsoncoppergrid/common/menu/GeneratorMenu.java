package com.cyx.crimsoncoppergrid.common.menu;

import com.cyx.crimsoncoppergrid.common.powerSystem.PowerAcceptorBlockEntity;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;

/**
 * 太阳能 / 风力发电机共用的「读数面板」菜单。
 *
 * <p>这两台设备都没有物品槽，菜单只负责把电量从服务端同步到客户端。
 */
public abstract class GeneratorMenu extends MachineMenu {

	/**
	 * 客户端用：空壳容器 + 空壳数据。
	 */
	protected GeneratorMenu(MenuType<?> type, int containerId, Inventory playerInventory) {
		this(type, containerId, playerInventory,
				new SimpleContainer(0), new SimpleContainerData(PowerAcceptorBlockEntity.DATA_COUNT));
	}

	/**
	 * 服务端用：方块实体既是容器也是 {@code ContainerData}。
	 *
	 * <p>把方块实体当容器传进去（而不是空壳），是为了让 {@code stillValid} 能走到
	 * {@code Container.stillValidBlockEntity} 的距离判定 —— 玩家走远了界面就该关掉。
	 */
	protected GeneratorMenu(MenuType<?> type, int containerId, Inventory playerInventory,
			PowerAcceptorBlockEntity machine) {
		this(type, containerId, playerInventory, machine, machine);
	}

	private GeneratorMenu(MenuType<?> type, int containerId, Inventory playerInventory,
			Container machine, ContainerData data) {
		super(type, containerId, playerInventory, machine, data);
	}

	/** 纯读数面板，没有物品槽。 */
	@Override
	protected void addMachineSlots(Container machine) {
	}

	// ------------------------------------------------------------ 界面读取

	public long getStored() {
		return readLong(PowerAcceptorBlockEntity.DATA_STORED);
	}

	public long getCapacity() {
		return readLong(PowerAcceptorBlockEntity.DATA_CAPACITY);
	}
}
