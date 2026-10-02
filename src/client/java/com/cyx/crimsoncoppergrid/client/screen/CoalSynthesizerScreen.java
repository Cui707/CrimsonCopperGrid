package com.cyx.crimsoncoppergrid.client.screen;

import com.cyx.crimsoncoppergrid.blockentity.CoalSynthesizerBlockEntity;
import com.cyx.crimsoncoppergrid.common.menu.CoalSynthesizerMenu;
import com.cyx.crimsoncoppergrid.common.powerSystem.PowerSystem;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 电力煤炭合成机界面：电量条 + 一个产物槽。
 *
 * <p>这里没有单独的进度条 —— 缓冲容量正好等于一块煤的电量，
 * 所以「存量 / 容量」本身就是进度。电量条下面写清这个换算关系，
 * 玩家一眼就知道还要多少电才出一块煤。
 */
public class CoalSynthesizerScreen extends MachineScreen<CoalSynthesizerMenu> {

	private static final int ENERGY_X = 28;
	private static final int ENERGY_Y = 20;
	private static final int ENERGY_WIDTH = 120;
	private static final int ENERGY_HEIGHT = 12;

	private static final int CENTER_X = 88;
	private static final int HINT_Y = 42;

	public CoalSynthesizerScreen(CoalSynthesizerMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractLabels(graphics, mouseX, mouseY);

		long stored = menu.getStored();
		long capacity = menu.getCapacity();
		double ratio = capacity > 0 ? (double) stored / capacity : 0.0D;
		drawBar(graphics, ENERGY_X, ENERGY_Y, ENERGY_WIDTH, ENERGY_HEIGHT, ratio, PROGRESS_COLOR);
		drawCenteredText(graphics, Component.translatable("gui.crimsoncoppergrid.energy_amount",
				PowerSystem.getLocalizedPower(stored), PowerSystem.getLocalizedPower(capacity)),
				CENTER_X, ENERGY_Y + 2, 0xFFFFFFFF);

		// 模板里已经写了「FE」，数值本身不能再带单位
		drawCenteredText(graphics,
				Component.translatable("gui.crimsoncoppergrid.coal.rate",
						PowerSystem.getLocalizedPowerNoSuffix(CoalSynthesizerBlockEntity.FE_PER_COAL)),
				CENTER_X, HINT_Y, LABEL_COLOR);
	}
}
