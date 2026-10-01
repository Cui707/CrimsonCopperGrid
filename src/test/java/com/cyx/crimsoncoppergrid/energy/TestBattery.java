package com.cyx.crimsoncoppergrid.energy;

/**
 * 测试替身：电池。语义与生产实现一致（单次收发上限 + 容量钳制 + 不允许负数）。
 *
 * <p>发电机与用电设备的替身也继承它，因为生产代码里那些设备本身就是 {@link EnergyStorage}。
 */
class TestBattery implements EnergyStorage {
	private final long capacity;
	private final long maxIo;
	private long stored;

	TestBattery(long capacity, long maxIo) {
		this(capacity, maxIo, 0L);
	}

	TestBattery(long capacity, long maxIo, long stored) {
		this.capacity = capacity;
		this.maxIo = maxIo;
		this.stored = Math.max(0, Math.min(capacity, stored));
	}

	/** 子类替身覆盖收发上限（发电机 maxReceive = 0）。 */
	protected long receiveLimit() {
		return maxIo;
	}

	protected long extractLimit() {
		return maxIo;
	}

	@Override
	public long getEnergyStored() {
		return stored;
	}

	@Override
	public long getEnergyCapacity() {
		return capacity;
	}

	@Override
	public long getMaxReceive() {
		return receiveLimit();
	}

	@Override
	public long getMaxExtract() {
		return extractLimit();
	}

	@Override
	public long receiveEnergy(long maxReceive, boolean simulate) {
		long accepted = Math.min(Math.min(maxReceive, receiveLimit()), capacity - stored);
		if (accepted <= 0) {
			return 0;
		}
		if (!simulate) {
			stored += accepted;
		}
		return accepted;
	}

	@Override
	public long extractEnergy(long maxExtract, boolean simulate) {
		long taken = Math.min(Math.min(maxExtract, extractLimit()), stored);
		if (taken <= 0) {
			return 0;
		}
		if (!simulate) {
			stored -= taken;
		}
		return taken;
	}

	long stored() {
		return stored;
	}
}
