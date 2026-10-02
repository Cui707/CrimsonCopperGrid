package com.cyx.crimsoncoppergrid.common.menu;

import com.cyx.crimsoncoppergrid.blockentity.PowerControllerBlockEntity;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

/**
 * 电力控制器界面：把 {@code GridStats} 的一次扫描结果摊成几行数字。
 * 和电池一样没有物品槽，纯读数面板。
 */
public class PowerControllerMenu extends MachineMenu {

	public PowerControllerMenu(int containerId, Inventory playerInventory) {
		this(containerId, playerInventory,
				new SimpleContainer(0),
				new SimpleContainerData(PowerControllerBlockEntity.DATA_COUNT));
	}

	public PowerControllerMenu(int containerId, Inventory playerInventory, PowerControllerBlockEntity controller) {
		this(containerId, playerInventory, controller, controller);
	}

	private PowerControllerMenu(int containerId, Inventory playerInventory, Container machine, ContainerData data) {
		super(ModMenuTypes.POWER_CONTROLLER, containerId, playerInventory, machine, data);
	}

	@Override
	protected void addMachineSlots(Container machine) {
		// 仪表盘没有物品槽
	}

	// ------------------------------------------------------------ 界面读取

	public boolean isConnected() {
		return data.get(PowerControllerBlockEntity.DATA_CONNECTED) != 0;
	}

	public int getCableCount() {
		return data.get(PowerControllerBlockEntity.DATA_CABLE_COUNT);
	}

	public int getGeneratorCount() {
		return data.get(PowerControllerBlockEntity.DATA_GENERATOR_COUNT);
	}

	public int getConsumerCount() {
		return data.get(PowerControllerBlockEntity.DATA_CONSUMER_COUNT);
	}

	public int getBatteryCount() {
		return data.get(PowerControllerBlockEntity.DATA_BATTERY_COUNT);
	}

	public long getGeneratorOutput() {
		return readLong(PowerControllerBlockEntity.DATA_GENERATOR_OUTPUT_LOW);
	}

	public long getConsumerInput() {
		return readLong(PowerControllerBlockEntity.DATA_CONSUMER_INPUT_LOW);
	}

	public long getBatteryStored() {
		return readLong(PowerControllerBlockEntity.DATA_BATTERY_STORED_LOW);
	}

	public long getBatteryCapacity() {
		return readLong(PowerControllerBlockEntity.DATA_BATTERY_CAPACITY_LOW);
	}

	public long getNetworkStored() {
		return readLong(PowerControllerBlockEntity.DATA_NETWORK_STORED_LOW);
	}
}
