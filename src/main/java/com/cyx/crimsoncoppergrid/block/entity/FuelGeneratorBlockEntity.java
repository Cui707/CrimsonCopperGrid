package com.cyx.crimsoncoppergrid.block.entity;

import com.cyx.crimsoncoppergrid.energy.EnergyProducer;
import com.cyx.crimsoncoppergrid.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 燃料发电机：烧原版燃料换电。
 *
 * <p>1 个「燃料刻」= 40 FE，与熔炉同步消耗燃料：一块煤在熔炉里烧 1600 刻，
 * 在这里也就是 1600 × 40 = 64 000 FE。
 *
 * <p>为什么自己管燃烧状态：要复刻熔炉的行为，最稳的是拥有自己的 {@code burnTime}，
 * 而不是用反射去改原版 {@code AbstractFurnaceBlockEntity} 的私有字段。
 */
public class FuelGeneratorBlockEntity extends AbstractEnergyBlockEntity implements EnergyProducer, Container {
	public static final int FUEL_SLOT = 0;
	public static final long FE_PER_FUEL_TICK = 40L;
	public static final long MAX_OUTPUT = 40L;
	public static final long CAPACITY = 4_000L;

	private long energy;
	private int burnTime;
	private int burnTimeTotal;
	private long lastProduced;
	private final ItemStack[] items = new ItemStack[] { ItemStack.EMPTY };

	public FuelGeneratorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.FUEL_GENERATOR, pos, state);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, FuelGeneratorBlockEntity entity) {
		entity.serverTick();
	}

	private void serverTick() {
		boolean changed = false;

		if (burnTime <= 0 && !this.getItem(FUEL_SLOT).isEmpty()) {
			int value = fuelValue(this.getItem(FUEL_SLOT));
			if (value > 0) {
				burnTime = value;
				burnTimeTotal = value;
				consumeFuel();
				changed = true;
			}
		}

		if (burnTime > 0) {
			burnTime--;
			energy = Math.min(CAPACITY, energy + FE_PER_FUEL_TICK);
			if (burnTime == 0) {
				burnTimeTotal = 0;
				changed = true;
			}
		}

		if (changed) {
			setChanged();
			syncToClient();
		}
	}

	// ------------------------------------------------------------ 发电

	@Override
	public long produceEnergy(long maxDemand) {
		long output = Math.min(Math.min(energy, MAX_OUTPUT), Math.max(0, maxDemand));
		energy -= output;
		this.lastProduced = output;
		if (output > 0) {
			setChanged();
		}
		return output;
	}

	@Override
	public long getMaxOutput() {
		return MAX_OUTPUT;
	}

	@Override
	public long getProducedEnergy() {
		return lastProduced;
	}

	// ------------------------------------------------------------ 能源存储（缓冲）

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
			setChanged();
		}
		return taken;
	}

	@Override
	protected void setEnergyStored(long value) {
		this.energy = Math.max(0, Math.min(CAPACITY, value));
	}

	// ------------------------------------------------------------ 燃料

	public int getBurnTime() {
		return burnTime;
	}

	public int getBurnTimeTotal() {
		return burnTimeTotal;
	}

	public boolean isBurning() {
		return burnTime > 0;
	}

	private static int fuelValue(ItemStack stack) {
		if (stack.isEmpty()) {
			return 0;
		}
		if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) {
			return 1600;
		}
		if (stack.is(Items.COAL_BLOCK)) {
			return 16000;
		}
		if (stack.is(Items.BLAZE_ROD)) {
			return 2400;
		}
		if (stack.is(Items.DRIED_KELP_BLOCK)) {
			return 4001;
		}
		if (stack.is(Items.LAVA_BUCKET)) {
			return 20000;
		}
		if (stack.is(Items.BAMBOO)) {
			return 50;
		}
		return 0;
	}

	private void consumeFuel() {
		ItemStack fuel = this.getItem(FUEL_SLOT);
		// 26.3 的燃料余料是 ItemStackTemplate（例如岩浆桶 -> 空桶），用 create() 变回 ItemStack
		ItemStack remainder = fuel.getItem().getCraftingRemainder().create();
		fuel.shrink(1);
		if (fuel.isEmpty()) {
			this.setItem(FUEL_SLOT, remainder);
		}
	}

	// ------------------------------------------------------------ 容器（单个燃料槽）

	@Override
	public int getContainerSize() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return this.getItem(FUEL_SLOT).isEmpty();
	}

	@Override
	public ItemStack getItem(int slot) {
		return this.items == null ? ItemStack.EMPTY : this.items[slot];
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		ItemStack stack = this.getItem(slot);
		if (stack.isEmpty()) {
			return ItemStack.EMPTY;
		}
		ItemStack split = stack.split(amount);
		this.setChanged();
		return split;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		ItemStack stack = this.getItem(slot);
		this.items[slot] = ItemStack.EMPTY;
		return stack;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		this.items[slot] = stack;
		this.setChanged();
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return fuelValue(stack) > 0;
	}

	@Override
	public boolean stillValid(net.minecraft.world.entity.player.Player player) {
		return Container.stillValidBlockEntity(this, player);
	}

	@Override
	public void clearContent() {
		this.items[FUEL_SLOT] = ItemStack.EMPTY;
	}

	// ------------------------------------------------------------ 存档与同步

	@Override
	protected void readEnergy(ValueInput input) {
		super.readEnergy(input);
		this.burnTime = input.getIntOr("BurnTime", 0);
		this.burnTimeTotal = input.getIntOr("BurnTimeTotal", 0);
		this.items[FUEL_SLOT] = input.read("Fuel", ItemStack.CODEC).orElse(ItemStack.EMPTY);
	}

	@Override
	protected void writeEnergy(ValueOutput output) {
		super.writeEnergy(output);
		output.putInt("BurnTime", burnTime);
		output.putInt("BurnTimeTotal", burnTimeTotal);
		output.store("Fuel", ItemStack.CODEC, this.items[FUEL_SLOT]);
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
