package com.cyx.crimsoncoppergrid.common.powerSystem;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.cyx.crimsoncoppergrid.blockentity.cable.CableBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

/**
 * 一张电网的静态画像：接了多少设备、能发多少电、能吃掉多少电、存了多少电。
 *
 * <h2>为什么统计的是「能力」而不是「实时功率」</h2>
 * 推流模型里没有中心结算器，谁都拿不到「这一 tick 全网流过了多少电」这个数 ——
 * 那正是删掉自研 Grid 的代价，也是它的收益（不再有中心对象需要维护）。
 * 能准确说出来的只有两种量：
 * <ul>
 *   <li><b>设备能力</b>：所有发电设备的输出上限之和、所有用电设备的输入上限之和，
 *       这是电网的「理论吞吐」；</li>
 *   <li><b>存量</b>：电池里存了多少、导线池里存了多少 —— 存量在涨就说明确实在发电。</li>
 * </ul>
 * 界面把这两类量分开显示，不去编一个看起来很精确、其实只是估算的「当前功率」。
 */
public record GridStats(
		boolean connected,
		int cables,
		int generators, long generatorOutput,
		int consumers, long consumerInput,
		int batteries, long batteryStored, long batteryCapacity,
		long networkStored) {

	/** 控制器旁边没有导线（或导线处于断路状态）时的空结果。 */
	public static final GridStats DISCONNECTED = new GridStats(false, 0, 0, 0, 0, 0, 0, 0, 0, 0);

	/**
	 * 从控制器所在位置扫一遍电网。
	 *
	 * <p>入口是「六个方向里第一个导通的导线」—— 控制器本身不参与能量传输，
	 * 它的角色相当于一个挂在电网上的仪表。旁边没有导线就返回
	 * {@link #DISCONNECTED}，界面会提示玩家先接线。
	 */
	public static GridStats scan(ServerLevel level, BlockPos controllerPos) {
		CableBlockEntity start = null;
		for (Direction direction : Direction.values()) {
			if (level.getBlockEntity(controllerPos.relative(direction)) instanceof CableBlockEntity cable
					&& cable.conducts()) {
				start = cable;
				break;
			}
		}
		if (start == null) {
			return DISCONNECTED;
		}

		List<CableBlockEntity> network = collectCables(level, start);

		int cables = network.size();
		int generators = 0;
		int consumers = 0;
		int batteries = 0;
		long generatorOutput = 0;
		long consumerInput = 0;
		long batteryStored = 0;
		long batteryCapacity = 0;
		long networkStored = 0;

		// 一台设备可能同时贴着好几段导线，用坐标集合保证只统计一次
		Set<BlockPos> visitedDevices = new HashSet<>();

		for (CableBlockEntity cable : network) {
			networkStored += cable.getEnergy();

			for (Direction direction : Direction.values()) {
				BlockPos devicePos = cable.getBlockPos().relative(direction);
				if (!visitedDevices.add(devicePos)) {
					continue;
				}
				if (!(level.getBlockEntity(devicePos) instanceof PowerAcceptorBlockEntity machine)) {
					continue;
				}

				long input = machine.getMaxInput(null);
				long output = machine.getMaxOutput(null);
				if (output > 0 && input > 0) {
					// 双向 = 电池
					batteries++;
					batteryStored += machine.getStored();
					batteryCapacity += machine.getMaxStoredPower();
				} else if (output > 0) {
					generators++;
					generatorOutput += output;
				} else if (input > 0) {
					consumers++;
					consumerInput += input;
				}
			}
		}

		return new GridStats(true, cables, generators, generatorOutput, consumers, consumerInput,
				batteries, batteryStored, batteryCapacity, networkStored);
	}

	/**
	 * 沿导线做一次只读的洪水填充。
	 *
	 * <p>与 tick 期间的组网共用同一套判定口径：电闸断开就不跨越（两侧是两张网）、
	 * 不跨未加载区块。区别只是这里全用局部变量，不碰 tick 的中间态，
	 * 因此可以随时调用。
	 */
	private static List<CableBlockEntity> collectCables(ServerLevel level, CableBlockEntity start) {
		List<CableBlockEntity> found = new ArrayList<>();
		Set<BlockPos> visited = new HashSet<>();
		Deque<CableBlockEntity> queue = new ArrayDeque<>();

		queue.add(start);
		visited.add(start.getBlockPos());

		while (!queue.isEmpty()) {
			CableBlockEntity current = queue.removeFirst();
			found.add(current);

			for (Direction direction : Direction.values()) {
				BlockPos neighbourPos = current.getBlockPos().relative(direction);
				if (!level.isLoaded(neighbourPos) || !(level.getBlockEntity(neighbourPos) instanceof CableBlockEntity adjacent)) {
					continue;
				}
				if (!adjacent.conducts() || !visited.add(neighbourPos)) {
					continue;
				}
				queue.add(adjacent);
			}
		}
		return found;
	}
}
