package com.example.utils;

import android.util.Log;

public class MatrixUtils {
	// Debugging
	private static final String TAG = "Matrix utility";
	
	public static float[][] multiplyMatrix4nF (float[][] m1, float[][] m2){
		if(m1[0].length != m2.length){
			return null;
		}
		
		int rows = m1.length;
		int cols = m2[0].length;
		float result[][] = new float[rows][cols];
		
		for(int i = 0; i < rows; i++){
			for(int j = 0; j < cols; j++){
				float sum = 0.f;
				for(int k = 0; k < m1[i].length; k++){
					sum += m1[i][k] * m2[k][j];
				}
				result[i][j] = sum;
			}
		}

		return result;
	}

	public static float[][] transposeMatrix (float[][] m){
		int cols = m.length;
		int rows = m[0].length;
		float[][] result = new float[rows][cols];
		
		for(int i = 0; i < rows; i++){
			for(int j = 0; j < cols; j++){
				result[i][j] = m[j][i];
			}
		}
		
		return result;
	}
	
	public static float[][] inverseMatrix(float[][] m) {
		int size = m.length;
		float[][] result = new float[size][size];
		float buf; // temporary data

		// make identity matrix
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				result[i][j] = (i == j) ? 1.f : 0.f;
			}
		}
		
		// sweep out
		for (int i = 0; i < size; i++) {
			// exception for division by zero
			if (m[i][i] == 0.f)
				return null;
			
			buf = 1 / m[i][i];
			for (int j = 0; j < size; j++) {
				m[i][j] *= buf;
				result[i][j] *= buf;
			}
			for (int j = 0; j < size; j++) {
				if (i != j) {
					buf = m[j][i];
					for (int k = 0; k < size; k++) {
						m[j][k] -= m[i][k] * buf;
						result[j][k] -= result[i][k] * buf;
					}
				}
			}
		}
		
		return result;
	}
}
