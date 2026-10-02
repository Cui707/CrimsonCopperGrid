package com.cyx.crimsoncoppergrid.client.screen;

import com.cyx.crimsoncoppergrid.blockentity.LavaGeneratorBlockEntity;
import com.cyx.crimsoncoppergrid.common.menu.LavaGeneratorMenu;
import com.cyx.crimsoncoppergrid.common.powerSystem.PowerSystem;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 电力岩浆机的读数面板：比另外两台多一根岩浆罐的条。
 *
 * <p>桶的进出不在这里 —— 那是方块层的右键交互（拿桶对着方块右键）。
 * 界面只负责「看清还剩多少」。
 */
public class LavaGeneratorScreen extends GeneratorScreen<LavaGeneratorMenu> {

	private static final int TANK_X = 28;
	private static final int TANK_Y = 44;
	private static final int TANK_WIDTH = 120;
	private static final int TANK_HEIGHT = 12;

	/** 岩浆条的颜色。 */
	private static final int LAVA_COLOR = 0xFFE2590B;

	public LavaGeneratorScreen(LavaGeneratorMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
	}

	@Override
	protected void drawInfo(GuiGraphicsExtractor graphics) {
		int tank = menu.getTank();
		int capacityMb = LavaGeneratorBlockEntity.TANK_CAPACITY_MB;

		drawBar(graphics, TANK_X, TANK_Y, TANK_WIDTH, TANK_HEIGHT,
				(double) tank / capacityMb, LAVA_COLOR);
		drawCenteredText(graphics, Component.translatable("gui.crimsoncoppergrid.lava.tank",
				PowerSystem.getLocalizedPower(tank), PowerSystem.getLocalizedPower(capacityMb)),
				CENTER_X, TANK_Y + 2, 0xFFFFFFFF);

		drawCenteredText(graphics, Component.translatable("gui.crimsoncoppergrid.lava.rate",
				PowerSystem.getLocalizedPower(LavaGeneratorBlockEntity.BUCKET_MB),
				PowerSystem.getLocalizedPower(
						LavaGeneratorBlockEntity.FE_PER_MB * LavaGeneratorBlockEntity.BUCKET_MB)),
				CENTER_X, TANK_Y + 18, LABEL_COLOR);
	}
}
