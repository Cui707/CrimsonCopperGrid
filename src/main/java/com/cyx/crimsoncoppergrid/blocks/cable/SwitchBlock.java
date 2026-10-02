package com.cyx.crimsoncoppergrid.blocks.cable;

import java.util.IdentityHashMap;
import java.util.Map;

import com.cyx.crimsoncoppergrid.blockentity.cable.CableBlockEntity;
import com.cyx.crimsoncoppergrid.blockentity.cable.SwitchBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * 铜制电闸：可以手动或由红石控制的一段「可断开的导线」。
 *
 * <p>它继承 {@link CableBlock}，因此六向连接属性、形状、含水这些行为完全复用，
 * 只多两个属性。真正的通断语义在 {@link SwitchBlockEntity#conducts()} ——
 * 断开时组网会在此截断。
 *
 * <h2>两个状态属性</h2>
 * <ul>
 *   <li>{@code POWERED}：true 表示**导通**（合闸），false 表示断开。放置时默认合闸，
 *       若旁边已有红石信号则直接以红石状态为准。</li>
 *   <li>{@code FACING}：水平朝向，指向放置者。模型用它把拉杆手柄摆到正确的一侧 ——
 *       合闸时手柄倒向放置者、断开时倒向背侧，和外形的拉杆是同一套读法。</li>
 * </ul>
 *
 * <p>外观上它是一个「导线中枢 + 安装面板 + 拉杆手柄」的组合体，
 * 也就是现实里电线上那种在线开关的样子；它仍然是一段导线，只是多了个能扳的手柄。
 */
public class SwitchBlock extends CableBlock {
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	/** 26.3 起没有 {@code DirectionProperty} 了，方向类属性统一是 {@code EnumProperty<Direction>}。 */
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	public SwitchBlock(Properties properties) {
		super(properties, 1.0 / 16.0);
		// super 的构造器已经登记过一次默认状态（那时 POWERED 取到的是 false），
		// 这里再登记一次把默认值改成「合闸」，并补上朝向。
		registerDefaultState(this.stateDefinition.any()
				.setValue(POWERED, true)
				.setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(POWERED, FACING);
	}

	/** 是否导通（合闸）。 */
	public static boolean isOpen(BlockState state) {
		return state.hasProperty(POWERED) && state.getValue(POWERED);
	}

	// ------------------------------------------------------------ 形状

	/**
	 * 面板 + 拉杆所占据的那一块体积（单位 1/16 格）。
	 *
	 * <p>它必须并进选取形状里，否则会出现「看得见、点不着」——方块模型可以画到多远，
	 * 玩家能点到的范围是由形状决定的，两者长期不一致会让人以为方块坏了。
	 * 盒子对居于方块中心；z 方向比 x 宽一格，因为手柄是沿 z 轴倾倒的，
	 * 扳到两边时杆头会伸到 z 的 ±4 附近。
	 */
	private static final VoxelShape SWITCH_BODY = Block.box(5.0, 6.0, 4.0, 11.0, 16.0, 12.0);

	/** 并集结果按方块状态缓存：形状查询每帧都在跑，不能每次现拼。 */
	private static final Map<BlockState, VoxelShape> SHAPE_CACHE = new IdentityHashMap<>();

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE_CACHE.computeIfAbsent(state,
				s -> Shapes.or(CableShapeUtil.getShape(s), SWITCH_BODY));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SwitchBlockEntity(pos, state);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		if (state == null) {
			return null;
		}
		// 放下时如果已经在红石信号范围内，直接以红石为准，避免出现「一放下就被红石改状态」的抖动。
		// 朝向取「玩家视线的反向」，也就是由方块指向放置者 —— 和原版拉杆的做法一致，
		// 这样模型里那个「合闸倒向正面」的手柄就总是倒向放置的人。
		return state
				.setValue(POWERED, !context.getLevel().hasNeighborSignal(context.getClickedPos()))
				.setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	// ------------------------------------------------------------ 手动操作

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		boolean next = !isOpen(state);
		level.setBlockAndUpdate(pos, state.setValue(POWERED, next));
		level.playSound(null, pos,
				next ? SoundEvents.LEVER_CLICK : SoundEvents.STONE_BUTTON_CLICK_OFF,
				SoundSource.BLOCKS, 0.4F, next ? 0.7F : 0.5F);

		// 通断变化会改变组网：自身要重算连接显示并作废能量目标缓存
		if (level.getBlockEntity(pos) instanceof CableBlockEntity cable) {
			cable.neighborUpdate();
		}
		return InteractionResult.SUCCESS_SERVER;
	}

	// ------------------------------------------------------------ 红石

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
			@Nullable Orientation orientation, boolean movedByPiston) {
		// 先跟随红石，再走父类的「通知方块实体重算」
		if (!level.isClientSide()) {
			boolean powered = !level.hasNeighborSignal(pos);
			if (powered != isOpen(state)) {
				level.setBlockAndUpdate(pos, state.setValue(POWERED, powered));
			}
		}
		super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
	}

	/** 电闸与电线外观一致，只是厚一点，让它在视觉上更容易被认出来。 */
	public static Properties switchProperties() {
		return CableBlock.cableProperties().strength(1.0F);
	}

	/** 保持与父类一致：让六邻在本方块被移除后重算连接。 */
	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level,
			BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		for (Direction side : Direction.values()) {
			BlockPos neighbor = pos.relative(side);
			if (level.isLoaded(neighbor)) {
				level.neighborChanged(neighbor, state.getBlock(), null);
			}
		}
	}
}
