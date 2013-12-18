package com.example.bluetoothclient;

import com.example.converter.ByteConverter;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

public class BluetoothActivity extends Activity
{
    // Debugging
    private static final String TAG = "Bluetooth Activity";
    private static final boolean D = false;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
    //private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothService mBluetoothService = null;
	
    /* ---------------------------------------------------------------------------------------- */
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");
		
		// 画面の向きを横で固定
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		// 全画面表示
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN); 
		// スリープ無効
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); 
		// タイトルの非表示
		requestWindowFeature(Window.FEATURE_NO_TITLE); 
		
		// viewの初期化と追加
		if(D) setContentView(R.layout.main);
		
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
	}
	
    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, BluetoothService.REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
            if (mBluetoothService == null) setupConnection();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mBluetoothService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mBluetoothService.getState(BluetoothService.MATE) == BluetoothService.STATE_NONE
            		 && mBluetoothService.getState(BluetoothService.APP) == BluetoothService.STATE_NONE) {
              // Start the Bluetooth chat services
              mBluetoothService.start();
            }
        }
    }
    
    /* ---------------------------------------------------------------------------------------- */
    private void setupConnection(){
    	Log.d(TAG, "setupConnection()");
    	
        // Initialize the BluetoothChatService to perform bluetooth connections
        mBluetoothService = new BluetoothService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    /* ---------------------------------------------------------------------------------------- */
    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mBluetoothService != null) mBluetoothService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }

    /* ---------------------------------------------------------------------------------------- */
    protected void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }    

    /* ---------------------------------------------------------------------------------------- */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.scan:
            // Launch the DeviceListActivity to see devices and do scan
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, BluetoothService.REQUEST_CONNECT_DEVICE);
            return true;
        case R.id.discoverable:
            // Ensure this device is discoverable by others
            ensureDiscoverable();
            return true;
        }
        return false;
    }
    
    /* ---------------------------------------------------------------------------------------- */
    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    protected void sendMessage(int length, int[] value, int mode) {
        // Check that we're actually connected before trying anything
        if (mBluetoothService.getState(mode) != BluetoothService.STATE_CONNECTED) {
            //Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Get the message bytes and order the BluetoothChatService to write
        int size = Integer.SIZE/Byte.SIZE;
        byte[] send = new byte[size * 4 * length];//byte変換
        ByteConverter.printInteger(send, size*0, length);
        for(int i = 1; i < length; i++){
        	ByteConverter.printInteger(send, size * i, value[i-1]);
        }
        mBluetoothService.write(send, mode);
        
        // Reset out string buffer to zero and clear the edit text field
        mOutStringBuffer.setLength(0);
    }
    
    /* ---------------------------------------------------------------------------------------- */
    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case BluetoothService.MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothService.STATE_CONNECTED:
                    break;
                case BluetoothService.STATE_CONNECTING:
                    break;
                case BluetoothService.STATE_LISTEN:
                case BluetoothService.STATE_NONE:
                    break;
                }
                break;
            case BluetoothService.MESSAGE_WRITE:
            	
                break;
            case BluetoothService.MESSAGE_READ:
                // get mode which send the message
                int mode = msg.arg2;
                if(mode == BluetoothService.MATE){
                    byte[] readBuf = (byte[]) msg.obj;
                	decodeMateMsg(msg.arg1, readBuf);
                } else if(mode == BluetoothService.APP){
                    byte[] readBuf = (byte[]) msg.obj;
                    decodeAppMsg(readBuf);
                }
                break;
            case BluetoothService.MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(BluetoothService.DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case BluetoothService.MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(BluetoothService.TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case BluetoothService.REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                if(device.getName().equals(BluetoothService.MATE_DEVICE)){
                	mBluetoothService.connect(device, BluetoothService.MATE);
                } else {
                	mBluetoothService.connect(device, BluetoothService.APP);
                }
            }
            break;
        case BluetoothService.REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupConnection();
            } else {
                // User did not enable Bluetooth or an error occured
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    
    /* ---------------------------------------------------------------------------------------- */
    // describe detail at override methods
	protected void decodeMateMsg(int bytes, byte[] buf) {
		// decode message from bluetooth mate arduino
	}
	
	protected void decodeMateString(String[] array) {
		// decode String message from bluetooth mate arduino
	}
	
	protected void decodeAppMsg(byte[] buf){
		// decode message from rear side android
	}
}