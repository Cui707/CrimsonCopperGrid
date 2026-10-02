package com.cyx.crimsoncoppergrid.common.powerSystem;

import org.jspecify.annotations.Nullable;

import com.cyx.crimsoncoppergrid.common.blockentity.MachineBaseBlockEntity;
import com.cyx.crimsoncoppergrid.common.menu.ContainerDataCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;
import team.reborn.energy.api.base.SimpleSidedEnergyContainer;

/**
 * 所有「接电机器」的公共父类：发电机、电池、用电设备都继承它。
 * 分层与命名对齐 TechReborn / RebornCore 的 {@code PowerAcceptorBlockEntity}（MIT）。
 *
 * <h2>能量语义来自 Team Reborn Energy</h2>
 * 内部缓冲是官方的 {@link SimpleSidedEnergyContainer}：可动态容量、可按方向限制收发速率，
 * 并且参与 Fabric 的 {@code Transaction} 快照机制 —— 一次转账要么整体成立，要么整体回滚，
 * 不会出现「电从源头扣掉了但没进到目标」的半截状态。这是引入这套 API 的核心收益。
 *
 * <h2>推流模型（与自研的中心化电网相反）</h2>
 * API 的官方约定是**推流**：电源负责把电推向邻居。因此本类在 tick 里做一件事 ——
 * 对六个方向各尝试一次 {@link EnergyStorageUtil#move}。
 * <ul>
 *   <li>纯用电设备把 {@code getBaseMaxOutput()} 定为 0，从而 {@code getMaxOutput()} 恒为 0，
 *       它这一侧的 {@code maxExtract} 也就是 0，推流自然变成空操作，不需要额外判断；</li>
 *   <li>发电机与电池则因为 {@code canProvideEnergy} 为真而真的往外推。</li>
 * </ul>
 *
 * <h2>与 TechReborn 原版的差异</h2>
 * 原版还有 {@code extraPowerStorage} / {@code extraPowerInput} / {@code extraTier} 三个
 * 「升级加成」字段与 {@code IListInfoProvider} 工具提示。CCG 没有升级体系，因此不引入；
 * 工具提示会在界面阶段随菜单一起补齐。
 */
public abstract class PowerAcceptorBlockEntity extends MachineBaseBlockEntity implements ContainerData {

	// ------------------------------------------------------------ 界面数据同步
	// 服务端这一份直接读真实字段，客户端那一份由 SimpleContainerData 承接同步值。
	//
	// 电量是 long，而 ContainerData 每个槽位在网络上只有 16 位有效，
	// 所以一个值占 SLOTS_PER_LONG（= 4）格。编解码见 ContainerDataCodec。

	/** 存量：占 {@code DATA_STORED} ~ {@code DATA_STORED + 3} 四格。 */
	public static final int DATA_STORED = 0;
	/** 容量：紧跟存量之后，同样四格。 */
	public static final int DATA_CAPACITY = DATA_STORED + ContainerDataCodec.SLOTS_PER_LONG;
	/** 最长的那一份（含子类追加的字段）。子类覆写时要一并覆写 {@link #getCount()}。 */
	public static final int DATA_COUNT = DATA_CAPACITY + ContainerDataCodec.SLOTS_PER_LONG;

	@Override
	public int getCount() {
		return DATA_COUNT;
	}

	@Override
	public int get(int index) {
		if (ContainerDataCodec.covers(index, DATA_STORED)) {
			return ContainerDataCodec.write(getStored(), ContainerDataCodec.chunkOf(index, DATA_STORED));
		}
		if (ContainerDataCodec.covers(index, DATA_CAPACITY)) {
			return ContainerDataCodec.write(getMaxStoredPower(), ContainerDataCodec.chunkOf(index, DATA_CAPACITY));
		}
		return 0;
	}

	/**
	 * 只有客户端会走到这里（服务端那份读的是真实字段，同步是单向的）。
	 * 客户端用 {@code SimpleContainerData} 时它的 {@code set} 自己会存，
	 * 所以这里保持空实现，子类也不需要关心。
	 */
	@Override
	public void set(int index, int value) {
	}


	private final SimpleSidedEnergyContainer energyContainer = new SimpleSidedEnergyContainer() {
		@Override
		public long getCapacity() {
			return PowerAcceptorBlockEntity.this.getMaxStoredPower();
		}

		@Override
		public long getMaxInsert(@Nullable Direction side) {
			return PowerAcceptorBlockEntity.this.getMaxInput(side);
		}

		@Override
		public long getMaxExtract(@Nullable Direction side) {
			return PowerAcceptorBlockEntity.this.canProvideEnergy(side)
					? PowerAcceptorBlockEntity.this.getMaxOutput(side)
					: 0;
		}

		@Override
		protected void onFinalCommit() {
			PowerAcceptorBlockEntity.this.onEnergyChanged();
		}
	};

	/** 延迟计算，避免在构造器里调用子类的抽象方法（那时子类字段还没初始化）。 */
	@Nullable
	private CcgEnergyTier blockEntityPowerTier;

