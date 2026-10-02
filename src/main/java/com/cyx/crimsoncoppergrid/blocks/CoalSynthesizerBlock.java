package com.cyx.crimsoncoppergrid.blocks;

import com.cyx.crimsoncoppergrid.blockentity.CoalSynthesizerBlockEntity;
import com.cyx.crimsoncoppergrid.common.blocks.BlockMachineBase;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 电力煤炭合成机。
 *
 * <p>手持物品右键 = 把攒下的煤炭全部收进背包；空手右键 = 查看当前进度。
 * 产出的煤也会每 20 刻自动尝试送进相邻容器，送不进去就攒着等玩家来取。
 */
public class CoalSynthesizerBlock extends BlockMachineBase {

	public CoalSynthesizerBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CoalSynthesizerBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		return takeCoal(level, pos, player);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
			BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (level.getBlockEntity(pos) instanceof CoalSynthesizerBlockEntity machine) {
			int percent = (int) (machine.getStored() * 100 / CoalSynthesizerBlockEntity.FE_PER_COAL);
			player.sendSystemMessage(Component.translatable("block.crimsoncoppergrid.coal_synthesizer.status",
					machine.getCoal(), percent).withStyle(ChatFormatting.GOLD));
		}
		return InteractionResult.SUCCESS_SERVER;
	}

	/** 把机器里攒的煤全部塞给玩家。 */
	private static InteractionResult takeCoal(Level level, BlockPos pos, Player player) {
		if (!(level.getBlockEntity(pos) instanceof CoalSynthesizerBlockEntity machine) || machine.getCoal() <= 0) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		ItemStack produced = machine.extractCoal();
		if (!produced.isEmpty() && !player.getInventory().add(produced)) {
			player.drop(produced, false, net.minecraft.util.Prediction.SERVER_ONLY);
		}
		return InteractionResult.SUCCESS_SERVER;
	}
}
