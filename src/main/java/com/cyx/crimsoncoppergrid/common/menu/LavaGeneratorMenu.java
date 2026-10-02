package com.cyx.crimsoncoppergrid.common.menu;

import com.cyx.crimsoncoppergrid.blockentity.LavaGeneratorBlockEntity;
import com.cyx.crimsoncoppergrid.common.powerSystem.PowerAcceptorBlockEntity;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 电力岩浆机界面。
 *
 * <p>布局与原版熔炉一致：输入槽在左（放空桶），输出槽在右（出岩浆桶），
 * 中间是灌注进度；下方电量条显示当前缓冲与每 tick 耗电。
 */
public class LavaGeneratorMenu extends MachineMenu {

	/** 输入槽（空桶）。 */
	static final int INPUT_X = 56;
	static final int INPUT_Y = 17;
	/** 输出槽（岩浆桶）。 */
	static final int OUTPUT_X = 116;
	static final int OUTPUT_Y = 35;

	public LavaGeneratorMenu(int containerId, Inventory playerInventory) {
		this(containerId, playerInventory,
				new SimpleContainer(LavaGeneratorBlockEntity.SLOT_COUNT),
				new SimpleContainerData(LavaGeneratorBlockEntity.DATA_COUNT));
	}

	public LavaGeneratorMenu(int containerId, Inventory playerInventory, LavaGeneratorBlockEntity generator) {
		this(containerId, playerInventory, generator, generator);
	}

	private LavaGeneratorMenu(int containerId, Inventory playerInventory, Container machine, ContainerData data) {
		super(ModMenuTypes.LAVA_GENERATOR, containerId, playerInventory, machine, data);
	}

	@Override
	protected void addMachineSlots(Container machine) {
		addSlot(new BucketSlot(machine, LavaGeneratorBlockEntity.SLOT_INPUT, INPUT_X, INPUT_Y));
		addSlot(new OutputSlot(machine, LavaGeneratorBlockEntity.SLOT_OUTPUT, OUTPUT_X, OUTPUT_Y));
	}

	/** 输入槽只接受空桶。 */
	private static class BucketSlot extends Slot {
		BucketSlot(Container container, int index, int x, int y) {
			super(container, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return stack.is(Items.BUCKET);
		}
	}

	/** 输出槽只出不进。 */
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

	public int getProgress() {
		return data.get(LavaGeneratorBlockEntity.DATA_PROGRESS);
	}

	public int getMaxProgress() {
		return Math.max(1, data.get(LavaGeneratorBlockEntity.DATA_MAX_PROGRESS));
	}
}
