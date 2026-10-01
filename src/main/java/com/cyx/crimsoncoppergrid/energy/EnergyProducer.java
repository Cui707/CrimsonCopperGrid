package com.cyx.crimsoncoppergrid.energy;

/**
 * 发电机需要实现的能力。每个结算周期由电网调用一次。
 */
public interface EnergyProducer {
	/**
	 * 本周期尝试发电。
	 *
	 * @param maxDemand 电网本周期还需要多少能量（不足则少发，避免浪费燃料）
	 * @return 实际发出的能量
	 */
	long produceEnergy(long maxDemand);

	/** 只读展示用：满负荷时的发电速率。 */
	long getMaxOutput();

	/** 本周期实际发出的能量，供电网记账与展示。 */
	long getProducedEnergy();
}
