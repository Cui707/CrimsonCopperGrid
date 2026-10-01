package com.cyx.crimsoncoppergrid.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;

/**
 * 电网结算的不变量测试。
 *
 * <p>通过 {@link Grid#settle(Grid.NodeResolver, Set, Set, Set)} 这个纯函数接缝驱动，
 * 不需要启动 Minecraft —— 但坐标用原版 {@link BlockPos}，省得再包一层。
 *
 * <p>覆盖的核心不变量：
 * <ul>
 *   <li><b>能量守恒</b>：产出 + 电池释放 == 消耗 + 电池净增，既不多也不少；</li>
 *   <li><b>不凭空造电</b>：没有电源就没有消耗，电池不会被抽成负数；</li>
 *   <li><b>不超发</b>：用电设备拿到的电不超过自己的需求；</li>
 *   <li><b>确定性</b>：相同输入两次结算结果一致。</li>
 * </ul>
 */
class GridSettlementTest {
	private static final BlockPos P1 = new BlockPos(1, 64, 1);
	private static final BlockPos P2 = new BlockPos(2, 64, 1);
	private static final BlockPos P3 = new BlockPos(3, 64, 1);
	private static final BlockPos B1 = new BlockPos(10, 64, 1);
	private static final BlockPos B2 = BlockPos.ZERO;

	// ------------------------------------------------------------------ 守恒

	@Test
	@DisplayName("有余量时：电池充入量 == 发电量 - 用电量")
	void surplusChargesBattery() {
		TestProducer generator = new TestProducer(100);
		TestConsumer consumer = new TestConsumer(30);
		TestBattery battery = new TestBattery(10_000, 1_000);

		Harness h = new Harness();
		h.producer(P1, generator);
		h.consumer(P2, consumer);
		h.battery(P3, battery);
		h.settle();

		assertEquals(30, consumer.consumed(), "用电设备应拿到全部需求");
		assertEquals(100, generator.produced(), "发电机满发，富余有电池接住");
		assertEquals(70, battery.stored(), "富余 70 应全部充进电池");
		assertConserved(generator, consumer, battery, 0L);
	}

	@Test
	@DisplayName("电池充满后：发电机只发得出的量，多余的根本不产生")
	void fullBatteryStopsGeneration() {
		TestProducer generator = new TestProducer(1_000);
		TestConsumer consumer = new TestConsumer(100);
		TestBattery battery = new TestBattery(200, 1_000, 200);

		Harness h = new Harness();
		h.producer(P1, generator);
		h.consumer(P2, consumer);
		h.battery(P3, battery);
		h.settle();

		assertEquals(200, battery.stored(), "电池已经满了，不该再变");
		assertEquals(100, consumer.consumed());
		assertEquals(100, generator.produced(), "电池满了就没有充电需求，发电机只发 100");
		assertConserved(generator, consumer, battery, 200L);
	}

	@Test
	@DisplayName("缺口由电池补齐：放电量 == 需求 - 发电量")
	void deficitDrainsBattery() {
		TestProducer generator = new TestProducer(10);
		TestConsumer consumer = new TestConsumer(100);
		TestBattery battery = new TestBattery(10_000, 1_000, 5_000);

		Harness h = new Harness();
		h.producer(P1, generator);
		h.consumer(P2, consumer);
		h.battery(P3, battery);
		h.settle();

		assertEquals(100, consumer.consumed(), "需求应被完全满足");
		assertEquals(5_000 - 90, battery.stored(), "电池应放出 90");
		assertEquals(10, generator.produced(), "发电机满发 10");
		assertConserved(generator, consumer, battery, 5_000L);
	}

	// ------------------------------------------------------------------ 不凭空造电

	@Test
	@DisplayName("没有任何电源时：不发电、不消耗、电池不变")
	void noPowerSourceMeansNothingHappens() {
		TestConsumer consumer = new TestConsumer(100);
		TestProducer generator = new TestProducer(100);

		Harness h = new Harness();
		h.consumer(P2, consumer);
		// 注意：生产者和电池都没有加进来
		h.settle();

		assertEquals(0, consumer.consumed(), "没有电源就不能用电");
		assertEquals(0, generator.produced(), "没接进电网的发电机不应该被调用");
	}

	@Test
	@DisplayName("电池掏空后：用电设备只能拿到存货，拿不到不存在的电")
	void emptyBatteryCannotOverdraw() {
		TestProducer generator = new TestProducer(10);
		TestConsumer consumer = new TestConsumer(1_000);
		TestBattery battery = new TestBattery(10_000, 1_000, 50);

		Harness h = new Harness();
		h.producer(P1, generator);
		h.consumer(P2, consumer);
		h.battery(P3, battery);
		h.settle();

		assertEquals(0, battery.stored(), "电池不能被抽成负数");
		assertEquals(60, consumer.consumed(), "只能拿到 10（发电）+ 50（电池）");
		assertConserved(generator, consumer, battery, 50L);
	}

