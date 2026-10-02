package com.cyx.crimsoncoppergrid.item;

import com.cyx.crimsoncoppergrid.blockentity.cable.CableBlockEntity;
import com.cyx.crimsoncoppergrid.blockentity.cable.CableTickManager;
import com.cyx.crimsoncoppergrid.common.powerSystem.PowerAcceptorBlockEntity;
import com.cyx.crimsoncoppergrid.common.powerSystem.PowerSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * 电网总览器：右键任何接电设备或电线，报出它当前的能量状况。
 *
 * <p>这是「电力控制器」的第一版形态。做成手持物品而不是方块 GUI，是因为它能在
 * 不增加界面代码量的前提下先把「看得见电网」这件事跑通；等到电网规模需要常驻
 * 监控面板时，再把同样的数据接进方块 GUI。
 *
 * <h2>数据来源已经换成新架构</h2>
 * 它读的是各设备自己的能量缓冲（{@link PowerAcceptorBlockEntity#getStored()}）
 * 与导线网络的池内电量，不再依赖任何全局电网对象 ——
 * 后者在改成 Team Reborn Energy 的推流模型后已经不存在了，也不需要存在：
 * 「电网」现在就是电线和设备本身。
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
		if (reportDevice(player, serverLevel, pos) || reportNetwork(player, serverLevel, pos)) {
			return InteractionResult.SUCCESS_SERVER;
		}

		player.sendSystemMessage(Component.translatable("item.crimsoncoppergrid.energy_meter.unknown")
				.withStyle(ChatFormatting.GRAY));
		return InteractionResult.SUCCESS_SERVER;
	}

	/** 对着机器右键：报出存量、容量、档位与收发速率。 */
	private static boolean reportDevice(ServerPlayer player, ServerLevel level, BlockPos pos) {
		if (!(level.getBlockEntity(pos) instanceof PowerAcceptorBlockEntity machine)) {
			return false;
		}

		player.sendSystemMessage(Component.translatable("item.crimsoncoppergrid.energy_meter.device_header",
				level.getBlockState(pos).getBlock().getName()).withStyle(ChatFormatting.GOLD));
		player.sendSystemMessage(Component.translatable("item.crimsoncoppergrid.energy_meter.device_stored",
				PowerSystem.getLocalizedPower(machine.getStored()),
				PowerSystem.getLocalizedPower(machine.getMaxStoredPower())).withStyle(ChatFormatting.AQUA));
		player.sendSystemMessage(Component.translatable("item.crimsoncoppergrid.energy_meter.device_io",
				machine.getTier().name(),
				machine.getMaxInput(null),
				machine.getMaxOutput(null)).withStyle(ChatFormatting.YELLOW));
		return true;
	}

	/**
	 * 对着电线（或电闸）右键：报出它所在网络的规模与池内电量。
	 *
	 * <p>注意这里统计的是「池子里的存量」，不是「这一瞬间流过的功率」——
	 * 推流模型下没有中心结算器来汇总瞬时功率，而池内存量恰好是玩家最直观的观测量：
	 * 它涨说明在存电，它降说明在放电。
	 */
	private static boolean reportNetwork(ServerPlayer player, ServerLevel level, BlockPos pos) {
		if (!(level.getBlockEntity(pos) instanceof CableBlockEntity)) {
			return false;
		}

		CableTickManager.NetworkSummary summary = CableTickManager.summarize(level, pos);
		if (summary == null) {
			player.sendSystemMessage(Component.translatable("item.crimsoncoppergrid.energy_meter.network_broken")
					.withStyle(ChatFormatting.GRAY));
			return true;
		}

		player.sendSystemMessage(Component.translatable("item.crimsoncoppergrid.energy_meter.network_header")
				.withStyle(ChatFormatting.GOLD));
		player.sendSystemMessage(Component.translatable("item.crimsoncoppergrid.energy_meter.network_size",
				summary.cables(),
				PowerSystem.getLocalizedPower(summary.stored()),
				PowerSystem.getLocalizedPower(summary.capacity())).withStyle(ChatFormatting.AQUA));
		return true;
	}
}
