package com.cyx.crimsoncoppergrid.common.blocks;

import com.cyx.crimsoncoppergrid.common.blockentity.MachineBaseBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 所有「机器类方块」的公共父类。分层与命名对齐 TechReborn / RebornCore 的
 * {@code BlockMachineBase}（MIT）。
 *
 * <p>统一提供两组方块状态：
 * <ul>
 *   <li>{@code FACING} —— 水平朝向，放置时朝玩家，支持旋转与镜像；</li>
 *   <li>{@code ACTIVE} —— 是否正在工作。发电机的「在发电」、熔炉的「在烧炼」都用它驱动贴图。</li>
 * </ul>
 *
 * <p>注意 {@code FACING} 的取值是「机器正面朝向」，也就是**放置时玩家朝向的反面**，
 * 这样机器放下后正面朝着玩家。
 */
public abstract class BlockMachineBase extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

	/**
	 * 全模组机器方块的通用外观：铜色、铜的音效。
	 *
	 * <p>放在基类里而不是各个方块类里，是为了让「所有机器长得像一套东西」
	 * 这件事只有一个修改点；将来要按机器档次分材质时，也在这里分流。
	 */
	public static Properties machineProperties() {
		return Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(2.0F).sound(SoundType.COPPER);
	}

	protected BlockMachineBase(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any()
				.setValue(FACING, Direction.NORTH)
				.setValue(ACTIVE, false));
	}

	// ------------------------------------------------------------ 方块状态访问

	public Direction getFacing(BlockState state) {
		return state.getValue(FACING);
	}

	public boolean isActive(BlockState state) {
		return state.getValue(ACTIVE);
	}

	/** 写回朝向。用 {@code setBlockAndUpdate} 以便同步与触发邻居更新。 */
	public void setFacing(Direction facing, Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() != this) {
			return;
		}
		level.setBlockAndUpdate(pos, state.setValue(FACING, facing));
	}

	/**
	 * 写回工作状态。
	 *
	 * <p>用 {@code setBlock}（不带邻居更新）而不是 {@code setBlockAndUpdate}：
	 * 工作是每刻都在变的状态，邻居不需要为此重新计算任何东西，省掉一轮邻居更新。
	 */
	public void setActive(boolean active, Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() != this || state.getValue(ACTIVE) == active) {
			return;
		}
		level.setBlock(pos, state.setValue(ACTIVE, active), Block.UPDATE_ALL);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, ACTIVE);
	}

	// ------------------------------------------------------------ 放置与旋转

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	// ------------------------------------------------------------ 生命周期钩子

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level.getBlockEntity(pos) instanceof MachineBaseBlockEntity machine) {
			machine.onPlace(level, pos, state, placer, stack);
		}
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (level.getBlockEntity(pos) instanceof MachineBaseBlockEntity machine) {
			machine.onBreak(level, pos, state, player);
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	// ------------------------------------------------------------ tick

	/**
	 * 默认只在服务端 tick。
	 *
	 * <p>客户端不需要跑机器逻辑 —— 界面与贴图所需的数据通过方块实体同步包送到。
	 */
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : MachineBaseBlockEntity.ticker();
	}

	// ------------------------------------------------------------ 界面

	/**
	 * 方块实体若实现了 {@link MenuProvider}，空手右键即可打开它的界面。
	 *
	 * <p>放在基类而不是各个方块里，是为了让「所有机器都能右键打开」只有一个修改点：
	 * 将来新加的机器只要方块实体实现 {@code MenuProvider}，界面就自动有了。
	 */
	@Override
	protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
		BlockEntity blockEntity = level.getBlockEntity(pos);
		return blockEntity instanceof MenuProvider provider ? provider : null;
	}

	/**
	 * 空手右键：能开界面就开界面，否则什么也不做。
	 *
	 * <p>26.3 的交互分发是「主手有物品 -> {@code useItemOn}」「主手空 -> 这里」，
	 * 两者互斥，所以「手持物品放料」与「空手开界面」可以共存而不打架。
	 */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
			BlockHitResult hit) {
		MenuProvider provider = getMenuProvider(state, level, pos);
		if (provider == null) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			player.openMenu(provider);
		}
		return InteractionResult.SUCCESS;
	}

	// ------------------------------------------------------------ 比较器

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity instanceof Container container) {
			return AbstractContainerMenu.getRedstoneSignalFromContainer(container);
		}
		return 0;
	}
}
