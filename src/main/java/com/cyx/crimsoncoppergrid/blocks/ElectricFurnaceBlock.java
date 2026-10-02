package com.cyx.crimsoncoppergrid.blocks;

import com.cyx.crimsoncoppergrid.blockentity.ElectricFurnaceBlockEntity;
import com.cyx.crimsoncoppergrid.common.blocks.BlockMachineBase;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 电力熔炉：用电烧炼原版熔炉配方。
 *
 * <p>手持物品右键 = 往输入槽快捷放料；空手右键 = 打开界面（见
 * {@link BlockMachineBase#useWithoutItem}，料也能在界面里放）。
 */
public class ElectricFurnaceBlock extends BlockMachineBase {

	public ElectricFurnaceBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ElectricFurnaceBlockEntity(pos, state);
	}

	/**
	 * 手持物品右键 = 往输入槽放东西。
	 *
	 * <p>26.3 的交互判定是「主手有物品 -> useItemOn」「主手空 -> useWithoutItem」，
	 * 两者不会同时触发，所以这里只管插入、状态提示交给 {@link #useWithoutItem}。
	 */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (stack.isEmpty()) {
			return InteractionResult.PASS;
		}
		if (!(level.getBlockEntity(pos) instanceof ElectricFurnaceBlockEntity machine)) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		ItemStack input = machine.getItem(ElectricFurnaceBlockEntity.SLOT_INPUT);
		int space = input.isEmpty() ? stack.getMaxStackSize() : input.getMaxStackSize() - input.getCount();
		boolean sameItem = input.isEmpty() || ItemStack.isSameItemSameComponents(input, stack);
		if (space <= 0 || !sameItem) {
			return InteractionResult.PASS;
		}
		int moved = Math.min(space, stack.getCount());
		if (input.isEmpty()) {
			machine.setItem(ElectricFurnaceBlockEntity.SLOT_INPUT, stack.copyWithCount(moved));
		} else {
			input.grow(moved);
			machine.setChanged();
		}
		if (!player.hasInfiniteMaterials()) {
			stack.shrink(moved);
		}
		return InteractionResult.SUCCESS_SERVER;
	}
}
