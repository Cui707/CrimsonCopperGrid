package com.cyx.crimsoncoppergrid.energy;

/**
 * 测试替身：发电机。
 *
 * <p>两处细节与生产代码保持一致，否则测出来的结论是错的：
 * <ol>
 *   <li>它**同时是 {@link EnergyStorage}** —— 电网正是靠这个接口发现节点的
 *       （真实发电机继承 AbstractEnergyBlockEntity）；</li>
 *   <li>它**只出不进**（接收上限为 0）—— 否则结算时的「富余充电」会把电
 *       倒进发电机自己的缓冲里，看起来像电池没充上。</li>
 * </ol>
 */
final class TestProducer extends TestBattery implements EnergyProducer {
	private final long maxOutput;
	private long produced;

	TestProducer(long maxOutput) {
		super(maxOutput * 10, maxOutput);
		this.maxOutput = maxOutput;
	}

	@Override
	protected long receiveLimit() {
		return 0L; // 发电机不接受能量
	}

	@Override
	public long produceEnergy(long maxDemand) {
		produced = Math.max(0, Math.min(maxOutput, maxDemand));
		return produced;
	}

	@Override
	public long getMaxOutput() {
		return maxOutput;
	}

	@Override
	public long getProducedEnergy() {
		return produced;
	}

	long produced() {
		return produced;
	}
}
