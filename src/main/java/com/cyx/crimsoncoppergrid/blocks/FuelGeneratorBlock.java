package com.cyx.crimsoncoppergrid.blocks;

import com.cyx.crimsoncoppergrid.blockentity.FuelGeneratorBlockEntity;
import com.cyx.crimsoncoppergrid.common.blocks.BlockMachineBase;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 燃料发电机的方块。机器类方块的行为（朝向、工作状态、tick、比较器）全在
 * {@link BlockMachineBase} 里，这里只负责把方块实例接到对应的方块实体上。
 */
public class FuelGeneratorBlock extends BlockMachineBase {

	public FuelGeneratorBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FuelGeneratorBlockEntity(pos, state);
	}
}
