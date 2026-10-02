package com.cyx.crimsoncoppergrid;

import com.cyx.crimsoncoppergrid.blockentity.cable.CableTickManager;
import com.cyx.crimsoncoppergrid.init.ModBlockEntities;
import com.cyx.crimsoncoppergrid.init.ModBlocks;
import com.cyx.crimsoncoppergrid.init.ModCreativeTab;
import com.cyx.crimsoncoppergrid.init.ModItems;
import com.cyx.crimsoncoppergrid.init.ModPowerRegistration;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CrimsonCopperGrid —— 只用原版铜与红石搭建的轻量级 Fabric 能源模组。
 *
 * <p>公共（客户端与服务端都会加载）入口。
 *
 * <h2>初始化顺序</h2>
 * <ol>
 *   <li>注册表：方块 -> 方块实体 -> 物品 -> 物品栏。方块实体要引用方块，所以顺序不能换；</li>
 *   <li>能量能力：把设备接进 Team Reborn Energy 的查阅表，并装好导线网络的 tick 钩子；</li>
 * </ol>
 * 能力的注册放在方块实体之后，是因为 {@code registerForBlockEntity} 需要拿到
 * 已注册的 {@code BlockEntityType} 对象。
 */
public class CrimsonCopperGrid implements ModInitializer {
	public static final String MOD_ID = "crimsoncoppergrid";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// 注册顺序有讲究：方块 -> 方块实体（要引用方块）-> 物品 -> 物品栏
		ModBlocks.init();
		ModBlockEntities.init();
		ModItems.init();
		ModCreativeTab.init();

		// 能量能力 + 导线网络结算
		ModPowerRegistration.init();
		CableTickManager.init();

		LOGGER.info("CrimsonCopperGrid 初始化完成：铜与红石的电网已就绪");
	}

	/** 便捷方法：按模组命名空间生成 Identifier。 */
	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