	protected PowerAcceptorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	// ------------------------------------------------------------ 子类必须给出的三个基准值

	/** 内部能量缓冲的容量。 */
	public abstract long getBaseMaxPower();

	/** 基础输出速率；不能向外供电的设备返回 0。 */
	public abstract long getBaseMaxOutput();

	/** 基础输入速率；不接受外部供电的设备返回 0。 */
	public abstract long getBaseMaxInput();

	// ------------------------------------------------------------ 对外接口

	/** 供 Fabric 的 {@code EnergyStorage.SIDED} 查询使用；参数为 null 表示「不区分方向的整块存储」。 */
	public EnergyStorage getSideEnergyStorage(@Nullable Direction side) {
		return energyContainer.getSideStorage(side);
	}

	/** 按基准值判定档位：纯发电设备看输出，其余看输入。 */
	public void checkTier() {
		long reference = getBaseMaxInput() == 0 ? getBaseMaxOutput() : getBaseMaxInput();
		blockEntityPowerTier = CcgEnergyTier.getTier(reference);
	}

	public CcgEnergyTier getTier() {
		if (blockEntityPowerTier == null) {
			checkTier();
		}
		return blockEntityPowerTier;
	}

	/** 还能再装多少。 */
	public long getFreeSpace() {
		return getMaxStoredPower() - getStored();
	}

	public void addEnergy(long amount) {
		setStored(getStored() + amount);
	}

	/** 扣电；不足时扣到 0 而不是变成负数。 */
	public void useEnergy(long amount) {
		if (getStored() > amount) {
			setStored(getStored() - amount);
		} else {
			setStored(0);
		}
	}

	/** 精确扣电：存量够就扣掉并返回 true，不够则一动不动。 */
	public boolean tryUseExact(long energy) {
		if (getStored() >= energy) {
			addEnergy(-energy);
			return true;
		}
		return false;
	}

	// ------------------------------------------------------------ 速率与存量

	public long getStored() {
		return energyContainer.amount;
	}

	public void setStored(long amount) {
		energyContainer.amount = Mth.clamp(amount, 0, getMaxStoredPower());
		setChanged();
	}

	public long getMaxStoredPower() {
		return getBaseMaxPower();
	}

	public long getMaxOutput(@Nullable Direction face) {
		return canProvideEnergy(face) ? getBaseMaxOutput() : 0;
	}

	public long getMaxInput(@Nullable Direction face) {
		return canAcceptEnergy(face) ? getBaseMaxInput() : 0;
	}

	/** 该面能否进电。默认都可以，子类可覆写（例如只让某一面接电）。 */
	protected boolean canAcceptEnergy(@Nullable Direction side) {
		return true;
	}

	/** 该面能否出电。默认都可以；纯用电设备覆写为 false。 */
	protected boolean canProvideEnergy(@Nullable Direction side) {
		return true;
	}

	// ------------------------------------------------------------ tick：把电推向邻居

	@Override
	public void tick(Level level, BlockPos pos, BlockState state, MachineBaseBlockEntity blockEntity) {
		super.tick(level, pos, state, blockEntity);
		if (!(level instanceof ServerLevel)) {
			return;
		}

		// 本设备不能出电（纯用电设备），或者自己没电 —— 两种情况下推流都是空操作
		if (getMaxOutput(null) <= 0 || getStored() <= 0) {
			powerChange = 0;
			powerLastTick = getStored();
			return;
		}

		for (Direction side : Direction.values()) {
			EnergyStorage target = EnergyStorage.SIDED.find(level, pos.relative(side), side.getOpposite());
			if (target == null) {
				continue;
			}
			EnergyStorageUtil.move(getSideEnergyStorage(side), target, Long.MAX_VALUE, null);
		}

		powerChange = getStored() - powerLastTick;
		powerLastTick = getStored();
	}

	/**
	 * 存量发生变化时的钩子（由 {@code SimpleSidedEnergyContainer} 在事务提交时回调）。
	 * 默认只标脏，需要刷新客户端的子类可以再补一次 {@code syncWithAll()}。
	 */
	protected void onEnergyChanged() {
		setChanged();
	}

	// ------------------------------------------------------------ 静态工具

	/** 比较器输出：按存量占容量的比例给 0~15。对齐 TechReborn 的同名方法。 */
	public static int calculateComparatorOutputFromEnergy(@Nullable BlockEntity blockEntity) {
		if (blockEntity instanceof PowerAcceptorBlockEntity storage && storage.getMaxStoredPower() > 0) {
			return Mth.ceil(storage.getStored() * 15.0 / storage.getMaxStoredPower());
		}
		return 0;
	}

	// ------------------------------------------------------------ 存档

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		// 独立子节点，将来加字段不会和别的存档数据撞名
		input.child("PowerAcceptor").ifPresent(data -> energyContainer.amount = data.getLongOr("energy", 0));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.child("PowerAcceptor").putLong("energy", getStored());
	}
}
