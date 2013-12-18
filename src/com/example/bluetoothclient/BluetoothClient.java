package com.example.bluetoothclient;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

public class BluetoothClient extends Activity implements Runnable{
	
	private static final String TAG = "BlueTooth Client";
	
	private BluetoothAdapter mBluetoothAdapter;
	
	private BluetoothDevice mBluetoothDevice;
	
	private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private final String DEVICE_NAME = "FireFly-E3FC";
	
	// socket
	private BluetoothSocket mBluetoothSocket;
	
	// thread
	private Thread mThread;
	private boolean isRunning;

	/* ---------------------------------------------------------------------------- */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		// Get device name
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		Set< BluetoothDevice > devices = mBluetoothAdapter.getBondedDevices();
		for( BluetoothDevice device : devices){
			if(device.getName().equals(DEVICE_NAME)){
				mBluetoothDevice = device;
				Log.i(TAG, "connected to "+DEVICE_NAME);
			}
		}
		
		// launch thread and connect via bluetooth
		mThread = new Thread(this);
		isRunning = true;
		mThread.start();
		
		// set initial state
		setViewContent(DEVICE_NAME, isRunning);
	}

	/* ---------------------------------------------------------------------------- */
	@Override
	protected void onPause(){
		super.onPause();
		
		isRunning = false;
		try{
			mBluetoothSocket.close();
		} catch(Exception e){
			
		}
	}
	
	/* ---------------------------------------------------------------------------- */
	@Override
	public void run(){
		InputStream mmInStream = null;
		Log.i(TAG, "thread start.");
		
		try{
			// Establish socket connection via Bluetooth using the device name got at the launch
			mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID);
			mBluetoothSocket.connect();
			mmInStream = mBluetoothSocket.getInputStream();
			
			// preserve buffer from input stream
			byte[] buffer = new byte[1024];
			// preserve size of the buffer
			int bytes;
			
			Log.i(TAG, "loop start.");
			while(isRunning){
				// read input stream
				bytes = mmInStream.read(buffer);
				
				// convert to String format
				String readMsg = new String(buffer, 0, bytes);
				
				// display if not null
				if(readMsg.trim() != null && !readMsg.trim().equals("")){
					Log.i(TAG, "value = "+readMsg.trim());
				} else {
					// TODO
				}
			}

		} catch(Exception e){
			Log.e(TAG, "Error in thread caused by " + e.toString());
			try{
				mBluetoothSocket.close();
			} catch(Exception ee){
				isRunning = false;
			}
		}
	}
	
	/* ---------------------------------------------------------------------------- */
	public void setViewContent(String name, boolean state){
		
	}
	
	/* ---------------------------------------------------------------------------- */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.bluetooth_client, menu);
		return true;
	}

}
