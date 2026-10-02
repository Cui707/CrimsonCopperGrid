package com.cyx.crimsoncoppergrid.blockentity;

import java.util.Optional;

import com.cyx.crimsoncoppergrid.common.menu.FuelGeneratorMenu;
import com.cyx.crimsoncoppergrid.common.powerSystem.PowerAcceptorBlockEntity;
import com.cyx.crimsoncoppergrid.init.ModBlockEntities;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.phys.Vec3;

/**
 * 燃料发电机：烧原版燃料换电。继承 {@link PowerAcceptorBlockEntity}，
 * 因此它自己就会把缓冲里的电推向四周的邻居。
 *
 * <p>数值与熔炉对齐：一个「燃料刻」产出 {@link #FE_PER_FUEL_TICK} FE。
 * 原版燃料能烧多久，这里就烧多久，所以木头、煤炭、木炭、岩浆桶、干海带块……
 * 只要原版熔炉能烧，这里都能烧。
 *
 * <p>为什么自己维护 {@code burnTime} 而不是复用原版 {@code AbstractFurnaceBlockEntity}：
 * 它的 {@code litTime} / {@code cookingProgress} 都是私有字段，只能靠反射硬塞，
 * 版本一升就碎。
 *
 * <h2>为什么输入恒为 0</h2>
 * 发电机的缓冲只出不进 —— {@link #getBaseMaxInput()} 返回 0，
 * 于是 {@code getMaxInput(face)} 恒为 0，外部往它插电会被直接拒绝。
 * 这不需要在 {@code canAcceptEnergy} 里额外判断。
 */
public class FuelGeneratorBlockEntity extends PowerAcceptorBlockEntity implements MenuProvider {

	public static final int SLOT_FUEL = 0;
	public static final int SLOT_COUNT = 1;

	// ---- 界面数据：在父类的「电量 + 容量」四格之后追加燃烧进度 ----
	public static final int DATA_BURN_TIME = PowerAcceptorBlockEntity.DATA_COUNT;
	public static final int DATA_BURN_TIME_TOTAL = PowerAcceptorBlockEntity.DATA_COUNT + 1;
	public static final int DATA_COUNT = PowerAcceptorBlockEntity.DATA_COUNT + 2;

	@Override
	public int getCount() {
		return DATA_COUNT;
	}

	@Override
	public int get(int index) {
		return switch (index) {
			case DATA_BURN_TIME -> burnTime;
			case DATA_BURN_TIME_TOTAL -> burnTimeTotal;
			default -> super.get(index);
		};
	}

	/** 一个燃料刻产生的能量。 */
	public static final long FE_PER_FUEL_TICK = 40L;
	/** 内部缓冲容量：够装 100 刻的产出，也就是 5 秒。 */
	public static final long CAPACITY = 4_000L;
	/** 每 tick 最多往外推多少。 */
	public static final long MAX_OUTPUT = 40L;

	private static final String KEY_BURN_TIME = "BurnTime";
	private static final String KEY_BURN_TIME_TOTAL = "BurnTimeTotal";
	private static final String KEY_FUEL = "Fuel";

	private int burnTime;
	private int burnTimeTotal;
	private ItemStack fuel = ItemStack.EMPTY;

