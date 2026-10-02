package com.cyx.crimsoncoppergrid.blocks.cable;

import com.cyx.crimsoncoppergrid.init.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * 电闸。
 *
 * <p>{@code POWERED} 为 true 时等同于一段电线，false 时断路；红石可以远程控制它。
 * 切换状态后会让六邻的电线立刻重算连接形状。
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

	/** 是否导通（开启状态）。 */
	public static boolean isOpen(BlockState state) {
		return state.hasProperty(POWERED) && state.getValue(POWERED);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		boolean next = !isOpen(state);
		level.setBlockAndUpdate(pos, state.setValue(POWERED, next));
		level.playSound(null, pos, next ? SoundEvents.LEVER_CLICK : SoundEvents.STONE_BUTTON_CLICK_OFF,
				SoundSource.BLOCKS, 0.4F, next ? 0.7F : 0.5F);
		refreshNeighbors(level, pos);
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, Orientation orientation, boolean movedByPiston) {
		super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
		if (level.isClientSide()) {
			return;
		}
		boolean signal = level.hasNeighborSignal(pos);
		if (signal != isOpen(state)) {
			level.setBlockAndUpdate(pos, state.setValue(POWERED, signal));
		}
		refreshNeighbors(level, pos);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		refreshNeighbors(level, pos);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		refreshNeighbors(level, pos);
	}

	/** 让六邻的电线立刻重算连接形状。 */
	public static void refreshNeighbors(Level level, BlockPos pos) {
		if (!(level instanceof ServerLevel)) {
			return;
		}
		for (Direction side : Direction.values()) {
			BlockPos neighbor = pos.relative(side);
			if (level.isLoaded(neighbor) && level.getBlockState(neighbor).getBlock() == ModBlocks.CABLE) {
				CableBlock.refresh(level, neighbor);
			}
		}
	}

	public static Properties switchProperties() {
		return Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(1.0F).sound(SoundType.COPPER);
	}
}
