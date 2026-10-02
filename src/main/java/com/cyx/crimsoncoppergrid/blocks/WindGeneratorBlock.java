package com.cyx.crimsoncoppergrid.blocks;

import com.cyx.crimsoncoppergrid.blockentity.WindGeneratorBlockEntity;
import com.cyx.crimsoncoppergrid.common.blocks.BlockMachineBase;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
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
}
