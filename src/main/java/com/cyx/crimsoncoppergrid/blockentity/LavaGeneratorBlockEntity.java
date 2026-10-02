package com.cyx.crimsoncoppergrid.blockentity;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.cyx.crimsoncoppergrid.common.menu.LavaGeneratorMenu;
import com.cyx.crimsoncoppergrid.common.powerSystem.PowerAcceptorBlockEntity;
import com.cyx.crimsoncoppergrid.init.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 电力岩浆机：耗电把空桶灌成岩浆桶。
 *
 * <p>界面像原版熔炉：左侧放空桶，右侧出岩浆桶，中间是灌注进度。
 * 只要左槽有空桶且还有电，进度就推进；进度满后，左槽扣掉一个空桶，
 * 右槽放进一个岩浆桶。
 *
 * <p>数值保留原来的 "每 mB 岩浆消耗的 FE"，也就是一桶岩浆要
 * {@link #FE_PER_MB} × 1000 = {@value #FE_PER_BUCKET} FE。
 * 每 {@value #FILL_TIME} 刻完成一桶，平均每刻 {@value #FE_PER_TICK} FE。
 */
public class LavaGeneratorBlockEntity extends PowerAcceptorBlockEntity implements MenuProvider, Container {

	public static final int SLOT_INPUT = 0;
	public static final int SLOT_OUTPUT = 1;
	public static final int SLOT_COUNT = 2;

	// ---- 界面数据：在父类的「电量 + 容量」四格之后追加进度 ----
	public static final int DATA_PROGRESS = PowerAcceptorBlockEntity.DATA_COUNT;
	public static final int DATA_MAX_PROGRESS = PowerAcceptorBlockEntity.DATA_COUNT + 1;
	public static final int DATA_COUNT = PowerAcceptorBlockEntity.DATA_COUNT + 2;

	@Override
	public int getCount() {
		return DATA_COUNT;
	}

	@Override
	public int get(int index) {
		return switch (index) {
			case DATA_PROGRESS -> progress;
			case DATA_MAX_PROGRESS -> maxProgress;
			default -> super.get(index);
		};
	}

	/** 每 1 mB 岩浆消耗的电量（保留旧常数，保证单位不变）。 */
	public static final long FE_PER_MB = 30L;
	public static final int BUCKET_MB = 1_000;
	/** 灌满一桶岩浆需要的总电量。 */
	public static final long FE_PER_BUCKET = FE_PER_MB * BUCKET_MB;
	/** 灌一桶耗时 600 刻（30 秒），与耗电量配合得到每刻需求。 */
	public static final int FILL_TIME = 600;
	/** 每 tick 稳定消耗的电量（总电量 / 总时间）。 */
	public static final long FE_PER_TICK = FE_PER_BUCKET / FILL_TIME;

	public static final long MAX_INPUT = 100L;
	/** 缓冲容量：正好够灌满一桶，避免进度在「差一点」时卡死。 */
	public static final long CAPACITY = FE_PER_BUCKET;

	private static final String KEY_PROGRESS = "Progress";
	private static final String KEY_MAX_PROGRESS = "MaxProgress";

	private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
	private int progress;
	private int maxProgress = FILL_TIME;

	public LavaGeneratorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.LAVA_GENERATOR, pos, state);
	}

	// ------------------------------------------------------------ 能量基准

	@Override
	public long getBaseMaxPower() {
		return CAPACITY;
	}

	/** 只用电，不发电。 */
	@Override
	public long getBaseMaxOutput() {
		return 0L;
	}

	@Override
	public long getBaseMaxInput() {
		return MAX_INPUT;
	}

	/** 只要缓冲没满就还能进电；要不要继续工作由 tick 内部判断。 */
	@Override
	protected boolean canAcceptEnergy(@Nullable Direction face) {
		return getStored() < CAPACITY;
	}

	// ------------------------------------------------------------ tick

	@Override
	protected void serverTick() {
		if (!(getLevel() instanceof ServerLevel)) {
			return;
		}

		if (!canWork()) {
			if (progress > 0) {
				progress = 0;
				setChanged();
			}
			setActive(false);
			return;
		}

		if (getStored() < FE_PER_TICK) {
			// 电不够就先停住，进度不倒退
			setActive(false);
			return;
		}

		useEnergy(FE_PER_TICK);
		progress++;
		setActive(true);

		if (progress >= maxProgress) {
			progress = 0;
			ItemStack input = items.get(SLOT_INPUT);
			input.shrink(1);
			ItemStack output = items.get(SLOT_OUTPUT);
			if (output.isEmpty()) {
				items.set(SLOT_OUTPUT, new ItemStack(Items.LAVA_BUCKET));
			} else {
				output.grow(1);
			}
			setChanged();
			syncWithAll();
		} else {
			setChanged();
		}
	}

	private boolean canWork() {
		ItemStack input = items.get(SLOT_INPUT);
		if (input.isEmpty() || !input.is(Items.BUCKET)) {
			return false;
		}
		ItemStack output = items.get(SLOT_OUTPUT);
		if (output.isEmpty()) {
			return true;
		}
		if (!output.is(Items.LAVA_BUCKET)) {
			return false;
		}
		return output.getCount() < output.getMaxStackSize();
	}

	// ------------------------------------------------------------ 容器

	@Override
	public int getContainerSize() {
		return SLOT_COUNT;
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack stack : items) {
			if (!stack.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack getItem(int slot) {
		return items.get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		ItemStack result = ContainerHelper.removeItem(items, slot, amount);
		if (!result.isEmpty()) {
			setChanged();
		}
		return result;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ContainerHelper.takeItem(items, slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		items.set(slot, stack);
		setChanged();
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot == SLOT_INPUT && stack.is(Items.BUCKET);
	}

	@Override
	public boolean stillValid(Player player) {
		return Container.stillValidBlockEntity(this, player);
	}

	@Override
	public void clearContent() {
		items.clear();
	}

	// ------------------------------------------------------------ 界面

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.crimsoncoppergrid.lava_generator");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return new LavaGeneratorMenu(containerId, playerInventory, this);
	}

	// ------------------------------------------------------------ 同步与存档

	@Override
	protected void onEnergyChanged() {
		setChanged();
		syncWithAll();
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.progress = input.getIntOr(KEY_PROGRESS, 0);
		this.maxProgress = Math.max(1, input.getIntOr(KEY_MAX_PROGRESS, FILL_TIME));
		ContainerHelper.loadAllItems(input, items);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt(KEY_PROGRESS, progress);
		output.putInt(KEY_MAX_PROGRESS, maxProgress);
		ContainerHelper.saveAllItems(output, items);
	}
}
