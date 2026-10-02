package com.cyx.crimsoncoppergrid.blocks;

import com.cyx.crimsoncoppergrid.blockentity.WindGeneratorBlockEntity;
import com.cyx.crimsoncoppergrid.common.blockentity.MachineBaseBlockEntity;
import com.cyx.crimsoncoppergrid.common.blocks.BlockMachineBase;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** 风力发电机的方块。 */
public class WindGeneratorBlock extends BlockMachineBase {

	public WindGeneratorBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new WindGeneratorBlockEntity(pos, state);
	}

	/**
	 * 这台机器客户端也要 tick。
	 *
	 * <p>基类默认只在服务端返回 ticker（机器逻辑本来就只在服务端跑），但叶轮的角度
	 * 是**每刻连续性**的动画状态，靠同步包送是送不出流畅感的 —— 所以这里两侧都 tick，
	 * 客户端那次只会走到 {@code WindGeneratorBlockEntity#clientTick}。
	 */
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return MachineBaseBlockEntity.ticker();
	}
}
