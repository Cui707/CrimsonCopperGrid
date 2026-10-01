package com.cyx.crimsoncoppergrid.energy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 一个「电网」：一组通过电线连通、或被开关分隔开的设备。
 *
 * <p>成员用坐标记录而不是直接的方块实体引用，避免区块卸载后持有陈旧的 BE。
 * 每次结算时通过 {@link ServerLevel#getBlockEntity(BlockPos)} 现取，
 * 拿不到（区块未加载/已被移除）就跳过该成员。
 */
public final class Grid {
	/** 每多少个游戏刻结算一次。1 秒 = 20 刻,所以 10 刻 = 每秒结算 2 次。 */
	public static final int SETTLE_INTERVAL = 10;

	private final ServerLevel level;
	private final Set<BlockPos> cables = new HashSet<>();
	private final Set<BlockPos> producers = new HashSet<>();
	private final Set<BlockPos> consumers = new HashSet<>();
	private final Set<BlockPos> batteries = new HashSet<>();

	public Grid(ServerLevel level) {
		this.level = level;
	}

	public ServerLevel level() {
		return level;
	}

	public Set<BlockPos> cables() {
		return cables;
	}

	public Set<BlockPos> allNodes() {
		Set<BlockPos> all = new HashSet<>(cables);
		all.addAll(producers);
		all.addAll(consumers);
		all.addAll(batteries);
		return all;
	}

	public int deviceCount() {
		return producers.size() + consumers.size() + batteries.size();
	}

	public boolean isEmpty() {
		return cables.isEmpty() && deviceCount() == 0;
	}

	// ---------------------------------------------------------------- 成员维护

	/** 把一个方块实体并入本电网。 */
	public void add(BlockPos pos, EnergyStorage storage) {
		if (storage instanceof EnergyProducer) {
			producers.add(pos);
		} else if (storage instanceof EnergyConsumer) {
			consumers.add(pos);
		} else {
			batteries.add(pos);
		}
	}

	/** 把仅作为导线的方块记入本电网（不参与能量结算，只影响连通性）。 */
	public void addCable(BlockPos pos) {
		cables.add(pos);
	}

	public void remove(BlockPos pos) {
		cables.remove(pos);
		producers.remove(pos);
		consumers.remove(pos);
		batteries.remove(pos);
	}

	public boolean contains(BlockPos pos) {
		return cables.contains(pos) || producers.contains(pos) || consumers.contains(pos) || batteries.contains(pos);
	}

	// ---------------------------------------------------------------- 结算

	/**
	 * 一次结算：电线本身不参与，只负责把发电机的电送到用电设备与电池。
	 *
	 * <p>优先级：发电机 -> 用电设备；不够就由电池放电补；用不完才去充电。
	 * 因此「有电池在充电」就意味着当前没有缺电的设备。
	 */
	public void settle() {
		List<ProducerRef> liveProducers = new ArrayList<>();
		List<ConsumerRef> liveConsumers = new ArrayList<>();
		List<BatteryRef> liveBatteries = new ArrayList<>();

		collectProducers(liveProducers);
		collectConsumers(liveConsumers);
		collectBatteries(liveBatteries);

		if (liveProducers.isEmpty() && liveBatteries.isEmpty()) {
			// 没有电源，用电设备本周期无电可用
			for (ConsumerRef ref : liveConsumers) {
				ref.consumer().consumeEnergy(0);
			}
			return;
		}

		// 先满足用电设备的全部需求
		long demand = 0;
		for (ConsumerRef ref : liveConsumers) {
			demand += Math.max(0, ref.consumer().wantedEnergy());
		}

		// 发电机按需求发电
		long generated = 0;
		for (ProducerRef ref : liveProducers) {
			long want = Math.max(0, demand - generated);
			if (want == 0) {
				ref.producer().produceEnergy(0);
				continue;
			}
			generated += Math.max(0, ref.producer().produceEnergy(want));
		}

		// 缺口由电池放电补上；discharged 是电池真实贡献，不能拿「缺口」当已补足
		long deficit = Math.max(0, demand - generated);
		long discharged = 0;
		if (deficit > 0) {
			discharged = discharge(liveBatteries, deficit);
		}
		long available = generated + discharged;

		for (ConsumerRef ref : liveConsumers) {
			ref.consumer().consumeEnergy(available);
		}

		// 富余 = 本周期发出的 - 本周期实际用掉的
		long used = 0;
		for (ConsumerRef ref : liveConsumers) {
			used += ref.consumer().getConsumedEnergy();
		}
		long surplus = generated - used;
		if (surplus > 0) {
			charge(liveBatteries, surplus);
		}
	}

	/** 让电池放电，返回实际补上的量。 */
	private static long discharge(List<BatteryRef> batteries, long deficit) {
		long remaining = deficit;
		for (BatteryRef ref : batteries) {
			if (remaining <= 0) {
				break;
			}
			long pulled = ref.storage().extractEnergy(Math.min(ref.storage().getMaxExtract(), remaining), false);
			if (pulled > 0) {
				remaining -= pulled;
			}
		}
		return deficit - remaining;
	}

	/** 把富余电量存入电池，返回实际存入的量。 */
	private static long charge(List<BatteryRef> batteries, long surplus) {
		long remaining = surplus;
		for (BatteryRef ref : batteries) {
			if (remaining <= 0) {
				break;
			}
			long accepted = ref.storage().receiveEnergy(Math.min(ref.storage().getMaxReceive(), remaining), false);
			if (accepted > 0) {
				remaining -= accepted;
			}
		}
		return surplus - remaining;
	}

	private void collectProducers(List<ProducerRef> out) {
		for (BlockPos pos : sorted(producers)) {
			BlockEntity be = level.getBlockEntity(pos);
			if (be instanceof EnergyProducer producer) {
				out.add(new ProducerRef(producer));
			}
		}
	}

	private void collectConsumers(List<ConsumerRef> out) {
		for (BlockPos pos : sorted(consumers)) {
			BlockEntity be = level.getBlockEntity(pos);
			if (be instanceof EnergyConsumer consumer) {
				out.add(new ConsumerRef(consumer));
			}
		}
	}

	private void collectBatteries(List<BatteryRef> out) {
		for (BlockPos pos : sorted(batteries)) {
			BlockEntity be = level.getBlockEntity(pos);
			if (be instanceof EnergyStorage storage && !(be instanceof EnergyProducer) && !(be instanceof EnergyConsumer)) {
				out.add(new BatteryRef(storage));
			}
		}
	}

	/** 固定顺序遍历，保证同一存档下结算结果可复现。 */
	private static List<BlockPos> sorted(Set<BlockPos> positions) {
		List<BlockPos> list = new ArrayList<>(positions);
		list.sort(Comparator.comparingLong(BlockPos::asLong));
		return list;
	}

	/** 只读统计：本电网中所有电池的（当前, 容量）。 */
	public long[] batteryTotals() {
		long stored = 0;
		long capacity = 0;
		for (BlockPos pos : batteries) {
			if (level.getBlockEntity(pos) instanceof EnergyStorage storage) {
				stored += storage.getEnergyStored();
				capacity += storage.getEnergyCapacity();
			}
		}
		return new long[] { stored, capacity };
	}

	/** 只读统计：所有发电机满负荷时的总发电速率。 */
	public long totalMaxOutput() {
		long total = 0;
		for (BlockPos pos : producers) {
			if (level.getBlockEntity(pos) instanceof EnergyProducer producer) {
				total += producer.getMaxOutput();
			}
		}
		return total;
	}

	private record ProducerRef(EnergyProducer producer) {
	}

	private record ConsumerRef(EnergyConsumer consumer) {
	}

	private record BatteryRef(EnergyStorage storage) {
	}
}
