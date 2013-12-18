package com.example.converter;

public class voltagePressureConverter {
	private static final float MAX_P = 0.835f;
	private static final float MIN_P = 0.250f;

	private static final float V_IN = 5.f;
	private static final float RESISTANCE = 1.5f; // [kΩ]

	/* ---------------------------------------------------------------------------------------- */
	// TODO
	// 電圧の取れるレンジが狭いので，回路の抵抗値とこの部分は見直しが必要
	/**
	 * 電圧から圧力(g)への変換について(式についてはデータシート参照) x軸に圧力[log(g)],縦軸に抵抗[kΩ]をとったとき、(30,　20)と(0.25, 10000)を通る直線のグラフになる。つまり,
	 * (R - 30) / (logX - log20) = - (30-0.25) /　log500
	 * また,電圧と抵抗の関係は 
	 * V[v] = - (1.5[kΩ] * 5[v]) / R[kΩ]
	 * これより、
	 *  (-7.5/V -　30)/log(X/20) = - 29.75 / log500
	 * 全範囲の電圧値を0.050f(R=30)から0.167f(検出限界値)までの範囲で変換し、最大値がMAX_PRESSになるように等倍する
	 */
	public static float convertVoltageToPressure(float p, float mag) {
		float fpshi = 20f * (float) Math.pow(500, (30 - V_IN*RESISTANCE/p) / 29.75);
		float maxFpshi = 20f * (float) Math.pow(500, (30 - V_IN*RESISTANCE/MAX_P) / 29.75); //MAX_Pは検出限界の最大
		float minFpshi = 20f * (float) Math.pow(500, (30 - V_IN*RESISTANCE/MIN_P) / 29.75); //MIN_Pは検出限界の最小値
		float value = fpshi / maxFpshi * MAX_P;
		float minValue = minFpshi / maxFpshi * MAX_P;

		// 圧力値を一定範囲になるように変換
		float result = value;
		if (result > MAX_P) {
			result = MAX_P;
		} else if (result < minValue){
			result = minValue;
		} else {
			result = (result - minValue) / (MAX_P - minValue) * MAX_P;
		}
		
		return result *= mag;
	}
}