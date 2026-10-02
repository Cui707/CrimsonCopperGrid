package com.cyx.crimsoncoppergrid.client.screen;

import com.cyx.crimsoncoppergrid.common.menu.GeneratorMenu;
import com.cyx.crimsoncoppergrid.common.powerSystem.PowerSystem;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 三台「只有电、没有物品栏」的设备共用的面板：太阳能、风力、电力岩浆机。
 *
 * <p>统一画一根电量条 + 一行「存量 / 容量」，下面留给子类写各自的两行说明。
 * 说明文字里的数值直接引用方块实体上的公开常量（例如
 * {@code SolarGeneratorBlockEntity.MAX_OUTPUT}）—— 它们都是编译期常量，
 * 客户端不需要任何额外的同步就能显示正确。
 *
 * <p>唯一需要同步的动态读数是岩浆机的罐内量，走 {@link GeneratorMenu#getTank()}。
 *
 * <h2>为什么不用界面里那台机器的「实时输出」</h2>
 * 发电量是在服务端的 tick 里算的，客户端拿不到（{@code currentOutput()} 在客户端恒为 0）。
 * 与其为了显示一个随时在变的数字再加一条同步通道，不如显示额定值 ——
 * 玩家真正需要知道的是「这台机器最多能给多少」，实时发不发得出来看方块上的
 * 「正在工作」贴图就够了。
 */
public abstract class GeneratorScreen<T extends GeneratorMenu> extends MachineScreen<T> {

	/** 电量条的位置与尺寸。 */
	protected static final int ENERGY_X = 28;
	protected static final int ENERGY_Y = 26;
	protected static final int ENERGY_WIDTH = 120;
	protected static final int ENERGY_HEIGHT = 14;
	/** 界面的水平中心线（176 / 2）。 */
	protected static final int CENTER_X = 88;
	/** 说明文字的第一行基线；第二行是它 + 12。 */
	protected static final int INFO_Y = 50;

	protected GeneratorScreen(T menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractLabels(graphics, mouseX, mouseY);

		long stored = menu.getStored();
		long capacity = menu.getCapacity();
		double ratio = capacity > 0 ? (double) stored / capacity : 0.0D;
		drawBar(graphics, ENERGY_X, ENERGY_Y, ENERGY_WIDTH, ENERGY_HEIGHT, ratio, ENERGY_COLOR);
		drawCenteredText(graphics, Component.translatable("gui.crimsoncoppergrid.energy_amount",
				PowerSystem.getLocalizedPower(stored), PowerSystem.getLocalizedPower(capacity)),
				CENTER_X, ENERGY_Y + 3, 0xFFFFFFFF);

		drawInfo(graphics);
	}

	/** 子类在这里写自己的说明文字（两行为宜，超出会压到玩家背包上）。 */
	protected abstract void drawInfo(GuiGraphicsExtractor graphics);
}
