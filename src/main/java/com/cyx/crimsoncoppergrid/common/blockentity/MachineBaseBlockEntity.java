package com.cyx.crimsoncoppergrid.common.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.cyx.crimsoncoppergrid.common.blocks.BlockMachineBase;

/**
 * 所有「机器类方块实体」的公共父类。分层与命名对齐 TechReborn / RebornCore 的
 * {@code MachineBaseBlockEntity}（MIT）。
 *
 * <h2>这一层负责什么</h2>
 * <ol>
 *   <li><b>tick 调度</b>：统一入口 {@link #tick}，子类只需要实现 {@link #serverTick()}；</li>
 *   <li><b>方块状态</b>：{@code FACING} / {@code ACTIVE} 的读写封装（{@link #getFacing()}、
 *       {@link #isActive()}、{@link #setActive(boolean)}）；</li>
 *   <li><b>客户端同步</b>：默认每 20 刻最多一次，避免每刻刷包；</li>
 *   <li><b>生命周期钩子</b>：{@link #onPlace} / {@link #onBreak}，由 {@link BlockMachineBase} 调用。</li>
 * </ol>
 *
 * <h2>与 TechReborn 原版的差异（有意省略，不是漏做）</h2>
 * RebornCore 的 {@code MachineBaseBlockEntity} 还挂着四套子系统，CCG 按自己的设计原则没有引入：
 * <ul>
 *   <li><b>升级槽</b>（{@code IUpgrade} / {@code IUpgradeable}）—— CCG 不做升级体系；</li>
 *   <li><b>红石配置</b>（{@code RedstoneConfiguration}）—— CCG 用独立的「电闸」方块在电网层面断通，
 *       不需要每台机器各自配红石行为；</li>
 *   <li><b>槽位 / 流体 I/O 配置</b>（{@code SlotConfiguration} / {@code FluidConfiguration}）——
 *       等界面阶段做菜单时再引入，否则现在没有东西消费它；</li>
 *   <li><b>多方块</b>（{@code writeMultiblock} / {@code MultiblockWriter}）—— CCG 不做多方块结构。</li>
 * </ul>
 * 将来若要移植其中任何一套，这里就是落点。
 *
 * <p>另外这里直接实现 {@link Container}（默认空实现），让有物品栏的机器只覆写需要的那几个方法，
 * 这也与 TechReborn 一致 —— 菜单（{@code MenuType}）要求方块实体是个容器。
 */
public abstract class MachineBaseBlockEntity extends BlockEntity
		implements BlockEntityTicker<MachineBaseBlockEntity>, Container {

	/** 客户端同步的最小间隔（刻）。20 刻 = 1 秒。 */
	private static final int SYNC_COOLDOWN = 20;

	private boolean markSync;
	private int tickTime;

	/** 上一刻与本刻的存量差，供界面显示「正在充/放电」。 */
	public long powerChange;
	public long powerLastTick;

	protected MachineBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/**
	 * 通用 ticker。方块侧 {@code getTicker} 直接返回它，这样每台机器不必各写一遍强转样板。
	 */
	@SuppressWarnings("unchecked")
	public static <T extends BlockEntity> BlockEntityTicker<T> ticker() {
		return (level, pos, state, blockEntity) -> ((MachineBaseBlockEntity) blockEntity)
				.tick(level, pos, state, (MachineBaseBlockEntity) blockEntity);
	}

	@Override
	public void tick(Level level, BlockPos pos, BlockState state, MachineBaseBlockEntity blockEntity) {
		if (tickTime == 0) {
			onLoad();
		}
		tickTime++;

		if (!(level instanceof net.minecraft.server.level.ServerLevel)) {
			clientTick();
			return;
		}

		serverTick();
		syncIfNecessary();
	}

	/** 区块载入后的首次 tick（相当于 TechReborn 里在 {@code tickTime == 0} 时做的初始化）。 */
	protected void onLoad() {
	}

	/** 子类的服务端逻辑。客户端不会走到这里。 */
	protected void serverTick() {
	}

	/**
	 * 子类的客户端逻辑，默认什么也不做。
	 *
	 * <p>绝大多数机器不需要它 —— 界面与贴图要的数据都由方块实体同步包送到客户端。
	 * 只有「渲染需要连续变化的量」的机器才用得上：风力发电机的叶轮角度就推进在这里。
	 *
	 * <p>注意 {@link com.cyx.crimsoncoppergrid.common.blocks.BlockMachineBase#getTicker}
	 * 默认只在服务端返回 ticker，所以想用这个钩子的机器必须自己覆写 {@code getTicker}。
	 */
	protected void clientTick() {
	}

	// ------------------------------------------------------------ 方块状态

	/** 方块朝向；不是本模组的机器方块时退回 NORTH。 */
	public Direction getFacing() {
		BlockState state = getBlockState();
		return state.getBlock() instanceof BlockMachineBase machine ? machine.getFacing(state) : Direction.NORTH;
	}

	public boolean isActive() {
		BlockState state = getBlockState();
		return state.getBlock() instanceof BlockMachineBase machine && machine.isActive(state);
	}

	/** 切换 {@code ACTIVE} 方块状态；状态没变时不写方块，避免无谓的邻居更新。 */
	public void setActive(boolean active) {
		Level level = getLevel();
		if (level == null) {
			return;
		}
		BlockState state = getBlockState();
		if (!(state.getBlock() instanceof BlockMachineBase machine)) {
			return;
		}
		if (machine.isActive(state) == active) {
			return;
		}
		machine.setActive(active, level, worldPosition);
	}

	// ------------------------------------------------------------ 生命周期钩子

	/** 由 {@link BlockMachineBase#setPlacedBy} 调用。 */
	public void onPlace(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
	}

	/** 由 {@link BlockMachineBase#playerWillDestroy} 调用。 */
	public void onBreak(Level level, BlockPos pos, BlockState state, Player player) {
	}

	// ------------------------------------------------------------ 同步

	/** 请求把方块实体数据发给周围客户端；实际发送会被合并到下一个同步窗口。 */
	public void syncWithAll() {
		this.markSync = true;
	}

	private void syncIfNecessary() {
		if (markSync && tickTime % SYNC_COOLDOWN == 0) {
			markSync = false;
			sendBlockUpdate();
		}
	}

	/** 立刻通知客户端刷新（方块状态与方块实体数据）。 */
	protected void sendBlockUpdate() {
		Level level = getLevel();
		if (level != null && !level.isClientSide()) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
		}
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}

	// ------------------------------------------------------------ 存档

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
	}

	// ------------------------------------------------------------ Container（默认空实现）

	@Override
	public int getContainerSize() {
		return 0;
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public ItemStack getItem(int slot) {
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ItemStack.EMPTY;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
	}

	@Override
	public boolean stillValid(Player player) {
		return Container.stillValidBlockEntity(this, player);
	}

	@Override
	public void clearContent() {
	}
}
