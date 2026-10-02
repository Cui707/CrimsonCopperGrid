package com.cyx.crimsoncoppergrid.blocks;

import com.cyx.crimsoncoppergrid.blockentity.BatteryBlockEntity;
import com.cyx.crimsoncoppergrid.common.blocks.BlockMachineBase;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 电池方块。
 *
 * <p>它覆写了比较器输出：{@link BlockMachineBase} 默认按物品栏算，而电池没有物品栏，
 * 那样输出永远是 0。改成按电量占比给 0~15，玩家就能用红石比较器做「电量低于一半就
 * 启动备用发电机」这类自动化，不需要打开界面盯着看。
 */
public class BatteryBlock extends BlockMachineBase {

	public BatteryBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BatteryBlockEntity(pos, state);
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		return com.cyx.crimsoncoppergrid.common.powerSystem.PowerAcceptorBlockEntity
				.calculateComparatorOutputFromEnergy(level.getBlockEntity(pos));
	}
}
