package com.cyx.crimsoncoppergrid.blocks.cable;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 定向连接方块的公共骨架：六向布尔属性 + 由属性推导出的形状。
 *
 * <p>重构说明（对齐 TechReborn 的 CableBlock / CableShapeUtil）：
 * 连接位的**计算**放在方块实体里，方块本身只负责「属性声明 + 形状 + 属性访问」，
 * 这样"谁该连谁"只有一个地方说了算。
 */
public abstract class AbstractConnectionBlock extends BaseEntityBlock {
	public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
	public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
	public static final BooleanProperty EAST = BlockStateProperties.EAST;
	public static final BooleanProperty WEST = BlockStateProperties.WEST;
	public static final BooleanProperty UP = BlockStateProperties.UP;
	public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

	public static final Map<Direction, BooleanProperty> PROPERTY_MAP = buildMap();

	private static Map<Direction, BooleanProperty> buildMap() {
		Map<Direction, BooleanProperty> map = new EnumMap<>(Direction.class);
		map.put(Direction.NORTH, NORTH);
		map.put(Direction.SOUTH, SOUTH);
		map.put(Direction.EAST, EAST);
		map.put(Direction.WEST, WEST);
		map.put(Direction.UP, UP);
		map.put(Direction.DOWN, DOWN);
		return map;
	}

	/** 连接臂的粗细（半宽，单位：格）。 */
	protected final double thickness;

	protected AbstractConnectionBlock(Properties properties, double thickness) {
		super(properties);
		this.thickness = thickness;
		BlockState state = this.stateDefinition.any();
		for (Direction side : Direction.values()) {
			state = state.setValue(property(side), false);
		}
		this.registerDefaultState(state);
	}

	public static BooleanProperty property(Direction side) {
		return PROPERTY_MAP.get(side);
	}

	/** 该方向是否伸出连接臂。 */
	public static boolean isConnected(BlockState state, Direction side) {
		BooleanProperty property = property(side);
		return state.hasProperty(property) && state.getValue(property);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
	}

	/** 由六向连接位拼出形状：中心方块 + 各方向伸展的臂。 */
	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		VoxelShape shape = Block.box(centerLo(), centerLo(), centerLo(), centerHi(), centerHi(), centerHi());
		for (Direction side : Direction.values()) {
			if (isConnected(state, side)) {
				shape = Shapes.or(shape, armShape(side));
			}
		}
		return shape;
	}

	private double centerLo() {
		return (0.5 - thickness) * 16.0;
	}

	private double centerHi() {
		return (0.5 + thickness) * 16.0;
	}

	private VoxelShape armShape(Direction side) {
		double a = centerLo();
		double b = centerHi();
		return switch (side) {
			case NORTH -> Block.box(a, a, 0.0, b, b, a);
			case SOUTH -> Block.box(a, a, b, b, b, 16.0);
			case WEST -> Block.box(0.0, a, a, a, b, b);
			case EAST -> Block.box(b, a, a, 16.0, b, b);
			case DOWN -> Block.box(a, 0.0, a, b, a, b);
			case UP -> Block.box(a, b, a, b, 16.0, b);
		};
	}

	public static Properties cableProperties() {
		return Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(0.6F).sound(SoundType.COPPER).noOcclusion();
	}
}
