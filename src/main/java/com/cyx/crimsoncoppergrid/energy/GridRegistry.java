package com.cyx.crimsoncoppergrid.energy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cyx.crimsoncoppergrid.block.CableBlock;
import com.cyx.crimsoncoppergrid.block.SwitchBlock;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * 每个 {@link ServerLevel} 一份的电网注册表。
 *
 * <p>组网策略：电线是纯导体。从某个设备出发，先沿电线洪水填充拿到所有可达设备，
 * 这些设备 + 途中经过的电线构成一个 {@link Grid}。电线之间不跨越未加载的区块
 * （拿不到的方块实体就当作断路），这样不会把远处未加载区域的设备错误地连成一片。
 *
 * <p>性能：不在每次结算时重新组网。只有在 {电线/设备} 被放置、破坏，或开关状态变化时，
 * 才把涉及的网络标脏，下一 tick 重建一次。
 */
public final class GridRegistry {
	private static final Map<ServerLevel, GridRegistry> REGISTRIES = new IdentityHashMap<>();
	private static boolean tickHookInstalled;

	private final ServerLevel level;
	private final List<Grid> grids = new ArrayList<>();
	private final Map<BlockPos, Grid> membership = new HashMap<>();
	private final Set<BlockPos> dirty = new LinkedHashSet<>();
	private final Map<BlockPos, BlockPos> cableOwners = new HashMap<>();
	private int tickCounter;

	private GridRegistry(ServerLevel level) {
		this.level = level;
	}

	public static GridRegistry get(ServerLevel level) {
		if (!tickHookInstalled) {
			tickHookInstalled = true;
			ServerTickEvents.END_SERVER_TICK.register(server -> {
				for (ServerLevel serverLevel : server.getAllLevels()) {
					get(serverLevel).tick();
				}
			});
			// 服务器停服时丢弃各世界的电网，避免跨存档残留陈旧的 Level 引用
			ServerLifecycleEvents.SERVER_STOPPED.register(server -> REGISTRIES.clear());
		}
		return REGISTRIES.computeIfAbsent(level, GridRegistry::new);
	}

	public static GridRegistry get(Level level) {
		return level instanceof ServerLevel serverLevel ? get(serverLevel) : null;
	}

	public ServerLevel level() {
		return level;
	}

	/** 世界卸载时丢弃状态，避免陈旧引用。 */
	public static void invalidate(ServerLevel level) {
		REGISTRIES.remove(level);
	}

	// ---------------------------------------------------------------- 对外接口

	/** 找到坐标所在的电网。 */
	public Grid gridAt(BlockPos pos) {
		return membership.get(pos);
	}

	/** 节点被放置/移除/状态变化时调用，标记其所属区域需要重新组网。 */
	public void markDirty(BlockPos pos) {
		dirty.add(pos.immutable());
	}

	/**
	 * 计算某个电线方块应该显示哪些连接方向。
	 *
	 * <p>规则：邻居是电线/导通的电闸 -> 连；邻居是带能量能力的设备 -> 连；其余不连。
	 * 这样悬空的电线不会长出多余的触手，「连接状态」的显示才有意义。
	 */
	public boolean shouldConnect(BlockPos cablePos, Direction side) {
		BlockPos neighbor = cablePos.relative(side);
		if (!level.isLoaded(neighbor)) {
			return false;
		}
		BlockState state = level.getBlockState(neighbor);
		if (SwitchBlock.isConductor(state)) {
			return true;
		}
		BlockEntity be = level.getBlockEntity(neighbor);
		return be instanceof EnergyStorage;
	}

	/** 该方块状态是否能导电（电线，或处于开启状态的电闸）。 */
	public static boolean isConductor(BlockState state) {
		return state.getBlock() instanceof CableBlock || SwitchBlock.isConductor(state);
	}

	// ---------------------------------------------------------------- tick

	private void tick() {
		rebuildDirty();

		tickCounter++;
		if (tickCounter < Grid.SETTLE_INTERVAL) {
			return;
		}
		tickCounter = 0;

		// 结算过程中可能触发重建（例如发电机耗尽了燃料把自己标脏），先快照
		for (Grid grid : new ArrayList<>(grids)) {
			if (!grid.isEmpty()) {
				grid.settle();
			}
		}
	}

