package com.cyx.crimsoncoppergrid.client.screen;

import com.cyx.crimsoncoppergrid.blockentity.WindGeneratorBlockEntity;
import com.cyx.crimsoncoppergrid.common.menu.WindGeneratorMenu;
import com.cyx.crimsoncoppergrid.common.powerSystem.PowerSystem;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 风力发电机的读数面板。
 *
 * <h2>面板要回答的问题</h2>
 * 风力的发电量不是常数：它由「正下方那座塔有多高」决定，塔还要踩在风机底座上。
 * 玩家光看机器本体看不出自己卡在哪一步 —— 塔没接底座？接了就太矮？
 * 上方被封了？所以面板把状态、当前塔高、档位表、硬性条件四件事一次写清楚：
 * <ol>
 *   <li><b>当前输出</b>（随塔高变化），没发电就直接说原因；</li>
 *   <li><b>当前塔高</b>：底座与发电机之间隔了几格，也就是「还差几格能升档」的依据；</li>
 *   <li><b>档位表</b>：几格对应多少 FE/t；</li>
 *   <li><b>硬性条件</b>：底座在塔底、上方留空。</li>
 * </ol>
 *
 * <h2>四行的数值来源</h2>
 * 第 1、2 行随世界变化，由 {@link WindGeneratorMenu} 从服务端同步过来；
 * 第 3、4 行引用的都是方块实体上的公开常量与静态换算方法，客户端直接编译期拿到。
 */
public class WindGeneratorScreen extends GeneratorScreen<WindGeneratorMenu> {

	/** 四行排满机器区，行距取满 {@link #LINE_HEIGHT}。 */
	private static final int OUTPUT_Y = 24;
	private static final int HEIGHT_Y = 36;
	private static final int SCALE_Y = 48;
	private static final int RULE_Y = 60;

	/** 正在发电：绿色。 */
	private static final int OUTPUT_ON_COLOR = 0xFF1B7A1B;
	/** 停机：灰色。 */
	private static final int OUTPUT_OFF_COLOR = 0xFF7A7A7A;

	public WindGeneratorScreen(WindGeneratorMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
	}

	@Override
	protected void drawInfo(GuiGraphicsExtractor graphics) {
		drawStatusLine(graphics);
		drawHeightLine(graphics);

		// 第 3 行：档位表。空格数直接引用方块实体上的常量与换算方法，
		// 改档位、改功率时面板会自动跟着变，不需要动语言文件。
		// 模板里已有「FE/t」，所以数值一律走不带单位的 NoSuffix 版本。
		drawCenteredText(graphics,
				Component.translatable("gui.crimsoncoppergrid.wind.scale",
						WindGeneratorBlockEntity.heightForTier(1),
						WindGeneratorBlockEntity.heightForTier(2),
						WindGeneratorBlockEntity.FULL_TOWER_HEIGHT,
						PowerSystem.getLocalizedPowerNoSuffix(WindGeneratorBlockEntity.outputForTier(1)),
						PowerSystem.getLocalizedPowerNoSuffix(WindGeneratorBlockEntity.outputForTier(2)),
						PowerSystem.getLocalizedPowerNoSuffix(WindGeneratorBlockEntity.outputForTier(3))),
				CENTER_X, SCALE_Y, LABEL_COLOR);

		// 第 4 行：两个硬性条件
		drawCenteredText(graphics, Component.translatable("gui.crimsoncoppergrid.wind.condition"),
				CENTER_X, RULE_Y, LABEL_COLOR);
	}

	/** 第 1 行：在发电就报输出，没发电就报原因。 */
	private void drawStatusLine(GuiGraphicsExtractor graphics) {
		int status = menu.getStatus();
		if (status == WindGeneratorBlockEntity.STATUS_GENERATING) {
			drawCenteredText(graphics,
					Component.translatable("gui.crimsoncoppergrid.wind.output",
							PowerSystem.getLocalizedPowerNoSuffix(menu.getOutput())),
					CENTER_X, OUTPUT_Y, OUTPUT_ON_COLOR);
			return;
		}
		Component reason = Component.translatable(switch (status) {
			case WindGeneratorBlockEntity.STATUS_OBSTRUCTED -> "gui.crimsoncoppergrid.wind.blocked";
			case WindGeneratorBlockEntity.STATUS_TOO_SHORT -> "gui.crimsoncoppergrid.wind.too_short";
			default -> "gui.crimsoncoppergrid.wind.no_base";
		}, WindGeneratorBlockEntity.TIER_STEP);
		drawCenteredText(graphics, reason, CENTER_X, OUTPUT_Y, OUTPUT_OFF_COLOR);
	}

	/** 第 2 行：当前塔高。没有底座时给一个占位符，别让玩家误以为是 0 格。 */
	private void drawHeightLine(GuiGraphicsExtractor graphics) {
		int height = menu.getTowerHeight();
		Component line = height < 0
				? Component.translatable("gui.crimsoncoppergrid.wind.height_none")
				: Component.translatable("gui.crimsoncoppergrid.wind.height", height);
		drawCenteredText(graphics, line, CENTER_X, HEIGHT_Y, LABEL_COLOR);
	}
}
