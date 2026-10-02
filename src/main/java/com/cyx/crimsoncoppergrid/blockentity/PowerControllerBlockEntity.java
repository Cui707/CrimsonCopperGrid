package com.cyx.crimsoncoppergrid.blockentity;

import com.cyx.crimsoncoppergrid.common.blockentity.MachineBaseBlockEntity;
import com.cyx.crimsoncoppergrid.common.menu.ContainerDataCodec;
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
	// 与 PowerAcceptorBlockEntity 同样按 16 位一组拆 long，见 ContainerDataCodec。
	// 五个 long 各占 SLOTS_PER_LONG（= 4）格，后面再跟五个小整数。

	public static final int DATA_GENERATOR_OUTPUT = 0;
	public static final int DATA_CONSUMER_INPUT = DATA_GENERATOR_OUTPUT + ContainerDataCodec.SLOTS_PER_LONG;
	public static final int DATA_BATTERY_STORED = DATA_CONSUMER_INPUT + ContainerDataCodec.SLOTS_PER_LONG;
	public static final int DATA_BATTERY_CAPACITY = DATA_BATTERY_STORED + ContainerDataCodec.SLOTS_PER_LONG;
	public static final int DATA_NETWORK_STORED = DATA_BATTERY_CAPACITY + ContainerDataCodec.SLOTS_PER_LONG;
	/** 以下五个是计数器，量级远小于 16 位上限，单格直传即可。 */
	public static final int DATA_CABLE_COUNT = DATA_NETWORK_STORED + ContainerDataCodec.SLOTS_PER_LONG;
	public static final int DATA_GENERATOR_COUNT = DATA_CABLE_COUNT + 1;
	public static final int DATA_CONSUMER_COUNT = DATA_CABLE_COUNT + 2;
	public static final int DATA_BATTERY_COUNT = DATA_CABLE_COUNT + 3;
	public static final int DATA_CONNECTED = DATA_CABLE_COUNT + 4;
	public static final int DATA_COUNT = DATA_CABLE_COUNT + 5;

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
		if (ContainerDataCodec.covers(index, DATA_GENERATOR_OUTPUT)) {
			return ContainerDataCodec.write(stats.generatorOutput(),
					ContainerDataCodec.chunkOf(index, DATA_GENERATOR_OUTPUT));
		}
		if (ContainerDataCodec.covers(index, DATA_CONSUMER_INPUT)) {
			return ContainerDataCodec.write(stats.consumerInput(),
					ContainerDataCodec.chunkOf(index, DATA_CONSUMER_INPUT));
		}
		if (ContainerDataCodec.covers(index, DATA_BATTERY_STORED)) {
			return ContainerDataCodec.write(stats.batteryStored(),
					ContainerDataCodec.chunkOf(index, DATA_BATTERY_STORED));
		}
		if (ContainerDataCodec.covers(index, DATA_BATTERY_CAPACITY)) {
			return ContainerDataCodec.write(stats.batteryCapacity(),
					ContainerDataCodec.chunkOf(index, DATA_BATTERY_CAPACITY));
		}
		if (ContainerDataCodec.covers(index, DATA_NETWORK_STORED)) {
			return ContainerDataCodec.write(stats.networkStored(),
					ContainerDataCodec.chunkOf(index, DATA_NETWORK_STORED));
		}
		return switch (index) {
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
