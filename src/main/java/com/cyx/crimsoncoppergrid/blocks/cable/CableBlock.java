package com.cyx.crimsoncoppergrid.blocks.cable;

import java.util.EnumMap;
import java.util.Map;

import com.cyx.crimsoncoppergrid.blockentity.cable.CableBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * 铜制电线。方块结构与命名对齐 TechReborn 的 {@code CableBlock}（MIT）。
 *
 * <h2>职责边界（这是与旧实现最本质的区别）</h2>
 * 方块本身只管三件事：<b>声明六向连接属性</b>、<b>给出形状</b>、<b>把邻居变化转告方块实体</b>。
 * 「谁该连谁」的判定与写回全部交给 {@link CableBlockEntity}，
 * 这样连接状态在图里和在能量传输里只可能有一份真相，不会出现两套口径打架。
 *
 * <h2>为什么必须覆写 {@link #neighborChanged}</h2>
 * 旧实现（v0.0.5）只在放置与移除时显式通知六邻，而「相邻电线不连」的表现说明了那条路径不够。
 * 这里改成由 MC 自己驱动的标准通道：任何邻居变化都会走到本方法，转告方块实体把目标缓存作废，
 * 下一 tick 重算连接位。这同时覆盖了放置、破坏、开关切换、区块载入等所有情形。
 *
 * <p>关于 v0.0.5 遗留的「坐标对、方块名对，但 {@code instanceof CableBlock} 为 false」——
 * 这在普通 Java 语义下不可能成立，只可能是同一个类被加载了两份（多半是搬家后未清理的编译产物）。
 * {@link CableBlockEntity} 的能量判定因此改走 Fabric 官方的能量查阅表，
 * 按注册的 {@code BlockEntityType} 对象分派，不依赖任何类的身份。
 */
public class CableBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
	public static final BooleanProperty EAST = BlockStateProperties.EAST;
	public static final BooleanProperty WEST = BlockStateProperties.WEST;
	public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
	public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
	public static final BooleanProperty UP = BlockStateProperties.UP;
	public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

	public static final Map<Direction, BooleanProperty> PROPERTY_MAP = buildPropertyMap();

	private static Map<Direction, BooleanProperty> buildPropertyMap() {
		Map<Direction, BooleanProperty> map = new EnumMap<>(Direction.class);
		map.put(Direction.EAST, EAST);
		map.put(Direction.WEST, WEST);
		map.put(Direction.NORTH, NORTH);
		map.put(Direction.SOUTH, SOUTH);
		map.put(Direction.UP, UP);
		map.put(Direction.DOWN, DOWN);
		return map;
	}

	/** 导线粗细的一半（单位：格）。1/16 格就是「一格 = 16 像素，线宽 2 像素」。 */
	private final double cableThickness;

	public CableBlock(Properties properties) {
		this(properties, 1.0 / 16.0);
	}

	protected CableBlock(Properties properties, double cableThickness) {
		super(properties);
		this.cableThickness = cableThickness;

		BlockState state = this.stateDefinition.any();
		for (Direction side : Direction.values()) {
			state = state.setValue(property(side), false);
		}
		registerDefaultState(state.setValue(WATERLOGGED, false));
	}

	// ------------------------------------------------------------ 属性访问

	public static BooleanProperty property(Direction side) {
		return PROPERTY_MAP.get(side);
	}

	/** 该方向是否伸出连接臂。取属性前先判存在，避免对没有这些属性的方块取值抛异常。 */
	public static boolean isConnected(BlockState state, Direction side) {
		BooleanProperty property = property(side);
		return state.hasProperty(property) && state.getValue(property);
	}

	public double getCableThickness() {
		return cableThickness;
	}

	// ------------------------------------------------------------ BaseEntityBlock

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CableBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		// 客户端不跑导线逻辑；需要的状态由方块实体同步包送到
		return level.isClientSide() ? null : CableBlockEntity.ticker();
	}

	// ------------------------------------------------------------ 方块状态

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(EAST, WEST, NORTH, SOUTH, UP, DOWN, WATERLOGGED);
	}

	@Override
	public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
		return defaultBlockState().setValue(WATERLOGGED,
				context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
	}

	// ------------------------------------------------------------ 邻居变化

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
			@Nullable Orientation orientation, boolean movedByPiston) {
		// 让方块实体作废目标缓存，下一 tick 重算连接位与能量目标
		if (level.getBlockEntity(pos) instanceof CableBlockEntity cable) {
			cable.neighborUpdate();
		}
		super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
	}

	// ------------------------------------------------------------ 形状

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return CableShapeUtil.getShape(state);
	}

	@Override
	protected VoxelShape getOcclusionShape(BlockState state) {
		return CableShapeUtil.getShape(state);
	}

	// ------------------------------------------------------------ 含水

	@Override
	public BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess tickAccess,
			BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		if (state.getValue(WATERLOGGED) && level instanceof LevelAccessor accessor) {
			accessor.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
		return super.updateShape(state, level, tickAccess, pos, direction, neighborPos, neighborState, random);
	}

	@Override
	public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
		return SimpleWaterloggedBlock.super.placeLiquid(level, pos, state, fluidState);
	}

	@Override
	public boolean canPlaceLiquid(@Nullable LivingEntity player, BlockGetter level,
			BlockPos pos, BlockState state, Fluid fluid) {
		return SimpleWaterloggedBlock.super.canPlaceLiquid(player, level, pos, state, fluid);
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	// ------------------------------------------------------------ 外观设置

	/** 电线与电闸共用的外观：铜色、薄、不挡视线。 */
	public static Properties cableProperties() {
		return Properties.of()
				.mapColor(MapColor.COLOR_ORANGE)
				.strength(0.8F)
				.sound(SoundType.COPPER)
				.noOcclusion();
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		// 自己没了，六邻的连接状态需要重算 —— 逐格通知，让它们的 neighborChanged 生效
		for (Direction side : Direction.values()) {
			BlockPos neighbor = pos.relative(side);
			if (level.isLoaded(neighbor)) {
				level.neighborChanged(neighbor, state.getBlock(), null);
			}
		}
	}
}
