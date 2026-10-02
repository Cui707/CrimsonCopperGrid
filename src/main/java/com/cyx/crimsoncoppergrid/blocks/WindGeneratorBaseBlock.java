package com.cyx.crimsoncoppergrid.blocks;

import net.minecraft.world.level.block.Block;

/**
 * 风机底座 —— 风力发电机的「塔基」，也是它测高的唯一依据。
 *
 * <h2>为什么必须有这么一个方块</h2>
 * 风力发电机的发电量按「塔高」分档（见 {@code WindGeneratorBlockEntity}），
 * 而塔高是<b>玩家搭出来的结构高度</b>，不是世界里的某个坐标。
 * 代码没办法从方块世界里看出「哪几块是玩家搭的、哪几块是地形」——
 * 一根插在平原上的土柱和它脚下的泥土在数据上完全一样。
 * 所以需要一个明确的锚点：底座放哪儿，塔就从哪儿开始量。
 *
 * <h2>副产品：与 Y 轴解耦</h2>
 * 按「与底座的高度差」计量之后，同一个 9 格塔在平原（y=64）和空岛（y=200）上
 * 输出完全一致 —— 玩家在哪里建基地都不吃亏，也不需要为了发电去爬山。
 *
 * <p>底座本身不发电、不接电、没有方块实体，纯粹是一个标记方块，
 * 所以直接继承 {@link Block} 而不是 {@code BlockMachineBase}
 * （后者会带来 {@code FACING} / {@code ACTIVE} 两个用不上的方块状态）。
 * 外观属性仍复用 {@code BlockMachineBase#machineProperties()}，
 * 保证它和机器是同一套材质语言。
 */
public class WindGeneratorBaseBlock extends Block {

	public WindGeneratorBaseBlock(Properties properties) {
		super(properties);
	}
}
