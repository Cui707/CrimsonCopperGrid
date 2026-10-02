package com.cyx.crimsoncoppergrid.init;

import com.cyx.crimsoncoppergrid.CrimsonCopperGrid;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTab.Row;
import net.minecraft.world.item.ItemStack;

/**
 * 创造模式物品栏（直接用原版构建器，不依赖 Fabric 的物品栏扩展事件，
 * 注册时机与普通注册项完全一致）。
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
				output.accept(ModBlocks.WIND_GENERATOR_BASE);
				output.accept(ModBlocks.COAL_SYNTHESIZER);
				output.accept(ModBlocks.LAVA_GENERATOR);
				output.accept(ModBlocks.ELECTRIC_FURNACE);
			})
			.build();

	private ModCreativeTab() {
	}

	public static void init() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, KEY, TAB);
	}
}
