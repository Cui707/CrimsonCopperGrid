package com.cyx.crimsoncoppergrid.init;

import com.cyx.crimsoncoppergrid.blockentity.cable.CableBlockEntity;
import com.cyx.crimsoncoppergrid.blockentity.cable.SwitchBlockEntity;
import com.cyx.crimsoncoppergrid.common.powerSystem.PowerAcceptorBlockEntity;

import team.reborn.energy.api.EnergyStorage;

/**
 * 把 CCG 的方块接进 Team Reborn Energy 的能力查阅表（{@code EnergyStorage.SIDED}）。
 *
 * <p>这张表是 Fabric 能力体系的标准入口：任何一段代码想知道「某个坐标能不能收发电」，
 * 都通过 {@code EnergyStorage.SIDED.find(level, pos, side)} 询问，由表里的 provider
 * 按方块实体类型分派。好处是第三方模组的管道 / 线缆也能和 CCG 的设备互通，
 * 不需要双方互相写兼容代码。
 *
 * <h2>为什么机器用 fallback 而不是逐个注册</h2>
 * 所有机器都继承 {@link PowerAcceptorBlockEntity}，能力实现完全相同（都转发给内部的
 * {@code SimpleSidedEnergyContainer}）。所以一条 {@code registerFallback} 就够了，
 * 不必为每台机器写一行 {@code registerForBlockEntity} —— 后者还要求传入
 * {@code BlockEntityType} 对象，多引入一处对注册顺序的依赖。
 * 这也正是 TechReborn 的做法（见 RebornCore 的 {@code RebornCore} 初始化）。
 *
 * <p>导线和电闸是例外：它们不是机器、不继承 {@link PowerAcceptorBlockEntity}
 * （导线不是「机器」，它是一段导体），所以单独注册。
 */
public final class ModPowerRegistration {

	private ModPowerRegistration() {
	}

	public static void init() {
		// 所有机器：一次性挂上按方向查询的能量能力
		EnergyStorage.SIDED.registerFallback((level, pos, state, blockEntity, direction) -> {
			if (blockEntity instanceof PowerAcceptorBlockEntity powerAcceptor) {
				return powerAcceptor.getSideEnergyStorage(direction);
			}
			return null;
		});

		// 导线与电闸：不继承机器的公共父类，因此单独按类型注册
		EnergyStorage.SIDED.registerForBlockEntity(CableBlockEntity::getSideEnergyStorage, ModBlockEntities.CABLE);
		EnergyStorage.SIDED.registerForBlockEntity(SwitchBlockEntity::getSideEnergyStorage, ModBlockEntities.SWITCH);
	}
}
