package com.cyx.crimsoncoppergrid.blockentity;

import com.cyx.crimsoncoppergrid.common.menu.BatteryMenu;
import com.cyx.crimsoncoppergrid.common.menu.ContainerDataCodec;
import com.cyx.crimsoncoppergrid.common.powerSystem.PowerAcceptorBlockEntity;
import com.cyx.crimsoncoppergrid.init.ModBlockEntities;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 储能电池：既能吃电、也能吐电，是全模组唯一的双向设备。
 *
 * <p>容量 {@link #CAPACITY}、单 tick 收发上限各 {@link #MAX_IO}，即每秒最多吞吐 2000 FE。
 * 这个上限足够削掉一次用电高峰，但不足以掩盖发电能力的长期缺口 ——
 * 电池是缓冲，不是电源。
 *
 * <h2>电网里没有它的位置吗</h2>
 * 有的，而且是关键位置。Team Reborn Energy 的推流模型下，发电机会把电推给邻居；
 * 如果邻居是电池，电就存下来了；等到发电机供不上时，电池作为「有存货的设备」
 * 同样会往邻居推电。整个过程中没有任何中心调度器，也不会出现
 * 「电从 A 扣了但 B 没收到」的半截状态 —— 每一步都由 Fabric 的
 * {@code Transaction} 兜底。
 */
public class BatteryBlockEntity extends PowerAcceptorBlockEntity implements MenuProvider {

	/** 容量。1 000 000 FE ≈ 燃料发电机烧 15.6 块煤的产出。 */
	public static final long CAPACITY = 1_000_000L;
	/** 单 tick 的输入 / 输出上限。 */
	public static final long MAX_IO = 1_000L;

	// ---- 界面数据：在父类的「存量 + 容量」之后追加净流量 ----
	public static final int DATA_POWER_CHANGE = PowerAcceptorBlockEntity.DATA_COUNT;
	public static final int DATA_COUNT = DATA_POWER_CHANGE + ContainerDataCodec.SLOTS_PER_LONG;

	@Override
	public int getCount() {
		return DATA_COUNT;
	}

	@Override
	public int get(int index) {
		if (ContainerDataCodec.covers(index, DATA_POWER_CHANGE)) {
			return ContainerDataCodec.write(powerChange, ContainerDataCodec.chunkOf(index, DATA_POWER_CHANGE));
		}
		return super.get(index);
	}

	public BatteryBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.BATTERY, pos, state);
	}

	// ------------------------------------------------------------ 能量基准

	@Override
	public long getBaseMaxPower() {
		return CAPACITY;
	}

	/** 双向：既能收也能发。 */
	@Override
	public long getBaseMaxOutput() {
		return MAX_IO;
	}

	@Override
	public long getBaseMaxInput() {
		return MAX_IO;
	}

	// ------------------------------------------------------------ tick

	/**
	 * 电池不需要业务逻辑 —— 收放电全在 {@link PowerAcceptorBlockEntity#tick} 的推流里。
	 * 这里只把「上一刻的净流量」映射到方块状态，让贴图能显示正在充还是正在放。
	 */
	@Override
	protected void serverTick() {
		setActive(powerChange != 0);
	}

	/** 电量一变就同步客户端，否则界面上的电量条不会动。 */
	@Override
	protected void onEnergyChanged() {
		setChanged();
		syncWithAll();
	}

	// ------------------------------------------------------------ 界面

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.crimsoncoppergrid.battery");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return new BatteryMenu(containerId, playerInventory, this);
	}

	// ------------------------------------------------------------ 存档
	// 存量本身由 PowerAcceptorBlockEntity 负责，这里只存「上一刻流量」用于界面显示。

	private static final String KEY_POWER_CHANGE = "PowerChange";

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.powerChange = input.getLongOr(KEY_POWER_CHANGE, 0L);
		this.powerLastTick = getStored();
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putLong(KEY_POWER_CHANGE, powerChange);
	}
}
