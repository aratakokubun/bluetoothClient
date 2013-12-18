package com.example.bluetoothclient;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManager;

import com.example.converter.ByteConverter;
import com.example.converter.analogVoltageConverter;
import com.example.uniformTouch.CL;

/**
 * Send and Receive Message using BlueTooth.
 * Information in the message contains
 * 0, message tag
 * 1, touch x coordinate / or resist id, type id, etc...
 * 2, touch y coordinate
 * 3, touch action
 * 4, touch pressure
 * 5, touch time
 */

public class Receiver extends BluetoothActivity {
	// Debugging
	private static final String TAG = "Recever";
	private static final boolean D = false;
	
	// Message size byte
	public static final int MESSAGE_SIZE = 9;
	public static final int FSR_NUM = 4;
	
	// Lock the back button
	private static final int EXIT_LOCK_COUNT = 5;
	private boolean onMenuControlled;
	private int onMenuControllCount;
	
	// Canvas layout
	private Rect scanArea;
	private Rect discoverableArea;
	// Window size
	protected int width;
	protected int height;
	
	// Temporary preserve FSR value[FSR_NUM]
	protected int[] fsrAnalog = new int[FSR_NUM];
	
	// Time manager
	private static final int TIME_BETWEEN_SCAN = 2 * 1000;
	private long scanLastTime;
	
	private static final int TIME_BETWEEN_DOUBLE_TOUCH = 500;
	private long lastTime;
	
    /* ---------------------------------------------------------------------- */
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		onMenuControlled = true;
		onMenuControllCount = 0;
		
		lastTime = System.currentTimeMillis();
		scanLastTime = System.currentTimeMillis();
		
		setCanvasLayout();
	}
	
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	private void setCanvasLayout(){
		if (  Integer.valueOf(android.os.Build.VERSION.SDK_INT) < 13 ) {
			Display display = ((WindowManager)getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
			width = display.getWidth();
			height = display.getHeight();
		} else {
			Display display = getWindowManager().getDefaultDisplay();
			Point size = new Point();
			display.getSize(size);
			width = size.x;
			height = size.y;
		}

		scanArea = new Rect((int)(width*CL.H00.left), (int)(height*CL.H00.top), (int)(width*CL.H00.right), (int)(height*CL.H00.bottom));
		discoverableArea = new Rect((int)(width*CL.H02.left), (int)(height*CL.H02.top), (int)(width*CL.H02.right), (int)(height*CL.H02.bottom));
	}
	
    /* ---------------------------------------------------------------------- */
    // receive BT message and decode it
    @Override
    protected void decodeMateMsg(int bytes, byte[] buffer){
    	// check header message
    	if(buffer[0] != 0x1 || bytes != MESSAGE_SIZE) return;
		
		// decompose message which is composed from FSR_NUM data of pressure
    	float[] p = new float[FSR_NUM];
    	
    	// TODO
		// Map reference to each FSRs
    	
    	for(int i = 0; i < FSR_NUM; i++){
    		int index = 1 + i*2;
    		int analogValue = ByteConverter.composeInt(buffer[index], buffer[index+1]);
    		p[i] = analogVoltageConverter.fsrToVoltage(analogValue);
    		// fsrAnalog[i] = analogValue;
    		// int calibratedValue = SensorCalibUtils.calib(analogValue, i);
    		// p[i] = analogVoltageConverter.fsrToVoltage(calibratedValue);
    	}
		
		// preserve pressure data according to the index
		handleFSRMessage(p);
    }
    
    @Override
    protected void decodeMateString(String[] array){
    	// check length
    	if(array.length != FSR_NUM) return;
    	
		// decompose message which is composed from FSR_NUM data of pressure
		float[] p = new float[4];
		for(int i = 0; i < FSR_NUM; i++){
    		int analogValue = Integer.valueOf(array[i].replaceAll("\"", ""));
    		p[i] = analogVoltageConverter.fsrToVoltage(analogValue);
			if(D) Log.i(TAG, "fsr at "+i+" = "+p[i]);
		}
		
		handleFSRMessage(p);
    }
    
    // Describe detail at override method
    protected void handleFSRMessage(float[] p){
    	// TODO
    }
    
    /* ---------------------------------------------------------------------- */
    @Override
    protected void decodeAppMsg(byte[] buf){
    	// decode size per byte;
    	int size = Integer.SIZE / Byte.SIZE;
    	
    	// read first byte (length)
    	int length = (int)ByteConverter.getInteger(buf, size*0);
    	
    	if(length == 1){
    		// length = 1 implies exit
    		onExit();
    		return;
    	} else if(length == 0){
    		// fatal error
    		if(D) Log.e(TAG, "fatal error occurred when decoding message.");
    		return;
    	}
    	
    	// array for data
    	int[] receiveData = new int[length];
		for (int i = 1; i < length; i++) {
			receiveData[i - 1] = ByteConverter.getInteger(buf, size * i);
		}
		handleTouchMessage(receiveData);
    }
    
    // Describe detail at override method
    protected void handleTouchMessage(int[] data){
    	
    }
    
    /* ---------------------------------------------------------------------- */
    // send BT message

    /* ---------------------------------------------------------------------- */
    // control key buttons(home, return, etc...)
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
      if(keyCode==KeyEvent.KEYCODE_BACK){
    	  if(onMenuControlled){
    		  onMenuControllCount++;
    		  if(onMenuControllCount >= EXIT_LOCK_COUNT){
    			  onMenuControlled = false;
    		  }
    		  return false;
    	  } else {
    		  onExit();
    		  return true;
    	  }
      } else if(keyCode==KeyEvent.KEYCODE_HOME){
    	  if(onMenuControlled){
    		  return false;
    	  } else {
    		  moveTaskToBack(true);
    		  return true;
    	  }
      }
      return false;
    }
	
    /* ---------------------------------------------------------------------- */
    public void scan(){
    	long nowTime = System.currentTimeMillis();
    	if(nowTime - scanLastTime > TIME_BETWEEN_SCAN){
	        // Launch the DeviceListActivity to see devices and do scan
	        Intent serverIntent = new Intent(this, DeviceListActivity.class);
	        startActivityForResult(serverIntent, BluetoothService.REQUEST_CONNECT_DEVICE);
	    	scanLastTime = nowTime;
    	}
    }
    
    public void discoverable(){
        // Ensure this device is discoverable by others
        ensureDiscoverable();
    }
    
    public void onExit(){
    	int[] i = {0};
    	sendMessage(1, i, BluetoothService.APP);
    	finish();
    	System.exit(0);
	}
    
    /* ---------------------------------------------------------------------- */
	@Override
	public boolean onTouchEvent(MotionEvent event){
		int x = (int)event.getX();
		int y = (int)event.getY();
		int action = (int)event.getAction();
		long nowTime = System.currentTimeMillis();
		
		if(action == MotionEvent.ACTION_UP){
			if(nowTime - lastTime < TIME_BETWEEN_DOUBLE_TOUCH){
				if(scanArea.left < x && scanArea.right > x
						&& scanArea.top < y && scanArea.bottom > y){
					scan();
				} else if(discoverableArea.left < x && discoverableArea.right > x
						&& discoverableArea.top < y && discoverableArea.bottom > y){
					discoverable();
				}
			}
			
			lastTime = nowTime;
		}		

		return true;
	}
}