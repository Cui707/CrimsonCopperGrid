package com.cyx.crimsoncoppergrid.block.entity;

import com.cyx.crimsoncoppergrid.energy.EnergyProducer;
import com.cyx.crimsoncoppergrid.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 风力发电机：越高的地方风越大。
 *
 * <p>海平面（y=62）附近约 10 FE/t，到 y≈200 达到上限 30 FE/t。
 * 需要上方有一格空间作为「迎风面」，埋在地里或封死在天花板下不发电。
 */
public class WindGeneratorBlockEntity extends AbstractEnergyBlockEntity implements EnergyProducer {
	public static final long MIN_OUTPUT = 10L;
	public static final long MAX_OUTPUT = 30L;
	public static final long CAPACITY = 1_000L;
	private static final int RAMP_TOP = 200;

	private long energy;
	private long lastProduced;

	public WindGeneratorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.WIND_GENERATOR, pos, state);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, WindGeneratorBlockEntity entity) {
		// 输出按结算时的位置与空间实时计算
	}

	@Override
	public long produceEnergy(long maxDemand) {
		long output = Math.min(Math.min(energy, currentOutput()), Math.max(0, maxDemand));
		energy -= output;
		this.lastProduced = output;
		return output;
	}

	private long currentOutput() {
		Level level = this.getLevel();
		if (level == null || level.isClientSide()) {
			return 0;
		}
		if (!level.getBlockState(this.getBlockPos().above()).isAir()) {
			return 0;
		}
		int y = this.getBlockPos().getY();
		double ratio = (double) (y - 62) / (RAMP_TOP - 62);
		ratio = Math.max(0.0, Math.min(1.0, ratio));
		return MIN_OUTPUT + Math.round((MAX_OUTPUT - MIN_OUTPUT) * ratio);
	}

	public boolean isGenerating() {
		return currentOutput() > 0;
	}

	@Override
	public long getMaxOutput() {
		return MAX_OUTPUT;
	}

	@Override
	public long getProducedEnergy() {
		return lastProduced;
	}

	@Override
	public long getEnergyStored() {
		return energy;
	}

	@Override
	public long getEnergyCapacity() {
		return CAPACITY;
	}

	@Override
	public long getMaxReceive() {
		return 0L;
	}

	@Override
	public long getMaxExtract() {
		return MAX_OUTPUT;
	}

	@Override
	public long receiveEnergy(long maxReceive, boolean simulate) {
		return 0L;
	}

	@Override
	public long extractEnergy(long maxExtract, boolean simulate) {
		long taken = Math.min(Math.min(maxExtract, MAX_OUTPUT), energy);
		if (taken > 0 && !simulate) {
			energy -= taken;
			lastProduced = taken;
		}
		return taken;
	}

	@Override
	protected void setEnergyStored(long value) {
		this.energy = Math.max(0, Math.min(CAPACITY, value));
	}
}
