package com.cyx.crimsoncoppergrid.registry;

import com.cyx.crimsoncoppergrid.CrimsonCopperGrid;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab.Row;

/**
 * 创造模式物品栏。
 *
 * <p>直接使用原版 {@link CreativeModeTab} 的构建器，不依赖 Fabric 的物品栏扩展事件 ——
 * 这样物品栏的注册时机与普通注册项完全一致，少一层时序不确定性。
 */
public final class ModCreativeTab {
	public static final ResourceKey<CreativeModeTab> KEY = ResourceKey.create(
			Registries.CREATIVE_MODE_TAB, CrimsonCopperGrid.id("main"));

	public static final CreativeModeTab TAB = CreativeModeTab.builder(Row.TOP, 10)
			.title(Component.translatable("itemGroup.crimsoncoppergrid.main"))
			.icon(() -> new ItemStack(ModBlocks.CABLE))
			.displayItems((parameters, output) -> {
				output.accept(ModBlocks.CABLE);
				output.accept(ModBlocks.SWITCH);
				output.accept(ModBlocks.BATTERY);
				output.accept(ModBlocks.FUEL_GENERATOR);
				output.accept(ModBlocks.SOLAR_GENERATOR);
				output.accept(ModBlocks.WIND_GENERATOR);
				output.accept(ModBlocks.COAL_SYNTHESIZER);
				output.accept(ModBlocks.LAVA_GENERATOR);
				output.accept(ModBlocks.ELECTRIC_FURNACE);
				output.accept(ModItems.ENERGY_METER);
			})
			.build();

	private ModCreativeTab() {
	}

	public static void init() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, KEY, TAB);
	}
}
