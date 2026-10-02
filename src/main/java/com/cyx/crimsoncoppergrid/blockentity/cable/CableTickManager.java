package com.cyx.crimsoncoppergrid.blockentity.cable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import team.reborn.energy.api.EnergyStorage;

/**
 * 导线网络的每 tick 结算。算法对齐 TechReborn 的 {@code CableTickManager}（MIT）。
 *
 * <h2>一个网络每 tick 只处理一次</h2>
 * 每段导线都在 tick 里调用 {@link #handleCableTick}，但只有「本 tick 还没被处理过的」
 * 那一段会真正触发：它沿导线做一次 BFS 收集整张网络，处理完把网络内所有导线标记为本 tick 已处理。
 * 其余导线的调用随即变成空转。
 *
 * <h2>一轮处理做三件事</h2>
 * <ol>
 *   <li><b>摊平</b>：把网络内所有导线的缓冲加总，作为池子里的能量；</li>
 *   <li><b>抽水与注水</b>：从相邻的外部储能抽电（最多抽到池子装满），
 *       再向相邻的外部储能送电（最多送出池子里的全部余量）；</li>
 *   <li><b>重新分配</b>：把池子里的能量按段数平均写回每段导线。</li>
 * </ol>
 *
 * <h2>相对 TechReborn 原版的两处改动</h2>
 * <ul>
 *   <li><b>状态按维度隔离</b>：原版把收集中间态放在静态字段里，跨维度共用。
 *       服务端单线程顺序 tick 各维度，所以实际不会串，但依赖这个前提不如直接按维度分开。
 *       这里每个 {@link ServerLevel} 一份实例。</li>
 *   <li><b>{@code ioBlocked} 无条件复位</b>：原版只在正常流程末尾复位，
 *       中途抛异常会让相关导线永久屏蔽收发（表现为「电网忽然不工作了」）。
 *       这里放进 {@code finally}。</li>
 * </ul>
 */
public final class CableTickManager {
	private static final Map<ServerLevel, CableTickManager> INSTANCES = new IdentityHashMap<>();
	private static boolean hookInstalled;
	/** 全局 tick 序号，用来做「本 tick 是否已处理」的判据。 */
	private static long tickCounter;

	private final ServerLevel level;
	private final List<CableBlockEntity> cableList = new ArrayList<>();
	private final List<OfferedEnergyStorage> targetStorages = new ArrayList<>();
	private final Deque<CableBlockEntity> bfsQueue = new ArrayDeque<>();

	private CableTickManager(ServerLevel level) {
		this.level = level;
	}

