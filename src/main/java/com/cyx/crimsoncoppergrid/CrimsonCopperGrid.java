package com.cyx.crimsoncoppergrid;

import com.cyx.crimsoncoppergrid.energy.SelfCheck;
import com.cyx.crimsoncoppergrid.init.ModBlockEntities;
import com.cyx.crimsoncoppergrid.init.ModBlocks;
import com.cyx.crimsoncoppergrid.init.ModCreativeTab;
import com.cyx.crimsoncoppergrid.init.ModItems;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CrimsonCopperGrid —— 只用原版铜与红石搭建的轻量级 Fabric 能源模组。
 *
 * <p>公共（客户端与服务端都会加载）入口。能量网络的核心逻辑在
 * {@code com.cyx.crimsoncoppergrid.energy} 包里，客户端的渲染 / 界面逻辑放到
 * {@code src/client} 下的 ClientModInitializer 中。
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

		// 启动自检：用真实世界验证几条容易写错的接线判定，结果直接打进日志
		SelfCheck.install();

		LOGGER.info("CrimsonCopperGrid 初始化完成：铜与红石的电网已就绪");
	}

	/** 便捷方法：按模组命名空间生成 Identifier。 */
	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
