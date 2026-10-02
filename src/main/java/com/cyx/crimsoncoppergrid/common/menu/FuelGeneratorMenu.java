package com.cyx.crimsoncoppergrid.common.menu;

import com.cyx.crimsoncoppergrid.blockentity.FuelGeneratorBlockEntity;
import com.cyx.crimsoncoppergrid.common.powerSystem.PowerAcceptorBlockEntity;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 燃料发电机界面：一个燃料槽 + 电量条 + 燃烧进度条。
 *
 * <p>槽位摆在界面正中偏下，燃烧进度条画在它正上方，读作「这里在烧东西」。
 * 之所以不照抄原版熔炉的槽位坐标，是因为这张界面没有输入/输出槽，
 * 只有孤零零一个燃料槽，居中的构图更平衡。
 */
public class FuelGeneratorMenu extends MachineMenu {

	/** 燃料槽。放在界面水平中心，燃烧条压在它上方。 */
	static final int FUEL_X = 80;
	static final int FUEL_Y = 53;

	public FuelGeneratorMenu(int containerId, Inventory playerInventory) {
		this(containerId, playerInventory,
				new SimpleContainer(FuelGeneratorBlockEntity.SLOT_COUNT),
				new SimpleContainerData(FuelGeneratorBlockEntity.DATA_COUNT));
	}

	public FuelGeneratorMenu(int containerId, Inventory playerInventory, FuelGeneratorBlockEntity generator) {
		this(containerId, playerInventory, generator, generator);
	}

	private FuelGeneratorMenu(int containerId, Inventory playerInventory, Container machine, ContainerData data) {
		super(ModMenuTypes.FUEL_GENERATOR, containerId, playerInventory, machine, data);
	}

	@Override
	protected void addMachineSlots(Container machine) {
		addSlot(new Slot(machine, FuelGeneratorBlockEntity.SLOT_FUEL, FUEL_X, FUEL_Y) {
			/** 放入权限交给方块实体判断：只有它认识的燃料才收。 */
			@Override
			public boolean mayPlace(ItemStack stack) {
				return container.canPlaceItem(getContainerSlot(), stack);
			}
		});
	}

	// ------------------------------------------------------------ 界面读取

	public long getStored() {
		return readLong(PowerAcceptorBlockEntity.DATA_STORED);
	}

	public long getCapacity() {
		return readLong(PowerAcceptorBlockEntity.DATA_CAPACITY);
	}

	/** 当前这份燃料还能烧多少刻。 */
	public int getBurnTime() {
		return data.get(FuelGeneratorBlockEntity.DATA_BURN_TIME);
	}

	/** 当前这份燃料总共能烧多少刻；为 0 表示没在烧。 */
	public int getBurnTimeTotal() {
		return data.get(FuelGeneratorBlockEntity.DATA_BURN_TIME_TOTAL);
	}
}
