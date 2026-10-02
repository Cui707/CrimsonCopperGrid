package com.cyx.crimsoncoppergrid.blocks.cable;

import com.cyx.crimsoncoppergrid.energy.EnergyLookup;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 电线。
 *
 * <p>对齐 TechReborn：方块本身只提供属性与形状，**连接位的计算与写回交给
 * {@link CableBlockEntity}**（放置时算一次，之后每 10 刻自愈重算）。
 */
public class CableBlock extends AbstractConnectionBlock {
	public CableBlock(Properties properties) {
		super(properties, 1.0 / 16.0);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CableBlockEntity(pos, state);
	}

	/**
	 * 放置后立刻重算一次形状（对齐 TechReborn 在 onPlaced 里做连接更新的做法），
	 * 这样新放下的电线也马上会让**相邻**已有电线长出对应方向的臂。
	 */
	@Override
	public void setPlacedBy(net.minecraft.world.level.Level level, BlockPos pos, BlockState state,
			net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level instanceof ServerLevel serverLevel) {
			refresh(serverLevel, pos);
			for (Direction side : Direction.values()) {
				refresh(serverLevel, pos.relative(side));
			}
		}
	}

	@Override
	public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
			Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
		if (level.isClientSide()) {
			return null;
		}
		return (lvl, pos, st, be) -> {
			if (be instanceof CableBlockEntity cable) {
				CableBlockEntity.serverTick(lvl, pos, st, cable);
			}
		};
	}

	/** 电线自身也提供能量能力（对齐 TechReborn：电线是能量节点）。 */
	public static boolean connects(Level level, BlockPos pos, Direction side) {
		return EnergyLookup.shouldConnect(level, pos, side);
	}

	/** 放置时通知：让这个坐标的电线立刻重算一次形状。 */
	public static void refresh(Level level, BlockPos pos) {
		if (level instanceof ServerLevel serverLevel && serverLevel.getBlockEntity(pos) instanceof CableBlockEntity cable) {
			cable.recomputeShape();
		}
	}
}