	@Test
	@DisplayName("多块电池一起放电：总量刚好补上缺口，且都不会被抽成负数")
	void multipleBatteriesShareDeficit() {
		TestProducer generator = new TestProducer(0);
		TestConsumer consumer = new TestConsumer(100);
		// 两块都够 100：实现会按固定顺序取用其中一块（结果与遍历顺序无关的断言才稳）
		TestBattery low = new TestBattery(1_000, 1_000, 40);
		TestBattery high = new TestBattery(1_000, 1_000, 500);

		Harness h = new Harness();
		h.producer(P1, generator);
		h.consumer(P2, consumer);
		h.battery(B1, low);
		h.battery(B2, high);
		h.settle();

		long drained = (40 - low.stored()) + (500 - high.stored());
		assertEquals(100, consumer.consumed(), "需求应被完全满足");
		assertEquals(100, drained, "总放电量应正好等于缺口，不多不少");
		assertTrue(low.stored() >= 0 && high.stored() >= 0, "任何一块电池都不能为负");
	}

	@Test
	@DisplayName("先被取用的电池不够时：会继续向下一块取电，两块都被用上")
	void dischargeFallsThroughToNextBattery() {
		TestProducer generator = new TestProducer(0);
		TestConsumer consumer = new TestConsumer(100);
		// 注意：遍历顺序按坐标排序，B2=(0,0,0) 在 B1=(10,64,1) 之前被取用。
		// 这里让「先被取的那块」只有 30，所以必然需要另一块接手，
		// 结论与实现内部的遍历顺序无关。
		TestBattery first = new TestBattery(1_000, 1_000, 30);
		TestBattery second = new TestBattery(1_000, 1_000, 500);

		Harness h = new Harness();
		h.producer(P1, generator);
		h.consumer(P2, consumer);
		h.battery(B1, second);
		h.battery(B2, first);
		h.settle();

		long fromFirst = 30 - first.stored();
		long fromSecond = 500 - second.stored();
		assertEquals(100, consumer.consumed(), "需求应被完全满足");
		assertEquals(30, fromFirst, "只有 30 的那块应被掏空");
		assertEquals(70, fromSecond, "剩余缺口必须由另一块补上（验证会继续向下一块取电）");
		assertEquals(100, fromFirst + fromSecond, "两块合起来正好补上缺口");
		assertTrue(first.stored() >= 0 && second.stored() >= 0, "两块都不能为负");
	}

	@Test
	@DisplayName("单块电池不够时：放电量受它自身存量限制，不会凭空补足")
	void dischargeIsLimitedByStored() {
		TestProducer generator = new TestProducer(0);
		TestConsumer consumer = new TestConsumer(1_000);
		TestBattery small = new TestBattery(1_000, 1_000, 30);

		Harness h = new Harness();
		h.producer(P1, generator);
		h.consumer(P2, consumer);
		h.battery(B1, small);
		h.settle();

		assertEquals(30, consumer.consumed(), "只能拿到电池里真实存在的 30");
		assertEquals(0, small.stored(), "电池被掏空但不为负");
		assertConserved(generator, consumer, small, 30L);
	}

	@Test
	@DisplayName("电池满电 + 发电有余：不会因为「存不下」而把电倒灌给用电设备")
	void storageCapNeverIncreasesSupply() {
		TestProducer generator = new TestProducer(100);
		TestConsumer consumer = new TestConsumer(10);
		TestBattery battery = new TestBattery(5, 1_000, 5);

		Harness h = new Harness();
		h.producer(P1, generator);
		h.consumer(P2, consumer);
		h.battery(P3, battery);
		h.settle();

		assertEquals(10, consumer.consumed(), "用电量必须由需求决定，不能因为电池满了就多给");
		assertEquals(5, battery.stored(), "已经满的电池不再接收");
		assertEquals(10, generator.produced(), "电池满了就没有充电需求，发电机只发 10");
	}

	// ------------------------------------------------------------------ 不超发

	@Test
	@DisplayName("总供给远超总需求时：总消耗不超过总需求")
	void supplyNeverExceedsDemand() {
		TestProducer generator = new TestProducer(10_000);
		TestConsumer first = new TestConsumer(100);
		TestConsumer second = new TestConsumer(200);
		TestBattery battery = new TestBattery(1_000_000, 10_000, 0);

		Harness h = new Harness();
		h.producer(P1, generator);
		h.consumer(P2, first);
		h.consumer(P3, second);
		h.battery(B1, battery);
		h.settle();

		assertEquals(100, first.consumed());
		assertEquals(200, second.consumed());
		assertEquals(300, first.consumed() + second.consumed(), "总消耗必须等于总需求");
		assertConserved(generator, new TestConsumer[] { first, second }, battery, 0L);
	}