	public FuelGeneratorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.FUEL_GENERATOR, pos, state);
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

	/** 发电机不接收外部供电。 */
	@Override
	public long getBaseMaxInput() {
		return 0L;
	}

	// ------------------------------------------------------------ tick

	@Override
	protected void serverTick() {
		Level level = getLevel();
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}

		boolean running = false;

		if (burnTime <= 0) {
			int value = fuelBurnTime(serverLevel, fuel);
			if (value > 0) {
				burnTime = value;
				burnTimeTotal = value;
				consumeFuel(serverLevel);
			}
		}

		if (burnTime > 0) {
			burnTime--;
			running = true;
			// 缓冲满了就让燃料继续烧、电白白散掉 —— 与熔炉烧着但没东西可烧的表现一致
			long space = getFreeSpace();
			if (space > 0) {
				addEnergy(Math.min(FE_PER_FUEL_TICK, space));
			}
		} else if (burnTimeTotal != 0) {
			burnTimeTotal = 0;
		}

		// 驱动贴图上的「正在工作」状态
		setActive(running);
	}

	// ------------------------------------------------------------ 燃料

	public int getBurnTime() {
		return burnTime;
	}

	public int getBurnTimeTotal() {
		return burnTimeTotal;
	}

	public boolean isBurning() {
		return burnTime > 0;
	}

	/**
	 * 判断物品是否为原版燃料。
	 *
	 * <p>1.21.2+ 把燃料信息放在 {@link DataComponents#COOKING_FUEL} 组件里，
	 * 数据驱动，不需要自己维护一个长列表。这里的判定只看组件是否存在，
	 * 客户端与服务端结论完全一致，因此 {@link #canPlaceItem} 可以放心用它。
	 */
	public static boolean isFuel(ItemStack stack) {
		return !stack.isEmpty() && stack.has(DataComponents.COOKING_FUEL);
	}

	/**
	 * 一件燃料能烧多久（刻）。
	 *
	 * <p>原版燃料数据用 {@link CookingFuel} 组件保存，里面可能引用
	 * {@code context_int_provider}（例如快烧方块会缩短时间），所以需要一个
	 * 包含本方块状态的 {@link LootContext} 才能算出真正数值。
	 *
	 * <h2>四个参数一个都不能少</h2>
	 * {@code CONTAINER_PROCESS} 这套参数表要求
	 * {@code BLOCK_STATE}、{@code BLOCK_ENTITY}、{@code ORIGIN}、{@code CONTAINER}
	 * 四项齐全，少任何一项 {@code create()} 都会抛
	 * {@code IllegalArgumentException: Missing required parameters}。
	 * {@code CONTAINER} 要的是 {@link net.minecraft.world.entity.SlotProvider}，
	 * 本类已经实现 {@link net.minecraft.world.Container}（它继承 SlotProvider），
	 * 所以直接传 {@code this} 即可 —— 原版
	 * {@code BaseContainerBlockEntity#getLootContext} 就是这么写的。
	 *
	 * <p>曾经漏掉 {@code CONTAINER} 一项，结果发电机每 tick 抛异常、
	 * 世界一加载就崩，只能删档。改动这里时请一并核对参数表。
	 */
	private int fuelBurnTime(ServerLevel level, ItemStack stack) {
		if (!isFuel(stack)) {
			return 0;
		}
		LootParams params = new LootParams.Builder(level)
				.withParameter(LootContextParams.BLOCK_STATE, getBlockState())
				.withParameter(LootContextParams.BLOCK_ENTITY, this)
				.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(getBlockPos()))
				.withParameter(LootContextParams.CONTAINER, this)
				.create(LootContextParamSets.CONTAINER_PROCESS);
		LootContext context = new LootContext.Builder(params).create(Optional.empty());
		return ResolvableInt.getFromItem(stack, DataComponents.COOKING_FUEL, CookingFuel::burnTime, context, 0);
	}

	private void consumeFuel(ServerLevel level) {
		// 26.3 的燃料余料是 ItemStackTemplate（例如岩浆桶 -> 空桶）。
		// 注意它可能为 null —— 绝大多数燃料（煤、木头……）都没有余料，
		// 直接 .create() 会 NPE 并把世界 tick 崩掉。
		ItemStackTemplate remainder = fuel.getItem().getCraftingRemainder();
		fuel.shrink(1);
		if (remainder != null) {
			ItemStack result = remainder.create();
			if (fuel.isEmpty()) {
				fuel = result;
			} else {
				// 槽里还剩着燃料时余料塞不进去，按原版熔炉的做法丢到方块旁
				Containers.dropItemStack(level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), result);
			}
		}
		setChanged();
	}

	// ------------------------------------------------------------ 容器（单个燃料槽）

	@Override
	public int getContainerSize() {
		return SLOT_COUNT;
	}

	@Override
	public boolean isEmpty() {
		return fuel.isEmpty();
	}

	@Override
	public ItemStack getItem(int slot) {
		return fuel;
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		ItemStack split = fuel.split(amount);
		if (!split.isEmpty()) {
			setChanged();
		}
		return split;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		ItemStack stack = fuel;
		fuel = ItemStack.EMPTY;
		return stack;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		fuel = stack;
		setChanged();
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return isFuel(stack);
	}

	@Override
	public void clearContent() {
		fuel = ItemStack.EMPTY;
	}

	// ------------------------------------------------------------ 界面

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.crimsoncoppergrid.fuel_generator");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return new FuelGeneratorMenu(containerId, playerInventory, this);
	}

	// ------------------------------------------------------------ 同步与存档

	/** 电量一变就让客户端知道（发电机的贴图与提示都依赖它）。 */
	@Override
	protected void onEnergyChanged() {
		setChanged();
		syncWithAll();
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.burnTime = input.getIntOr(KEY_BURN_TIME, 0);
		this.burnTimeTotal = input.getIntOr(KEY_BURN_TIME_TOTAL, 0);
		this.fuel = input.read(KEY_FUEL, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt(KEY_BURN_TIME, burnTime);
		output.putInt(KEY_BURN_TIME_TOTAL, burnTimeTotal);
		output.store(KEY_FUEL, ItemStack.OPTIONAL_CODEC, fuel);
	}
}
