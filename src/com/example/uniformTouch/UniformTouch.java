package com.example.uniformTouch;

import android.os.Bundle;
import android.widget.TextView;

import com.example.bluetoothclient.R;
import com.example.bluetoothclient.Receiver;
import com.example.utils.UniformFSRTouchUtils;

public class UniformTouch extends Receiver{
	// Debugging
	private static final boolean D = true;
	private static final boolean VISUALIZATION = false;
	private static final String TAG = "TouchInfoController";
	
	// touch point and pressure
	public static final int LEFT = 0;
	public static final int RIGHT = 1;
	
	private float[] fsr = new float[Receiver.FSR_NUM];
	private Touch[] touch = new Touch[2];
	
	/* ------------------------------------------------------------------------------ */
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		if(D){
			if(VISUALIZATION){
				setContentView(R.layout.uniformed_touch_visualization);
			} else {
				setContentView(R.layout.uniformed_touch);
			}
		}
		
		init();
	}
	
	private void init(){
		for(int i = 0; i < Receiver.FSR_NUM; i++){
			fsr[i] = 0.f;
		}
		
		for(int i = 0; i < 2; i++){
			touch[i] = new Touch();
		}
		
		initView();
	}

	/* ------------------------------------------------------------------------------ */
	public boolean initView(){
		if(!D) return false;
		
		if(VISUALIZATION){
			// TODO
		} else {
			final TextView tx1 = (TextView) findViewById(R.id.touch_x1);
			final TextView ty1 = (TextView) findViewById(R.id.touch_y1);
			final TextView tp1 = (TextView) findViewById(R.id.touch_p1);
			final TextView tx2 = (TextView) findViewById(R.id.touch_x2);
			final TextView ty2 = (TextView) findViewById(R.id.touch_y2);
			final TextView tp2 = (TextView) findViewById(R.id.touch_p2);
			
			tx1.setText(String.valueOf(0));
			ty1.setText(String.valueOf(0));
			tp1.setText(String.valueOf(0));
			tx2.setText(String.valueOf(0));
			ty2.setText(String.valueOf(0));
			tp2.setText(String.valueOf(0));
		}
		
		return true;
	}
	
	private boolean changeView(Touch[] t){
		if(!D) return false;
		
		if(VISUALIZATION){
			// TODO
		} else{
			final TextView tx1 = (TextView) findViewById(R.id.touch_x1);
			final TextView ty1 = (TextView) findViewById(R.id.touch_y1);
			final TextView tp1 = (TextView) findViewById(R.id.touch_p1);
			final TextView tx2 = (TextView) findViewById(R.id.touch_x2);
			final TextView ty2 = (TextView) findViewById(R.id.touch_y2);
			final TextView tp2 = (TextView) findViewById(R.id.touch_p2);
			
			if(t[LEFT].getEnabled()){
				tx1.setText(String.valueOf(t[LEFT].getX()));
				ty1.setText(String.valueOf(t[LEFT].getY()));
				tp1.setText(String.valueOf(t[LEFT].getP()));
			} else {
				tx1.setText(String.valueOf(0));
				ty1.setText(String.valueOf(0));
				tp1.setText(String.valueOf(0.f));
			}
			
			if(t[RIGHT].getEnabled()){
				tx2.setText(String.valueOf(t[RIGHT].getX()));
				ty2.setText(String.valueOf(t[RIGHT].getY()));
				tp2.setText(String.valueOf(t[RIGHT].getP()));
			} else {
				tx2.setText(String.valueOf(0));
				ty2.setText(String.valueOf(0));
				tp2.setText(String.valueOf(0.f));
			}
		}
		
		return true;
	}

	/* ------------------------------------------------------------------------------ */
	public Touch getTouch(int index){
		return touch[index];
	}
	
	/* ------------------------------------------------------------------------------ */
	// Override methods to get bluetooth messages of touch coordinates and pressures
	@Override
	protected void handleFSRMessage(float[] fsr){
		if(fsr.length != Receiver.FSR_NUM) return;
		
		this.fsr = fsr;
	}
	
	@Override
	// Uniforming touch coordinates and pressures is called when rear touch information is handled.
	protected void handleTouchMessage(int[] data){
		Touch[] t = UniformFSRTouchUtils.calculatePressureOfEachTouchPoint(data, fsr, super.width, super.height);
		// Touch[] t = UniformFSRTouchUtils.calculatePressureOfEachTouchAndAttachPoint(data, fsr, super.width, super.height);
		
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
		
		changeView(touch);
	}
}