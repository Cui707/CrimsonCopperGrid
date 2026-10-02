package com.cyx.crimsoncoppergrid.client.screen;

import com.cyx.crimsoncoppergrid.common.menu.BatteryMenu;
import com.cyx.crimsoncoppergrid.common.powerSystem.PowerSystem;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 电池界面：一块电量表。
 *
 * <p>从左到右的「正在充电 / 正在放电 / 待机」提示读的是菜单里同步过来的净流量 ——
 * 推流模型下没有中心调度器可以问「谁在耗电」，但设备自己的存量变化就是最直接的答案。
 */
public class BatteryScreen extends MachineScreen<BatteryMenu> {

	private static final int BAR_X = 28;
	private static final int BAR_Y = 30;
	private static final int BAR_WIDTH = 120;
	private static final int BAR_HEIGHT = 22;
	private static final int CENTER_X = 88;

	public BatteryScreen(BatteryMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractLabels(graphics, mouseX, mouseY);

		long stored = menu.getStored();
		long capacity = menu.getCapacity();
		double ratio = capacity > 0 ? (double) stored / capacity : 0.0D;

		drawCenteredText(graphics, Component.translatable("gui.crimsoncoppergrid.battery.energy"),
				CENTER_X, BAR_Y - 11, LABEL_COLOR);
		drawBar(graphics, BAR_X, BAR_Y, BAR_WIDTH, BAR_HEIGHT, ratio, ENERGY_COLOR);

		// 条内居中显示百分比，省掉一行文字的位置
		drawCenteredText(graphics, Component.literal(Math.round(ratio * 100) + "%"),
				CENTER_X, BAR_Y + 7, 0xFFFFFFFF);

		drawCenteredText(graphics, Component.translatable("gui.crimsoncoppergrid.energy_amount",
				PowerSystem.getLocalizedPower(stored), PowerSystem.getLocalizedPower(capacity)),
				CENTER_X, BAR_Y + BAR_HEIGHT + 8, LABEL_COLOR);

		long change = menu.getPowerChange();
		// 模板里已经写了「FE/t」，所以数值走不带单位的版本
		Component status = change > 0
				? Component.translatable("gui.crimsoncoppergrid.battery.charging",
						PowerSystem.getLocalizedPowerNoSuffix(change))
				: change < 0
						? Component.translatable("gui.crimsoncoppergrid.battery.discharging",
								PowerSystem.getLocalizedPowerNoSuffix(-change))
						: Component.translatable("gui.crimsoncoppergrid.battery.idle");
		drawCenteredText(graphics, status, CENTER_X, BAR_Y + BAR_HEIGHT + 20, LABEL_COLOR);
	}
}
