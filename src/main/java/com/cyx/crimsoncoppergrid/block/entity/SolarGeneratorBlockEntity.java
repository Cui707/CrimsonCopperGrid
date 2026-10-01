package com.cyx.crimsoncoppergrid.block.entity;

import com.cyx.crimsoncoppergrid.energy.EnergyProducer;
import com.cyx.crimsoncoppergrid.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 太阳能发电机：只在白天、晴天、上方能见到天空时发电。
 *
 * <p>判定用的是亮度（{@code getMaxLocalRawBrightness}）+ 天空可见性，
 * 这样在洞里、在玻璃下面（玻璃不挡天光但会挡直接视野）等情形都有合理表现。
 */
public class SolarGeneratorBlockEntity extends AbstractEnergyBlockEntity implements EnergyProducer {
	public static final long MAX_OUTPUT = 20L;
	public static final long CAPACITY = 1_000L;

	private long energy;
	private long lastProduced;

	public SolarGeneratorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.SOLAR_GENERATOR, pos, state);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, SolarGeneratorBlockEntity entity) {
		// 能量在结算时按当时的天光计算，这里不需要额外逻辑
	}

	@Override
	public long produceEnergy(long maxDemand) {
		long capacityNow = currentOutput();
		long output = Math.min(Math.min(energy, capacityNow), Math.max(0, maxDemand));
		energy -= output;
		this.lastProduced = output;
		return output;
	}

	/** 按当前时间/天气算出这一周期能发多少（尚未扣除需求）。 */
	private long currentOutput() {
		Level level = this.getLevel();
		if (level == null || level.isClientSide()) {
			return 0;
		}
		if (!level.isBrightOutside() || level.isRaining()) {
			return 0;
		}
		// 26.3 没有 canSeeSky，直接用「正上方格子的天空光是否为满值」判断是否露天
		BlockPos above = this.getBlockPos().above();
		if (level.getBrightness(LightLayer.SKY, above) < 15) {
			return 0;
		}
		return MAX_OUTPUT;
	}

	/** 供 GUI / 物品提示展示：当前是否在发电。 */
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
