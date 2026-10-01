package com.cyx.crimsoncoppergrid.block.entity;

import com.cyx.crimsoncoppergrid.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 电池：只进不出的缓冲（对电网而言）。
 *
 * <p>容量 1 000 000 FE，单次收发上限 1 000 FE —— 即每秒最多吞吐 2 000 FE，
 * 足够在一瞬间顶住用电高峰，但不足以掩盖发电能力的长期缺口。
 */
public class BatteryBlockEntity extends AbstractEnergyBlockEntity {
	public static final long CAPACITY = 1_000_000L;
	public static final long MAX_IO = 1_000L;

	private long energy;

	public BatteryBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.BATTERY, pos, state);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, BatteryBlockEntity entity) {
		// 电池自身不需要 tick，能量由电网结算时直接读写
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
		return MAX_IO;
	}

	@Override
	public long getMaxExtract() {
		return MAX_IO;
	}

	@Override
	public long receiveEnergy(long maxReceive, boolean simulate) {
		long accepted = Math.min(Math.min(maxReceive, MAX_IO), CAPACITY - energy);
		if (accepted <= 0) {
			return 0;
		}
		if (!simulate) {
			energy += accepted;
			setChanged();
			syncToClient();
		}
		return accepted;
	}

	@Override
	public long extractEnergy(long maxExtract, boolean simulate) {
		long taken = Math.min(Math.min(maxExtract, MAX_IO), energy);
		if (taken <= 0) {
			return 0;
		}
		if (!simulate) {
			energy -= taken;
			setChanged();
			syncToClient();
		}
		return taken;
	}

	/** 电量变化时让客户端能刷新显示（每次结算最多一次，开销可接受）。 */
	protected void syncToClient() {
		Level level = this.getLevel();
		if (level != null && !level.isClientSide()) {
			level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
		}
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return this.saveCustomOnly(registries);
	}

	@Override
	protected void setEnergyStored(long value) {
		this.energy = Math.max(0, Math.min(CAPACITY, value));
	}
}
