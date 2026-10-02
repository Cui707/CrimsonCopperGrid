package com.cyx.crimsoncoppergrid.common.powerSystem;

/**
 * 能量档位。结构与取值对齐 TechReborn 的 {@code RcEnergyTier}（MIT）。
 *
 * <p>档位只是**描述性的**：它不限制任何东西，只是把「这是一台什么量级的设备」
 * 用一个枚举表达出来，供工具提示、界面与将来的升级系统引用。
 * 真正的 I/O 上限始终由各机器的 {@code getBaseMaxInput()} / {@code getBaseMaxOutput()} 决定。
 *
 * <p>CCG 当前用到的是低档位：
 * <ul>
 *   <li>{@link #MICRO} —— 铜制电线的传导量级；</li>
 *   <li>{@link #LOW} —— 太阳能（20 FE/t）、风力（10~30 FE/t）、电力熔炉（20 FE/t）；</li>
 *   <li>{@link #MEDIUM} —— 燃料发电机（40 FE/t）、电力煤炭合成机、电力岩浆机；</li>
 *   <li>{@link #HIGH} 及以上 —— 电池的单次吞吐（1 000 FE），以及留给将来的余量。</li>
 * </ul>
 *
 * <p>保留 HIGH ~ INFINITE 五个未使用的档位，是为了将来从 TechReborn 移植子系统时
 * 不必再做一次档位映射。
 */
public enum CcgEnergyTier {
	MICRO(8, 8),
	LOW(32, 32),
	MEDIUM(128, 128),
	HIGH(512, 512),
	EXTREME(2048, 2048),
	INSANE(8192, 8192),
	INFINITE(Integer.MAX_VALUE, Integer.MAX_VALUE);

	private final int maxInput;
	private final int maxOutput;

	CcgEnergyTier(int maxInput, int maxOutput) {
		this.maxInput = maxInput;
		this.maxOutput = maxOutput;
	}

	public int getMaxInput() {
		return maxInput;
	}

	public int getMaxOutput() {
		return maxOutput;
	}

	/** 找出能容纳给定速率的最低档位；超出全部档位时返回 {@link #INFINITE}。 */
	public static CcgEnergyTier getTier(long power) {
		for (CcgEnergyTier tier : values()) {
			if (tier.maxInput >= power) {
				return tier;
			}
		}
		return INFINITE;
	}
}
