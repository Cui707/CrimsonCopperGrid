package com.cyx.crimsoncoppergrid.blockentity;

import com.cyx.crimsoncoppergrid.common.menu.ContainerDataCodec;
import com.cyx.crimsoncoppergrid.common.menu.WindGeneratorMenu;
import com.cyx.crimsoncoppergrid.common.powerSystem.PowerAcceptorBlockEntity;
import com.cyx.crimsoncoppergrid.init.ModBlockEntities;
import com.cyx.crimsoncoppergrid.init.ModBlocks;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 风力发电机：塔越高，风越大。
 *
 * <h2>怎么量「塔高」</h2>
 * 塔高 = <b>发电机正下方那一列里，与它间隔的方块数</b>，一直往下数到
 * {@link ModBlocks#WIND_GENERATOR_BASE 风机底座} 为止。底座本身不算在内 ——
 * 所以「底座 + 3 格方块 + 发电机」的塔高是 3，不是 4。
 *
 * <p>用底座当锚点而不是用 Y 坐标，是因为塔高是玩家搭出来的结构，
 * 世界数据里看不出哪几块是玩家放的。走「与底座的高度差」之后，
 * 同样的塔在平原和空岛上输出完全一致。
 *
 * <h2>发电档位</h2>
 * <pre>
 *   塔高 1~2    → 0 FE/t    （太矮，形不成风道）
 *   塔高 3~5    → {@link #MIN_OUTPUT} FE/t
 *   塔高 6~8    → 20 FE/t
 *   塔高 ≥9     → {@link #MAX_OUTPUT} FE/t
 * </pre>
 *
 * <h2>停机条件</h2>
 * <ol>
 *   <li><b>正上方被挡住</b> —— 风口不能封死，见 {@link #STATUS_OBSTRUCTED}；</li>
 *   <li><b>下方没有底座</b>（或塔身中间断开了）—— 见 {@link #STATUS_NO_BASE}；</li>
 *   <li><b>塔太矮</b> —— 见 {@link #STATUS_TOO_SHORT}。</li>
 * </ol>
 *
 * <h2>叶轮</h2>
 * 转动的叶轮由客户端渲染器（{@code WindTurbineRenderer}，移植自 TechReborn）画，
 * 角度推进在两个 {@code public} 字段里：{@link #bladeAngle} 与 {@link #spinSpeed}。
 * 它们**不入存档、不参与同步** —— 纯粹是客户端每帧插值用的动画状态，
 * 所以放在方块实体上而不是渲染器里：渲染器每帧都会重建 render state，存不住。
 *
 * <h2>为什么塔高在客户端也自己算</h2>
 * 扫塔只读「方块世界里那一列的方块」，客户端手上就有这份数据，不需要同步。
 * 叶轮转速要跟着档位走，每刻都变，靠同步包送既浪费又容易抖 ——
 * 所以两侧各跑一遍 {@link #scanTowerHeight()}，逻辑只有一份，结果必然一致。
 * 同步通道只服务于界面（{@link #DATA_TOWER_HEIGHT} / {@link #DATA_STATUS}）。
 *
 * <h2>为什么扫塔要缓存</h2>
 * 一个没插在底座上的发电机（比如直接放在地面上）会一路扫到 {@link #SCAN_LIMIT} 才放弃，
 * 每刻都扫纯属浪费 —— 塔高只在玩家放/拆方块时才会变，所以按
 * {@link #RESCAN_INTERVAL} 刻的节奏重扫一次就够，慢了也不过半秒。
 */
public class WindGeneratorBlockEntity extends PowerAcceptorBlockEntity implements MenuProvider {

	// ------------------------------------------------------------ 发电参数

	/** 最低档的发电量。 */
	public static final long MIN_OUTPUT = 10L;
	/** 最高档的发电量。 */
	public static final long MAX_OUTPUT = 30L;
	/** 缓冲容量：满输出也要 33 秒才填满，只是给推流做缓冲用。 */
	public static final long CAPACITY = 1_000L;

	/** 一共几档（10 / 20 / 30）。 */
	public static final int TIER_COUNT = 3;
	/** 每升一档需要的塔高增量。 */
	public static final int TIER_STEP = 3;
	/** 满输出所需的塔高（= {@link #TIER_STEP} × {@link #TIER_COUNT} = 9）。 */
	public static final int FULL_TOWER_HEIGHT = TIER_STEP * TIER_COUNT;

	// ------------------------------------------------------------ 状态

	/** 正在发电。 */
	public static final int STATUS_GENERATING = 0;
	/** 正上方被挡住，风口封死。 */
	public static final int STATUS_OBSTRUCTED = 1;
	/** 正下方找不到风机底座（含塔身中间断开）。 */
	public static final int STATUS_NO_BASE = 2;
	/** 找到底座了，但塔高不足 {@link #TIER_STEP} 格。 */
	public static final int STATUS_TOO_SHORT = 3;

	/** {@link #towerHeight} 的特殊值：没有底座 / 塔身断开。 */
	public static final int NO_TOWER = -1;

	/**
	 * 往下最多扫多少格。
	 *
	 * <p>满输出只要 {@link #FULL_TOWER_HEIGHT} 格，留 {@link #SCAN_LIMIT} 是为了
	 * 给「底座在很远的下方」留足余量；同时兜住「发电机直接放在地面上」的情况 ——
	 * 否则会一路扫到基岩，每半秒扫近 400 格。
	 */
	private static final int SCAN_LIMIT = 64;

	/** 两次扫塔之间的间隔（刻）。10 刻 = 0.5 秒，放方块后几乎立刻能看到反馈。 */
	private static final int RESCAN_INTERVAL = 10;

	// ---- 界面数据：在父类的「存量 + 容量」之后追加输出、塔高、状态 ----

	/** 当前发电量（long，占 {@code SLOTS_PER_LONG} 格）。 */
	public static final int DATA_OUTPUT = PowerAcceptorBlockEntity.DATA_COUNT;
	/** 当前塔高（1 格）；{@link #NO_TOWER} 表示没有底座。 */
	public static final int DATA_TOWER_HEIGHT = DATA_OUTPUT + ContainerDataCodec.SLOTS_PER_LONG;
	/** 状态（1 格），取值见 {@code STATUS_*} 常量。 */
	public static final int DATA_STATUS = DATA_TOWER_HEIGHT + 1;
	/** 本类的数据槽位总数。 */
	public static final int DATA_COUNT = DATA_STATUS + 1;

	/** 缓存的塔高；{@link #NO_TOWER} 表示这一列找不到底座。 */
	private int towerHeight = NO_TOWER;
	/** 距离下次重扫还有几刻。 */
	private int rescanCountdown;

	/** 当前这一刻的发电量，也是发给客户端的那一份。 */
	private long output;
	/** 当前状态，取值见 {@code STATUS_*} 常量。 */
	private int status = STATUS_NO_BASE;

	@Override
	public int getCount() {
		return DATA_COUNT;
	}

	@Override
	public int get(int index) {
		if (ContainerDataCodec.covers(index, DATA_OUTPUT)) {
			return ContainerDataCodec.write(output, ContainerDataCodec.chunkOf(index, DATA_OUTPUT));
		}
		if (index == DATA_TOWER_HEIGHT) {
			return towerHeight;
		}
		if (index == DATA_STATUS) {
			return status;
		}
		return super.get(index);
	}

	public WindGeneratorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.WIND_GENERATOR, pos, state);
	}

	// ------------------------------------------------------------ 叶轮动画（仅客户端）

	/** 叶轮当前角度（弧度）。{@code public}：渲染器直接读。 */
	public float bladeAngle;
	/** 叶轮当前角速度（弧度/刻）。 */
	public float spinSpeed;

	/** 满风时的角速度。0.20 弧度/刻 ≈ 每秒 4 弧度，与 TechReborn 的风车同速。 */
	private static final float MAX_SPIN = 0.20F;
	/** 最低档时的角速度。 */
	private static final float MIN_SPIN = 0.06F;
	/** 起转与停转的加速度（弧度/刻²）。停得比起得慢一点，看起来更像有惯性。 */
	private static final float SPIN_ACCEL = 0.002F;
	private static final float SPIN_DECEL = 0.0015F;

	// ------------------------------------------------------------ 能量基准

	@Override
	public long getBaseMaxPower() {
		return CAPACITY;
	}

	@Override
	public long getBaseMaxOutput() {
		return MAX_OUTPUT;
	}

	@Override
	public long getBaseMaxInput() {
		return 0L;
	}

	// ------------------------------------------------------------ tick

	@Override
	protected void serverTick() {
		updateTower();
		status = evaluateStatus();
		output = status == STATUS_GENERATING ? towerOutput() : 0L;
		if (output > 0) {
			long space = getFreeSpace();
			if (space > 0) {
				addEnergy(Math.min(output, space));
			}
		}
		setActive(output > 0);
	}

	/**
	 * 客户端每刻推一次叶轮角度。
	 *
	 * <p>「在不在发电」取同步过来的 {@code ACTIVE} 方块状态，转速取本地扫出来的档位 ——
	 * 两者的来源不同但结论一致：服务端的 {@code output > 0} 与 {@code ACTIVE} 是同一件事。
	 */
	@Override
	protected void clientTick() {
		updateTower();
		float target = 0F;
		if (isActive() && towerOutput() > 0) {
			target = (float) (MIN_SPIN + (MAX_SPIN - MIN_SPIN) * speedRatio());
		}
		spinSpeed = Mth.approach(spinSpeed, target, target > spinSpeed ? SPIN_ACCEL : SPIN_DECEL);
		bladeAngle += spinSpeed;
		// 长时间挂机后浮点精度会掉，绕一圈就归零
		if (bladeAngle >= Mth.TWO_PI) {
			bladeAngle -= Mth.TWO_PI;
		}
	}

	// ------------------------------------------------------------ 塔高

	/** 按 {@link #RESCAN_INTERVAL} 的节奏重扫塔高；两侧都跑。 */
	private void updateTower() {
		if (rescanCountdown > 0) {
			rescanCountdown--;
			return;
		}
		rescanCountdown = RESCAN_INTERVAL;
		towerHeight = scanTowerHeight();
	}

	/**
	 * 沿正下方那一列往下找风机底座，数中间隔了多少格。
	 *
	 * <p>途中一旦遇到空气就判定「塔身断开」—— 中间镂空的塔不算塔，
	 * 否则玩家可以把底座放在地上、发电机直接浮在 9 格高空，白拿满输出。
	 *
	 * @return 间隔的方块数；找不到底座或塔身断开返回 {@link #NO_TOWER}
	 */
	private int scanTowerHeight() {
		Level level = getLevel();
		if (level == null) {
			return NO_TOWER;
		}
		BlockPos origin = getBlockPos();
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		int floor = Math.max(level.getMinY(), origin.getY() - SCAN_LIMIT);
		int height = 0;
		for (int y = origin.getY() - 1; y >= floor; y--) {
			cursor.set(origin.getX(), y, origin.getZ());
			BlockState state = level.getBlockState(cursor);
			if (state.is(ModBlocks.WIND_GENERATOR_BASE)) {
				return height;
			}
			if (state.isAir()) {
				return NO_TOWER;
			}
			height++;
		}
		return NO_TOWER;
	}

	/** 缓存的塔高；{@link #NO_TOWER} 表示没有底座。 */
	public int getTowerHeight() {
		return towerHeight;
	}

	/** 按塔高换算出的发电量，不考虑「上方是否被挡住」。 */
	private long towerOutput() {
		return outputForHeight(towerHeight);
	}

	/**
	 * 塔高换算成发电量。按 {@link #TIER_STEP} 一档递增：
	 * 3~5 格 {@link #MIN_OUTPUT}，6~8 格 20，9 格及以上 {@link #MAX_OUTPUT}。
	 */
	private static long outputForHeight(int height) {
		if (height < TIER_STEP) {
			return 0L;
		}
		return outputForTier(height / TIER_STEP);
	}

	/**
	 * 第 {@code tier} 档（从 1 开始）的发电量。
	 *
	 * <p>面板要写「3/6/9 格 → 10/20/30 FE/t」这张档位表。让界面调这个方法而不是
	 * 在语言文件里写死数字，改档位或改功率时面板会自动跟着变。
	 */
	public static long outputForTier(int tier) {
		int clamped = Mth.clamp(tier, 1, TIER_COUNT);
		return MIN_OUTPUT + (MAX_OUTPUT - MIN_OUTPUT) * (clamped - 1) / (TIER_COUNT - 1);
	}

	/** 第 {@code tier} 档所需的塔高（从 1 开始）。 */
	public static int heightForTier(int tier) {
		return TIER_STEP * Mth.clamp(tier, 1, TIER_COUNT);
	}

	/** 当前发电量在 {@link #MIN_OUTPUT}~{@link #MAX_OUTPUT} 里的位置，0~1。叶轮转速用它。 */
	private double speedRatio() {
		return (double) (towerOutput() - MIN_OUTPUT) / (MAX_OUTPUT - MIN_OUTPUT);
	}

	// ------------------------------------------------------------ 状态

	/**
	 * 判定这一刻的状态。
	 *
	 * <p>顺序就是玩家的排查顺序：先看风口封没封、再看塔基在不在、最后看塔够不够高。
	 * 第一个不满足的条件就是「停机原因」，会原样显示在面板上。
	 */
	private int evaluateStatus() {
		Level level = getLevel();
		if (level == null) {
			return STATUS_OBSTRUCTED;
		}
		if (!level.getBlockState(getBlockPos().above()).isAir()) {
			return STATUS_OBSTRUCTED;
		}
		if (towerHeight == NO_TOWER) {
			return STATUS_NO_BASE;
		}
		if (towerHeight < TIER_STEP) {
			return STATUS_TOO_SHORT;
		}
		return STATUS_GENERATING;
	}

	/** 上一 tick 的发电量同步值；0 表示没在发电。客户端界面读它。 */
	public long getOutput() {
		return output;
	}

	/** 上一 tick 的状态同步值，取值见 {@code STATUS_*} 常量。客户端界面读它。 */
	public int getStatus() {
		return status;
	}

	public boolean isGenerating() {
		return status == STATUS_GENERATING;
	}

	// ------------------------------------------------------------ 界面

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.crimsoncoppergrid.wind_generator");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return new WindGeneratorMenu(containerId, playerInventory, this);
	}
}
