package com.cyx.crimsoncoppergrid.blockentity;

import com.cyx.crimsoncoppergrid.common.menu.SolarGeneratorMenu;
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
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 太阳能发电机：只在白天、晴天、正上方露天时发电。
 *
 * <p>判定用「正上方那格的天空光是否为满值」而不是简单看维度光照 ——
 * 这样在洞里、在屋檐下都不发电，而玻璃天窗（玻璃不挡天光）下面仍然发电，
 * 表现与玩家的直觉一致。
 *
 * <p>发电方式是每 tick 往自己的缓冲里加 {@link #MAX_OUTPUT}，
 * 缓冲满了自然就不再增加，限流是免费的。之后的传输由
 * {@link PowerAcceptorBlockEntity} 的推流逻辑负责，这里不用管。
 */
public class SolarGeneratorBlockEntity extends PowerAcceptorBlockEntity implements MenuProvider {

	/** 每 tick 的发电量。 */
	public static final long MAX_OUTPUT = 20L;
	/** 缓冲容量：够扛 50 tick 的阴云，也就是 2.5 秒。 */
	public static final long CAPACITY = 1_000L;

	public SolarGeneratorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.SOLAR_GENERATOR, pos, state);
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

	/** 当前这一刻的发电量：受时间、天气与遮挡共同决定。 */
	public long currentOutput() {
		Level level = getLevel();
		if (level == null || level.isClientSide()) {
			return 0;
		}
		if (!level.isBrightOutside() || level.isRaining()) {
			return 0;
		}
		// 26.3 没有 canSeeSky，直接用「正上方格子的天空光是否为满值」判断是否露天
		if (level.getBrightness(LightLayer.SKY, getBlockPos().above()) < 15) {
			return 0;
		}
		return MAX_OUTPUT;
	}

	public boolean isGenerating() {
		return currentOutput() > 0;
	}

	// ------------------------------------------------------------ 界面

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.crimsoncoppergrid.solar_generator");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return new SolarGeneratorMenu(containerId, playerInventory, this);
	}
}
