package com.cyx.crimsoncoppergrid.blockentity;

import org.jspecify.annotations.Nullable;

import com.cyx.crimsoncoppergrid.common.menu.LavaGeneratorMenu;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 电力岩浆机：耗电自造岩浆，内部有 {@link #TANK_CAPACITY_MB} mB 容量的小罐子。
 *
 * <p>数值：{@link #FE_PER_MB} FE 换 1 mB 岩浆，也就是 <b>1 桶岩浆 ≈ 750 000 FE</b>。
 * 一块煤在燃料发电机里值 64 000 FE，所以烧满一桶岩浆大约要 12 块煤 ——
 * 它是全模组最耗电的设备，用它换来的「可再生岩浆」才不至于破坏生存平衡。
 *
 * <p>罐子只做整数（mB）存储，没有实现完整的流体 API；桶的进出在方块层处理。
 */
public class LavaGeneratorBlockEntity extends PowerAcceptorBlockEntity implements MenuProvider {

	// ---- 界面数据：在父类的「电量 + 容量」四格之后追加罐内岩浆量 ----
	public static final int DATA_TANK = PowerAcceptorBlockEntity.DATA_COUNT;
	public static final int DATA_COUNT = PowerAcceptorBlockEntity.DATA_COUNT + 1;

	@Override
	public int getCount() {
		return DATA_COUNT;
	}

	@Override
	public int get(int index) {
		return switch (index) {
			case DATA_TANK -> lavaMb;
			default -> super.get(index);
		};
	}

	/** 每 1 mB 岩浆消耗的电量。 */
	public static final long FE_PER_MB = 30L;
	public static final int TANK_CAPACITY_MB = 4_000;
	/** 每 tick 最多进多少电。 */
	public static final long MAX_INPUT = 60L;
	/** 缓冲容量：2 秒的输入量，够攒下几次产出。 */
	public static final long CAPACITY = FE_PER_MB * 4L;
	public static final int BUCKET_MB = 1_000;

	private static final String KEY_LAVA = "Lava";

	private int lavaMb;

	public LavaGeneratorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.LAVA_GENERATOR, pos, state);
	}

	// ------------------------------------------------------------ 能量基准

	@Override
	public long getBaseMaxPower() {
		return CAPACITY;
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

	/** 罐子满了就不再要电 —— 这台机器是全模组最耗电的，能省就省。 */
	@Override
	protected boolean canAcceptEnergy(@Nullable Direction face) {
		return lavaMb < TANK_CAPACITY_MB;
	}

	// ------------------------------------------------------------ tick

	@Override
	protected void serverTick() {
		boolean working = false;

		// 攒够 1 mB 的电量就产出一格。用 while 是因为每 tick 可能攒下不止一份
		while (getStored() >= FE_PER_MB && lavaMb < TANK_CAPACITY_MB) {
			useEnergy(FE_PER_MB);
			lavaMb++;
			working = true;
		}
		if (working) {
			setChanged();
			syncWithAll();
		}
		setActive(lavaMb < TANK_CAPACITY_MB && getStored() > 0);
	}

	// ------------------------------------------------------------ 罐子操作

	public int getLavaMb() {
		return lavaMb;
	}

	public int getTankCapacityMb() {
		return TANK_CAPACITY_MB;
	}

	public boolean isFull() {
		return lavaMb >= TANK_CAPACITY_MB;
	}

	/**
	 * 用桶取出一桶岩浆。
	 *
	 * @return 装满岩浆的桶；罐里不足一桶时返回空
	 */
	public ItemStack fillBucket() {
		if (lavaMb < BUCKET_MB) {
			return ItemStack.EMPTY;
		}
		lavaMb -= BUCKET_MB;
		setChanged();
		syncWithAll();
		return new ItemStack(Items.LAVA_BUCKET);
	}

	/**
	 * 把一桶岩浆倒进罐里。
	 *
	 * @return 空桶；罐里放不下时返回空
	 */
	public ItemStack drainBucket() {
		if (TANK_CAPACITY_MB - lavaMb < BUCKET_MB) {
			return ItemStack.EMPTY;
		}
		lavaMb += BUCKET_MB;
		setChanged();
		syncWithAll();
		return new ItemStack(Items.BUCKET);
	}

	// ------------------------------------------------------------ 界面

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.crimsoncoppergrid.lava_generator");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return new LavaGeneratorMenu(containerId, playerInventory, this);
	}

	// ------------------------------------------------------------ 同步与存档

	@Override
	protected void onEnergyChanged() {
		setChanged();
		syncWithAll();
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.lavaMb = input.getIntOr(KEY_LAVA, 0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt(KEY_LAVA, lavaMb);
	}
}
