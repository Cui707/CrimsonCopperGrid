package com.cyx.crimsoncoppergrid.common.menu;

import com.cyx.crimsoncoppergrid.blockentity.BatteryBlockEntity;
import com.cyx.crimsoncoppergrid.common.powerSystem.PowerAcceptorBlockEntity;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

/**
 * 电池界面。电池没有物品槽 —— 它的界面就是一块电量表：
 * 存量条 + 数字读数，外加一行「正在充 / 正在放」的净流量提示。
 *
 * <p>因此这里的机器容器是**零格**的：{@code machineSlotCount() == 0}，
 * {@link MachineMenu#quickMoveStack} 会直接放行，让 Shift 点击在背包内部正常工作。
 */
public class BatteryMenu extends MachineMenu {

	public BatteryMenu(int containerId, Inventory playerInventory) {
		// 客户端：空壳容器 + 空壳数据，实际内容靠服务端的同步包填
		this(containerId, playerInventory,
				new SimpleContainer(0),
				new SimpleContainerData(BatteryBlockEntity.DATA_COUNT));
	}

	public BatteryMenu(int containerId, Inventory playerInventory, BatteryBlockEntity battery) {
		// 服务端：方块实体本身就是 Container，也是 ContainerData
		this(containerId, playerInventory, battery, battery);
	}

	private BatteryMenu(int containerId, Inventory playerInventory, Container machine, ContainerData data) {
		super(ModMenuTypes.BATTERY, containerId, playerInventory, machine, data);
	}

	@Override
	protected void addMachineSlots(Container machine) {
		// 电池没有物品槽
	}

	// ------------------------------------------------------------ 界面读取

	public long getStored() {
		return readLong(PowerAcceptorBlockEntity.DATA_STORED_LOW);
	}

	public long getCapacity() {
		return readLong(PowerAcceptorBlockEntity.DATA_CAPACITY_LOW);
	}

	/** 上一个 tick 的净流量：正数在充电，负数在放电。 */
	public long getPowerChange() {
		return readLong(BatteryBlockEntity.DATA_POWER_CHANGE_LOW);
	}
}
