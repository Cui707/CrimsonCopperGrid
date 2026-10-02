package com.cyx.crimsoncoppergrid.client.screen;

import com.cyx.crimsoncoppergrid.blockentity.SolarGeneratorBlockEntity;
import com.cyx.crimsoncoppergrid.common.menu.SolarGeneratorMenu;
import com.cyx.crimsoncoppergrid.common.powerSystem.PowerSystem;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 太阳能发电机的读数面板。 */
public class SolarGeneratorScreen extends GeneratorScreen<SolarGeneratorMenu> {

	public SolarGeneratorScreen(SolarGeneratorMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
	}

	@Override
	protected void drawInfo(GuiGraphicsExtractor graphics) {
		drawCenteredText(graphics,
				Component.translatable("gui.crimsoncoppergrid.solar.rated",
						PowerSystem.getLocalizedPower(SolarGeneratorBlockEntity.MAX_OUTPUT)),
				CENTER_X, INFO_Y, LABEL_COLOR);
		drawCenteredText(graphics, Component.translatable("gui.crimsoncoppergrid.solar.condition"),
				CENTER_X, INFO_Y + 12, LABEL_COLOR);
	}
}
