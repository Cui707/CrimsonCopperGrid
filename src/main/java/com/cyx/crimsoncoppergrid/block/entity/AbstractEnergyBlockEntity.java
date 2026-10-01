package com.cyx.crimsoncoppergrid.block.entity;

import com.cyx.crimsoncoppergrid.energy.EnergyStorage;
import com.cyx.crimsoncoppergrid.energy.GridRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 所有「接电」方块实体的公共父类。
 *
 * <p>负责三件事：
 * <ol>
 *   <li>声明自己属于哪张电网（{@link EnergyStorage} 的实现由子类给出）；</li>
 *   <li>放置/移除/区块载入时把电网标脏，触发重新组网；</li>
 *   <li>统一的存档 key（{@code Energy}）。</li>
 * </ol>
 */
public abstract class AbstractEnergyBlockEntity extends BlockEntity implements EnergyStorage {
	protected static final String KEY_ENERGY = "Energy";

	/** 本周期实际产生的能量，供展示与统计。 */
	private long lastProduced;
	/** 本周期实际消耗的能量，供展示与统计。 */
	private long lastConsumed;

	protected AbstractEnergyBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	protected GridRegistry gridRegistry() {
		Level level = this.getLevel();
		return level instanceof ServerLevel serverLevel ? GridRegistry.get(serverLevel) : null;
	}

	/** 通知电网：本坐标的拓扑或归属可能变了。 */
	public void markGridDirty() {
		GridRegistry registry = gridRegistry();
		if (registry != null) {
			registry.markDirty(this.getBlockPos());
		}
	}

	@Override
	public void setRemoved() {
		markGridDirty();
		super.setRemoved();
	}

	@Override
	public void clearRemoved() {
		super.clearRemoved();
		markGridDirty();
	}

	// ------------------------------------------------------------ 统计字段

	public long getLastProduced() {
		return lastProduced;
	}

	protected void setLastProduced(long value) {
		this.lastProduced = Math.max(0, value);
	}

	public long getLastConsumed() {
		return lastConsumed;
	}

	protected void setLastConsumed(long value) {
		this.lastConsumed = Math.max(0, value);
	}

	// ------------------------------------------------------------ 存档

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		readEnergy(input);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		writeEnergy(output);
	}

	protected void readEnergy(ValueInput input) {
		setEnergyStored(input.getLongOr(KEY_ENERGY, 0L));
	}

	protected void writeEnergy(ValueOutput output) {
		output.putLong(KEY_ENERGY, getEnergyStored());
	}

	/** 子类按自己的容量钳制后写回。 */
	protected abstract void setEnergyStored(long value);

	/** 只读展示用：简短的能量描述。 */
	public String energySummary() {
		return getEnergyStored() + " / " + getEnergyCapacity() + " FE";
	}
}
