package com.cyx.crimsoncoppergrid.client.screen;

import com.cyx.crimsoncoppergrid.common.menu.ElectricFurnaceMenu;
import com.cyx.crimsoncoppergrid.common.powerSystem.PowerSystem;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 电力熔炉界面。
 *
 * <p>槽位与原版熔炉同坐标（输入 56,17 / 输出 116,35），所以直接借了原版熔炉的
 * 背景贴图；原版「火焰」的位置改成电量条，「箭头」的位置改成烧炼进度条。
 */
public class ElectricFurnaceScreen extends MachineScreen<ElectricFurnaceMenu> {

	/** 原版熔炉的进度箭头位置。 */
	private static final int PROGRESS_X = 79;
	private static final int PROGRESS_Y = 35;
	private static final int PROGRESS_WIDTH = 24;
	private static final int PROGRESS_HEIGHT = 16;

	private static final int ENERGY_X = 28;
	private static final int ENERGY_Y = 58;
	private static final int ENERGY_WIDTH = 120;
	private static final int ENERGY_HEIGHT = 12;
	private static final int CENTER_X = 88;

	public ElectricFurnaceScreen(ElectricFurnaceMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractLabels(graphics, mouseX, mouseY);

		int maxProgress = menu.getMaxProgress();
		double progressRatio = maxProgress > 0 ? (double) menu.getProgress() / maxProgress : 0.0D;
		drawBar(graphics, PROGRESS_X, PROGRESS_Y, PROGRESS_WIDTH, PROGRESS_HEIGHT, progressRatio, PROGRESS_COLOR);

		long stored = menu.getStored();
		long capacity = menu.getCapacity();
		double energyRatio = capacity > 0 ? (double) stored / capacity : 0.0D;
		drawBar(graphics, ENERGY_X, ENERGY_Y, ENERGY_WIDTH, ENERGY_HEIGHT, energyRatio, ENERGY_COLOR);
		drawCenteredText(graphics, Component.translatable("gui.crimsoncoppergrid.energy_amount",
				PowerSystem.getLocalizedPower(stored), PowerSystem.getLocalizedPower(capacity)),
				CENTER_X, ENERGY_Y + 2, 0xFFFFFFFF);
	}
}
