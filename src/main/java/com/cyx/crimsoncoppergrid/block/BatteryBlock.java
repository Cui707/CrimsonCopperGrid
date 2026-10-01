package com.cyx.crimsoncoppergrid.block;

import com.cyx.crimsoncoppergrid.block.entity.BatteryBlockEntity;
import com.cyx.crimsoncoppergrid.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/**
 * 电池方块。能量数值通过方块实体的同步包送到客户端，用于贴图/提示显示。
 */
public class BatteryBlock extends BaseEntityBlock {
	public BatteryBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BatteryBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide()) {
			return null;
		}
		return createTicker(type, ModBlockEntities.BATTERY, BatteryBlockEntity::serverTick);
	}

	@SuppressWarnings("unchecked")
	protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTicker(
			BlockEntityType<A> givenType, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker) {
		return expectedType == givenType ? (BlockEntityTicker<A>) ticker : null;
	}

	public static Properties batteryProperties() {
		return Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(2.0F).sound(SoundType.COPPER);
	}

	/** 电网重组后由 {@link com.cyx.crimsoncoppergrid.energy.GridRegistry} 调用。 */
	public static void onTopologyChanged(Level level, BlockPos pos) {
		if (level instanceof ServerLevel serverLevel) {
			com.cyx.crimsoncoppergrid.energy.GridRegistry.get(serverLevel).markDirty(pos);
		}
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		onTopologyChanged(level, pos);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		onTopologyChanged(level, pos);
	}
}
