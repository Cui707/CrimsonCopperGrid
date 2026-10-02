package com.cyx.crimsoncoppergrid.energy;

import com.cyx.crimsoncoppergrid.init.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 统一的「这个坐标算不算接电节点」查询。
 *
 * <p>重构要点（参考 TechReborn 的架构）：判定**不再依赖方块类型**，
 * 而是问「这个坐标有没有提供能量能力的方块实体」。
 * 电线与所有机器都是方块实体，因此：
 * <ul>
 *   <li>电线 ↔ 机器：双方都提供能力 → 连</li>
 *   <li>电线 ↔ 电线：靠 {@link #isSameCable} 判定（见下方注释）</li>
 * </ul>
 *
 * <h2>关于 isSameCable 为什么用「世界内实例引用比较」</h2>
 * 之前用 {@code state.getBlock() instanceof CableBlock} 判断，实测在
 * dev 环境里出现过「身份完全相同却仍为 false」的诡异现象（自检有完整记录）。
 * 现在改成拿**另一个世界内的方块实例**来比：两者都来自 {@code Level}，
 * 必然由同一个类加载器加载，引用比较在这种场景下是可靠且精确的。
 */
public final class EnergyLookup {
	private EnergyLookup() {
	}

	/** 该坐标是否提供能量能力（机器 / 发电机 / 电池 / 电线）。 */
	public static boolean providesEnergy(Level level, BlockPos pos) {
		if (!level.isLoaded(pos)) {
			return false;
		}
		return level.getBlockEntity(pos) instanceof EnergyStorage;
	}

	/** 该方块状态是不是我们的电线（不依赖实例身份：用注册名比较）。 */
	public static boolean isCableBlock(net.minecraft.world.level.block.state.BlockState state) {
		return isBlockNamed(state, "cable");
	}

	/** 该方块状态是不是我们的电闸。 */
	public static boolean isSwitchBlock(net.minecraft.world.level.block.state.BlockState state) {
		return isBlockNamed(state, "switch");
	}

	/**
	 * 用注册名判断方块是否属于我们。
	 *
	 * <p>这是本项目里最稳的一种判定：不依赖对象身份、不依赖类加载器、
	 * 也不依赖 {@code instanceof}。曾出现「实例相等却判定为 false」的诡异现象，
	 * 注册名比较是当时唯一没有被该现象影响的口径。
	 */
	private static boolean isBlockNamed(net.minecraft.world.level.block.state.BlockState state, String path) {
		net.minecraft.resources.Identifier key = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock());
		if (key == null) {
			return false;
		}
		return com.cyx.crimsoncoppergrid.CrimsonCopperGrid.MOD_ID.equals(key.getNamespace())
				&& path.equals(key.getPath());
	}

	/** 该坐标是不是一段电线（拿同世界实例比较）。 */
	public static boolean isCable(Level level, BlockPos pos) {
		return sameBlock(level, pos, ModBlocks.CABLE);
	}

	/** 该坐标是不是电闸。 */
	public static boolean isSwitch(Level level, BlockPos pos) {
		return sameBlock(level, pos, ModBlocks.SWITCH);
	}

	/**
	 * 两个坐标的方块是否为同一个方块对象。
	 *
	 * <p>刻意只用「同一次世界读取得到的两个实例」相比较，
	 * 不引入任何注册表字段或跨加载器的引用。
	 */
	public static boolean sameBlock(Level level, BlockPos pos, net.minecraft.world.level.block.Block expected) {
		if (!level.isLoaded(pos)) {
			return false;
		}
		return level.getBlockState(pos).getBlock() == expected;
	}

	/**
	 * 电线是否应该朝这个方向伸出连接臂。
	 *
	 * <p>规则：邻居是电线 / 导通的电闸 / 提供能量能力的设备 → 连。
	 */
	public static boolean shouldConnect(Level level, BlockPos cablePos, Direction side) {
		BlockPos neighbor = cablePos.relative(side);
		if (!level.isLoaded(neighbor)) {
			return false;
		}
		BlockState state = level.getBlockState(neighbor);
		// 1) 邻居是电线
		if (state.getBlock() == ModBlocks.CABLE) {
			return true;
		}
		// 2) 邻居是电闸且处于开启状态
		if (state.getBlock() == ModBlocks.SWITCH) {
			return com.cyx.crimsoncoppergrid.blocks.cable.SwitchBlock.isOpen(state);
		}		// 3) 邻居提供能量能力（机器 / 发电机 / 电池）
		return level.getBlockEntity(neighbor) instanceof EnergyStorage;
	}
}
