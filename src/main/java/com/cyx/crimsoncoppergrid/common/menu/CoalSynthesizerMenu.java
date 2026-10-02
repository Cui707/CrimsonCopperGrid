package com.cyx.crimsoncoppergrid.common.menu;

import com.cyx.crimsoncoppergrid.blockentity.CoalSynthesizerBlockEntity;
import com.cyx.crimsoncoppergrid.common.powerSystem.PowerAcceptorBlockEntity;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 电力煤炭合成机界面：一个产物槽 + 电量条。
 *
 * <p>这台机器不需要输入槽 —— 它的「原料」就是电。电量条同时就是进度条：
 * 缓冲容量正好等于一块煤所需的电量，所以存量攒满即产出一块。
 * 因此界面上不再单独画进度条，只把电量条读成进度即可。
 *
 * <p>产物槽只能取不能放（{@link OutputSlot}）；煤也会被机器每 20 刻自动塞进相邻容器，
 * 所以槽里常常是空的，那是正常的。
 */
public class CoalSynthesizerMenu extends MachineMenu {

	/** 产物槽。摆在界面水平中心。 */
	static final int OUTPUT_X = 80;
	static final int OUTPUT_Y = 53;

	public CoalSynthesizerMenu(int containerId, Inventory playerInventory) {
		this(containerId, playerInventory,
				new SimpleContainer(CoalSynthesizerBlockEntity.SLOT_COUNT),
				new SimpleContainerData(CoalSynthesizerBlockEntity.DATA_COUNT));
	}

	public CoalSynthesizerMenu(int containerId, Inventory playerInventory, CoalSynthesizerBlockEntity synthesizer) {
		this(containerId, playerInventory, synthesizer, synthesizer);
	}

	private CoalSynthesizerMenu(int containerId, Inventory playerInventory, Container machine, ContainerData data) {
		super(ModMenuTypes.COAL_SYNTHESIZER, containerId, playerInventory, machine, data);
	}

	@Override
	protected void addMachineSlots(Container machine) {
		addSlot(new OutputSlot(machine, CoalSynthesizerBlockEntity.SLOT_OUTPUT, OUTPUT_X, OUTPUT_Y));
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
		return readLong(PowerAcceptorBlockEntity.DATA_STORED);
	}

	public long getCapacity() {
		return readLong(PowerAcceptorBlockEntity.DATA_CAPACITY);
	}

	public int getCoal() {
		return data.get(CoalSynthesizerBlockEntity.DATA_COAL);
	}
}
