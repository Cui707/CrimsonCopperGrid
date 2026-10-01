package com.cyx.crimsoncoppergrid.registry;

import java.util.function.BiFunction;
import java.util.function.Function;

import com.cyx.crimsoncoppergrid.CrimsonCopperGrid;
import com.cyx.crimsoncoppergrid.block.AbstractGeneratorBlock;
import com.cyx.crimsoncoppergrid.block.BatteryBlock;
import com.cyx.crimsoncoppergrid.block.CableBlock;
import com.cyx.crimsoncoppergrid.block.CoalSynthesizerBlock;
import com.cyx.crimsoncoppergrid.block.ElectricFurnaceBlock;
import com.cyx.crimsoncoppergrid.block.LavaGeneratorBlock;
import com.cyx.crimsoncoppergrid.block.SwitchBlock;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * 方块注册。
 *
 * <p>26.3 的注册要求：{@code Properties} 必须显式 {@code setId(ResourceKey)}，
 * 否则启动时会因为拿不到 id 而报错。这里用 {@link #register} 统一处理，
 * 顺便把对应的 {@link BlockItem} 一起注册好。
 */
public final class ModBlocks {
	public static final CableBlock CABLE = register("cable", CableBlock::new, CableBlock.cableProperties());
	public static final SwitchBlock SWITCH = register("switch", SwitchBlock::new, SwitchBlock.switchProperties());
	public static final BatteryBlock BATTERY = register("battery", BatteryBlock::new, BatteryBlock.batteryProperties());
	public static final AbstractGeneratorBlock.Fuel FUEL_GENERATOR = register("fuel_generator", AbstractGeneratorBlock.Fuel::new, AbstractGeneratorBlock.generatorProperties());
	public static final AbstractGeneratorBlock.Solar SOLAR_GENERATOR = register("solar_generator", AbstractGeneratorBlock.Solar::new, AbstractGeneratorBlock.generatorProperties());
	public static final AbstractGeneratorBlock.Wind WIND_GENERATOR = register("wind_generator", AbstractGeneratorBlock.Wind::new, AbstractGeneratorBlock.generatorProperties());
	public static final CoalSynthesizerBlock COAL_SYNTHESIZER = register("coal_synthesizer", CoalSynthesizerBlock::new, CoalSynthesizerBlock.machineProperties());
	public static final LavaGeneratorBlock LAVA_GENERATOR = register("lava_generator", LavaGeneratorBlock::new, LavaGeneratorBlock.machineProperties());
	public static final ElectricFurnaceBlock ELECTRIC_FURNACE = register("electric_furnace", ElectricFurnaceBlock::new, ElectricFurnaceBlock.machineProperties());

	private ModBlocks() {
	}

	/** 只用来触发类初始化，保证注册在正确的时机发生。 */
	public static void init() {
	}

	private static ResourceKey<Block> blockKey(String name) {
		return ResourceKey.create(Registries.BLOCK, CrimsonCopperGrid.id(name));
	}

	private static ResourceKey<Item> itemKey(String name) {
		return ResourceKey.create(Registries.ITEM, CrimsonCopperGrid.id(name));
	}

	private static <T extends Block> T register(String name, Function<BlockBehaviour.Properties, T> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = blockKey(name);
		T block = factory.apply(properties.setId(key));
		Registry.register(BuiltInRegistries.BLOCK, key, block);
		registerBlockItem(name, block);
		return block;
	}

	private static void registerBlockItem(String name, Block block) {
		ResourceKey<Item> itemKey = itemKey(name);
		Item item = new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix());
		Registry.register(BuiltInRegistries.ITEM, itemKey, item);
	}

	/** 供需要「方块 + 自定义物品」的场景使用（例如电池物品）。 */
	public static <T extends Item> T registerItem(String name, BiFunction<ResourceKey<Item>, Item.Properties, T> factory) {
		ResourceKey<Item> itemKey = itemKey(name);
		T item = factory.apply(itemKey, new Item.Properties().setId(itemKey));
		Registry.register(BuiltInRegistries.ITEM, itemKey, item);
		return item;
	}
}