	/** 在模组入口调用一次，让 tick 序号从世界开始前就在累加。 */
	public static void init() {
		if (hookInstalled) {
			return;
		}
		hookInstalled = true;
		ServerTickEvents.START_SERVER_TICK.register(server -> tickCounter++);
		// 停服时丢弃各世界的中间态，避免跨存档残留陈旧的 Level 引用
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			INSTANCES.clear();
			tickCounter = 0;
		});
	}

	public static CableTickManager get(ServerLevel level) {
		if (!hookInstalled) {
			init();
		}
		return INSTANCES.computeIfAbsent(level, CableTickManager::new);
	}

	/** 导线 tick 的入口。 */
	public static void handleCableTick(CableBlockEntity startingCable) {
		if (!(startingCable.getLevel() instanceof ServerLevel serverLevel)) {
			return;
		}
		get(serverLevel).tick(startingCable);
	}

	private void tick(CableBlockEntity startingCable) {
		if (!shouldTickCable(startingCable) || !startingCable.conducts()) {
			return;
		}

		try {
			gatherCables(startingCable);
			if (cableList.isEmpty()) {
				return;
			}

			long networkCapacity = 0;
			long networkAmount = 0;
			for (CableBlockEntity cable : cableList) {
				networkAmount += cable.getEnergy();
				networkCapacity += CableBlockEntity.BUFFER_CAPACITY;

				cable.appendTargets(targetStorages);
				// 组网期间直接操作池内总量，所以要暂时屏蔽本体进出，
				// 否则外部（例如第三方管道反过来指向导线）会让账目与实际不同步
				cable.ioBlocked = true;
			}

			// 兜底：存档异常时池内总量可能超过容量
			if (networkAmount > networkCapacity) {
				networkAmount = networkCapacity;
			}

			// 先从外部抽电，再向外部送电
			networkAmount += dispatchTransfer(EnergyStorage::extract, networkCapacity - networkAmount);
			networkAmount -= dispatchTransfer(EnergyStorage::insert, networkAmount);

			// 摊平回每段导线
			int cableCount = cableList.size();
			for (CableBlockEntity cable : cableList) {
				cable.setEnergy(networkAmount / cableCount);
				networkAmount -= cable.getEnergy();
				cableCount--;
				cable.sync();
			}
		} finally {
			// 无论正常结束还是中途异常，都必须把 ioBlocked 复位
			for (CableBlockEntity cable : cableList) {
				cable.ioBlocked = false;
			}
			cableList.clear();
			targetStorages.clear();
			bfsQueue.clear();
		}
	}

	private static boolean shouldTickCable(CableBlockEntity cable) {
		// 每个网络每 tick 只处理一次
		if (cable.lastTick() == tickCounter) {
			return false;
		}
		// 不在已加载区块里的导线不参与
		ServerLevel level = cable.getLevel() instanceof ServerLevel sw ? sw : null;
		return level != null && level.isLoaded(cable.getBlockPos());
	}

	/** 沿导线做一次 BFS，把所有物理相连且在导通的导线收集起来。 */
	private void gatherCables(CableBlockEntity start) {
		if (!shouldTickCable(start)) {
			return;
		}
		bfsQueue.add(start);
		start.markTicked(tickCounter);
		cableList.add(start);

		while (!bfsQueue.isEmpty()) {
			CableBlockEntity current = bfsQueue.removeFirst();
			for (Direction direction : Direction.values()) {
				if (!(current.getAdjacentBlockEntity(direction) instanceof CableBlockEntity adjacent)) {
					continue;
				}
				// 电闸断开 → 在此截断，两侧成为两张独立的网
				if (!adjacent.conducts()) {
					continue;
				}
				if (!shouldTickCable(adjacent)) {
					continue;
				}
				bfsQueue.add(adjacent);
				adjacent.markTicked(tickCounter);
				cableList.add(adjacent);
			}
		}
	}

	/**
	 * 对全部目标执行一轮同向搬运。
	 *
	 * <p>先按「实际能搬多少」升序排序再依次分配，让余量少的目标先拿，
	 * 避免被大目标一次吃光；排序前先打乱，是为了在能力相同的目标之间平均分配。
	 * 每次搬运的额度还要除以「剩余目标数」，这样一轮下来余量能摊得比较均匀。
	 */
	private long dispatchTransfer(TransferOperation operation, long maxAmount) {
		List<SortableStorage> sorted = new ArrayList<>(targetStorages.size());
		for (OfferedEnergyStorage storage : targetStorages) {
			sorted.add(new SortableStorage(operation, storage));
		}
		Collections.shuffle(sorted);
		sorted.sort(Comparator.comparingLong(sortable -> sortable.simulationResult));

		try (Transaction transaction = Transaction.openOuter()) {
			long transferred = 0;
			for (int i = 0; i < sorted.size(); i++) {
				SortableStorage target = sorted.get(i);
				int remainingTargets = sorted.size() - i;
				long remainingAmount = maxAmount - transferred;
				long targetMaxAmount = Math.min(
						remainingAmount / remainingTargets,
						CableBlockEntity.TRANSFER_RATE);

				long localTransferred = operation.transfer(
						target.storage.storage(), targetMaxAmount, transaction);
				if (localTransferred > 0) {
					transferred += localTransferred;
					// 标记该方向，阻止同一轮内的反向重复搬运
					target.storage.afterTransfer();
				}
			}
			transaction.commit();
			return transferred;
		}
	}

	@FunctionalInterface
	private interface TransferOperation {
		long transfer(EnergyStorage storage, long maxAmount, TransactionContext transaction);
	}

	/** 目标 + 试算结果。试算在开事务外做，用来排序。 */
	private static final class SortableStorage {
		private final OfferedEnergyStorage storage;
		private final long simulationResult;

		SortableStorage(TransferOperation operation, OfferedEnergyStorage storage) {
			this.storage = storage;
			// 在只读事务里试算一次；不 commit，所以不会真正改数据
			try (Transaction tx = Transaction.openOuter()) {
				this.simulationResult = operation.transfer(storage.storage(), Long.MAX_VALUE, tx);
			}
		}
	}

	/** 供自检与调试：当前世界已登记的网络中间态规模（正常应为 0）。 */
	public int pendingCables() {
		return cableList.size();
	}

	// ------------------------------------------------------------ 只读查询

	/**
	 * 从某段导线出发，统计它所在网络的规模与池内电量。供电网总览器使用。
	 *
	 * <p>刻意不复用 {@link #gatherCables} —— 那个方法会写实例字段（{@code cableList} /
	 * {@code bfsQueue} / {@code tickCounter} 标记），只能在 tick 期间调用。
	 * 在 tick 之外碰它会把中间态留在实例里，污染同 tick 的正常结算。
	 * 这里用一组局部容器重做一遍 BFS，多花一点内存，换「随时可调用」。
	 *
	 * @return 该坐标不是导通导线的一部分时返回 {@code null}
	 */
	@Nullable
	public static NetworkSummary summarize(ServerLevel level, BlockPos startPos) {
		if (!(level.getBlockEntity(startPos) instanceof CableBlockEntity start) || !start.conducts()) {
			return null;
		}

		List<CableBlockEntity> found = new ArrayList<>();
		Set<BlockPos> visited = new HashSet<>();
		Deque<CableBlockEntity> queue = new ArrayDeque<>();
		queue.add(start);
		visited.add(start.getBlockPos());

		while (!queue.isEmpty()) {
			CableBlockEntity current = queue.removeFirst();
			found.add(current);
			for (Direction direction : Direction.values()) {
				if (!(current.getAdjacentBlockEntity(direction) instanceof CableBlockEntity adjacent)) {
					continue;
				}
				// 电闸断开 → 两侧是两张网，这里与组网保持同一口径
				if (!adjacent.conducts() || !level.isLoaded(adjacent.getBlockPos())) {
					continue;
				}
				if (visited.add(adjacent.getBlockPos())) {
					queue.add(adjacent);
				}
			}
		}

		long stored = 0;
		for (CableBlockEntity cable : found) {
			stored += cable.getEnergy();
		}
		return new NetworkSummary(found.size(), stored, (long) found.size() * CableBlockEntity.BUFFER_CAPACITY);
	}

	/** 一张导线网络的概况：多少段、池里有多少电、上限是多少。 */
	public record NetworkSummary(int cables, long stored, long capacity) {
	}
}
