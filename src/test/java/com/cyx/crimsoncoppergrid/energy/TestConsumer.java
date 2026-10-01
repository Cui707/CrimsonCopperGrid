package com.cyx.crimsoncoppergrid.energy;

/**
 * 测试替身：用电设备。
 *
 * <p>与生产代码一致，同时实现 {@link EnergyStorage}（真实用电设备同样是
 * AbstractEnergyBlockEntity 的子类），否则电网不会把它识别为节点。
 */
final class TestConsumer extends TestBattery implements EnergyConsumer {
	private final long demand;
	private long consumed;

	TestConsumer(long demand) {
		// 用电设备没有缓冲：容量与吞吐都按需求给足
		super(Math.max(1, demand), Math.max(1, demand));
		this.demand = demand;
	}

	@Override
	public long wantedEnergy() {
		return demand;
	}

	@Override
	public long consumeEnergy(long available) {
		consumed = Math.max(0, Math.min(demand, available));
		return consumed;
	}

	@Override
	public long getConsumedEnergy() {
		return consumed;
	}

	long consumed() {
		return consumed;
	}
}
