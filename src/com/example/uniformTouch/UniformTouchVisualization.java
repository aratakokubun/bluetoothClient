package com.example.uniformTouch;

import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;

import com.example.bluetoothclient.Receiver;
import com.example.converter.voltagePressureConverter;
import com.example.utils.SensorCalibUtilsF;
import com.example.utils.UniformFSRTouchUtils;

public class UniformTouchVisualization extends Receiver{
	// Debugging
	private static final boolean D = true;
	private static final String TAG = "TouchInfoController";
	
	private static final int LEFT = 0;
	private static final int RIGHT = 1;
	
	private UniformTouchSurface touchSurface;
	
	private float[] fsrOrigin = new float[Receiver.FSR_NUM];
	private float[] fsr = new float[Receiver.FSR_NUM];
	private Touch[] touch = new Touch[2];
	
	/* ------------------------------------------------------------------------------ */
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		// Reference to surface view
		touchSurface = new UniformTouchSurface(this, CL.NEXUS7_WIDTH, CL.NEXUS7_HEIGHT);
		setContentView(touchSurface);
		
		for(int i = 0; i < 2; i++){
			touch[i] = new Touch();
		}
	}
	
	/* ------------------------------------------------------------------------------ */
	// Override methods to get BT messages of touch coordinates and pressures
	@Override
	protected void handleFSRMessage(float[] fsr){
		if(fsr.length != Receiver.FSR_NUM) return;
		
		for(int i = 0; i < Receiver.FSR_NUM; i++){
			// TODO
			float coefficient = 1.f;
			this.fsr[i] = voltagePressureConverter.convertVoltageToPressure(fsr[i], coefficient);
			this.fsrOrigin[i] = this.fsr[i];
		}
		
		SensorCalibUtilsF.calibAll(fsr);
		touchSurface.setFsr(fsr);
	}
	
	@Override
	// Uniforming touch coordinates and pressures is called when rear touch information is handled.
	protected void handleTouchMessage(int[] data){
		// If touch event newly occurred, change calibration value of FSR.
		calibOnTouch(data);
		// Calibrate value of float FSR
		for(int i = 0; i < Receiver.FSR_NUM; i++){
			fsr[i] = SensorCalibUtilsF.calib(fsrOrigin[i], i);
		}
		
		Touch[] t = UniformFSRTouchUtils.calculatePressureOfEachTouchPoint(data, fsr, super.width, super.height);
		
		if(t == null){
			for(int i = 0; i < 2; i++){
				touch[i].setEnabled(false);
			}
		} else {
			int size = t.length;
			
			if(size == 1){
				int index = (t[0].getX() < super.width/2) ? LEFT : RIGHT;
				touch[index] = t[0];
				touch[1-index].setEnabled(false);
			} else {
				int leftIndex = (t[0].getX() < t[1].getX()) ? 0 : 1;
				touch[LEFT] = t[leftIndex];
				touch[RIGHT] = t[1-leftIndex];
			}
		}
		
		touchSurface.setFsr(fsr);
		touchSurface.setTouch(touch);
	}
	
	// MotionEvent.ACTION_DOWN occurred when there is no touch before, and now single or double are touch executed.
	// MotionEvent.ACTION_UP occurred when there is a single touch before, and now the single touch is released.
	// MotionEvent.ACTION_POINTER_UP/DOWN occurred add or decrease touch when there are already touches and remain least of a touch after that.
	private void calibOnTouch(int[] data){
		int action = data[2];
		
		if(action == MotionEvent.ACTION_UP){
			setCalib(true);
		} else if(action == MotionEvent.ACTION_DOWN){
			setCalib(false);
		}
	}
	
	/* ------------------------------------------------------------------------------- */
	@Override
	public boolean onTouchEvent(MotionEvent event){
		super.onTouchEvent(event);
				
		return true;
	}

	/* ------------------------------------------------------------------------------- */
	public void setCalib(boolean isEmptize){
		if(D) Log.e(TAG, "Set Calib Value");
		if(isEmptize){
			SensorCalibUtilsF.setCalib();
		} else {
			SensorCalibUtilsF.setCalib(fsrOrigin);
		}
	}
	
	/* ------------------------------------------------------------------------------- */
	public static void throwLog(String str, int type){
		if(!D) return;
		
		switch(type){
		case Log.VERBOSE:
			Log.v(TAG, str);
			break;
		case Log.DEBUG:
			Log.d(TAG, str);
			break;
		case Log.INFO:
			Log.i(TAG, str);
			break;
		case Log.WARN:
			Log.w(TAG, str);
			break;
		case Log.ERROR:
			Log.e(TAG, str);
			break;
		case Log.ASSERT:
			Log.wtf(TAG, str);
			break;
		}
	}
	
	/* ------------------------------------------------------------------------------ */
	@Override
	public void onDestroy() {
		touchSurface.killThread();
		super.onDestroy();
	}
}