package com.cyx.crimsoncoppergrid.block.entity;

import com.cyx.crimsoncoppergrid.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 电力岩浆机：耗电自造岩浆，内部有 4 桶容量的小罐子。
 *
 * <p>数值：30 FE/t 换 1 mB lava / 25 t，也就是 **1 桶岩浆 ≈ 750 000 FE**。
 * 一块煤在燃料发电机里值 64 000 FE，所以烧满一桶岩浆大约要 12 块煤 ——
 * 它是全模组最耗电的设备，用它换来的「可再生岩浆」才不至于破坏生存平衡。
 *
 * <p>罐子只做整数（mB）存储，没有实现完整的流体 API；桶的进出在方块层处理。
 */
public class LavaGeneratorBlockEntity extends AbstractConsumerBlockEntity {
	public static final long FE_PER_MB = 30L;
	public static final int TANK_CAPACITY_MB = 4_000;
	public static final long MAX_INPUT = 60L;
	public static final int BUCKET_MB = 1_000;

	private int lavaMb;
	private long progress;

	public LavaGeneratorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.LAVA_GENERATOR, pos, state);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, LavaGeneratorBlockEntity entity) {
		// 产出在电网结算时推进
	}

	@Override
	public long consumeEnergy(long available) {
		resetAccounting();
		if (lavaMb >= TANK_CAPACITY_MB) {
			return 0L;
		}
		long need = Math.min(MAX_INPUT, FE_PER_MB - progress);
		requestEnergy(need);
		long used = Math.min(need, Math.max(0, available));
		if (used <= 0) {
			return 0L;
		}
		progress += used;
		pushConsumed(used);
		if (progress >= FE_PER_MB) {
			progress -= FE_PER_MB;
			lavaMb = Math.min(TANK_CAPACITY_MB, lavaMb + 1);
		}
		setChanged();
		syncToClient();
		return used;
	}

	@Override
	protected long getMaxInput() {
		return MAX_INPUT;
	}

	// ------------------------------------------------------------ 罐子操作

	public int getLavaMb() {
		return lavaMb;
	}

	public int getTankCapacityMb() {
		return TANK_CAPACITY_MB;
	}

	public boolean isFull() {
		return lavaMb >= TANK_CAPACITY_MB;
	}

	/**
	 * 用桶取出一桶岩浆。
	 *
	 * @return 装满岩浆的桶；罐里不足一桶时返回空
	 */
	public ItemStack fillBucket() {
		if (lavaMb < BUCKET_MB) {
			return ItemStack.EMPTY;
		}
		lavaMb -= BUCKET_MB;
		setChanged();
		syncToClient();
		return new ItemStack(Items.LAVA_BUCKET);
	}

	/**
	 * 把一桶岩浆倒进罐里。
	 *
	 * @return 空桶；罐里放不下时返回空
	 */
	public ItemStack drainBucket() {
		if (TANK_CAPACITY_MB - lavaMb < BUCKET_MB) {
			return ItemStack.EMPTY;
		}
		lavaMb += BUCKET_MB;
		setChanged();
		syncToClient();
		return new ItemStack(Items.BUCKET);
	}

	// ------------------------------------------------------------ 展示与存档

	@Override
	public long getEnergyStored() {
		return progress;
	}

	@Override
	public long getEnergyCapacity() {
		return FE_PER_MB;
	}

	@Override
	protected void setEnergyStored(long value) {
		this.progress = Math.max(0, Math.min(FE_PER_MB, value));
	}

	@Override
	protected void readEnergy(ValueInput input) {
		this.progress = input.getLongOr("Progress", 0L);
		this.lavaMb = input.getIntOr("Lava", 0);
	}

	@Override
	protected void writeEnergy(ValueOutput output) {
		output.putLong("Progress", progress);
		output.putInt("Lava", lavaMb);
	}

	private void syncToClient() {
		Level level = this.getLevel();
		if (level != null && !level.isClientSide()) {
			level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
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
}
