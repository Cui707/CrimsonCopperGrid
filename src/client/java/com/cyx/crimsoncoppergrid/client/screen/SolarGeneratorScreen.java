package com.cyx.crimsoncoppergrid.client.screen;

import com.cyx.crimsoncoppergrid.blockentity.SolarGeneratorBlockEntity;
import com.cyx.crimsoncoppergrid.common.menu.SolarGeneratorMenu;
import com.cyx.crimsoncoppergrid.common.powerSystem.PowerSystem;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 太阳能发电机的读数面板。
 *
 * <h2>为什么没有电量条</h2>
 * 太阳能的内部缓冲只有 {@code CAPACITY}（1000 FE）格，按 {@code MAX_OUTPUT}（20 FE/t）算，
 * 一发电 2.5 秒就贴到上限、旁边接了负载又会被推空。无论哪种情况读数都对玩家没有信息量，
 * 反而让人以为出了问题（「为什么一直是 984/1k？」）。
 * 所以这里不画电量条，改在同一个位置直接回答玩家唯一关心的问题：
 * <b>现在在不在发电，不在的话是因为什么。</b>
 *
 * <h2>状态从哪来</h2>
 * 「能不能发电」由服务端的时间、天气与遮挡共同决定，客户端算不出来，
 * 所以由方块实体在 tick 里判定好、通过 {@link SolarGeneratorMenu#getStatus()} 同步过来。
 */
public class SolarGeneratorScreen extends GeneratorScreen<SolarGeneratorMenu> {

	/** 状态行基线：最上面、最醒目的一行。 */
	private static final int STATUS_Y = 26;
	/** 额定输出基线。 */
	private static final int RATED_Y = 44;
	/** 发电条件提示基线。 */
	private static final int CONDITION_Y = 60;

	/** 正在发电：绿色。 */
	private static final int STATUS_ON_COLOR = 0xFF1B7A1B;
	/** 停机：灰色。 */
	private static final int STATUS_OFF_COLOR = 0xFF7A7A7A;

	public SolarGeneratorScreen(SolarGeneratorMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
	}

	@Override
	protected void drawInfo(GuiGraphicsExtractor graphics) {
		drawStatusLine(graphics, STATUS_Y);

		// 模板里已经写了「FE/t」，所以这里用不带单位的 NoSuffix 版本，
		// 否则会拼成「额定输出 20 FE FE/t」。
		drawCenteredText(graphics,
				Component.translatable("gui.crimsoncoppergrid.solar.rated",
						PowerSystem.getLocalizedPowerNoSuffix(SolarGeneratorBlockEntity.MAX_OUTPUT)),
				CENTER_X, RATED_Y, LABEL_COLOR);
		drawCenteredText(graphics, Component.translatable("gui.crimsoncoppergrid.solar.condition"),
				CENTER_X, CONDITION_Y, LABEL_COLOR);
	}

	/**
	 * 画「状态：正在发电」这样一行。
	 *
	 * <p>标签与取值分开画，是为了让取值单独上色 —— 绿色一眼扫到「在工作」，
	 * 灰色则提示该去看看原因了。居中要靠自己把两段的宽度加起来算。
	 */
	private void drawStatusLine(GuiGraphicsExtractor graphics, int y) {
		int status = menu.getStatus();
		Component label = Component.translatable("gui.crimsoncoppergrid.status");
		Component value = Component.translatable(statusKey(status));
		int labelWidth = font.width(label);
		int x = CENTER_X - (labelWidth + font.width(value)) / 2;

		drawText(graphics, label, x, y, LABEL_COLOR);
		drawText(graphics, value, x + labelWidth, y, statusColor(status));
	}

	/** 状态 → 语言键。未知取值按「停机」处理，宁可少说也不要显示乱码。 */
	private static String statusKey(int status) {
		return switch (status) {
			case SolarGeneratorBlockEntity.STATUS_GENERATING -> "gui.crimsoncoppergrid.solar.status.generating";
			case SolarGeneratorBlockEntity.STATUS_RAIN -> "gui.crimsoncoppergrid.solar.status.rain";
			case SolarGeneratorBlockEntity.STATUS_OBSTRUCTED -> "gui.crimsoncoppergrid.solar.status.obstructed";
			default -> "gui.crimsoncoppergrid.solar.status.night";
		};
	}

	private static int statusColor(int status) {
		return status == SolarGeneratorBlockEntity.STATUS_GENERATING ? STATUS_ON_COLOR : STATUS_OFF_COLOR;
	}
}
