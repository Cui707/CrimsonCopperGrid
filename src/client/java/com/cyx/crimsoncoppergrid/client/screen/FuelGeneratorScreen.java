package com.cyx.crimsoncoppergrid.client.screen;

import com.cyx.crimsoncoppergrid.common.menu.FuelGeneratorMenu;
import com.cyx.crimsoncoppergrid.common.powerSystem.PowerSystem;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 燃料发电机界面：电量条 + 燃烧进度条 + 一个燃料槽。
 *
 * <p>燃烧条画在燃料槽正上方，读作「槽里的东西正在这里烧」。
 * 燃料耗尽时进度条归零，同时方块上的「正在工作」贴图也会停。
 */
public class FuelGeneratorScreen extends MachineScreen<FuelGeneratorMenu> {

	private static final int ENERGY_X = 28;
	private static final int ENERGY_Y = 20;
	private static final int ENERGY_WIDTH = 120;
	private static final int ENERGY_HEIGHT = 12;

	/** 燃烧条：压在燃料槽（80,53）上方。 */
	private static final int BURN_X = 79;
	private static final int BURN_Y = 36;
	private static final int BURN_WIDTH = 18;
	private static final int BURN_HEIGHT = 16;

	private static final int CENTER_X = 88;

	public FuelGeneratorScreen(FuelGeneratorMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractLabels(graphics, mouseX, mouseY);

		long stored = menu.getStored();
		long capacity = menu.getCapacity();
		double energyRatio = capacity > 0 ? (double) stored / capacity : 0.0D;
		drawBar(graphics, ENERGY_X, ENERGY_Y, ENERGY_WIDTH, ENERGY_HEIGHT, energyRatio, ENERGY_COLOR);
		drawCenteredText(graphics, Component.translatable("gui.crimsoncoppergrid.energy_amount",
				PowerSystem.getLocalizedPower(stored), PowerSystem.getLocalizedPower(capacity)),
				CENTER_X, ENERGY_Y + 2, 0xFFFFFFFF);

		int total = menu.getBurnTimeTotal();
		int remaining = menu.getBurnTime();
		double burnRatio = total > 0 ? (double) remaining / total : 0.0D;
		drawBar(graphics, BURN_X, BURN_Y, BURN_WIDTH, BURN_HEIGHT, burnRatio, PROGRESS_COLOR);

		if (remaining > 0) {
			drawText(graphics, Component.translatable("gui.crimsoncoppergrid.fuel.remaining", remaining),
					BURN_X + BURN_WIDTH + 6, BURN_Y + 4, LABEL_COLOR);
		}
	}
}
