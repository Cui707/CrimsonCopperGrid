package com.cyx.crimsoncoppergrid.client.mixin;

import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 客户端注入示例骨架：验证 client 源集的 mixin 配置生效。
 * 不需要客户端注入时，可以直接删除本文件与 crimsoncoppergrid.client.mixins.json。
 */
@Mixin(Minecraft.class)
public class CrimsonCopperGridClientMixin {
	@Inject(at = @At("HEAD"), method = "run")
	private void crimsoncoppergrid$onRun(CallbackInfo info) {
		// 注入到 Minecraft.run() 的起始位置
	}
}
