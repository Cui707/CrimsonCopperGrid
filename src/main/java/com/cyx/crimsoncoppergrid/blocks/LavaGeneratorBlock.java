package com.cyx.crimsoncoppergrid.blocks;

import com.cyx.crimsoncoppergrid.blockentity.LavaGeneratorBlockEntity;
import com.cyx.crimsoncoppergrid.common.blocks.BlockMachineBase;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 电力岩浆机。
 *
 * <p>桶的进出现在通过界面完成：左侧放空桶，右侧出岩浆桶。
 * 方块层不再重写 {@code useItemOn}，直接继承基类 —— 任何情况下右键都能打开界面。
 * 详见 {@link BlockMachineBase#useWithoutItem}。
 */
public class LavaGeneratorBlock extends BlockMachineBase {

	public LavaGeneratorBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new LavaGeneratorBlockEntity(pos, state);
	}
}
