package com.cyx.crimsoncoppergrid.common.menu;

import com.cyx.crimsoncoppergrid.blockentity.ElectricFurnaceBlockEntity;
import com.cyx.crimsoncoppergrid.common.powerSystem.PowerAcceptorBlockEntity;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 电力熔炉界面。槽位坐标刻意与原版熔炉完全一致
 * （输入 56,17 / 输出 116,35，玩家背包也是标准位置），
 * 这样可以直接复用原版熔炉的那张容器贴图当背景，一行贴图资源都不用新增。
 *
 * <p>原版熔炉的「燃料槽」位置留给电量条 —— 都是「给机器供能的东西」，
 * 位置对应得刚好。
 */
public class ElectricFurnaceMenu extends MachineMenu {

	/** 输入槽（原版熔炉的原料位）。 */
	static final int INPUT_X = 56;
	static final int INPUT_Y = 17;
	/** 输出槽（原版熔炉的产物位）。 */
	static final int OUTPUT_X = 116;
	static final int OUTPUT_Y = 35;

	public ElectricFurnaceMenu(int containerId, Inventory playerInventory) {
		this(containerId, playerInventory,
				new SimpleContainer(ElectricFurnaceBlockEntity.SLOT_COUNT),
				new SimpleContainerData(ElectricFurnaceBlockEntity.DATA_COUNT));
	}

	public ElectricFurnaceMenu(int containerId, Inventory playerInventory, ElectricFurnaceBlockEntity furnace) {
		this(containerId, playerInventory, furnace, furnace);
	}

	private ElectricFurnaceMenu(int containerId, Inventory playerInventory, Container machine, ContainerData data) {
		super(ModMenuTypes.ELECTRIC_FURNACE, containerId, playerInventory, machine, data);
	}

	@Override
	protected void addMachineSlots(Container machine) {
		addSlot(new MachineSlot(machine, ElectricFurnaceBlockEntity.SLOT_INPUT, INPUT_X, INPUT_Y));
		addSlot(new OutputSlot(machine, ElectricFurnaceBlockEntity.SLOT_OUTPUT, OUTPUT_X, OUTPUT_Y));
	}

	/** 放入权限交给容器自己判断（方块实体上的 {@code canPlaceItem} 是权威）。 */
	private static class MachineSlot extends Slot {
		MachineSlot(Container container, int index, int x, int y) {
			super(container, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return container.canPlaceItem(getContainerSlot(), stack);
		}
	}

	/** 产物槽只能取、不能放。 */
	private static class OutputSlot extends Slot {
		OutputSlot(Container container, int index, int x, int y) {
			super(container, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}
	}

	// ------------------------------------------------------------ 界面读取

	public long getStored() {
		return readLong(PowerAcceptorBlockEntity.DATA_STORED_LOW);
	}

	public long getCapacity() {
		return readLong(PowerAcceptorBlockEntity.DATA_CAPACITY_LOW);
	}

	public int getProgress() {
		return data.get(ElectricFurnaceBlockEntity.DATA_PROGRESS);
	}

	public int getMaxProgress() {
		return Math.max(1, data.get(ElectricFurnaceBlockEntity.DATA_MAX_PROGRESS));
	}
}
