package com.cyx.crimsoncoppergrid.mixin;

import net.minecraft.server.MinecraftServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.cyx.crimsoncoppergrid.CrimsonCopperGrid;

/**
 * 服务端注入示例。此处仅打印一条日志，用来验证 mixin 配置生效；
 * 真正的电网逻辑（区块加载时重建能量网络等）可以挂在这里。
 */
@Mixin(MinecraftServer.class)
public class CrimsonCopperGridMixin {
	@Inject(at = @At("HEAD"), method = "loadLevel")
	private void crimsoncoppergrid$onLoadLevel(CallbackInfo info) {
		CrimsonCopperGrid.LOGGER.info("CrimsonCopperGrid: 服务端世界加载完成，准备接管铜制电网");
	}
}
