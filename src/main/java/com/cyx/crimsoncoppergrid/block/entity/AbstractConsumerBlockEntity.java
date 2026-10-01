package com.cyx.crimsoncoppergrid.block.entity;

import com.cyx.crimsoncoppergrid.energy.EnergyConsumer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 所有用电设备的公共父类：统一处理「本周期报了多少需求、实际用了多少」的记账。
 *
 * <p>子类只需要在业务逻辑真正推进时调用 {@link #pushConsumed(long)}，
 * 并在 {@link #consumeEnergy(long)} 里做实际工作。
 */
public abstract class AbstractConsumerBlockEntity extends AbstractEnergyBlockEntity implements EnergyConsumer {
	/** 本周期向电网提出的需求。 */
	private long wanted;
	/** 本周期实际消耗。 */
	private long consumed;

	protected AbstractConsumerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public long wantedEnergy() {
		return wanted;
	}

	@Override
	public long getConsumedEnergy() {
		return consumed;
	}

	/** 供子类设置本周期需求。 */
	protected void requestEnergy(long amount) {
		this.wanted = Math.max(0, amount);
	}

	/**
	 * 记账：本周期实际用掉了一部分电。
	 *
	 * <p>{@link #consumeEnergy(long)} 返回后电网会读 {@link #getConsumedEnergy()}，
	 * 所以子类必须在返回前把实际消耗记录下来。
	 */
	protected void pushConsumed(long amount) {
		this.consumed = Math.max(0, amount);
	}

	/** 每个结算周期开始时清空记账。 */
	protected void resetAccounting() {
		this.wanted = 0;
		this.consumed = 0;
	}

	@Override
	public long getMaxReceive() {
		return getMaxInput();
	}

	@Override
	public long getMaxExtract() {
		return 0L;
	}

	@Override
	public long receiveEnergy(long maxReceive, boolean simulate) {
		return 0L;
	}

	@Override
	public long extractEnergy(long maxExtract, boolean simulate) {
		return 0L;
	}

	/** 单周期最多能吃进多少电。 */
	protected abstract long getMaxInput();
}
