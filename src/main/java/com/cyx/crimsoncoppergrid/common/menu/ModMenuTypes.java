package com.cyx.crimsoncoppergrid.common.menu;

import com.cyx.crimsoncoppergrid.CrimsonCopperGrid;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/**
 * 菜单类型注册（对齐 TechReborn 的 {@code GuiType}，但 CCG 的界面不需要向客户端
 * 传方块坐标，所以直接用原版 {@code MenuType} 即可，不必动用 Fabric 的
 * {@code ExtendedMenuType}）。
 *
 * <p><b>注意</b>：26.3 里 {@code MenuType} 的构造函数是包私有的，而 Fabric API 通过
 * 传递性访问拓宽（{@code fabric-transitive-access-wideners-v1}）把它打开为
 * {@code protected}，因此这里可以直接 {@code new}。这与 TechReborn 的做法一致。
 */
public final class ModMenuTypes {

	public static final MenuType<BatteryMenu> BATTERY =
			register("battery", BatteryMenu::new);

	public static final MenuType<ElectricFurnaceMenu> ELECTRIC_FURNACE =
			register("electric_furnace", ElectricFurnaceMenu::new);

	public static final MenuType<FuelGeneratorMenu> FUEL_GENERATOR =
			register("fuel_generator", FuelGeneratorMenu::new);

	public static final MenuType<CoalSynthesizerMenu> COAL_SYNTHESIZER =
			register("coal_synthesizer", CoalSynthesizerMenu::new);

	public static final MenuType<SolarGeneratorMenu> SOLAR_GENERATOR =
			register("solar_generator", SolarGeneratorMenu::new);

	public static final MenuType<WindGeneratorMenu> WIND_GENERATOR =
			register("wind_generator", WindGeneratorMenu::new);

	public static final MenuType<LavaGeneratorMenu> LAVA_GENERATOR =
			register("lava_generator", LavaGeneratorMenu::new);

	private ModMenuTypes() {
	}

	/** 触发类加载，让上面的静态字段完成注册。 */
	public static void init() {
	}

	private static <T extends AbstractContainerMenu> MenuType<T> register(String name, MenuType.MenuSupplier<T> factory) {
		MenuType<T> type = new MenuType<>(factory, FeatureFlags.VANILLA_SET);
		return Registry.register(BuiltInRegistries.MENU, CrimsonCopperGrid.id(name), type);
	}
}
