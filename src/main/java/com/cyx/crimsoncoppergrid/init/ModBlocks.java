package com.cyx.crimsoncoppergrid.init;

import java.util.function.BiFunction;
import java.util.function.Function;

import com.cyx.crimsoncoppergrid.CrimsonCopperGrid;
import com.cyx.crimsoncoppergrid.blocks.cable.CableBlock;
import com.cyx.crimsoncoppergrid.blocks.cable.SwitchBlock;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * 方块注册（对齐 TechReborn 的 {@code init} 包集中声明）。
 *
 * <p>顺序有讲究：方块 -> 方块实体（要引用方块）-> 物品 -> 物品栏。
 * 参考 Java 类初始化的时机，由 {@link CrimsonCopperGrid#onInitialize()} 显式触发。
 */
public final class ModBlocks {
	// ---- 输电 ----
	public static final CableBlock CABLE = register("cable", CableBlock::new, CableBlock.cableProperties());
	public static final SwitchBlock SWITCH = register("switch", SwitchBlock::new, SwitchBlock.switchProperties());
	public static final com.cyx.crimsoncoppergrid.block.BatteryBlock BATTERY =
			register("battery", com.cyx.crimsoncoppergrid.block.BatteryBlock::new,
					com.cyx.crimsoncoppergrid.block.BatteryBlock.batteryProperties());
	// ---- 发电 ----
	public static final com.cyx.crimsoncoppergrid.block.AbstractGeneratorBlock.Fuel FUEL_GENERATOR =
			register("fuel_generator", com.cyx.crimsoncoppergrid.block.AbstractGeneratorBlock.Fuel::new,
					com.cyx.crimsoncoppergrid.block.AbstractGeneratorBlock.generatorProperties());
	public static final com.cyx.crimsoncoppergrid.block.AbstractGeneratorBlock.Solar SOLAR_GENERATOR =
			register("solar_generator", com.cyx.crimsoncoppergrid.block.AbstractGeneratorBlock.Solar::new,
					com.cyx.crimsoncoppergrid.block.AbstractGeneratorBlock.generatorProperties());
	public static final com.cyx.crimsoncoppergrid.block.AbstractGeneratorBlock.Wind WIND_GENERATOR =
			register("wind_generator", com.cyx.crimsoncoppergrid.block.AbstractGeneratorBlock.Wind::new,
					com.cyx.crimsoncoppergrid.block.AbstractGeneratorBlock.generatorProperties());
	// ---- 用电 ----
	public static final com.cyx.crimsoncoppergrid.block.CoalSynthesizerBlock COAL_SYNTHESIZER =
			register("coal_synthesizer", com.cyx.crimsoncoppergrid.block.CoalSynthesizerBlock::new,
					com.cyx.crimsoncoppergrid.block.CoalSynthesizerBlock.machineProperties());
	public static final com.cyx.crimsoncoppergrid.block.LavaGeneratorBlock LAVA_GENERATOR =
			register("lava_generator", com.cyx.crimsoncoppergrid.block.LavaGeneratorBlock::new,
					com.cyx.crimsoncoppergrid.block.LavaGeneratorBlock.machineProperties());
	public static final com.cyx.crimsoncoppergrid.block.ElectricFurnaceBlock ELECTRIC_FURNACE =
			register("electric_furnace", com.cyx.crimsoncoppergrid.block.ElectricFurnaceBlock::new,
					com.cyx.crimsoncoppergrid.block.ElectricFurnaceBlock.machineProperties());

	private ModBlocks() {
	}

	public static void init() {
	}

	private static ResourceKey<Block> blockKey(String name) {
		return ResourceKey.create(Registries.BLOCK, CrimsonCopperGrid.id(name));
	}

	private static ResourceKey<Item> itemKey(String name) {
		return ResourceKey.create(Registries.ITEM, CrimsonCopperGrid.id(name));
	}

	private static <T extends Block> T register(String name, Function<BlockBehaviour.Properties, T> factory,
			BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = blockKey(name);
		T block = factory.apply(properties.setId(key));
		Registry.register(BuiltInRegistries.BLOCK, key, block);
		registerBlockItem(name, block);
		return block;
	}

	private static void registerBlockItem(String name, Block block) {
		ResourceKey<Item> key = itemKey(name);
		Item item = new BlockItem(block, new Item.Properties().setId(key).useBlockDescriptionPrefix());
		Registry.register(BuiltInRegistries.ITEM, key, item);
	}

	/** 供需要「无对应方块的物品」或自定义物品使用。 */
	public static <T extends Item> T registerItem(String name, BiFunction<ResourceKey<Item>, Item.Properties, T> factory) {
		ResourceKey<Item> key = itemKey(name);
		T item = factory.apply(key, new Item.Properties().setId(key));
		Registry.register(BuiltInRegistries.ITEM, key, item);
		return item;
	}
}
