package com.cyx.crimsoncoppergrid.block;

import com.cyx.crimsoncoppergrid.energy.GridRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 电闸 / 开关。
 *
 * <p>{@code POWERED} 为 true 时它等同于一段电线，false 时是断路。红石可以远程控制它，
 * 所以它天然也能当「红石控制的电网总闸」用。
 */
public class SwitchBlock extends Block {
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

	public SwitchBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(POWERED, true));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(POWERED);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		boolean next = !state.getValue(POWERED);
		level.setBlock(pos, state.setValue(POWERED, next), Block.UPDATE_ALL);
		level.playSound(null, pos, next ? SoundEvents.LEVER_CLICK : SoundEvents.STONE_BUTTON_CLICK_OFF, SoundSource.BLOCKS, 0.4F, next ? 0.7F : 0.5F);
		if (level instanceof ServerLevel serverLevel) {
			GridRegistry registry = GridRegistry.get(serverLevel);
			registry.markDirty(pos);
			registry.refreshAround(pos);
		}
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, Orientation orientation, boolean movedByPiston) {
		super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
		if (level.isClientSide()) {
			return;
		}
		boolean signal = level.hasNeighborSignal(pos);
		if (signal != state.getValue(POWERED)) {
			level.setBlock(pos, state.setValue(POWERED, signal), Block.UPDATE_ALL);
			if (level instanceof ServerLevel serverLevel) {
				GridRegistry registry = GridRegistry.get(serverLevel);
				registry.markDirty(pos);
				registry.refreshAround(pos);
			}
		}
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if (level instanceof ServerLevel serverLevel) {
			GridRegistry.get(serverLevel).markDirty(pos);
		}
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		GridRegistry.get(level).markDirty(pos);
	}

	public static Properties switchProperties() {
		return Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(1.0F).sound(SoundType.COPPER);
	}

	public static boolean isConductor(BlockState state) {
		// 必须先判断方块类型：POWERED 是电闸独有的属性，
		// 对草方块之类的状态直接 getValue 会抛 IllegalArgumentException
		// （这个 bug 是在实机点电线时被抓到的，见 README 的 0.0.4 说明）。
		return state.hasProperty(POWERED) && state.getValue(POWERED);
	}
}
