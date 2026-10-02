package com.cyx.crimsoncoppergrid.client.screen;

import com.cyx.crimsoncoppergrid.common.menu.PowerControllerMenu;
import com.cyx.crimsoncoppergrid.common.powerSystem.PowerSystem;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 电力控制器界面：电网总览读数。
 *
 * <p>刻意把「能力上限」和「存量」分成两组显示：
 * 前者是这套电网理论上能吞吐多少，后者是此刻实际攒下了多少。
 * 推流模型下没有瞬时功率可读，把两者混在一起会给出一个看着精确、其实含糊的数字。
 */
public class PowerControllerScreen extends MachineScreen<PowerControllerMenu> {

	private static final int TEXT_X = 12;
	private static final int LINE_HEIGHT = 13;
	private static final int FIRST_LINE_Y = 22;

	public PowerControllerScreen(PowerControllerMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractLabels(graphics, mouseX, mouseY);

		if (!menu.isConnected()) {
			drawCenteredText(graphics, Component.translatable("gui.crimsoncoppergrid.controller.disconnected"),
					88, FIRST_LINE_Y + 16, LABEL_COLOR);
			return;
		}

		int y = FIRST_LINE_Y;
		graphics.text(font, Component.translatable("gui.crimsoncoppergrid.controller.cables",
				menu.getCableCount(),
				PowerSystem.getLocalizedPower(menu.getNetworkStored()),
				PowerSystem.getLocalizedPower((long) menu.getCableCount() * CABLE_BUFFER)),
				TEXT_X, y, LABEL_COLOR);
		y += LINE_HEIGHT;

		graphics.text(font, Component.translatable("gui.crimsoncoppergrid.controller.generators",
				menu.getGeneratorCount(),
				PowerSystem.getLocalizedPower(menu.getGeneratorOutput())),
				TEXT_X, y, LABEL_COLOR);
		y += LINE_HEIGHT;

		graphics.text(font, Component.translatable("gui.crimsoncoppergrid.controller.consumers",
				menu.getConsumerCount(),
				PowerSystem.getLocalizedPower(menu.getConsumerInput())),
				TEXT_X, y, LABEL_COLOR);
		y += LINE_HEIGHT;

		graphics.text(font, Component.translatable("gui.crimsoncoppergrid.controller.batteries",
				menu.getBatteryCount(),
				PowerSystem.getLocalizedPower(menu.getBatteryStored()),
				PowerSystem.getLocalizedPower(menu.getBatteryCapacity())),
				TEXT_X, y, LABEL_COLOR);
	}

	/**
	 * 导线单段缓冲容量。放在界面侧只是为了把「导线池上限」显示出来，
	 * 口径与 {@code CableBlockEntity.BUFFER_CAPACITY} 保持一致（32 × 4）。
	 */
	private static final long CABLE_BUFFER = 128L;
}
