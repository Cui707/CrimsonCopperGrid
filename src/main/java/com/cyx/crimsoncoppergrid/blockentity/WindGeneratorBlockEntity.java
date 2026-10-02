package com.cyx.crimsoncoppergrid.blockentity;

import com.cyx.crimsoncoppergrid.common.menu.WindGeneratorMenu;
import com.cyx.crimsoncoppergrid.common.powerSystem.PowerAcceptorBlockEntity;
import com.cyx.crimsoncoppergrid.init.ModBlockEntities;

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
 * 风力发电机：越高的地方风越大。
 *
 * <p>海平面（y={@link #SEA_LEVEL}）附近约 {@link #MIN_OUTPUT} FE/t，到 y≈{@code RAMP_TOP}
 * 达到上限 {@link #MAX_OUTPUT} FE/t。正上方需要有一格空间作为「迎风面」——
 * 埋在地里或封死在天花板下不发电。
 *
 * <h2>叶轮</h2>
 * 转动的叶轮由客户端渲染器（{@code WindTurbineRenderer}，移植自 TechReborn）画，
 * 角度推进在两个 {@code public} 字段里：{@link #bladeAngle} 与 {@link #spinSpeed}。
 * 它们**不入存档、不参与同步** —— 纯粹是客户端每帧插值用的动画状态，
 * 所以放在方块实体上而不是渲染器里：渲染器每帧都会重建 render state，存不住。
 *
 * <p>风速越高叶轮转得越快，风速为 0（被封住）时叶轮缓缓停下，而不是硬切。
 */
public class WindGeneratorBlockEntity extends PowerAcceptorBlockEntity implements MenuProvider {

	public static final long MIN_OUTPUT = 10L;
	public static final long MAX_OUTPUT = 30L;
	public static final long CAPACITY = 1_000L;

	/** 海平面，发电量从这一层开始爬升。 */
	public static final int SEA_LEVEL = 62;

	/** 到达满输出所需的高度。 */
	private static final int RAMP_TOP = 200;

	// ------------------------------------------------------------ 叶轮动画（仅客户端）

	/** 叶轮当前角度（弧度）。{@code public}：渲染器直接读。 */
	public float bladeAngle;
	/** 叶轮当前角速度（弧度/刻）。 */
	public float spinSpeed;

	/** 满风时的角速度。0.20 弧度/刻 ≈ 每秒 4 弧度，与 TechReborn 的风车同速。 */
	private static final float MAX_SPIN = 0.20F;
	/** 刚够转起来的角速度，海平面附近就是它。 */
	private static final float MIN_SPIN = 0.06F;
	/** 起转与停转的加速度（弧度/刻²）。停得比起得慢一点，看起来更像有惯性。 */
	private static final float SPIN_ACCEL = 0.002F;
	private static final float SPIN_DECEL = 0.0015F;

	public WindGeneratorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.WIND_GENERATOR, pos, state);
	}

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
		long output = currentOutput();
		if (output > 0) {
			long space = getFreeSpace();
			if (space > 0) {
				addEnergy(Math.min(output, space));
			}
		}
		setActive(output > 0);
	}

	/** 当前这一刻的发电量：由高度与上方空间共同决定。 */
	public long currentOutput() {
		Level level = getLevel();
		if (level == null || level.isClientSide()) {
			return 0;
		}
		if (!level.getBlockState(getBlockPos().above()).isAir()) {
			return 0;
		}
		return MIN_OUTPUT + Math.round((MAX_OUTPUT - MIN_OUTPUT) * windRatio(getBlockPos().getY()));
	}

	/** 高度换算成的风力系数，0 = 海平面，1 = {@code RAMP_TOP} 及以上。 */
	private static double windRatio(int y) {
		return Mth.clamp((double) (y - SEA_LEVEL) / (RAMP_TOP - SEA_LEVEL), 0.0, 1.0);
	}

	/**
	 * 客户端每刻推一次叶轮角度。
	 *
	 * <p>「在不在发电」直接读同步过来的 {@code ACTIVE} 方块状态 —— 服务端那边
	 * {@code output > 0} 与「上方通透」是等价的（只要通，最低也有 {@link #MIN_OUTPUT}），
	 * 所以客户端不必再复算一遍发电量。
	 */
	@Override
	protected void clientTick() {
		float target = 0F;
		if (isActive()) {
			target = (float) (MIN_SPIN + (MAX_SPIN - MIN_SPIN) * windRatio(getBlockPos().getY()));
		}
		spinSpeed = Mth.approach(spinSpeed, target, target > spinSpeed ? SPIN_ACCEL : SPIN_DECEL);
		bladeAngle += spinSpeed;
		// 长时间挂机后浮点精度会掉，绕一圈就归零
		if (bladeAngle >= Mth.TWO_PI) {
			bladeAngle -= Mth.TWO_PI;
		}
	}

	public boolean isGenerating() {
		return currentOutput() > 0;
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
