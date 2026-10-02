package com.cyx.crimsoncoppergrid.init;

import java.util.Set;

import com.cyx.crimsoncoppergrid.CrimsonCopperGrid;
import com.cyx.crimsoncoppergrid.blockentity.BatteryBlockEntity;
import com.cyx.crimsoncoppergrid.blockentity.CoalSynthesizerBlockEntity;
import com.cyx.crimsoncoppergrid.blockentity.ElectricFurnaceBlockEntity;
import com.cyx.crimsoncoppergrid.blockentity.FuelGeneratorBlockEntity;
import com.cyx.crimsoncoppergrid.blockentity.LavaGeneratorBlockEntity;
import com.cyx.crimsoncoppergrid.blockentity.SolarGeneratorBlockEntity;
import com.cyx.crimsoncoppergrid.blockentity.WindGeneratorBlockEntity;
import com.cyx.crimsoncoppergrid.blockentity.cable.CableBlockEntity;
import com.cyx.crimsoncoppergrid.blockentity.cable.SwitchBlockEntity;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * 方块实体类型注册。
 *
 * <p>26.3 已没有 {@code BlockEntityType.Builder}，构造函数本身就是
 * {@code (BlockEntitySupplier<T>, Set<Block>)}。
 *
 * <p>能量能力（{@code EnergyStorage.SIDED}）不在这里登记 —— 它统一由
 * {@link ModPowerRegistration} 处理，因为那边的 fallback 能一次性覆盖所有机器，
 * 不需要按类型逐个注册。
 */
public final class ModBlockEntities {
	public static final BlockEntityType<CableBlockEntity> CABLE = register(
			"cable", CableBlockEntity::new, ModBlocks.CABLE);

	public static final BlockEntityType<SwitchBlockEntity> SWITCH = register(
			"switch", SwitchBlockEntity::new, ModBlocks.SWITCH);

	public static final BlockEntityType<BatteryBlockEntity> BATTERY = register(
			"battery", BatteryBlockEntity::new, ModBlocks.BATTERY);

	public static final BlockEntityType<FuelGeneratorBlockEntity> FUEL_GENERATOR = register(
			"fuel_generator", FuelGeneratorBlockEntity::new, ModBlocks.FUEL_GENERATOR);

	public static final BlockEntityType<SolarGeneratorBlockEntity> SOLAR_GENERATOR = register(
			"solar_generator", SolarGeneratorBlockEntity::new, ModBlocks.SOLAR_GENERATOR);

	public static final BlockEntityType<WindGeneratorBlockEntity> WIND_GENERATOR = register(
			"wind_generator", WindGeneratorBlockEntity::new, ModBlocks.WIND_GENERATOR);

	public static final BlockEntityType<CoalSynthesizerBlockEntity> COAL_SYNTHESIZER = register(
			"coal_synthesizer", CoalSynthesizerBlockEntity::new, ModBlocks.COAL_SYNTHESIZER);

	public static final BlockEntityType<LavaGeneratorBlockEntity> LAVA_GENERATOR = register(
			"lava_generator", LavaGeneratorBlockEntity::new, ModBlocks.LAVA_GENERATOR);

	public static final BlockEntityType<ElectricFurnaceBlockEntity> ELECTRIC_FURNACE = register(
			"electric_furnace", ElectricFurnaceBlockEntity::new, ModBlocks.ELECTRIC_FURNACE);

	private ModBlockEntities() {
	}

	public static void init() {
	}

	private static <T extends BlockEntity> BlockEntityType<T> register(
			String name, BlockEntityType.BlockEntitySupplier<T> supplier, Block... blocks) {
		ResourceKey<BlockEntityType<?>> key = ResourceKey.create(
				Registries.BLOCK_ENTITY_TYPE, CrimsonCopperGrid.id(name));
		BlockEntityType<T> type = new BlockEntityType<>(supplier, Set.of(blocks));
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, key, type);
		return type;
	}
}
