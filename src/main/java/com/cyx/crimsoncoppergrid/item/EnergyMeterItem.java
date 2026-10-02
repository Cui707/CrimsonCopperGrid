package com.cyx.crimsoncoppergrid.item;

import com.cyx.crimsoncoppergrid.energy.Grid;
import com.cyx.crimsoncoppergrid.energy.GridRegistry;
import com.cyx.crimsoncoppergrid.init.ModBlocks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 电网总览器：右键任何接电方块，报出所在电网的发电能力、电池余量与设备数量。
 *
 * <p>这是「电力控制器」的第一版形态。做成手持物品而不是方块 GUI，是因为它能在
 * 不增加界面代码量的前提下先把「看得见电网」这件事跑通；等到电网规模需要
 * 常驻监控面板时，再把同样的数据接进方块 GUI。
 */
public class EnergyMeterItem extends Item {
	public EnergyMeterItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (!(context.getPlayer() instanceof ServerPlayer player) || !(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.PASS;
		}

		BlockPos pos = context.getClickedPos();
		GridRegistry registry = GridRegistry.get(serverLevel);
		Grid grid = registry.gridAt(pos);

		if (grid == null && isCable(level.getBlockState(pos))) {
			// 落点正好是电线：从它出发找一次
			registry.markDirty(pos);
		}

		if (grid == null) {
			player.sendSystemMessage(Component.translatable("item.crimsoncoppergrid.energy_meter.no_grid")
					.withStyle(ChatFormatting.GRAY));
			return InteractionResult.SUCCESS_SERVER;
		}

		long[] totals = grid.batteryTotals();
		long maxOutput = grid.totalMaxOutput();
		player.sendSystemMessage(Component.translatable("item.crimsoncoppergrid.energy_meter.header")
				.withStyle(ChatFormatting.GOLD));
		player.sendSystemMessage(Component.translatable("item.crimsoncoppergrid.energy_meter.devices",
				grid.deviceCount(), grid.cables().size()).withStyle(ChatFormatting.AQUA));
		player.sendSystemMessage(Component.translatable("item.crimsoncoppergrid.energy_meter.generation",
				maxOutput).withStyle(ChatFormatting.GREEN));
		player.sendSystemMessage(Component.translatable("item.crimsoncoppergrid.energy_meter.storage",
				totals[0], totals[1]).withStyle(ChatFormatting.YELLOW));
		return InteractionResult.SUCCESS_SERVER;
	}

	private static boolean isCable(BlockState state) {
		return state.getBlock() == ModBlocks.CABLE || state.getBlock() == ModBlocks.SWITCH;
	}
}
