package com.cyx.crimsoncoppergrid.blocks;

import com.cyx.crimsoncoppergrid.blockentity.PowerControllerBlockEntity;
import com.cyx.crimsoncoppergrid.common.blocks.BlockMachineBase;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 电力控制器方块。行为全在基类与方块实体里，这里只做接线。
 *
 * <p>它是「仪表」而不是「设备」：空手右键打开电网总览面板，
 * 面板里的数字来自 {@link PowerControllerBlockEntity} 对整张电网的扫描。
 */
public class PowerControllerBlock extends BlockMachineBase {

	public PowerControllerBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new PowerControllerBlockEntity(pos, state);
	}
}
