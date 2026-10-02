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
 * <p>这两台设备都没有物品槽，菜单只负责把数值从服务端同步到客户端。
 * 两个子类都<b>不显示内部缓冲存量</b>：它们的缓冲只有 2.5~33 秒的量，
 * 读数不是贴着上限就是 0，对玩家没有信息量 —— 面板改为显示「在不在发电」，
 * 所以各自追加了自己的数据字段（见 {@code SolarGeneratorMenu#getStatus()}、
 * {@code WindGeneratorMenu#getOutput()}）。
 */
public abstract class GeneratorMenu extends MachineMenu {

	/**
	 * 客户端用：空壳容器 + 空壳数据。
	 *
	 * <p>{@code dataCount} 必须由子类传入**自己**的 {@code DATA_COUNT} ——
	 * 客户端这个 {@link SimpleContainerData} 是按这个长度建表的，
	 * 服务端多出的槽位收到时会因为下标越界而抛异常。
	 */
	protected GeneratorMenu(MenuType<?> type, int containerId, Inventory playerInventory, int dataCount) {
		this(type, containerId, playerInventory, new SimpleContainer(0), new SimpleContainerData(dataCount));
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
}