	@Test
	@DisplayName("电池还有空间时：发电机按「用电需求 + 电池本周期可接收量」发电")
	void generationFillsFreeStorage() {
		TestProducer generator = new TestProducer(100_000);
		TestConsumer consumer = new TestConsumer(100);
		TestBattery battery = new TestBattery(1_000_000, 50_000, 0);

		Harness h = new Harness();
		h.producer(P1, generator);
		h.consumer(P2, consumer);
		h.battery(P3, battery);
		h.settle();

		assertEquals(100, consumer.consumed());
		// 需求 = 用电 100 + 电池本周期可接收 50 000（受 maxReceive 限制，不是剩余容量）
		assertEquals(50_100, generator.produced(), "发电量以「这一周期真的存得下」为限");
		assertEquals(50_000, battery.stored(), "电池被填满到本周期能接收的上限");
		assertConserved(generator, consumer, battery, 0L);
	}

	@Test
	@DisplayName("电池能接收的空间大于单次上限时：发电机不会发出无处可去的电")
	void generationNeverExceedsReceivable() {
		TestProducer generator = new TestProducer(1_000_000);
		TestConsumer consumer = new TestConsumer(0);
		TestBattery battery = new TestBattery(1_000_000, 5_000, 0);

		Harness h = new Harness();
		h.producer(P1, generator);
		h.consumer(P2, consumer);
		h.battery(P3, battery);
		h.settle();

		assertEquals(5_000, generator.produced(), "只发电池这一周期收得下的量");
		assertEquals(5_000, battery.stored());
		assertConserved(generator, consumer, battery, 0L);
	}

	// ------------------------------------------------------------------ 确定性

	@Test
	@DisplayName("同样输入跑两遍结果一致（成员遍历顺序固定）")
	void settlementIsDeterministic() {
		TestBattery batteryA = new TestBattery(1_000, 1_000, 100);
		TestBattery batteryB = new TestBattery(1_000, 1_000, 100);

		new Harness().withGenerator(50).withConsumers(30, 40).withBattery(batteryA).settle();
		new Harness().withGenerator(50).withConsumers(30, 40).withBattery(batteryB).settle();

		assertEquals(batteryA.stored(), batteryB.stored(), "两次结算必须得到相同结果");
	}

	// ------------------------------------------------------------------ 断言辅助

	/**
	 * 能量守恒：产出 == 所有用电设备的消耗之和 + 电池的净增量。
	 *
	 * <p>只传入「结算前的电池存量」，放电量由前后差值算出，
	 * 避免把放电量与初始存量两个 long 参数传反而得出恒真的错误结论。
	 */
	private static void assertConserved(TestProducer generator, TestConsumer[] consumers, TestBattery battery,
			long batteryStoredBefore) {
		long produced = generator.produced();
		long consumed = 0;
		for (TestConsumer consumer : consumers) {
			consumed += consumer.consumed();
		}
		long netGain = battery.stored() - batteryStoredBefore;
		assertTrue(battery.stored() >= 0, "任何时刻电池都不能为负");
		assertTrue(netGain >= -batteryStoredBefore, "电池放出的电不能超过它原有的存量");
		assertEquals(produced, consumed + netGain,
				"能量守恒：产出 " + produced + " 应等于 消耗 " + consumed + " + 电池净增 " + netGain);
	}

	private static void assertConserved(TestProducer generator, TestConsumer consumer, TestBattery battery,
			long batteryStoredBefore) {
		assertConserved(generator, new TestConsumer[] { consumer }, battery, batteryStoredBefore);
	}

	// ------------------------------------------------------------------ 测试脚手架

	/** 把「坐标 -> 设备」做成内存映射，代替真实世界的查询。
	 *  三类设备用不同的 x 段，避免坐标碰撞把设备互相覆盖。 */
	private static final class Harness {
		private final java.util.Map<BlockPos, Object> nodes = new java.util.HashMap<>();
		private final Set<BlockPos> producers = new java.util.HashSet<>();
		private final Set<BlockPos> consumers = new java.util.HashSet<>();
		private final Set<BlockPos> batteries = new java.util.HashSet<>();
		private int producerSlot = 1;
		private int consumerSlot = 1_000;
		private int batterySlot = 2_000;

		void producer(BlockPos pos, TestProducer producer) {
			nodes.put(pos, producer);
			producers.add(pos);
		}

		void consumer(BlockPos pos, TestConsumer consumer) {
			nodes.put(pos, consumer);
			consumers.add(pos);
		}

		void battery(BlockPos pos, TestBattery battery) {
			nodes.put(pos, battery);
			batteries.add(pos);
		}

		Harness withGenerator(long output) {
			producer(new BlockPos(producerSlot++, 64, 0), new TestProducer(output));
			return this;
		}

		Harness withConsumers(long... demands) {
			for (long demand : demands) {
				consumer(new BlockPos(consumerSlot++, 64, 1), new TestConsumer(demand));
			}
			return this;
		}

		Harness withBattery(TestBattery battery) {
			battery(new BlockPos(batterySlot++, 64, 2), battery);
			return this;
		}

		void settle() {
			Grid.settle(pos -> (EnergyStorage) nodes.get(pos), producers, consumers, batteries);
		}
	}
}
