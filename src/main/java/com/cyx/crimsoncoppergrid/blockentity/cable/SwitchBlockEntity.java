package com.cyx.crimsoncoppergrid.blockentity.cable;

import com.cyx.crimsoncoppergrid.blocks.cable.SwitchBlock;
import com.cyx.crimsoncoppergrid.init.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 铜制电闸的方块实体。这是 CCG 相对 TechReborn 新增的东西 ——
 * TechReborn 没有「电网总闸」，它的线缆通断靠拆线。
 *
 * <p>实现方式刻意做到最薄：电闸就是一段**可以断路**的导线，
 * 唯一的行为差异是 {@link #conducts()}。断开时：
 * <ul>
 *   <li>{@code conducts()} 返回 false，组网 BFS 在此截断 —— 两侧变成两张独立的网，
 *       这是「电网总闸」语义的落点，而不是简单地让电闸自己不通电；</li>
 *   <li>连带 {@code allowTransfer} 也返回 false，所以断开期间电闸既不收也不放电，
 *       缓冲里可能残留的一点电会原样留到重新合闸后再汇入池子（不销毁、不凭空产生）。</li>
 * </ul>
 *
 * <p>注意接线端的**显示**不受通断影响：{@link CableBlockEntity#isCableLike} 只看方块实体类型，
 * 所以导线始终会朝电闸伸出连接臂 —— 这与真实电闸一致（线是接在端子上的，只是闸刀没合）。
 */
public class SwitchBlockEntity extends CableBlockEntity {

	public SwitchBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.SWITCH, pos, state);
	}

	@Override
	public boolean conducts() {
		BlockState state = getBlockState();
		return state.getBlock() instanceof SwitchBlock && SwitchBlock.isOpen(state);
	}
}
