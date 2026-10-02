package com.cyx.crimsoncoppergrid.client.screen;

import com.cyx.crimsoncoppergrid.common.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * 所有 CCG 机器界面的公共父类。
 *
 * <h2>26.3 的界面绘制入口</h2>
 * 26.3 把界面渲染拆成了「提取（extract）—上屏」两段：屏幕只负责把要画的东西提取成
 * 状态，不再直接绘制。因此覆写点从旧版的 {@code renderBg} 变成了：
 * <ul>
 *   <li>{@link #extractBackground} —— 画底图，坐标需自行加上 {@code leftPos/topPos}；</li>
 *   <li>{@link #extractLabels} —— 画文字与装饰，原版已把坐标系平移到界面左上角。</li>
 * </ul>
 * 物品槽由原版在两者之间绘制，所以进度条画在 {@code extractLabels} 里正好压在槽位下面。
 *
 * <h2>背景用的是原版贴图</h2>
 * 直接借原版熔炉的容器贴图（176×166）当底图：它的下半部分本来就是标准的玩家背包，
 * 机器区则被一块原版底色的面板盖掉，换成各机器自己的布局。
 * <b>整个界面层不新增任何美术资源。</b>
 */
public abstract class MachineScreen<T extends MachineMenu> extends AbstractContainerScreen<T> {

	/** 原版熔炉容器贴图：176×166，含标准玩家背包区。 */
	private static final Identifier BACKGROUND =
			Identifier.withDefaultNamespace("textures/gui/container/furnace.png");

	/** 原版容器界面的底色（浅灰）。 */
	protected static final int PANEL_COLOR = 0xFFC6C6C6;
	/** 进度条：空槽、外框、文字。 */
	protected static final int BAR_EMPTY_COLOR = 0xFF545454;
	protected static final int BAR_FRAME_COLOR = 0xFF373737;
	protected static final int LABEL_COLOR = 0xFF404040;
	/** 电量条填充色：红石红。 */
	protected static final int ENERGY_COLOR = 0xFFAA0000;
	/** 进度条填充色：铜色。 */
	protected static final int PROGRESS_COLOR = 0xFFB87333;

	protected MachineScreen(T menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		// super 负责在原版规则下把背景压暗（世界内界面是半透明黑）
		super.extractBackground(graphics, mouseX, mouseY, partialTick);

		graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND,
				leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);

		// 原版贴图的机器区画的是熔炉自己的槽位与火焰，这里盖成一块干净面板
		graphics.fill(leftPos + 7, topPos + 16, leftPos + 169, topPos + 78, PANEL_COLOR);
	}

	/**
	 * 画一条水平进度条。坐标相对界面左上角，与原版 {@code extractLabels} 一致。
	 *
	 * @param ratio 0~1，超出范围会被夹紧
	 */
	protected void drawBar(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
			double ratio, int fillColor) {
		int filled = (int) Math.round(width * Mth.clamp(ratio, 0.0D, 1.0D));
		graphics.fill(x, y, x + width, y + height, BAR_EMPTY_COLOR);
		if (filled > 0) {
			graphics.fill(x, y, x + filled, y + height, fillColor);
		}
		graphics.outline(x - 1, y - 1, x + width + 1, y + height + 1, BAR_FRAME_COLOR);
	}

	/** 在界面上居中画一行字（{@code centerX} 是相对界面的中心线）。 */
	protected void drawCenteredText(GuiGraphicsExtractor graphics, Component text, int centerX, int y, int color) {
		graphics.centeredText(font, text, centerX, y, color);
	}
}
