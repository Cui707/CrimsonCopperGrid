package com.cyx.crimsoncoppergrid.blockentity;

import com.cyx.crimsoncoppergrid.common.menu.SolarGeneratorMenu;
import com.cyx.crimsoncoppergrid.common.powerSystem.PowerAcceptorBlockEntity;
import com.cyx.crimsoncoppergrid.init.ModBlockEntities;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 太阳能发电机：只在白天、晴天、正上方露天时发电。
 *
 * <p>判定用「正上方那格的天空光是否为满值」而不是简单看维度光照 ——
 * 这样在洞里、在屋檐下都不发电，而玻璃天窗（玻璃不挡天光）下面仍然发电，
 * 表现与玩家的直觉一致。
 *
 * <p>发电方式是每 tick 往自己的缓冲里加 {@link #MAX_OUTPUT}，
 * 缓冲满了自然就不再增加，限流是免费的。之后的传输由
 * {@link PowerAcceptorBlockEntity} 的推流逻辑负责，这里不用管。
 *
 * <h2>为什么界面不显示缓冲电量</h2>
 * 缓冲只有 {@link #CAPACITY} FE，按每 tick {@link #MAX_OUTPUT} 算，
 * 只要在发电且旁边没接东西，<b>2.5 秒就贴到上限</b>；旁边接了东西又会被推空。
 * 无论哪种情况，那个读数对玩家都几乎没有信息量 —— 玩家真正想知道的是
 * 「现在到底在不在发电、不在的话是为什么」。所以面板改为显示 {@link #getStatus() 状态}，
 * 由服务端判定后通过 {@code ContainerData} 同步过去。
 */
public class SolarGeneratorBlockEntity extends PowerAcceptorBlockEntity implements MenuProvider {

	/** 每 tick 的发电量。 */
	public static final long MAX_OUTPUT = 20L;
	/** 缓冲容量：够扛 50 tick 的阴云，也就是 2.5 秒。 */
	public static final long CAPACITY = 1_000L;

	// ---- 界面数据：在父类的「存量 + 容量」之后追加一个状态格 ----

	/** 状态格。取值见下面的 {@code STATUS_*} 常量。 */
	public static final int DATA_STATUS = PowerAcceptorBlockEntity.DATA_COUNT;
	/** 本类的数据槽位总数（父类 8 格 + 状态 1 格）。 */
	public static final int DATA_COUNT = DATA_STATUS + 1;

	/** 正在发电。 */
	public static final int STATUS_GENERATING = 0;
	/** 夜里（或该维度没有昼夜）。 */
	public static final int STATUS_NIGHT = 1;
	/** 正在下雨或雷雨。 */
	public static final int STATUS_RAIN = 2;
	/** 正上方被挡住，见不到天。 */
	public static final int STATUS_OBSTRUCTED = 3;

	/** 上一个服务端 tick 判定的状态，也是发给客户端的那一份。 */
	private int status = STATUS_NIGHT;

	@Override
	public int getCount() {
		return DATA_COUNT;
	}

	@Override
	public int get(int index) {
		if (index == DATA_STATUS) {
			return status;
		}
		return super.get(index);
	}

	public SolarGeneratorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.SOLAR_GENERATOR, pos, state);
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
		status = evaluateStatus();
		if (status == STATUS_GENERATING) {
			long space = getFreeSpace();
			if (space > 0) {
				addEnergy(Math.min(MAX_OUTPUT, space));
			}
		}
		setActive(status == STATUS_GENERATING);
	}

	/** 当前状态，取值见 {@code STATUS_*} 常量。客户端请读同步过来的那一份。 */
	public int getStatus() {
		return status;
	}

	/**
	 * 判定这一刻的状态。
	 *
	 * <p>顺序对应玩家的排查顺序，所以第一个不满足的条件就是「停机原因」：
	 * 先看天黑了没，再看下没下雨，最后看是不是被上方挡住了。
	 * 露天判定用「正上方那格的天空光是否为满值」而不是简单看维度光照 ——
	 * 这样在洞里、在屋檐下都不发电，而玻璃天窗（玻璃不挡天光）下面仍然发电，
	 * 表现与玩家的直觉一致。
	 */
	private int evaluateStatus() {
		Level level = getLevel();
		if (level == null || !level.isBrightOutside()) {
			return STATUS_NIGHT;
		}
		if (level.isRaining()) {
			return STATUS_RAIN;
		}
		if (level.getBrightness(LightLayer.SKY, getBlockPos().above()) < 15) {
			return STATUS_OBSTRUCTED;
		}
		return STATUS_GENERATING;
	}

	/** 当前这一刻的发电量。仅服务端有意义（客户端读同步过来的 {@link #getStatus()}）。 */
	public long currentOutput() {
		return evaluateStatus() == STATUS_GENERATING ? MAX_OUTPUT : 0L;
	}

	public boolean isGenerating() {
		return currentOutput() > 0;
	}

	// ------------------------------------------------------------ 界面

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.crimsoncoppergrid.solar_generator");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return new SolarGeneratorMenu(containerId, playerInventory, this);
	}
}
