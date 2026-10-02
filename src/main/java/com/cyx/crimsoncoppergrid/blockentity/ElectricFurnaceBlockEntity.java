package com.cyx.crimsoncoppergrid.blockentity;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.cyx.crimsoncoppergrid.common.menu.ElectricFurnaceMenu;
import com.cyx.crimsoncoppergrid.common.powerSystem.PowerAcceptorBlockEntity;
import com.cyx.crimsoncoppergrid.init.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 电力熔炉：拿电当燃料的熔炉。
 *
 * <p>没有继承原版 {@code AbstractFurnaceBlockEntity} —— 它的 {@code litTime} /
 * {@code cookingProgress} 都是私有字段，只能靠反射硬塞「虚拟燃料」，很脆。
 * 这里直接复用原版的 {@code RecipeType.SMELTING} 配方表，自己维护一份进度：
 * 每个原版烧炼刻消耗 {@link #FE_PER_COOK_TICK} FE，{@link #COOK_TIME} 刻烧好一个物品。
 */
public class ElectricFurnaceBlockEntity extends PowerAcceptorBlockEntity implements MenuProvider {

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
			case DATA_PROGRESS -> cookProgress;
			case DATA_MAX_PROGRESS -> maxProgress;
			default -> super.get(index);
		};
	}

	/** 每个「烧炼刻」消耗的能量。 */
	public static final long FE_PER_COOK_TICK = 10L;
	/** 原版熔炉的默认烧炼时长。 */
	public static final int COOK_TIME = 200;
	/** 每 tick 最多进多少电。 */
	public static final long MAX_INPUT = 20L;
	/** 缓冲容量：正好够烧完一个物品（200 刻 × 10 FE）再稍有余量。 */
	public static final long CAPACITY = FE_PER_COOK_TICK * COOK_TIME;

	private static final String KEY_PROGRESS = "Progress";
	private static final String KEY_MAX_PROGRESS = "MaxProgress";

	private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
	private int cookProgress;
	private int maxProgress = COOK_TIME;

	public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.ELECTRIC_FURNACE, pos, state);
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

	/** 没东西可烧、或输出槽满了，就不再要电。 */
	@Override
	protected boolean canAcceptEnergy(@Nullable Direction face) {
		return getStored() < CAPACITY;
	}

	// ------------------------------------------------------------ tick

	@Override
	protected void serverTick() {
		if (!(getLevel() instanceof ServerLevel serverLevel)) {
			return;
		}

		ItemStack input = items.get(SLOT_INPUT);
		if (input.isEmpty()) {
			resetProgress();
			return;
		}

		Optional<RecipeHolder<SmeltingRecipe>> found = serverLevel.recipeAccess()
				.getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(input), serverLevel);
		if (found.isEmpty()) {
			resetProgress();
			return;
		}

		SmeltingRecipe recipe = found.get().value();
		maxProgress = Math.max(1, recipe.cookingTime());
		ItemStack result = recipe.assemble(new SingleRecipeInput(input));
		if (result.isEmpty() || !canPlaceResult(items.get(SLOT_OUTPUT), result)) {
			resetProgress();
			return;
		}

		// 电不够就停在原地，进度不倒退（玩家的直观预期：电来了就继续，而不是从头再来）
		if (getStored() < FE_PER_COOK_TICK) {
			setActive(false);
			return;
		}

		useEnergy(FE_PER_COOK_TICK);
		cookProgress++;
		setActive(true);

		if (cookProgress >= maxProgress) {
			cookProgress = 0;
			input.shrink(1);
			ItemStack output = items.get(SLOT_OUTPUT);
			if (output.isEmpty()) {
				items.set(SLOT_OUTPUT, result.copy());
			} else {
				output.grow(result.getCount());
			}
			setChanged();
			syncWithAll();
		}
	}

	private void resetProgress() {
		if (cookProgress != 0) {
			cookProgress = 0;
			setChanged();
		}
		setActive(false);
	}

	private static boolean canPlaceResult(ItemStack output, ItemStack result) {
		if (output.isEmpty()) {
			return true;
		}
		if (!ItemStack.isSameItemSameComponents(output, result)) {
			return false;
		}
		return output.getCount() + result.getCount() <= output.getMaxStackSize();
	}

	public int getProgress() {
		return cookProgress;
	}

	public int getMaxProgress() {
		return maxProgress;
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
		ItemStack stack = net.minecraft.world.ContainerHelper.removeItem(items, slot, amount);
		if (!stack.isEmpty()) {
			setChanged();
		}
		return stack;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return net.minecraft.world.ContainerHelper.takeItem(items, slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		items.set(slot, stack);
		setChanged();
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot == SLOT_INPUT;
	}

	@Override
	public void clearContent() {
		items.clear();
	}

	// ------------------------------------------------------------ 同步与存档

	@Override
	protected void onEnergyChanged() {
		setChanged();
		syncWithAll();
	}

	// ------------------------------------------------------------ 界面

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.crimsoncoppergrid.electric_furnace");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return new ElectricFurnaceMenu(containerId, playerInventory, this);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.cookProgress = input.getIntOr(KEY_PROGRESS, 0);
		this.maxProgress = Math.max(1, input.getIntOr(KEY_MAX_PROGRESS, COOK_TIME));
		net.minecraft.world.ContainerHelper.loadAllItems(input, items);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt(KEY_PROGRESS, cookProgress);
		output.putInt(KEY_MAX_PROGRESS, maxProgress);
		net.minecraft.world.ContainerHelper.saveAllItems(output, items);
	}
}
