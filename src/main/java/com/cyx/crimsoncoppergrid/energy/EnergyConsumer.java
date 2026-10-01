package com.cyx.crimsoncoppergrid.energy;

/**
 * 用电设备需要实现的能力。每个结算周期由电网调用一次。
 */
public interface EnergyConsumer {
	/**
	 * 本周期希望消耗多少能量（电网据此汇总总需求）。
	 * 返回 0 表示这台设备当前不需要电。
	 */
	long wantedEnergy();

	/**
	 * 尝试按给定额度运行本周期。
	 *
	 * @param available 电网本周期能提供的能量上限
	 * @return 实际消耗的能量（不超过 available）
	 */
	long consumeEnergy(long available);

	/** 本周期实际消耗的能量，供电网记账。 */
	long getConsumedEnergy();
}
