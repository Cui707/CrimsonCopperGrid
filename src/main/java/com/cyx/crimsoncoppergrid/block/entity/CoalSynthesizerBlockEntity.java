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
 * 电力煤炭合成机：把电变成煤。
 *
 * <p>8 000 FE = 1 块煤。作为对比，燃料发电机烧一块煤产出 64 000 FE ——
 * 也就是说这条链路本身是亏的（8:1），它的价值在于把「多余的可再生电力」
 * 变成「可携带、可储存的燃料」，而不是当成永动机。
 */
public class CoalSynthesizerBlockEntity extends AbstractConsumerBlockEntity {
	public static final long FE_PER_COAL = 8_000L;
	/** 单周期最多输入，等于每周期发电能力的上限约束。 */
	public static final long MAX_INPUT = 200L;
	public static final int MAX_COAL = 64;

	private long progress;
	private int coal;
	private int ejectTimer;

	public CoalSynthesizerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.COAL_SYNTHESIZER, pos, state);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, CoalSynthesizerBlockEntity entity) {
		// 每 20 刻尝试把产出的煤送进相邻容器，送不进去就继续攒着
		if (++entity.ejectTimer >= 20) {
			entity.ejectTimer = 0;
			if (entity.coal > 0) {
				ItemStack stack = new ItemStack(Items.COAL, entity.coal);
				if (com.cyx.crimsoncoppergrid.block.CoalSynthesizerBlock.ejectToNeighbor(level, pos, stack)) {
					entity.coal = 0;
					entity.setChanged();
					entity.syncToClient();
				}
			}
		}
	}

	@Override
	public long consumeEnergy(long available) {
		resetAccounting();
		if (coal >= MAX_COAL) {
			return 0L;
		}
		long need = Math.min(MAX_INPUT, FE_PER_COAL - progress);
		requestEnergy(need);
		long used = Math.min(need, Math.max(0, available));
		if (used <= 0) {
			return 0L;
		}
		progress += used;
		pushConsumed(used);
		if (progress >= FE_PER_COAL) {
			progress -= FE_PER_COAL;
			coal++;
		}
		setChanged();
		syncToClient();
		return used;
	}

	@Override
	protected long getMaxInput() {
		return MAX_INPUT;
	}

	// ------------------------------------------------------------ 存取

	public int getCoal() {
		return coal;
	}

	/** 玩家取走全部煤炭。 */
	public ItemStack extractCoal() {
		if (coal <= 0) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = new ItemStack(Items.COAL, coal);
		coal = 0;
		setChanged();
		syncToClient();
		return stack;
	}

	public long getProgress() {
		return progress;
	}

	// ------------------------------------------------------------ 展示

	@Override
	public long getEnergyStored() {
		return progress;
	}

	@Override
	public long getEnergyCapacity() {
		return FE_PER_COAL;
	}

	@Override
	protected void setEnergyStored(long value) {
		this.progress = Math.max(0, Math.min(FE_PER_COAL, value));
	}

	@Override
	protected void readEnergy(ValueInput input) {
		this.progress = input.getLongOr("Progress", 0L);
		this.coal = input.getIntOr("Coal", 0);
	}

	@Override
	protected void writeEnergy(ValueOutput output) {
		output.putLong("Progress", progress);
		output.putInt("Coal", coal);
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
