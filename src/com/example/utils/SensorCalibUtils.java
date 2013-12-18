package com.example.utils;

import android.util.Log;

import com.example.bluetoothclient.Receiver;

/**
 * Each FSR sensor value is not zero because of the default value of weight of the plate and noises in the circuit.
 * Thus, the default value have to be cut off and map it to get the true value of pushing.
 */
public class SensorCalibUtils {
	// Debugging
	private static final String TAG = "Sensor Calibration Utility";
	
	private static final int[] CALIB_FSR = {
		0,	// f0 (0, 0)
		0, // f1 (w, 0)
		0, // f2 (0, h)
		0  // f3 (w, h)
	};
	
	// Use present values for calibration of FSR
	private static int[] calib = new int[Receiver.FSR_NUM];
	private static boolean isUseDynamic = true;
	
	/* -------------------------------------------------------------- */
	public static void calibAll(int[] fsr){
		if(fsr.length != Receiver.FSR_NUM) return;
		
		for(int i = 0; i < Receiver.FSR_NUM; i++){
			fsr[i] -= isUseDynamic ? calib[i] : CALIB_FSR[i];
		}
	}
	
	public static int calib(int fsr, int index){
		int result = fsr - (isUseDynamic ? calib[index] : CALIB_FSR[index]);
		return result<0 ? 0 : result;
	}
	
	public static void showCalibValue(int[] fsr){
		String str = new String();
		
		for(int i = 0; i < fsr.length; i++){
			str += "value[" + i + "] = " + fsr[i] + ", ";
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
	
	public static void setCalib(int[] fsr){
		if(!isUseDynamic || fsr.length != Receiver.FSR_NUM) return;
		
		for(int i = 0; i < Receiver.FSR_NUM; i++){
			calib[i] = fsr[i];
		}
	}
	
	public static void setCalib(){
		if(!isUseDynamic) return;
		
		for(int i = 0; i < Receiver.FSR_NUM; i++){
			calib[i] = 0;
		}
	}
	
	/* -------------------------------------------------------------- */
	public int map(int value, int lo, int hi){
		if(hi-lo <= 0) return 0;
		
		return lo + (int)((hi-lo) * (value / (float)hi) );
	}
}