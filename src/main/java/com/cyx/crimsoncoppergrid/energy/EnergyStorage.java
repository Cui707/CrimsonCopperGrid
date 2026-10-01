package com.cyx.crimsoncoppergrid.energy;

/**
 * 能量存储接口，语义与 FE（Forge Energy）/ Team Reborn Energy 一致：
 *
 * <ul>
 *   <li>单位是抽象的「能量」，不承诺与现实物理单位换算；</li>
 *   <li>所有数值都是整数，避免同步与存档产生浮点误差；</li>
 *   <li>{@code maxReceive} / {@code maxExtract} 为 0 表示该方向不可用，
 *       这是「发电机只出不进」「用电设备只进不出」的表达方式。</li>
 * </ul>
 *
 * <p>约定：实现者内部自己管理网络归属与结算，不需要在这里做 tick 逻辑。
 */
public interface EnergyStorage {
	/** 当前已存能量。 */
	long getEnergyStored();

	/** 容量上限。 */
	long getEnergyCapacity();

	/** 单次（每个结算周期）最多能接收多少。 */
	long getMaxReceive();

	/** 单次（每个结算周期）最多能抽出多少。 */
	long getMaxExtract();

	default boolean canReceive() {
		return getMaxReceive() > 0;
	}

	default boolean canExtract() {
		return getMaxExtract() > 0;
	}

	/**
	 * 尝试接收能量。
	 *
	 * @return 实际接收的量，必须落在 [0, maxReceive] 且不超过剩余容量
	 */
	long receiveEnergy(long maxReceive, boolean simulate);

	/**
	 * 尝试抽出能量。
	 *
	 * @return 实际抽出的量，必须落在 [0, maxExtract] 且不超过当前存量
	 */
	long extractEnergy(long maxExtract, boolean simulate);
}
