package com.cyx.crimsoncoppergrid.block;

import java.util.Map;

import com.cyx.crimsoncoppergrid.energy.GridRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 电线：纯导体，本身不参与能量结算，只负责把两端的设备连通。
 *
 * <p>六个方向各有一个布尔属性表示「是否连接」。这个属性写进 blockstate 而不是
 * 方块实体，好处是同步与渲染都走原版机制，不需要额外的网络包。
 */
public class CableBlock extends Block {
	public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
	public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
	public static final BooleanProperty EAST = BlockStateProperties.EAST;
	public static final BooleanProperty WEST = BlockStateProperties.WEST;
	public static final BooleanProperty UP = BlockStateProperties.UP;
	public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

	/** setBlock 用的更新标志：通知客户端 + 通知邻居。 */
	public static final int UPDATE_FLAGS = Block.UPDATE_ALL;

	/** 电线芯的碰撞体积（6/16 见方）。 */
	private static final VoxelShape CORE = Block.box(6.0, 6.0, 6.0, 10.0, 10.0, 10.0);

	public CableBlock(Properties properties) {
		super(properties);
		BlockState state = this.stateDefinition.any();
		for (Direction side : Direction.values()) {
			state = state.setValue(propertyFor(side), false);
		}
		this.registerDefaultState(state);
	}

	/** 方向 -> 属性 的映射，避免到处写 switch。 */
	public static BooleanProperty propertyFor(Direction side) {
		return switch (side) {
			case NORTH -> NORTH;
			case SOUTH -> SOUTH;
			case EAST -> EAST;
			case WEST -> WEST;
			case UP -> UP;
			case DOWN -> DOWN;
		};
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState state = this.defaultBlockState();
		if (level instanceof ServerLevel serverLevel) {
			GridRegistry registry = GridRegistry.get(serverLevel);
			for (Direction side : Direction.values()) {
				state = state.setValue(propertyFor(side), registry.shouldConnect(pos, side));
			}
		}
		return state;
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

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, Orientation orientation, boolean movedByPiston) {
		super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
		onTopologyChanged(level, pos);
	}

	private static void onTopologyChanged(Level level, BlockPos pos) {
		if (level instanceof ServerLevel serverLevel) {
			GridRegistry registry = GridRegistry.get(serverLevel);
			registry.markDirty(pos);
			registry.refreshAround(pos);
		}
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		VoxelShape shape = CORE;
		double lo = 7.0 / 16.0;
		double hi = 9.0 / 16.0;
		if (state.getValue(NORTH)) {
			shape = Shapes.or(shape, Block.box(lo, lo, 0.0, hi, hi, 6.0));
		}
		if (state.getValue(SOUTH)) {
			shape = Shapes.or(shape, Block.box(lo, lo, 10.0, hi, hi, 16.0));
		}
		if (state.getValue(WEST)) {
			shape = Shapes.or(shape, Block.box(0.0, lo, lo, 6.0, hi, hi));
		}
		if (state.getValue(EAST)) {
			shape = Shapes.or(shape, Block.box(10.0, lo, lo, 16.0, hi, hi));
		}
		if (state.getValue(DOWN)) {
			shape = Shapes.or(shape, Block.box(lo, 0.0, lo, hi, 6.0, hi));
		}
		if (state.getValue(UP)) {
			shape = Shapes.or(shape, Block.box(lo, 10.0, lo, hi, 16.0, hi));
		}
		return shape;
	}

	/** 供注册时使用的一组属性设置。 */
	public static Properties cableProperties() {
		return Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(0.6F).sound(SoundType.COPPER).noOcclusion();
	}

	/** 六个方向属性，便于遍历。 */
	public static Map<Direction, BooleanProperty> directions() {
		return Map.of(
				Direction.NORTH, NORTH,
				Direction.SOUTH, SOUTH,
				Direction.EAST, EAST,
				Direction.WEST, WEST,
				Direction.UP, UP,
				Direction.DOWN, DOWN);
	}
}
