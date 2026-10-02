package com.cyx.crimsoncoppergrid.common.powerSystem;

import java.util.Locale;

/**
 * 能量数值的展示格式化。对齐 TechReborn / RebornCore 的 {@code PowerSystem}（MIT），
 * 但单位从 TechReborn 的 {@code E} 换成 CCG 的 {@code FE}。
 *
 * <p>三种口径，用途不同：
 * <ul>
 *   <li>{@link #getLocalizedPower(long)} —— 带量级后缀，界面与提示里读起来最省地方；
 *   <li>{@link #getLocalizedPowerFull(long)} —— 完整数字，需要精确值（例如存档对比）时用；
 *   <li>{@link #getLocalizedPowerNoSuffix(long)} —— 带量级但不带单位，留给本来就写了单位的地方。
 * </ul>
 */
public final class PowerSystem {
	/** CCG 的能量单位。与 FE（Forge Energy）语义一致，也即 Team Reborn Energy 的语义。 */
	public static final String ABBREVIATION = "FE";

	private static final char[] MAGNITUDE = { 'k', 'M', 'G', 'T' };

	private PowerSystem() {
	}

	/** 例：{@code 1234 -> "1.2 kFE"}、{@code 64000 -> "64 kFE"}、{@code 500 -> "500 FE"}。 */
	public static String getLocalizedPower(long power) {
		return getLocalizedPowerNoSuffix(power) + " " + ABBREVIATION;
	}

	/** 同上，但不追加单位后缀。 */
	public static String getLocalizedPowerNoSuffix(long power) {
		boolean negative = power < 0;
		double value = Math.abs((double) power);

		if (value < 1000) {
			return (negative ? "-" : "") + (long) value;
		}

		int magnitude = 0;
		value /= 1000.0;
		while (value >= 1000 && magnitude < MAGNITUDE.length - 1) {
			value /= 1000.0;
			magnitude++;
		}

		// 保留一位小数；四舍五入后若又满 1000（例如 999_999 -> 1000.0k）就再升一级
		double rounded = Math.round(value * 10.0) / 10.0;
		if (rounded >= 1000 && magnitude < MAGNITUDE.length - 1) {
			rounded /= 1000.0;
			magnitude++;
		}

		String text = String.format(Locale.ROOT, "%.1f", rounded);
		if (text.endsWith(".0")) {
			text = text.substring(0, text.length() - 2);
		}
		return (negative ? "-" : "") + text + MAGNITUDE[magnitude];
	}

	/** 完整数字，带千分位与单位。例：{@code 64000 -> "64,000 FE"}。 */
	public static String getLocalizedPowerFull(long power) {
		return String.format(Locale.ROOT, "%,d", power) + " " + ABBREVIATION;
	}
}
