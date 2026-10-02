package com.cyx.crimsoncoppergrid.energy;

import com.cyx.crimsoncoppergrid.CrimsonCopperGrid;
import com.cyx.crimsoncoppergrid.blocks.cable.AbstractConnectionBlock;
import com.cyx.crimsoncoppergrid.init.ModBlocks;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 启动自检：用**真实的 ServerLevel** 验证几条容易写错的判定，结果直接打进日志。
 *
 * <p>为什么要有它：接线这类问题靠读代码推断反复出错过，而「进游戏摆方块看一眼」
 * 又依赖人工操作。自检把关键判定变成可自动观察的结论。
 *
 * <p>自检只写在测试坐标上，结束后还原方块。
 */
public final class SelfCheck {
	private static boolean done;

	private SelfCheck() {
	}

	public static void install() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			if (done) {
				return;
			}
			done = true;
			run(server.overworld());
		});
	}

	private static void run(ServerLevel level) {
		int pass = 0;
		int fail = 0;

		// 1) 非我们方块不能被误判（历史上对草方块取 POWERED 抛过异常）
		for (BlockState state : new BlockState[] {
				Blocks.GRASS_BLOCK.defaultBlockState(),
				Blocks.STONE.defaultBlockState(),
				Blocks.AIR.defaultBlockState() }) {
			try {
				boolean cable = EnergyLookup.isCableBlock(state);
				boolean sw = EnergyLookup.isSwitchBlock(state);
				if (!cable && !sw) {
					pass++;
				} else {
					fail++;
					CrimsonCopperGrid.LOGGER.error("[自检] {} 被误判为我们的方块（cable={} switch={}）", state.getBlock(), cable, sw);
				}
			} catch (Throwable t) {
				fail++;
				CrimsonCopperGrid.LOGGER.error("[自检] 对 {} 判定时抛异常: {}", state.getBlock(), t.toString());
			}
		}

		// 2) 真实世界里放两段电线：判定 + 连接位写回
		BlockPos cableA = new BlockPos(0, 70, 0);
		BlockPos cableB = cableA.east();
		BlockState savedA = level.getBlockState(cableA);
		BlockState savedB = level.getBlockState(cableB);
		try {
			level.setBlock(cableA, ModBlocks.CABLE.defaultBlockState(), 3);
			level.setBlock(cableB, ModBlocks.CABLE.defaultBlockState(), 3);

			BlockState atA = level.getBlockState(cableA);
			if (EnergyLookup.isCableBlock(atA)) {
				pass++;
			} else {
				fail++;
				CrimsonCopperGrid.LOGGER.error("[自检] 世界里的电线未被 isCableBlock 识别：block={} 注册名={}",
						atA.getBlock(), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(atA.getBlock()));
			}

			if (EnergyLookup.shouldConnect(level, cableA, Direction.EAST)) {
				pass++;
			} else {
				fail++;
				CrimsonCopperGrid.LOGGER.error("[自检] shouldConnect 对相邻电线返回 false：neighbor={}",
						level.getBlockState(cableB).getBlock());
			}

			// 方块实体写回连接位（重构后的核心路径）：两侧都要重算
			if (level.getBlockEntity(cableA) instanceof com.cyx.crimsoncoppergrid.blocks.cable.CableBlockEntity a
					&& level.getBlockEntity(cableB) instanceof com.cyx.crimsoncoppergrid.blocks.cable.CableBlockEntity b) {
				a.recomputeShape();
				b.recomputeShape();
				boolean eastSet = AbstractConnectionBlock.isConnected(level.getBlockState(cableA), Direction.EAST);
				boolean westSet = AbstractConnectionBlock.isConnected(level.getBlockState(cableB), Direction.WEST);
				if (eastSet && westSet) {
					pass++;
				} else {
					fail++;
					CrimsonCopperGrid.LOGGER.error("[自检] 连接位未写回：A.east={} B.west={}", eastSet, westSet);
				}
			} else {
				fail++;
				CrimsonCopperGrid.LOGGER.error("[自检] 电线缺少方块实体：A={} B={}",
						level.getBlockEntity(cableA), level.getBlockEntity(cableB));
			}
		} catch (Throwable t) {
			fail++;
			CrimsonCopperGrid.LOGGER.error("[自检] 电线连通性验证抛异常", t);
		} finally {
			level.setBlock(cableA, savedA, 3);
			level.setBlock(cableB, savedB, 3);
		}

		// 3) 电线接机器：机器提供能量能力时应连上
		BlockPos machinePos = new BlockPos(4, 70, 0);
		BlockPos cableC = machinePos.west();
		BlockState savedMachine = level.getBlockState(machinePos);
		BlockState savedCable = level.getBlockState(cableC);
		try {
			level.setBlock(machinePos, ModBlocks.BATTERY.defaultBlockState(), 3);
			level.setBlock(cableC, ModBlocks.CABLE.defaultBlockState(), 3);
			if (EnergyLookup.shouldConnect(level, cableC, Direction.EAST)) {
				pass++;
			} else {
				fail++;
				CrimsonCopperGrid.LOGGER.error("[自检] shouldConnect 对相邻电池返回 false：neighbor={} be={}",
						level.getBlockState(machinePos).getBlock(), level.getBlockEntity(machinePos));
			}
		} catch (Throwable t) {
			fail++;
			CrimsonCopperGrid.LOGGER.error("[自检] 电线接机器验证抛异常", t);
		} finally {
			level.setBlock(machinePos, savedMachine, 3);
			level.setBlock(cableC, savedCable, 3);
		}

		CrimsonCopperGrid.LOGGER.info("[自检] 完成：通过 {} 项，失败 {} 项", pass, fail);
	}
}
