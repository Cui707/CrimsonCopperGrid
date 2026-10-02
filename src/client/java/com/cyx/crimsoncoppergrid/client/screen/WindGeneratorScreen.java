package com.cyx.crimsoncoppergrid.client.screen;

import com.cyx.crimsoncoppergrid.blockentity.WindGeneratorBlockEntity;
import com.cyx.crimsoncoppergrid.common.menu.WindGeneratorMenu;
import com.cyx.crimsoncoppergrid.common.powerSystem.PowerSystem;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 风力发电机的读数面板。 */
public class WindGeneratorScreen extends GeneratorScreen<WindGeneratorMenu> {

	public WindGeneratorScreen(WindGeneratorMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
	}

	@Override
	protected void drawInfo(GuiGraphicsExtractor graphics) {
		drawCenteredText(graphics,
				Component.translatable("gui.crimsoncoppergrid.wind.rated",
						PowerSystem.getLocalizedPower(WindGeneratorBlockEntity.MIN_OUTPUT),
						PowerSystem.getLocalizedPower(WindGeneratorBlockEntity.MAX_OUTPUT)),
				CENTER_X, INFO_Y, LABEL_COLOR);
		drawCenteredText(graphics, Component.translatable("gui.crimsoncoppergrid.wind.condition"),
				CENTER_X, INFO_Y + 12, LABEL_COLOR);
	}
}
