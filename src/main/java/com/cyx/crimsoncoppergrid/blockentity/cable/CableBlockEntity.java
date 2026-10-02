package com.cyx.crimsoncoppergrid.blockentity.cable;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.cyx.crimsoncoppergrid.blocks.cable.CableBlock;
import com.cyx.crimsoncoppergrid.init.ModBlockEntities;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleSidedEnergyContainer;

/**
 * 铜制电线的方块实体。结构与命名对齐 TechReborn 的 {@code CableBlockEntity}（MIT）。
 *
 * <h2>导线 = 一个共享能量池</h2>
 * 每段导线自己带一小格缓冲（{@link #BUFFER_CAPACITY}），物理上相连的一段段导线把缓冲
 * 汇成一个池子：{@link CableTickManager} 每 tick 把池内总能量按段数摊平，再与池子外面的
 * 储能（发电机、电池、用电设备）做一次收/发。于是「电网」不需要一个中心对象，
 * 它本身就是这段缓冲。这也是 Team Reborn Energy 推流模型下的标准做法。
 *
 * <h2>两件事分别由谁负责</h2>
 * <ul>
 *   <li><b>连接显示</b>（六向 blockstate）—— 本类的 {@link #updateConnections()}，
 *       在邻居变化后重算一次。与能量无关，因此断开状态的电闸也能正确显示接线；</li>
 *   <li><b>能量收发的对象</b>—— {@link #appendTargets(List)}，结果带缓存，
 *       由 {@code neighbourUpdate()} 作废。缓存让每 tick 不必重复查询六个方向的储能。</li>
 * </ul>
 * 这两件事在 TechReborn 原版里是耦合在一起的（都在 {@code appendTargets} 里顺手写 blockstate）。
 * 这里拆开，是因为耦合会导致「不参与能量网络的方块永远不更新外观」，
 * 而电闸断开时恰恰就是这种情况。
 *
 * <h2>为什么不依赖类的身份</h2>
 * v0.0.5 出现过「邻居方块名确实是 {@code crimsoncoppergrid:cable}，但
 * {@code instanceof CableBlock} 为 false」的现象 —— 普通 Java 语义下这只能解释为
 * 同一个类被加载了两份。因此这里的两个判定都换成了更稳的口径：
 * <ul>
 *   <li>「邻居是不是导线」用 {@link #isCableLike(BlockEntity)}：比较**方块实体类型对象**，
 *       取到的是注册表里那一个实例，不受类加载影响；</li>
 *   <li>「邻居有没有能量能力」用 Fabric 官方的 {@link EnergyStorage#SIDED} 查阅表，
 *       它按注册的方块实体类型分派 provider，同样不碰类的身份。</li>
 * </ul>
 */
public class CableBlockEntity extends BlockEntity implements BlockEntityTicker<CableBlockEntity> {

	/** 铜制导线的单次传输速率。铜是最基础的导体，取 LOW 档的下沿。 */
	public static final long TRANSFER_RATE = 32L;
	/** 每段导线自身的缓冲容量。网络总缓冲 = 段数 × 这个值。 */
	public static final long BUFFER_CAPACITY = TRANSFER_RATE * 4L;

	final SimpleSidedEnergyContainer energyContainer = new SimpleSidedEnergyContainer() {
		@Override
		public long getCapacity() {
			return BUFFER_CAPACITY;
		}

		@Override
		public long getMaxInsert(@Nullable Direction side) {
			return allowTransfer(side) ? TRANSFER_RATE : 0;
		}

		@Override
		public long getMaxExtract(@Nullable Direction side) {
			return allowTransfer(side) ? TRANSFER_RATE : 0;
		}

		@Override
		protected void onFinalCommit() {
			CableBlockEntity.this.setChanged();
		}
	};

	/** 上一次参与组网的 tick 序号，用来保证每个网络每 tick 只被处理一次。 */
	private long lastTick = -1;
	/** 能量目标缓存；null 表示需要重建。 */
	@Nullable
	private List<CableTarget> targets;
	/** 六个方向的查询缓存，避免每 tick 反复构造。 */
	@SuppressWarnings("unchecked")
	private final BlockApiCache<EnergyStorage, Direction>[] adjacentCaches = new BlockApiCache[6];
	/**
	 * 位掩码：本轮已经搬运过的方向。防止同一轮里对同一方向既取又送，
	 * 导致两段导线之间来回弹跳、把电反复搬运却不产生净效果。
	 */
	int blockedSides;
	/** 组网期间暂时屏蔽本体的收发电，避免外部（例如第三方管道）与网络记账打架。 */
	boolean ioBlocked;
	/** 连接显示是否需要重算。 */
	private boolean connectionsDirty = true;

	protected CableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public CableBlockEntity(BlockPos pos, BlockState state) {
		this(ModBlockEntities.CABLE, pos, state);
	}

	public static <T extends BlockEntity> BlockEntityTicker<T> ticker() {
		return (level, pos, state, blockEntity) -> ((CableBlockEntity) blockEntity)
				.tick(level, pos, state, (CableBlockEntity) blockEntity);
	}

	@Override
	public void tick(Level level, BlockPos pos, BlockState state, CableBlockEntity blockEntity) {
		if (level.isClientSide()) {
			return;
		}
		if (connectionsDirty) {
			updateConnections();
		}
		CableTickManager.handleCableTick(this);
	}

	// ------------------------------------------------------------ 对外接口

	/** 供 {@link EnergyStorage#SIDED} 查询使用。 */
	public EnergyStorage getSideEnergyStorage(@Nullable Direction side) {
		return energyContainer.getSideStorage(side);
	}

