package com.cyx.crimsoncoppergrid.blockentity;

import com.cyx.crimsoncoppergrid.common.powerSystem.PowerAcceptorBlockEntity;
import com.cyx.crimsoncoppergrid.init.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 燃料发电机：烧原版燃料换电。继承 {@link PowerAcceptorBlockEntity}，
 * 因此它自己就会把缓冲里的电推向四周的邻居。
 *
 * <p>数值与熔炉对齐：一个「燃料刻」产出 {@link #FE_PER_FUEL_TICK} FE，
 * 一块煤在熔炉里烧 1600 刻，在这里也就是 1600 × 40 = 64 000 FE。
 *
 * <p>为什么自己维护 {@code burnTime} 而不是复用原版 {@code AbstractFurnaceBlockEntity}：
 * 它的 {@code litTime} / {@code cookingProgress} 都是私有字段，只能靠反射硬塞，
 * 版本一升就碎。
 *
 * <h2>为什么输入恒为 0</h2>
 * 发电机的缓冲只出不进 —— {@link #getBaseMaxInput()} 返回 0，
 * 于是 {@code getMaxInput(face)} 恒为 0，外部往它插电会被直接拒绝。
 * 这不需要在 {@code canAcceptEnergy} 里额外判断。
 */
public class FuelGeneratorBlockEntity extends PowerAcceptorBlockEntity {

	public static final int SLOT_FUEL = 0;

	/** 一个燃料刻产生的能量。 */
	public static final long FE_PER_FUEL_TICK = 40L;
	/** 内部缓冲容量：够装 100 刻的产出，也就是 5 秒。 */
	public static final long CAPACITY = 4_000L;
	/** 每 tick 最多往外推多少。 */
	public static final long MAX_OUTPUT = 40L;

	private static final String KEY_BURN_TIME = "BurnTime";
	private static final String KEY_BURN_TIME_TOTAL = "BurnTimeTotal";
	private static final String KEY_FUEL = "Fuel";

	private int burnTime;
	private int burnTimeTotal;
	private ItemStack fuel = ItemStack.EMPTY;

	public FuelGeneratorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.FUEL_GENERATOR, pos, state);
	}

	// ------------------------------------------------------------ 能量基准

	@Override
	public long getBaseMaxPower() {
		return CAPACITY;
	}

	@Override
	public long getBaseMaxOutput() {
		return MAX_OUTPUT;
	}

	/** 发电机不接收外部供电。 */
	@Override
	public long getBaseMaxInput() {
		return 0L;
	}

	// ------------------------------------------------------------ tick

	@Override
	protected void serverTick() {
		boolean running = false;

		if (burnTime <= 0) {
			int value = fuelValue(fuel);
			if (value > 0) {
				burnTime = value;
				burnTimeTotal = value;
				consumeFuel();
			}
		}

		if (burnTime > 0) {
			burnTime--;
			running = true;
			// 缓冲满了就让燃料继续烧、电白白散掉 —— 与熔炉烧着但没东西可烧的表现一致
			long space = getFreeSpace();
			if (space > 0) {
				addEnergy(Math.min(FE_PER_FUEL_TICK, space));
			}
		} else if (burnTimeTotal != 0) {
			burnTimeTotal = 0;
		}

		// 驱动贴图上的「正在工作」状态
		setActive(running);
	}

	// ------------------------------------------------------------ 燃料

	public int getBurnTime() {
		return burnTime;
	}

	public int getBurnTimeTotal() {
		return burnTimeTotal;
	}

	public boolean isBurning() {
		return burnTime > 0;
	}

	/**
	 * 原版燃料的燃烧时长（刻）。只列了常见燃料，与原版熔炉的取值一致。
	 *
	 * <p>不直接查原版燃料表，是因为它的取值方式在各版本间变动过；
	 * 这里显式列出，行为可预期，也方便按模组的平衡需要单独调整。
	 */
	private static int fuelValue(ItemStack stack) {
		if (stack.isEmpty()) {
			return 0;
		}
		if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) {
			return 1600;
		}
		if (stack.is(Items.COAL_BLOCK)) {
			return 16000;
		}
		if (stack.is(Items.BLAZE_ROD)) {
			return 2400;
		}
		if (stack.is(Items.DRIED_KELP_BLOCK)) {
			return 4001;
		}
		if (stack.is(Items.LAVA_BUCKET)) {
			return 20000;
		}
		if (stack.is(Items.BAMBOO)) {
			return 50;
		}
		return 0;
	}

	private void consumeFuel() {
		// 26.3 的燃料余料是 ItemStackTemplate（例如岩浆桶 -> 空桶），用 create() 变回 ItemStack
		ItemStack remainder = fuel.getItem().getCraftingRemainder().create();
		fuel.shrink(1);
		if (fuel.isEmpty() && !remainder.isEmpty()) {
			fuel = remainder;
		}
		setChanged();
	}

	// ------------------------------------------------------------ 容器（单个燃料槽）

	@Override
	public int getContainerSize() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return fuel.isEmpty();
	}

	@Override
	public ItemStack getItem(int slot) {
		return fuel;
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		ItemStack split = fuel.split(amount);
		if (!split.isEmpty()) {
			setChanged();
		}
		return split;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		ItemStack stack = fuel;
		fuel = ItemStack.EMPTY;
		return stack;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		fuel = stack;
		setChanged();
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return fuelValue(stack) > 0;
	}

	@Override
	public void clearContent() {
		fuel = ItemStack.EMPTY;
	}

	// ------------------------------------------------------------ 同步与存档

	/** 电量一变就让客户端知道（发电机的贴图与提示都依赖它）。 */
	@Override
	protected void onEnergyChanged() {
		setChanged();
		syncWithAll();
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.burnTime = input.getIntOr(KEY_BURN_TIME, 0);
		this.burnTimeTotal = input.getIntOr(KEY_BURN_TIME_TOTAL, 0);
		this.fuel = input.read(KEY_FUEL, ItemStack.CODEC).orElse(ItemStack.EMPTY);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt(KEY_BURN_TIME, burnTime);
		output.putInt(KEY_BURN_TIME_TOTAL, burnTimeTotal);
		output.store(KEY_FUEL, ItemStack.CODEC, fuel);
	}
}
