package com.cyx.crimsoncoppergrid.common.menu;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 所有 CCG 机器界面的公共父类。分层与命名对齐 TechReborn / RebornCore 的
 * {@code BuiltScreenHandler}（MIT），但省掉了它的链式 DSL —— CCG 的机器槽位少且固定，
 * 用抽象方法 {@link #addMachineSlots} 直接写更直白。
 *
 * <h2>两个构造器，两侧各用一半</h2>
 * 原版容器菜单的通行做法是「服务端持有真容器、客户端持有一个空壳容器」：
 * <ul>
 *   <li><b>服务端</b>（{@code (int, Inventory, 方块实体)}）—— 直接拿方块实体当容器，
 *       因为它本身就实现了 {@link Container}；</li>
 *   <li><b>客户端</b>（{@code (int, Inventory)}，即 {@code MenuType} 的工厂签名）——
 *       新建一个同尺寸的空壳容器。里面的物品不靠这个容器填，而是靠服务端发来的
 *       槽位同步包。这样客户端菜单就**不需要知道方块坐标**，也就不必给
 *       {@code MenuType} 附加任何自定义数据。</li>
 * </ul>
 *
 * <h2>数据同步</h2>
 * 电量、烧炼进度这类非物品数据走 {@link ContainerData}：服务端每 tick 由
 * {@code AbstractContainerMenu#broadcastChanges()} 比对一次，变了才发包。
 * 所以 <b>服务端那份 {@code ContainerData} 必须直接读方块实体</b>，
 * 不能是快照 —— 否则永远比对不出变化。
 */
public abstract class MachineMenu extends AbstractContainerMenu {

	/** 玩家背包：3 行主背包 + 1 行快捷栏。 */
	protected static final int PLAYER_SLOT_COUNT = 36;

	/** 玩家背包的槽位坐标，与原版容器界面一致。 */
	private static final int PLAYER_INV_X = 8;
	private static final int PLAYER_INV_Y = 84;
	private static final int HOTBAR_Y = 142;

	private final Container machine;

	/** 服务端直接读方块实体，客户端读同步来的快照。 */
	protected final ContainerData data;

	protected MachineMenu(@Nullable MenuType<?> type, int containerId, Inventory playerInventory,
			Container machine, ContainerData data) {
		super(type, containerId);
		this.machine = machine;
		this.data = data;
		addMachineSlots(machine);
		addDataSlots(data);
		addPlayerInventory(playerInventory);
	}

	/**
	 * 从 {@link ContainerData} 里读回一个 64 位值。
	 *
	 * <p>长整型在网络上是按 16 位一组、共四格传过来的 —— 为什么要这样拆，
	 * 以及为什么不能按「低 32 位 + 高 32 位」拆，见 {@link ContainerDataCodec}。
	 *
	 * @param firstIndex 该值第一格的下标，见 {@code PowerAcceptorBlockEntity} 的 {@code DATA_*} 常量
	 */
	protected long readLong(int firstIndex) {
		return ContainerDataCodec.read(data, firstIndex);
	}

	/** 子类在这里 {@code addSlot}。没有物品槽的机器留空即可。 */
	protected abstract void addMachineSlots(Container machine);

	/**
	 * 机器自己占用的槽位数量；玩家背包从它之后开始。
	 *
	 * <p>客户端屏幕要用它来区分「哪些是机器槽位」（前 {@code machineSlotCount} 个），
	 * 因为背景贴图被自绘面板盖过之后，需要按槽位坐标把底框补画回来。
	 */
	public int machineSlotCount() {
		return machine.getContainerSize();
	}

	protected void addPlayerInventory(Inventory inventory) {
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				addSlot(new Slot(inventory, column + row * 9 + 9,
						PLAYER_INV_X + column * 18, PLAYER_INV_Y + row * 18));
			}
		}
		for (int column = 0; column < 9; column++) {
			addSlot(new Slot(inventory, column, PLAYER_INV_X + column * 18, HOTBAR_Y));
		}
	}

	/**
	 * Shift 点击：机器槽位与背包之间来回搬。没有机器槽位时（电池）什么都不做，
	 * 让 Shift 点击在背包内部正常排序。
	 */
	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (slot == null || !slot.hasItem()) {
			return ItemStack.EMPTY;
		}

		ItemStack stackInSlot = slot.getItem();
		ItemStack original = stackInSlot.copy();
		int machineSlots = machineSlotCount();
		int playerStart = machineSlots;
		int playerEnd = machineSlots + PLAYER_SLOT_COUNT;

		if (machineSlots == 0) {
			return ItemStack.EMPTY;
		}

		if (index < machineSlots) {
			if (!moveItemStackTo(stackInSlot, playerStart, playerEnd, true)) {
				return ItemStack.EMPTY;
			}
		} else if (!moveItemStackTo(stackInSlot, 0, machineSlots, false)) {
			return ItemStack.EMPTY;
		}

		if (stackInSlot.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		if (stackInSlot.getCount() == original.getCount()) {
			return ItemStack.EMPTY;
		}
		slot.onTake(player, stackInSlot);
		return original;
	}

	@Override
	public boolean stillValid(Player player) {
		return machine.stillValid(player);
	}
}
