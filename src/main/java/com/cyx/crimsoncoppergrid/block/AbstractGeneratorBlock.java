package com.cyx.crimsoncoppergrid.block;

import com.cyx.crimsoncoppergrid.block.entity.FuelGeneratorBlockEntity;
import com.cyx.crimsoncoppergrid.block.entity.SolarGeneratorBlockEntity;
import com.cyx.crimsoncoppergrid.block.entity.WindGeneratorBlockEntity;
import com.cyx.crimsoncoppergrid.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;

/**
 * 三种发电机的公共父类：都是「水平朝向 + 有方块实体」的机器。
 */
public abstract class AbstractGeneratorBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	protected AbstractGeneratorBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if (level instanceof ServerLevel serverLevel) {
			com.cyx.crimsoncoppergrid.energy.GridRegistry.get(serverLevel).markDirty(pos);
		}
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		com.cyx.crimsoncoppergrid.energy.GridRegistry.get(level).markDirty(pos);
	}

	/** 发电机系列的通用外观设置。 */
	public static Properties generatorProperties() {
		return Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(2.0F).sound(SoundType.COPPER).noOcclusion();
	}

	@SuppressWarnings("unchecked")
	protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTicker(
			BlockEntityType<A> givenType, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker) {
		return expectedType == givenType ? (BlockEntityTicker<A>) ticker : null;
	}

	/** 燃料发电机。 */
	public static class Fuel extends AbstractGeneratorBlock {
		public Fuel(Properties properties) {
			super(properties);
		}

		@Override
		public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
			return new FuelGeneratorBlockEntity(pos, state);
		}

		@Override
		public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
			return level.isClientSide() ? null : createTicker(type, ModBlockEntities.FUEL_GENERATOR, FuelGeneratorBlockEntity::serverTick);
		}
	}

	/** 太阳能发电机。 */
	public static class Solar extends AbstractGeneratorBlock {
		public Solar(Properties properties) {
			super(properties);
		}

		@Override
		public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
			return new SolarGeneratorBlockEntity(pos, state);
		}

		@Override
		public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
			return level.isClientSide() ? null : createTicker(type, ModBlockEntities.SOLAR_GENERATOR, SolarGeneratorBlockEntity::serverTick);
		}
	}

	/** 风力发电机。 */
	public static class Wind extends AbstractGeneratorBlock {
		public Wind(Properties properties) {
			super(properties);
		}

		@Override
		public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
			return new WindGeneratorBlockEntity(pos, state);
		}

		@Override
		public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
			return level.isClientSide() ? null : createTicker(type, ModBlockEntities.WIND_GENERATOR, WindGeneratorBlockEntity::serverTick);
		}
	}
}