	private void rebuildDirty() {
		if (dirty.isEmpty()) {
			return;
		}
		// 先把脏坐标映射到各自的网络；同一网络的多个脏坐标只重建一次
		Set<Grid> affected = new LinkedHashSet<>();
		for (BlockPos pos : dirty) {
			Grid grid = membership.get(pos);
			if (grid != null) {
				affected.add(grid);
			} else {
				// 新放置的方块还没有归属，需要从它的邻居里找一个现有网络来重建
				for (Direction side : Direction.values()) {
					Grid neighborGrid = membership.get(pos.relative(side));
					if (neighborGrid != null) {
						affected.add(neighborGrid);
						break;
					}
				}
			}
		}
		// 原本归属某个网络、后来失去归属的坐标，也要按「周围网络」处理
		for (BlockPos pos : dirty) {
			if (membership.get(pos) == null) {
				for (Direction side : Direction.values()) {
					Grid neighborGrid = membership.get(pos.relative(side));
					if (neighborGrid != null) {
						affected.add(neighborGrid);
						break;
					}
				}
			}
		}

		dirty.clear();
		for (Grid grid : affected) {
			rebuild(grid);
		}
	}

	/** 丢弃一个网络的全部成员关系，然后从它的种子重新生长。 */
	private void rebuild(Grid grid) {
		Set<BlockPos> seeds = grid.allNodes();
		discard(grid);
		for (BlockPos seed : seeds) {
			buildFrom(seed);
		}
	}

	private void discard(Grid grid) {
		for (BlockPos pos : grid.allNodes()) {
			membership.remove(pos);
			cableOwners.remove(pos);
		}
		grids.remove(grid);
	}

	/**
	 * 从一个坐标出发，沿电线收集可达设备，组装成一个新的电网。
	 * 电线本身不参与能量结算，但会把两端的设备连到一起。
	 */
	private void buildFrom(BlockPos seed) {
		if (!level.isLoaded(seed) || membership.containsKey(seed)) {
			return;
		}
		BlockEntity seedEntity = level.getBlockEntity(seed);
		if (!(seedEntity instanceof EnergyStorage)) {
			return;
		}

		Grid grid = new Grid(level);
		Set<BlockPos> visited = new LinkedHashSet<>();
		List<BlockPos> frontier = new ArrayList<>();
		frontier.add(seed.immutable());

		while (!frontier.isEmpty()) {
			BlockPos current = frontier.remove(frontier.size() - 1);
			if (!visited.add(current)) {
				continue;
			}
			if (!level.isLoaded(current)) {
				continue;
			}
			BlockState currentState = level.getBlockState(current);
			if (isConductor(currentState)) {
				grid.addCable(current);
			} else if (level.getBlockEntity(current) instanceof EnergyStorage storage) {
				grid.add(current, storage);
			} else {
				// 不是导体也不是设备，不扩散
				continue;
			}
			for (Direction side : Direction.values()) {
				BlockPos neighbor = current.relative(side);
				if (!visited.contains(neighbor)) {
					frontier.add(neighbor.immutable());
				}
			}
		}

		for (BlockPos pos : grid.allNodes()) {
			if (membership.containsKey(pos)) {
				// 已被其它网络领走，说明这次生长是重复的，回滚
				for (BlockPos claimed : grid.allNodes()) {
					membership.remove(claimed);
					cableOwners.remove(claimed);
				}
				return;
			}
		}
		for (BlockPos pos : grid.allNodes()) {
			membership.put(pos, grid);
		}
		if (!grid.isEmpty()) {
			grids.add(grid);
		}
	}

	/** 供调试/展示：当前所有网络。 */
	public List<Grid> grids() {
		return grids;
	}

	/** 让电线在邻居变化后重算自己的连接形态。 */
	public void refreshCable(BlockPos pos) {
		if (!level.isLoaded(pos)) {
			return;
		}
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof CableBlock)) {
			return;
		}
		BlockState updated = state;
		for (Direction side : Direction.values()) {
			BooleanProperty property = CableBlock.propertyFor(side);
			if (!updated.hasProperty(property)) {
				continue;
			}
			updated = updated.setValue(property, shouldConnect(pos, side));
		}
		if (updated != state) {
			level.setBlock(pos, updated, CableBlock.UPDATE_FLAGS);
		}
	}

	/** 让某个坐标及其六邻的电线刷新连接显示。 */
	public void refreshAround(BlockPos pos) {
		refreshCable(pos);
		for (Direction side : Direction.values()) {
			refreshCable(pos.relative(side));
		}
	}

	/** 该坐标是否被任何网络记录（用于清理判断）。 */
	public boolean isKnown(BlockPos pos) {
		return membership.containsKey(pos) || cableOwners.containsKey(pos);
	}

	/** 重建时清理已消失的成员。 */
	void prune() {
		for (Iterator<Grid> it = grids.iterator(); it.hasNext();) {
			Grid grid = it.next();
			grid.allNodes().removeIf(pos -> !level.isLoaded(pos) || level.getBlockEntity(pos) == null);
			if (grid.isEmpty()) {
				it.remove();
			}
		}
	}
}
