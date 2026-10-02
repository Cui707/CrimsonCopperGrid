package com.cyx.crimsoncoppergrid.block.entity;

import java.util.Optional;

import com.cyx.crimsoncoppergrid.init.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 电力熔炉：拿电当燃料的熔炉。
 *
 * <p>没有继承原版 {@code AbstractFurnaceBlockEntity} —— 它的 {@code litTime} /
 * {@code cookingProgress} 都是私有字段，只能靠反射硬塞「虚拟燃料」，很脆。
 * 这里直接复用原版的 {@code RecipeType.SMELTING} 配方表，自己维护一份进度：
 * 每个原版烧炼刻消耗 {@link #FE_PER_COOK_TICK} FE，200 刻烧好一个物品。
 */
public class ElectricFurnaceBlockEntity extends AbstractConsumerBlockEntity implements Container {
	public static final int SLOT_INPUT = 0;
	public static final int SLOT_FUEL = 1;
	public static final int SLOT_OUTPUT = 2;
	public static final int SLOT_COUNT = 3;

	/** 每个「烧炼刻」消耗的能量。一块煤在燃料发电机里是 40 FE/刻，这里取 1/4。 */
	public static final long FE_PER_COOK_TICK = 10L;
	public static final long MAX_INPUT = 20L;
	/** 原版熔炉的默认烧炼时长。 */
	public static final int COOK_TIME = 200;

	private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
	private int workProgress;
	private int maxProgress = COOK_TIME;
	/** 本刻能否推进（等于本刻的需求），由 tick 阶段算出，供电网汇总。 */
	private long cookingFuel;
	private ItemStack recipeResult = ItemStack.EMPTY;

	public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.ELECTRIC_FURNACE, pos, state);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, ElectricFurnaceBlockEntity entity) {
		// 需求必须在电网「汇总需求」之前声明，所以推进逻辑放在 tick 里，
		// 不能在 consumeEnergy() 里才第一次提出需求 —— 那样电网会以为没人要电。
		if (level.isClientSide() || !(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
			return;
		}
		entity.cookingFuel = 0;
		Optional<RecipeHolder<SmeltingRecipe>> found = entity.findRecipe(serverLevel);
		if (found.isEmpty()) {
			entity.workProgress = 0;
			return;
		}
		SmeltingRecipe recipe = found.get().value();
		entity.maxProgress = Math.max(1, recipe.cookingTime());
		entity.recipeResult = recipe.assemble(new SingleRecipeInput(entity.getItem(SLOT_INPUT)));
		if (entity.recipeResult.isEmpty() || !canPlaceResult(entity.getItem(SLOT_OUTPUT), entity.recipeResult)) {
			entity.workProgress = 0;
			return;
		}
		entity.workProgress = Math.min(entity.maxProgress, entity.workProgress + 1);
		entity.cookingFuel = FE_PER_COOK_TICK;
		if (entity.workProgress >= entity.maxProgress) {
			entity.workProgress = 0;
			entity.getItem(SLOT_INPUT).shrink(1);
			ItemStack output = entity.getItem(SLOT_OUTPUT);
			if (output.isEmpty()) {
				entity.setItem(SLOT_OUTPUT, entity.recipeResult.copy());
			} else {
				output.grow(entity.recipeResult.getCount());
			}
		}
		entity.setChanged();
		entity.syncIfNeeded();
	}

	@Override
	public long wantedEnergy() {
		// 本刻能推进就只要这一份能量；电网凑不齐就不推进（进度已在 tick 里加过了）
		return cookingFuel;
	}

	@Override
	public long consumeEnergy(long available) {
		resetAccounting();
		if (cookingFuel <= 0) {
			return 0L;
		}
		long used = Math.min(cookingFuel, Math.max(0, available));
		if (used < cookingFuel) {
			// 电不够：本刻白烧，把刚刚推进的那一格退回去
			workProgress = Math.max(0, workProgress - 1);
		}
		pushConsumed(used);
		return used;
	}

	private Optional<RecipeHolder<SmeltingRecipe>> findRecipe(net.minecraft.server.level.ServerLevel serverLevel) {
		ItemStack input = this.getItem(SLOT_INPUT);
		if (input.isEmpty()) {
			return Optional.empty();
		}
		return serverLevel.recipeAccess().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(input), serverLevel);
	}

	@Override
	protected long getMaxInput() {
		return MAX_INPUT;
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
		return workProgress;
	}

	public int getMaxProgress() {
		return maxProgress;
	}

	/** 燃料槽目前只是装饰性接口，保留给以后「加速烧炼」用。 */
	public ItemStack getFuelStack() {
		return this.getItem(SLOT_FUEL);
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
		ItemStack stack = ContainerHelper.removeItem(items, slot, amount);
		if (!stack.isEmpty()) {
			setChanged();
		}
		return stack;
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
		return slot == SLOT_INPUT;
	}

	@Override
	public boolean stillValid(Player player) {
		return Container.stillValidBlockEntity(this, player);
	}

	@Override
	public void clearContent() {
		items.clear();
	}

	// ------------------------------------------------------------ 存档与展示

	@Override
	public long getEnergyStored() {
		return workProgress;
	}

	@Override
	public long getEnergyCapacity() {
		return maxProgress;
	}

	@Override
	protected void setEnergyStored(long value) {
		this.workProgress = (int) Math.max(0, Math.min(maxProgress, value));
	}

	@Override
	protected void readEnergy(ValueInput input) {
		this.workProgress = input.getIntOr("Progress", 0);
		this.maxProgress = Math.max(1, input.getIntOr("MaxProgress", COOK_TIME));
		ContainerHelper.loadAllItems(input, items);
	}

	@Override
	protected void writeEnergy(ValueOutput output) {
		output.putInt("Progress", workProgress);
		output.putInt("MaxProgress", maxProgress);
		ContainerHelper.saveAllItems(output, items);
	}

	/** 每秒同步一次进度就够了，不必每刻刷方块实体数据。 */
	private void syncIfNeeded() {
		if (++syncTimer >= 20) {
			syncTimer = 0;
			syncToClient();
		}
	}

	private int syncTimer;

	private void syncToClient() {
		Level level = this.getLevel();
		if (level != null && !level.isClientSide()) {
			level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
		}
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return this.saveCustomOnly(registries);
	}
}
