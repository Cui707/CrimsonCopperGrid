package com.cyx.crimsoncoppergrid.blockentity.cable;

import net.minecraft.core.Direction;
import team.reborn.energy.api.EnergyStorage;

/**
 * 一根导线旁边查到的能量存储，附带「已经从这个方向搬过电」的记账。
 * 对齐 TechReborn 的同名 record（MIT）。
 *
 * <p>存在的理由：导线既要向邻居 <i>取</i> 电，也要向邻居 <i>送</i> 电。
 * 如果同一 tick 里对同一个方向既取又送，两个导线之间就会来回弹跳，
 * 把电反复搬运却不产生任何净效果。每搬完一次就在源导线的 {@code blockedSides}
 * 上置位对应方向，这一轮里该方向就不会再被反向操作。
 */
record OfferedEnergyStorage(CableBlockEntity sourceCable, Direction direction, EnergyStorage storage) {

	/** 本次搬运完成后调用：标记该方向，阻止同一轮内的重复搬运。 */
	void afterTransfer() {
		sourceCable.blockedSides |= 1 << direction.ordinal();
	}
}
