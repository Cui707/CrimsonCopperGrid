package com.cyx.crimsoncoppergrid.common.menu;

import net.minecraft.world.inventory.ContainerData;

/**
 * {@code ContainerData} 的数值编解码。
 *
 * <h2>为什么一个 long 要占四格</h2>
 * {@link ContainerData} 的槽位在 Java 侧声明为 {@code int}，但原版把它发到客户端时走的
 * {@code ClientboundContainerSetDataPacket}，两个字段都是 <b>{@code writeShort}</b>：
 *
 * <pre>{@code
 * private void write(FriendlyByteBuf buffer) {
 *     buffer.writeContainerId(this.containerId);
 *     buffer.writeShort(this.id);     // 下标：16 位
 *     buffer.writeShort(this.value);  // 数值：16 位
 * }
 * }</pre>
 *
 * 也就是说<b>每个槽位在网络上只有 16 位有效，超出部分被静默丢弃</b>，既不报错也不警告。
 *
 * <p>CCG 最早按「低 32 位 + 高 32 位」把 long 拆成两格，于是 {@code 1_000_000}
 * （{@code 0x000F4240}）发到客户端只剩 {@code 0x4240 = 16960}；存量也一样被截断，
 * 结果 {@code stored > capacity}，电量条整体填满、百分比算出 133%。这是一个
 * 「编译通过、运行不报错、界面默默显示错数」的坑，因此单独拎出来集中处理。
 *
 * <p>这里按 <b>16 位一组</b>拆，一个 64 位值固定占 {@link #SLOTS_PER_LONG} 格 ——
 * 正好无损，不多不少。
 *
 * <h2>负数怎么还原</h2>
 * 发送端用无符号右移取片段；接收端 {@code readShort} 会把最高位当符号位做扩展，
 * 所以先用 {@code & 0xFFFF} 抹掉扩展出的那些 1 再拼。按位拼回去的结果与原值的
 * 补码逐位相同，负数（例如电池的净流量）也能原样还原。
 */
public final class ContainerDataCodec {

	/** 每个槽位在网络上真正有效的位宽。 */
	public static final int BITS_PER_SLOT = 16;

	/** 取一个片段用的掩码。 */
	public static final int SLOT_MASK = (1 << BITS_PER_SLOT) - 1;

	/** 一个 {@code long} 要占用的槽位数。 */
	public static final int SLOTS_PER_LONG = Long.SIZE / BITS_PER_SLOT;

	private ContainerDataCodec() {
	}

	/**
	 * 服务端侧：取出第 {@code chunk} 个片段（从 0 开始）准备发出去。
	 * 返回值天然落在 0~65535，被 {@code writeShort} 砍掉的只会是本来就无意义的位。
	 */
	public static int write(long value, int chunk) {
		return (int) ((value >>> (chunk * BITS_PER_SLOT)) & SLOT_MASK);
	}

	/** 客户端侧：把从 {@code firstIndex} 起的 {@link #SLOTS_PER_LONG} 格拼回原值。 */
	public static long read(ContainerData data, int firstIndex) {
		long value = 0L;
		for (int chunk = 0; chunk < SLOTS_PER_LONG; chunk++) {
			value |= (data.get(firstIndex + chunk) & (long) SLOT_MASK) << (chunk * BITS_PER_SLOT);
		}
		return value;
	}

	/** 某个下标是否落在从 {@code firstIndex} 开始的那个 long 里。 */
	public static boolean covers(int index, int firstIndex) {
		return index >= firstIndex && index < firstIndex + SLOTS_PER_LONG;
	}

	/** 某个下标在它所属的那个 long 里是第几个片段。调用前应先用 {@link #covers} 判定。 */
	public static int chunkOf(int index, int firstIndex) {
		return index - firstIndex;
	}
}
