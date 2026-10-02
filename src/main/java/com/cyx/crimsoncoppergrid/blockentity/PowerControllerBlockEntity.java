package com.cyx.crimsoncoppergrid.blockentity;

import com.cyx.crimsoncoppergrid.common.blockentity.MachineBaseBlockEntity;
import com.cyx.crimsoncoppergrid.common.menu.PowerControllerMenu;
import com.cyx.crimsoncoppergrid.common.powerSystem.GridStats;
import com.cyx.crimsoncoppergrid.init.ModBlockEntities;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 电力控制器：挂在电网上的仪表盘。
 *
 * <h2>它自己不传输能量</h2>
 * 控制器不继承 {@code PowerAcceptorBlockEntity} —— 它既不发电也不用电，
 * 只是「六个方向里挨着一段导线」然后从那里把整张网扫一遍。
 * 因此它不会出现在自己的统计结果里。
 *
 * <h2>扫描有成本，所以按周期刷新</h2>
 * 整张网要 BFS 一遍，还要对每段导线的邻居做分类。这个开销按 tick 做没必要 ——
 * 电网规模不会每 50 毫秒变一次。这里每 {@link #RESCAN_INTERVAL} 刻重扫一次，
 * 界面读的是缓存，玩家看到的数字最多滞后一秒。
 */
public class PowerControllerBlockEntity extends MachineBaseBlockEntity implements MenuProvider, ContainerData {

	/** 重新扫描电网的间隔（刻）。 */
	private static final long RESCAN_INTERVAL = 20;

	// ------------------------------------------------------------ 界面数据
	// 与 PowerAcceptorBlockEntity 一样，长整型按「低 32 位 + 高 32 位」拆两格。

	public static final int DATA_GENERATOR_OUTPUT_LOW = 0;
	public static final int DATA_GENERATOR_OUTPUT_HIGH = 1;
	public static final int DATA_CONSUMER_INPUT_LOW = 2;
	public static final int DATA_CONSUMER_INPUT_HIGH = 3;
	public static final int DATA_BATTERY_STORED_LOW = 4;
	public static final int DATA_BATTERY_STORED_HIGH = 5;
	public static final int DATA_BATTERY_CAPACITY_LOW = 6;
	public static final int DATA_BATTERY_CAPACITY_HIGH = 7;
	public static final int DATA_NETWORK_STORED_LOW = 8;
	public static final int DATA_NETWORK_STORED_HIGH = 9;
	public static final int DATA_CABLE_COUNT = 10;
	public static final int DATA_GENERATOR_COUNT = 11;
	public static final int DATA_CONSUMER_COUNT = 12;
	public static final int DATA_BATTERY_COUNT = 13;
	public static final int DATA_CONNECTED = 14;
	public static final int DATA_COUNT = 15;

	private GridStats stats = GridStats.DISCONNECTED;

	public PowerControllerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.POWER_CONTROLLER, pos, state);
	}

	// ------------------------------------------------------------ tick

	@Override
	protected void serverTick() {
		long now = getLevel() != null ? getLevel().getGameTime() : 0L;
		if (now % RESCAN_INTERVAL != 0) {
			return;
		}
		if (getLevel() instanceof ServerLevel serverLevel) {
			stats = GridStats.scan(serverLevel, getBlockPos());
		}
	}

	// ------------------------------------------------------------ ContainerData

	@Override
	public int getCount() {
		return DATA_COUNT;
	}

	@Override
	public int get(int index) {
		return switch (index) {
			case DATA_GENERATOR_OUTPUT_LOW -> (int) (stats.generatorOutput() & 0xFFFFFFFFL);
			case DATA_GENERATOR_OUTPUT_HIGH -> (int) (stats.generatorOutput() >> 32);
			case DATA_CONSUMER_INPUT_LOW -> (int) (stats.consumerInput() & 0xFFFFFFFFL);
			case DATA_CONSUMER_INPUT_HIGH -> (int) (stats.consumerInput() >> 32);
			case DATA_BATTERY_STORED_LOW -> (int) (stats.batteryStored() & 0xFFFFFFFFL);
			case DATA_BATTERY_STORED_HIGH -> (int) (stats.batteryStored() >> 32);
			case DATA_BATTERY_CAPACITY_LOW -> (int) (stats.batteryCapacity() & 0xFFFFFFFFL);
			case DATA_BATTERY_CAPACITY_HIGH -> (int) (stats.batteryCapacity() >> 32);
			case DATA_NETWORK_STORED_LOW -> (int) (stats.networkStored() & 0xFFFFFFFFL);
			case DATA_NETWORK_STORED_HIGH -> (int) (stats.networkStored() >> 32);
			case DATA_CABLE_COUNT -> stats.cables();
			case DATA_GENERATOR_COUNT -> stats.generators();
			case DATA_CONSUMER_COUNT -> stats.consumers();
			case DATA_BATTERY_COUNT -> stats.batteries();
			case DATA_CONNECTED -> stats.connected() ? 1 : 0;
			default -> 0;
		};
	}

	@Override
	public void set(int index, int value) {
		// 单向同步：客户端那份 SimpleContainerData 自己会存
	}

	// ------------------------------------------------------------ 界面

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.crimsoncoppergrid.power_controller");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return new PowerControllerMenu(containerId, playerInventory, this);
	}
}
