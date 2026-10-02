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
 * <p>手持可烧炼的东西右键 = 往输入槽快捷放料；空手右键 = 打开界面（见
 * {@link BlockMachineBase#useWithoutItem}，料也能在界面里放）。
 *
 * <h2>为什么「放不进去」时要返回 {@code TRY_WITH_EMPTY_HAND} 而不是 {@code PASS}</h2>
 * 26.3 的方块交互是这样的（客户端 {@code MultiPlayerGameMode#performUseItemOn}
 * 与服务端 {@code ServerPlayerGameMode#useItemOn} 完全对称）：
 * <pre>
 * result = state.useItemOn(...)                 // 空手也会调用！传进去的是空栈
 * if (result.consumesAction()) return result;
 * if (result instanceof TryEmptyHandInteraction) {
 *     return state.useWithoutItem(...)          // ← 开界面走这里
 * }
 * </pre>
 * 也就是说 {@code useItemOn} 是「第一道闸」，它必须把「我不处理，请接着走空手分支」
 * 这件事<b>明确说出来</b>——那就是 {@code TRY_WITH_EMPTY_HAND}，
 * 也正是 {@code BlockBehaviour} 的默认返回值。
 * 若这里返回 {@code PASS}，会被当成「已经表过态、不用再往下走」，
 * {@code useWithoutItem} 永远不会被调用，界面自然打不开。
 * （{@code BlockBehaviour.useWithoutItem} 的默认值才是不再继续的 {@code PASS}，两者语义不同，很容易搞混。）
 */
public class ElectricFurnaceBlock extends BlockMachineBase {

	public ElectricFurnaceBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ElectricFurnaceBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		// 空手，或这里根本不是电力熔炉 —— 交给空手分支（那里会开界面）
		if (stack.isEmpty() || !(level.getBlockEntity(pos) instanceof ElectricFurnaceBlockEntity machine)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}

		ItemStack input = machine.getItem(ElectricFurnaceBlockEntity.SLOT_INPUT);
		int space = input.isEmpty() ? stack.getMaxStackSize() : input.getMaxStackSize() - input.getCount();
		boolean sameItem = input.isEmpty() || ItemStack.isSameItemSameComponents(input, stack);
		// 塞不进去（槽里是别的东西，或已经满了）也退回空手分支把界面打开，
		// 免得玩家举着一堆东西点半天毫无反应。
		if (space <= 0 || !sameItem) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}

		// 客户端必须与服务端得出同一个结论，否则会出现「客户端以为放好了、服务端没放」。
		// 所以判定全部放在 isClientSide 分支之前。
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
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
