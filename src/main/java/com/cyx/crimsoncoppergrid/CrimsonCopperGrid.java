package com.cyx.crimsoncoppergrid;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CrimsonCopperGrid —— 只用原版铜与红石搭建的轻量级 Fabric 能源模组。
 *
 * <p>公共（客户端与服务端都会加载）入口。能量网络的核心逻辑应当放在这里，
 * 客户端的渲染 / 界面逻辑放到 {@code src/client} 下的 ClientModInitializer 中。
 */
public class CrimsonCopperGrid implements ModInitializer {
	public static final String MOD_ID = "crimsoncoppergrid";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// 这里的代码会在 Minecraft 进入“可以加载模组”的状态后立即执行。
		// 此时部分资源（如方块模型、语言文件）可能还没初始化完成，注册时要留意。
		LOGGER.info("CrimsonCopperGrid 初始化：铜与红石的电网即将上线");
	}

	/** 便捷方法：按模组命名空间生成 Identifier。 */
	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
