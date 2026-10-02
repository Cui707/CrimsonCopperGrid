package com.cyx.crimsoncoppergrid.blockentity;

import org.jspecify.annotations.Nullable;

import com.cyx.crimsoncoppergrid.common.menu.CoalSynthesizerMenu;
import com.cyx.crimsoncoppergrid.common.powerSystem.PowerAcceptorBlockEntity;
import com.cyx.crimsoncoppergrid.init.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 电力煤炭合成机：把电变成煤。
 *
 * <p>{@link #FE_PER_COAL} FE = 1 块煤。作为对比，燃料发电机烧一块煤产出 64 000 FE ——
 * 也就是说这条链路本身是亏的（8:1）。它的价值在于把「多余的可再生电力」变成
 * 「可携带、可储存的燃料」，而不是当成永动机。
 *
 * <h2>为什么进度直接用缓冲存量表示</h2>
 * 这台机器的缓冲容量正好设成一煤所需的电量（{@link #FE_PER_COAL}），
 * 于是「存量攒够了」与「进度满了」是同一件事，不需要再单独维护一个进度字段。
 * 少一套账，就少一类对不上的 bug。
 */
public class CoalSynthesizerBlockEntity extends PowerAcceptorBlockEntity implements MenuProvider {

	/** 产物槽。煤在机器里以「一块一格」的整数形式攒着，这个槽位就是它的对外接口。 */
	public static final int SLOT_OUTPUT = 0;
	public static final int SLOT_COUNT = 1;

	// ---- 界面数据：在父类的「电量 + 容量」四格之后追加已存煤块数 ----
	public static final int DATA_COAL = PowerAcceptorBlockEntity.DATA_COUNT;
	public static final int DATA_COUNT = PowerAcceptorBlockEntity.DATA_COUNT + 1;

	@Override
	public int getCount() {
		return DATA_COUNT;
	}

	@Override
	public int get(int index) {
		return switch (index) {
			case DATA_COAL -> coal;
			default -> super.get(index);
		};
	}

	/** 合成一块煤所需的电量。 */
	public static final long FE_PER_COAL = 8_000L;
	/** 每 tick 最多进多少电。 */
	public static final long MAX_INPUT = 200L;
	/** 最多攒多少块煤等玩家来取。 */
	public static final int MAX_COAL = 64;

	/** 尝试把煤送进相邻容器的间隔（刻）。 */
	private static final int EJECT_INTERVAL = 20;

	private int coal;
	private int ejectTimer;

	public CoalSynthesizerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.COAL_SYNTHESIZER, pos, state);
	}

	// ------------------------------------------------------------ 能量基准

	/** 缓冲容量 = 一块煤的电量，因此存量即进度。 */
	@Override
	public long getBaseMaxPower() {
		return FE_PER_COAL;
	}

	/** 只用电，不发电。 */
	@Override
	public long getBaseMaxOutput() {
		return 0L;
	}

	@Override
	public long getBaseMaxInput() {
		return MAX_INPUT;
	}

	/** 煤攒满了就不再要电，避免白白浪费。 */
	@Override
	protected boolean canAcceptEnergy(@Nullable Direction face) {
		return coal < MAX_COAL;
	}

	// ------------------------------------------------------------ tick

	@Override
	protected void serverTick() {
		boolean working = false;

		// 攒够一块煤的电量就产出一块。用 while 是为了在电很足时不被单 tick 限制卡住
		while (getStored() >= FE_PER_COAL && coal < MAX_COAL) {
			useEnergy(FE_PER_COAL);
			coal++;
			working = true;
		}
		if (working) {
			setChanged();
			syncWithAll();
		}
		setActive(coal < MAX_COAL && getStored() > 0);

		if (++ejectTimer >= EJECT_INTERVAL) {
			ejectTimer = 0;
			ejectCoal();
		}
	}

	/** 定期把攒下的煤塞进相邻容器（箱子、漏斗等）；塞不进去就继续攒着。 */
	private void ejectCoal() {
		if (coal <= 0) {
			return;
		}
		Level level = getLevel();
		if (level == null) {
			return;
		}
		if (ejectToNeighbor(level, getBlockPos(), new ItemStack(Items.COAL, coal))) {
			coal = 0;
			setChanged();
			syncWithAll();
		}
	}

	// ------------------------------------------------------------ 存取

	public int getCoal() {
		return coal;
	}

	/** 玩家取走全部煤炭。 */
	public ItemStack extractCoal() {
		ItemStack stack = takeAll();
		if (!stack.isEmpty()) {
			setChanged();
			syncWithAll();
		}
		return stack;
	}

	// ------------------------------------------------------------ 容器（单个产物槽）

	/*
	 * 煤在机器内部只是一个整数，但菜单需要一个真正的 Container 才能摆出槽位。
	 * 这里的做法是「把那个整数伪装成一个槽」：读的时候现造一个等量的煤炭堆，
	 * 写的时候只取数量。原版 {@code Slot} 的每次操作最后都会走 {@code setItem} /
	 * {@code removeItem}，所以不会出现改了副本却丢同步的情况。
	 *
	 * <p>槽位对外恒不可放入（{@code canPlaceItem} 返回 false），玩家只能取。
	 */

	@Override
	public int getContainerSize() {
		return SLOT_COUNT;
	}

	@Override
	public boolean isEmpty() {
		return coal <= 0;
	}

	@Override
	public ItemStack getItem(int slot) {
		return coal <= 0 ? ItemStack.EMPTY : new ItemStack(Items.COAL, coal);
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		int taken = Math.min(amount, coal);
		if (taken <= 0) {
			return ItemStack.EMPTY;
		}
		coal -= taken;
		setChanged();
		syncWithAll();
		return new ItemStack(Items.COAL, taken);
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		ItemStack stack = takeAll();
		setChanged();
		return stack;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		coal = stack.isEmpty() ? 0 : Math.min(stack.getCount(), MAX_COAL);
		setChanged();
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return false;
	}

	@Override
	public void clearContent() {
		coal = 0;
	}

	private ItemStack takeAll() {
		if (coal <= 0) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = new ItemStack(Items.COAL, coal);
		coal = 0;
		return stack;
	}

	// ------------------------------------------------------------ 界面

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.crimsoncoppergrid.coal_synthesizer");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return new CoalSynthesizerMenu(containerId, playerInventory, this);
	}

	// ------------------------------------------------------------ 同步与存档

	@Override
	protected void onEnergyChanged() {
		setChanged();
		syncWithAll();
	}

	private static final String KEY_COAL = "Coal";

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.coal = input.getIntOr(KEY_COAL, 0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt(KEY_COAL, coal);
	}

	// ------------------------------------------------------------ 静态工具

	/**
	 * 把一叠物品送进相邻容器之一。
	 *
	 * <p>放在这里而不是方块类里，是因为它只跟方块实体有关，与方块状态无关；
	 * 方块层不该承担物品搬运这种逻辑。
	 *
	 * @return 是否成功送出
	 */
	public static boolean ejectToNeighbor(Level level, BlockPos pos, ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		for (Direction side : Direction.values()) {
			BlockPos target = pos.relative(side);
			if (!level.isLoaded(target)) {
				continue;
			}
			if (!(level.getBlockEntity(target) instanceof net.minecraft.world.Container container)) {
				continue;
			}
			for (int slot = 0; slot < container.getContainerSize(); slot++) {
				if (!container.canPlaceItem(slot, stack)) {
					continue;
				}
				ItemStack existing = container.getItem(slot);
				if (existing.isEmpty()) {
					container.setItem(slot, stack.copy());
					container.setChanged();
					return true;
				}
				if (ItemStack.isSameItemSameComponents(existing, stack)
						&& existing.getCount() + stack.getCount() <= existing.getMaxStackSize()) {
					existing.grow(stack.getCount());
					container.setChanged();
					return true;
				}
			}
		}
		return false;
	}
}