	/** 是否作为导体参与组网。电闸断开时覆写为 false，组网 BFS 会在此处截断。 */
	public boolean conducts() {
		return true;
	}

	long lastTick() {
		return lastTick;
	}

	void markTicked(long tick) {
		lastTick = tick;
	}

	// ------------------------------------------------------------ 连接显示

	/**
	 * 邻居变化时调用：连接显示与能量目标都作废，下一 tick 重算。
	 *
	 * <p>这是修掉「相邻电线不连」的关键路径 —— 由 {@code CableBlock.neighborChanged}
	 * 触发，因此放置、破坏、开关切换、区块载入都会走到这里。
	 */
	public void neighborUpdate() {
		connectionsDirty = true;
		targets = null;
	}

	/** 按当前邻居重算六个连接位并写回方块状态。状态没变就不写，避免无谓的邻居更新风暴。 */
	public void updateConnections() {
		if (!(getLevel() instanceof ServerLevel serverLevel)) {
			return;
		}
		BlockState state = getBlockState();
		if (!(state.getBlock() instanceof CableBlock)) {
			return;
		}

		BlockState updated = state;
		for (Direction side : Direction.values()) {
			updated = updated.setValue(CableBlock.property(side), shouldConnect(serverLevel, side));
		}
		if (updated != state) {
			serverLevel.setBlockAndUpdate(worldPosition, updated);
		}
		connectionsDirty = false;
	}

	private boolean shouldConnect(ServerLevel level, Direction side) {
		BlockPos neighborPos = worldPosition.relative(side);
		if (!level.isLoaded(neighborPos)) {
			return false;
		}
		// 邻居是导线 / 电闸 —— 无论电闸通断，接线端都连着，所以只看方块实体类型
		if (isCableLike(getAdjacentBlockEntity(side))) {
			return true;
		}
		// 邻居提供能量能力（发电机、电池、用电设备，以及任何第三方储能）
		return EnergyStorage.SIDED.find(level, neighborPos, side.getOpposite()) != null;
	}

	/**
	 * 邻居是不是导线或电闸。
	 *
	 * <p>比较方块实体类型对象而不是 {@code instanceof} —— 取到的是注册表里那一个实例，
	 * 即使同一个类被加载了两份也仍然成立。
	 */
	public static boolean isCableLike(@Nullable BlockEntity blockEntity) {
		if (blockEntity == null) {
			return false;
		}
		BlockEntityType<?> type = blockEntity.getType();
		return type == ModBlockEntities.CABLE || type == ModBlockEntities.SWITCH;
	}

	// ------------------------------------------------------------ 能量目标

	/** 收集本段导线可以直接收发的储能；由组网过程调用。 */
	void appendTargets(List<OfferedEnergyStorage> targetStorages) {
		if (!(getLevel() instanceof ServerLevel)) {
			return;
		}

		if (targets == null) {
			targets = new ArrayList<>();
			for (Direction direction : Direction.values()) {
				// 相邻导线属于同一个池子，不是收发的对象
				if (isCableLike(getAdjacentBlockEntity(direction))) {
					continue;
				}
				BlockApiCache<EnergyStorage, Direction> cache = getAdjacentCache(direction);
				if (cache.find(direction.getOpposite()) != null) {
					targets.add(new CableTarget(direction, cache));
				}
			}
		}

		for (CableTarget target : targets) {
			EnergyStorage storage = target.find();
			if (storage == null) {
				// 目标没了（被拆/区块未加载）：作废缓存，下一 tick 重建
				targets = null;
			} else {
				targetStorages.add(new OfferedEnergyStorage(this, target.direction(), storage));
			}
		}

		blockedSides = 0;
	}

	@Nullable
	BlockEntity getAdjacentBlockEntity(Direction direction) {
		if (!(getLevel() instanceof ServerLevel)) {
			return null;
		}
		return getAdjacentCache(direction).getBlockEntity();
	}

	private BlockApiCache<EnergyStorage, Direction> getAdjacentCache(Direction direction) {
		int index = direction.get3DDataValue();
		if (adjacentCaches[index] == null) {
			adjacentCaches[index] = BlockApiCache.create(
					EnergyStorage.SIDED, (ServerLevel) getLevel(), worldPosition.relative(direction));
		}
		return adjacentCaches[index];
	}

	/** 某个方向是否允许能量进出：导通、且本轮没有被反向操作过。 */
	private boolean allowTransfer(@Nullable Direction side) {
		if (side == null) {
			return true;
		}
		return conducts() && !ioBlocked && (blockedSides & (1 << side.ordinal())) == 0;
	}

	// ------------------------------------------------------------ 快捷访问（组网用）

	public long getEnergy() {
		return energyContainer.amount;
	}

	public void setEnergy(long energy) {
		energyContainer.amount = energy;
	}

	public void sync() {
		setChanged();
	}

	// ------------------------------------------------------------ 存档与同步

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		energyContainer.amount = input.getLongOr("energy", 0);
		// 存档里的连接位可能与实际邻居不符（例如中途装了别的模组），载入后重算一次
		connectionsDirty = true;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putLong("energy", energyContainer.amount);
	}

	@Override
	public void clearRemoved() {
		super.clearRemoved();
		neighborUpdate();
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}

	/** 一个方向的收发目标。缓存的是查询器而不是查询结果，这样邻居方块实体被替换时能自然察觉。 */
	private record CableTarget(Direction direction, BlockApiCache<EnergyStorage, Direction> cache) {

		@Nullable
		EnergyStorage find() {
			return cache.find(direction.getOpposite());
		}
	}
}
