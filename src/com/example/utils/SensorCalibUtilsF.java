package com.example.utils;

import android.util.Log;

import com.example.bluetoothclient.Receiver;

/**
 * Each FSR sensor value is not zero because of the default value of weight of the plate and noises in the circuit.
 * Thus, the default value have to be cut off and map it to get the true value of pushing.
 */
public class SensorCalibUtilsF {
	// Debugging
	private static final String TAG = "Sensor Calibration Utility";
	
	private static final float[] CALIB_FSR = {
		0.f, // f0 (0, 0)
		0.f, // f1 (w, 0)
		0.f, // f2 (0, h)
		0.f  // f3 (w, h)
	};
	
	// Use present values for calibration of FSR
	// The size of the calibration array is defined as Receiver.FSR_NUM
	private static float[] calib = {
		0.f, // f0 (0, 0)
		0.f, // f1 (w, 0)
		0.f, // f2 (0, h)
		0.f  // f3 (w, h)
	};
	private static boolean isUseDynamic = true;
	
	/* -------------------------------------------------------------- */
	public static void calibAll(float[] fsr){
		if(fsr.length != Receiver.FSR_NUM) return;
		
		for(int i = 0; i < Receiver.FSR_NUM; i++){
			fsr[i] -= isUseDynamic ? calib[i] : CALIB_FSR[i];
		}
	}
	
	public static float calib(float fsr, int index){
		float result = fsr - (isUseDynamic ? calib[index] : CALIB_FSR[index]);
		return result<0.f ? 0.f : result;
	}
	
	public static void showCalibValue(){
		String str = new String();
		
		for(int i = 0; i < calib.length; i++){
			str += "value[" + i + "] = " + calib[i] + ", ";
		}
		Log.i(TAG, str);
	}
	
	/* -------------------------------------------------------------- */
	public static void setIsUseDynamic(boolean is){
		isUseDynamic = is;
	}
	
	public static void setIsUseDynamic(){
		isUseDynamic = !isUseDynamic;
	}
	
	public static void setCalib(float[] fsr){
		if(!isUseDynamic || fsr.length != Receiver.FSR_NUM) return;
		
		for(int i = 0; i < Receiver.FSR_NUM; i++){
			calib[i] = fsr[i];
		}
	}
	
	public static void setCalib(){
		if(!isUseDynamic) return;
		
		for(int i = 0; i < Receiver.FSR_NUM; i++){
			calib[i] = 0.f;
		}
	}
	
	/* -------------------------------------------------------------- */
	public int map(int value, int lo, int hi){
		if(hi-lo <= 0) return 0;
		
		return lo + (int)((hi-lo) * (value / (float)hi) );
	}
}