package com.cyx.crimsoncoppergrid.blocks;

import com.cyx.crimsoncoppergrid.blockentity.LavaGeneratorBlockEntity;
import com.cyx.crimsoncoppergrid.common.blocks.BlockMachineBase;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 电力岩浆机。
 *
 * <p>右键交互：拿岩浆桶倒进去、拿空桶舀出来、空手查看当前岩浆量。
 */
public class LavaGeneratorBlock extends BlockMachineBase {

	public LavaGeneratorBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new LavaGeneratorBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof LavaGeneratorBlockEntity machine)) {
			return InteractionResult.PASS;
		}
		boolean lavaBucket = stack.is(Items.LAVA_BUCKET);
		boolean emptyBucket = stack.is(Items.BUCKET);
		if (!lavaBucket && !emptyBucket) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		ItemStack replacement = lavaBucket ? machine.drainBucket() : machine.fillBucket();
		if (replacement.isEmpty()) {
			player.sendSystemMessage(Component.translatable(lavaBucket
					? "block.crimsoncoppergrid.lava_generator.tank_full"
					: "block.crimsoncoppergrid.lava_generator.tank_empty").withStyle(ChatFormatting.RED));
			return InteractionResult.SUCCESS_SERVER;
		}

		if (!player.hasInfiniteMaterials()) {
			stack.shrink(1);
			if (stack.isEmpty()) {
				player.setItemInHand(hand, replacement);
			} else if (!player.getInventory().add(replacement)) {
				player.drop(replacement, false, net.minecraft.util.Prediction.SERVER_ONLY);
			}
		}
		level.playSound(null, pos, lavaBucket ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_FILL_LAVA,
				SoundSource.BLOCKS, 1.0F, 1.0F);
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
			BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (level.getBlockEntity(pos) instanceof LavaGeneratorBlockEntity machine) {
			player.sendSystemMessage(Component.translatable("block.crimsoncoppergrid.lava_generator.status",
					machine.getLavaMb(), machine.getTankCapacityMb()).withStyle(ChatFormatting.GOLD));
		}
		return InteractionResult.SUCCESS_SERVER;
	}
}
