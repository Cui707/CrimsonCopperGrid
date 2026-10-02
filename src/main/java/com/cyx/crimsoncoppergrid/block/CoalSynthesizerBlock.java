package com.cyx.crimsoncoppergrid.block;

import com.cyx.crimsoncoppergrid.block.entity.CoalSynthesizerBlockEntity;
import com.cyx.crimsoncoppergrid.init.ModBlockEntities;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 电力煤炭合成机。
 *
 * <p>侧面右键可以查看进度与已存煤炭；产出的煤会自动尝试送进相邻容器，
 * 送不进去就攒在机器里等玩家来取。
 */
public class CoalSynthesizerBlock extends BaseEntityBlock {
	public CoalSynthesizerBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CoalSynthesizerBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide()) {
			return null;
		}
		return AbstractGeneratorBlock.createTicker(type, ModBlockEntities.COAL_SYNTHESIZER, CoalSynthesizerBlockEntity::serverTick);
	}

	/**
	 * 手持物品右键 = 把攒下的煤炭全部收进背包（以物品形式拿走产出）。
	 */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
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

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (level.getBlockEntity(pos) instanceof CoalSynthesizerBlockEntity machine) {
			player.sendSystemMessage(Component.translatable("block.crimsoncoppergrid.coal_synthesizer.status",
					machine.getCoal(),
					machine.getEnergyStored() * 100 / CoalSynthesizerBlockEntity.FE_PER_COAL).withStyle(ChatFormatting.GOLD));
		}
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		markGrid(level, pos);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		markGrid(level, pos);
	}

	static void markGrid(Level level, BlockPos pos) {
		if (level instanceof ServerLevel serverLevel) {
			com.cyx.crimsoncoppergrid.energy.GridRegistry.get(serverLevel).markDirty(pos);
		}
	}

	public static Properties machineProperties() {
		return Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(2.0F).sound(SoundType.COPPER);
	}

	/**
	 * 把产出的煤送进相邻容器之一。
	 *
	 * @return 是否成功送出
	 */
	public static boolean ejectToNeighbor(Level level, BlockPos pos, ItemStack stack) {
		if (stack.isEmpty() || !(level instanceof ServerLevel serverLevel)) {
			return false;
		}
		for (Direction side : Direction.values()) {
			BlockPos target = pos.relative(side);
			if (!level.isLoaded(target)) {
				continue;
			}
			if (!(serverLevel.getBlockEntity(target) instanceof Container container)) {
				continue;
			}
			for (int slot = 0; slot < container.getContainerSize(); slot++) {
				if (!container.canPlaceItem(slot, stack)) {
					continue;
				}
				ItemStack existing = container.getItem(slot);
				if (existing.isEmpty()) {
					container.setItem(slot, stack.copy());
					container.setChanged();
					return true;
				}
				if (ItemStack.isSameItemSameComponents(existing, stack)
						&& existing.getCount() + stack.getCount() <= existing.getMaxStackSize()) {
					existing.grow(stack.getCount());
					container.setChanged();
					return true;
				}
			}
		}
		return false;
	}
}
