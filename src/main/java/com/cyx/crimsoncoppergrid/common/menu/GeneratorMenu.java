package com.cyx.crimsoncoppergrid.common.menu;

import com.cyx.crimsoncoppergrid.common.powerSystem.PowerAcceptorBlockEntity;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;

/**
 * 太阳能 / 风力 / 电力岩浆机共用的「读数面板」菜单。
 *
 * <p>这三台设备都<strong>没有物品槽</strong>：太阳能与风力什么都不用放，
 * 岩浆机的桶进出走方块层的右键交互。所以这个菜单一个槽都不加，
 * 只负责把电量（以及岩浆机的罐内量）从服务端同步到客户端。
 *
 * <h2>为什么三台各有一个子类</h2>
 * 菜单本身三台完全一样，但 {@code MenuType} 必须一一对应到各自的屏幕
 * —— 客户端是靠「打开界面包里带的 MenuType」来决定创建哪个 Screen 的。
 * 子类只做一件事：把属于自己的那个 {@code MenuType} 传给基类，
 * 这样菜单里缓存的类型与屏幕注册的类型永远一致。
 *
 * <h2>客户端空壳数据的格子数由子类给定</h2>
 * 客户端构造菜单时并不知道对面是哪台机器，所以数组大小只能靠子类显式传进来。
 * <b>必须与自己那台机器的 {@code DATA_COUNT} 一致</b>：服务端每下发一个更大的下标，
 * 客户端就会数组越界崩掉。宁可写成常量引用，也不要在这里凭感觉填个数字。
 */
public abstract class GeneratorMenu extends MachineMenu {

	/** 岩浆机罐内量的下标；太阳能与风力没有这个字段，读出来恒为 0。 */
	public static final int DATA_TANK = PowerAcceptorBlockEntity.DATA_COUNT;

	/**
	 * 客户端用：空壳容器 + 空壳数据。
	 *
	 * @param dataSlotCount 对应机器 {@code DATA_COUNT} 的槽位数
	 */
	protected GeneratorMenu(MenuType<?> type, int containerId, Inventory playerInventory, int dataSlotCount) {
		this(type, containerId, playerInventory, new SimpleContainer(0), new SimpleContainerData(dataSlotCount));
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

	/** 岩浆机罐内量（mB）；没有罐子的机器返回 0。 */
	public int getTank() {
		return data.getCount() > DATA_TANK ? data.get(DATA_TANK) : 0;
	}
}
