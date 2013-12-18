package com.example.converter;

public class voltagePressureConverter {
	private static final float MAX_P = 0.835f;
	private static final float MIN_P = 0.250f;

	private static final float V_IN = 5.f;
	private static final float RESISTANCE = 1.5f; // [k��]

	/* ---------------------------------------------------------------------------------------- */
	// TODO
	// �d���̎��郌���W�������̂ŁC��H�̒�R�l�Ƃ��̕����͌��������K�v
	/**
	 * �d�����爳��(g)�ւ̕ϊ��ɂ���(���ɂ��Ă̓f�[�^�V�[�g�Q��) x���Ɉ���[log(g)],�c���ɒ�R[k��]���Ƃ����Ƃ��A(30,�@20)��(0.25, 10000)��ʂ钼���̃O���t�ɂȂ�B�܂�,
	 * (R - 30) / (logX - log20) = - (30-0.25) /�@log500
	 * �܂�,�d���ƒ�R�̊֌W�� 
	 * V[v] = - (1.5[k��] * 5[v]) / R[k��]
	 * ������A
	 *  (-7.5/V -�@30)/log(X/20) = - 29.75 / log500
	 * �S�͈͂̓d���l��0.050f(R=30)����0.167f(���o���E�l)�܂ł͈̔͂ŕϊ����A�ő�l��MAX_PRESS�ɂȂ�悤�ɓ��{����
	 */
	public static float convertVoltageToPressure(float p, float mag) {
		float fpshi = 20f * (float) Math.pow(500, (30 - V_IN*RESISTANCE/p) / 29.75);
		float maxFpshi = 20f * (float) Math.pow(500, (30 - V_IN*RESISTANCE/MAX_P) / 29.75); //MAX_P�͌��o���E�̍ő�
		float minFpshi = 20f * (float) Math.pow(500, (30 - V_IN*RESISTANCE/MIN_P) / 29.75); //MIN_P�͌��o���E�̍ŏ��l
		float value = fpshi / maxFpshi * MAX_P;
		float minValue = minFpshi / maxFpshi * MAX_P;

		// ���͒l�����͈͂ɂȂ�悤�ɕϊ�
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