package com.cyx.crimsoncoppergrid.blocks.cable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 由六向连接位拼出电线的碰撞/渲染形状。对齐 TechReborn 的同名类（MIT）。
 *
 * <p>形状是纯函数：{@code (方块, 六个布尔值) -> VoxelShape}，所以结果可以放心缓存。
 * 用 {@link IdentityHashMap} 而不是 HashMap —— 方块状态在 MC 里是池化的单例，
 * 用身份比较既正确又比 {@code equals} 快。
 *
 * <p>{@link Block#box} 的坐标单位是 1/16 格，与模型 JSON 里 {@code from/to} 的单位一致，
 * 所以这里的数字可以直接和资源文件对照着看。
 */
public final class CableShapeUtil {
	private static final Map<BlockState, VoxelShape> SHAPE_CACHE = new IdentityHashMap<>();

	private CableShapeUtil() {
	}

	public static VoxelShape getShape(BlockState state) {
		return SHAPE_CACHE.computeIfAbsent(state, CableShapeUtil::buildShape);
	}

	private static VoxelShape buildShape(BlockState state) {
		if (!(state.getBlock() instanceof CableBlock cable)) {
			return Shapes.block();
		}

		double half = cable.getCableThickness() * 16.0;
		double lo = 8.0 - half;
		double hi = 8.0 + half;

		VoxelShape shape = Block.box(lo, lo, lo, hi, hi, hi);
		List<VoxelShape> arms = new ArrayList<>(6);
		for (Direction side : Direction.values()) {
			if (CableBlock.isConnected(state, side)) {
				arms.add(armShape(side, lo, hi));
			}
		}
		return Shapes.or(shape, arms.toArray(new VoxelShape[0]));
	}

	private static VoxelShape armShape(Direction side, double lo, double hi) {
		return switch (side) {
			case NORTH -> Block.box(lo, lo, 0.0, hi, hi, lo);
			case SOUTH -> Block.box(lo, lo, hi, hi, hi, 16.0);
			case WEST -> Block.box(0.0, lo, lo, lo, hi, hi);
			case EAST -> Block.box(hi, lo, lo, 16.0, hi, hi);
			case DOWN -> Block.box(lo, 0.0, lo, hi, lo, hi);
			case UP -> Block.box(lo, hi, lo, hi, 16.0, hi);
		};
	}
}
