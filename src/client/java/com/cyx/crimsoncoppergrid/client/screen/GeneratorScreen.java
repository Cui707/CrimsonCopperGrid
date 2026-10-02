package com.cyx.crimsoncoppergrid.client.screen;

import com.cyx.crimsoncoppergrid.common.menu.GeneratorMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 两台「只有电、没有物品栏」的发电机共用的面板：太阳能、风力。
 *
 * <p>这里只提供排版基准（中心线与行距），内容全交给子类的 {@link #drawInfo}。
 *
 * <h2>为什么不画电量条了</h2>
 * 两台发电机的内部缓冲都只有 {@code CAPACITY}（1000 FE）：太阳能 2.5 秒就填满，
 * 风力满输出也只要 33 秒。推流模型下旁边没接东西就贴着上限、接了负载又被推空，
 * 无论哪种情况这个读数都对玩家没有信息量 —— 玩家真正想知道的是
 * 「现在到底在不在发电、在发多少、没在发是为什么」。所以两台机器都改成一句话式的实时读数：
 * 太阳能报状态（含停机原因），风力报当前输出 + 塔高 + 档位表。
 *
 * <h2>为什么说明文字里的数值不用同步</h2>
 * 额定值之类的常量直接引用方块实体上的公开字段或静态方法（例如
 * {@code WindGeneratorBlockEntity.outputForTier(int)}）—— 它们都是编译期能算出来的，
 * 客户端不需要任何额外的同步就能显示正确。只有<b>随世界变化</b>的量
 * （太阳能的状态、风力的输出与塔高）才走 {@code ContainerData}。
 */
public abstract class GeneratorScreen<T extends GeneratorMenu> extends MachineScreen<T> {

	/** 界面的水平中心线（176 / 2）。 */
	protected static final int CENTER_X = 88;

	/**
	 * 正文行距。
	 *
	 * <p>机器区（y 16~78）垂直只有 62px，一行文字占 9px，所以最多排 4 行 ——
	 * 子类自己决定从哪一行开始，别超过这个数，否则会压到下面的玩家背包标题上。
	 */
	protected static final int LINE_HEIGHT = 12;

	protected GeneratorScreen(T menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractLabels(graphics, mouseX, mouseY);
		drawInfo(graphics);
	}

	/** 子类在这里写自己的说明文字（4 行为宜，超出会压到玩家背包上）。 */
	protected abstract void drawInfo(GuiGraphicsExtractor graphics);
}
