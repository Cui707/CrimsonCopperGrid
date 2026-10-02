package com.cyx.crimsoncoppergrid.blockentity;

import org.jspecify.annotations.Nullable;

import com.cyx.crimsoncoppergrid.common.powerSystem.PowerAcceptorBlockEntity;
import com.cyx.crimsoncoppergrid.init.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
public class CoalSynthesizerBlockEntity extends PowerAcceptorBlockEntity {

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
		if (coal <= 0) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = new ItemStack(Items.COAL, coal);
		coal = 0;
		setChanged();
		syncWithAll();
		return stack;
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
