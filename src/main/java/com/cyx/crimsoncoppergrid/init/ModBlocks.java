package com.cyx.crimsoncoppergrid.init;

import java.util.function.BiFunction;
import java.util.function.Function;

import com.cyx.crimsoncoppergrid.CrimsonCopperGrid;
import com.cyx.crimsoncoppergrid.blocks.BatteryBlock;
import com.cyx.crimsoncoppergrid.blocks.CoalSynthesizerBlock;
import com.cyx.crimsoncoppergrid.blocks.ElectricFurnaceBlock;
import com.cyx.crimsoncoppergrid.blocks.FuelGeneratorBlock;
import com.cyx.crimsoncoppergrid.blocks.LavaGeneratorBlock;
import com.cyx.crimsoncoppergrid.blocks.PowerControllerBlock;
import com.cyx.crimsoncoppergrid.blocks.SolarGeneratorBlock;
import com.cyx.crimsoncoppergrid.blocks.WindGeneratorBlock;
import com.cyx.crimsoncoppergrid.blocks.cable.CableBlock;
import com.cyx.crimsoncoppergrid.blocks.cable.SwitchBlock;
import com.cyx.crimsoncoppergrid.common.blocks.BlockMachineBase;

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
 *
 * <p>所有机器方块共用一个属性工厂 {@link BlockMachineBase#machineProperties()}，
 * 这样「全模组的机器是同一套材质语言」只需要维护一处。
 */
public final class ModBlocks {
	// ---- 输电 ----
	public static final CableBlock CABLE = register("cable", CableBlock::new, CableBlock.cableProperties());
	public static final SwitchBlock SWITCH = register("switch", SwitchBlock::new, SwitchBlock.switchProperties());
	// ---- 储能 ----
	public static final BatteryBlock BATTERY = register("battery", BatteryBlock::new, BlockMachineBase.machineProperties());
	// ---- 发电 ----
	public static final FuelGeneratorBlock FUEL_GENERATOR =
			register("fuel_generator", FuelGeneratorBlock::new, BlockMachineBase.machineProperties());
	public static final SolarGeneratorBlock SOLAR_GENERATOR =
			register("solar_generator", SolarGeneratorBlock::new, BlockMachineBase.machineProperties());
	public static final WindGeneratorBlock WIND_GENERATOR =
			register("wind_generator", WindGeneratorBlock::new, BlockMachineBase.machineProperties());
	// ---- 用电 ----
	public static final CoalSynthesizerBlock COAL_SYNTHESIZER =
			register("coal_synthesizer", CoalSynthesizerBlock::new, BlockMachineBase.machineProperties());
	public static final LavaGeneratorBlock LAVA_GENERATOR =
			register("lava_generator", LavaGeneratorBlock::new, BlockMachineBase.machineProperties());
	public static final ElectricFurnaceBlock ELECTRIC_FURNACE =
			register("electric_furnace", ElectricFurnaceBlock::new, BlockMachineBase.machineProperties());
	// ---- 仪表 ----
	public static final PowerControllerBlock POWER_CONTROLLER =
			register("power_controller", PowerControllerBlock::new, BlockMachineBase.machineProperties());

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
