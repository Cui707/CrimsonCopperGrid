package com.cyx.crimsoncoppergrid.blocks.cable;

import com.cyx.crimsoncoppergrid.energy.EnergyLookup;
import com.cyx.crimsoncoppergrid.init.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 电线的方块实体：负责连接位的计算与写回（对齐 TechReborn 的 CableBlockEntity）。
 *
 * <p>为什么把这件事放在方块实体里：
 * <ol>
 *   <li>方块实体能拿到 Level，可以做「同世界实例」的比较，判定更可靠；</li>
 *   <li>放置、邻居变化、周期自愈都收敛到一个方法，不会出现「某条路径漏了重算」；</li>
 *   <li>与 TechReborn 的架构一致，后续要加能量传输也有地方放。</li>
 * </ol>
 */
public class CableBlockEntity extends BlockEntity {
	/** 每次重算之间的间隔（刻）。20 刻 = 1 秒。 */
	public static final int REFRESH_INTERVAL = 10;

	private int cooldown;

	public CableBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.CABLE, pos, state);
	}

	/** 放置 / 邻居变化时外部调用：立刻重算一次。 */
	public void recomputeShape() {
		Level level = this.getLevel();
		if (!(level instanceof ServerLevel)) {
			return;
		}
		BlockState state = this.getBlockState();
		if (!(state.getBlock() instanceof CableBlock)) {
			return;
		}
		BlockState updated = state;
		for (Direction side : Direction.values()) {
			updated = updated.setValue(AbstractConnectionBlock.property(side),
					EnergyLookup.shouldConnect(level, this.getBlockPos(), side));
		}
		if (updated != state) {
			level.setBlockAndUpdate(this.getBlockPos(), updated);
		}
		this.cooldown = REFRESH_INTERVAL;
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, CableBlockEntity entity) {
		if (--entity.cooldown > 0) {
			return;
		}
		entity.cooldown = REFRESH_INTERVAL;
		entity.recomputeShape();
	}

	@Override
	public void clearRemoved() {
		super.clearRemoved();
		// 区块载入后立刻对齐一次，避免存档里的旧形状残留
		this.cooldown = 0;
	}
}
