package com.cyx.crimsoncoppergrid.client.screen;

import com.cyx.crimsoncoppergrid.common.menu.LavaGeneratorMenu;
import com.cyx.crimsoncoppergrid.common.powerSystem.PowerSystem;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 电力岩浆机界面。
 *
 * <p>输入槽 / 输出槽 / 进度条的位置与原版熔炉完全一致，所以能直接复用原版熔炉贴图。
 * 电量条放在进度条下方，并在条内居中显示当前缓冲量。
 */
public class LavaGeneratorScreen extends MachineScreen<LavaGeneratorMenu> {

	/** 进度箭头位置（原版熔炉的进度条坐标）。 */
	private static final int PROGRESS_X = 79;
	private static final int PROGRESS_Y = 35;
	private static final int PROGRESS_WIDTH = 24;
	private static final int PROGRESS_HEIGHT = 16;

	private static final int ENERGY_X = 28;
	private static final int ENERGY_Y = 58;
	private static final int ENERGY_WIDTH = 120;
	private static final int ENERGY_HEIGHT = 12;
	private static final int CENTER_X = 88;

	/** 左侧提示文字位置。 */
	private static final int HINT_X = 10;
	private static final int HINT_Y = 46;

	public LavaGeneratorScreen(LavaGeneratorMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractLabels(graphics, mouseX, mouseY);

		drawText(graphics, Component.translatable("gui.crimsoncoppergrid.lava.hint"),
				HINT_X, HINT_Y, LABEL_COLOR);

		int maxProgress = menu.getMaxProgress();
		double progressRatio = maxProgress > 0 ? (double) menu.getProgress() / maxProgress : 0.0D;
		drawBar(graphics, PROGRESS_X, PROGRESS_Y, PROGRESS_WIDTH, PROGRESS_HEIGHT,
				progressRatio, PROGRESS_COLOR);

		long stored = menu.getStored();
		long capacity = menu.getCapacity();
		double energyRatio = capacity > 0 ? (double) stored / capacity : 0.0D;
		drawBar(graphics, ENERGY_X, ENERGY_Y, ENERGY_WIDTH, ENERGY_HEIGHT, energyRatio, ENERGY_COLOR);
		drawCenteredText(graphics, Component.translatable("gui.crimsoncoppergrid.energy_amount",
				PowerSystem.getLocalizedPower(stored), PowerSystem.getLocalizedPower(capacity)),
				CENTER_X, ENERGY_Y + 2, 0xFFFFFFFF);
	}
}
