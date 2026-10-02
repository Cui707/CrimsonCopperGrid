package com.cyx.crimsoncoppergrid.blockentity;

import com.cyx.crimsoncoppergrid.common.menu.WindGeneratorMenu;
import com.cyx.crimsoncoppergrid.common.powerSystem.PowerAcceptorBlockEntity;
import com.cyx.crimsoncoppergrid.init.ModBlockEntities;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 风力发电机：越高的地方风越大。
 *
 * <p>海平面（y=62）附近约 {@link #MIN_OUTPUT} FE/t，到 y≈{@code RAMP_TOP} 达到上限
 * {@link #MAX_OUTPUT} FE/t。正上方需要有一格空间作为「迎风面」——
 * 埋在地里或封死在天花板下不发电。
 */
public class WindGeneratorBlockEntity extends PowerAcceptorBlockEntity implements MenuProvider {

	public static final long MIN_OUTPUT = 10L;
	public static final long MAX_OUTPUT = 30L;
	public static final long CAPACITY = 1_000L;

	/** 到达满输出所需的高度。 */
	private static final int RAMP_TOP = 200;

	public WindGeneratorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.WIND_GENERATOR, pos, state);
	}

	// ------------------------------------------------------------ 能量基准

	@Override
	public long getBaseMaxPower() {
		return CAPACITY;
	}

	@Override
	public long getBaseMaxOutput() {
		return MAX_OUTPUT;
	}

	@Override
	public long getBaseMaxInput() {
		return 0L;
	}

	// ------------------------------------------------------------ tick

	@Override
	protected void serverTick() {
		long output = currentOutput();
		if (output > 0) {
			long space = getFreeSpace();
			if (space > 0) {
				addEnergy(Math.min(output, space));
			}
		}
		setActive(output > 0);
	}

	/** 当前这一刻的发电量：由高度与上方空间共同决定。 */
	public long currentOutput() {
		Level level = getLevel();
		if (level == null || level.isClientSide()) {
			return 0;
		}
		if (!level.getBlockState(getBlockPos().above()).isAir()) {
			return 0;
		}
		int y = getBlockPos().getY();
		double ratio = (double) (y - 62) / (RAMP_TOP - 62);
		ratio = Math.max(0.0, Math.min(1.0, ratio));
		return MIN_OUTPUT + Math.round((MAX_OUTPUT - MIN_OUTPUT) * ratio);
	}

	public boolean isGenerating() {
		return currentOutput() > 0;
	}

	// ------------------------------------------------------------ 界面

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.crimsoncoppergrid.wind_generator");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return new WindGeneratorMenu(containerId, playerInventory, this);
	}
}
