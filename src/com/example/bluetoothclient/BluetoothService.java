package com.example.bluetoothclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.Vector;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BluetoothService {
    // Debugging
    private static final String TAG = "BluetoothService";
    private static final boolean D = false;

    // Message types sent from the BluetoothChatService Handler
   public static final int MESSAGE_STATE_CHANGE = 1;
   public static final int MESSAGE_READ = 2;
   public static final int MESSAGE_WRITE = 3;
   public static final int MESSAGE_DEVICE_NAME = 4;
   public static final int MESSAGE_TOAST = 5;

   // Key names received from the BluetoothChatService Handler
   public static final String DEVICE_NAME = "device_name";
   public static final String TOAST = "toast";

   // Intent request codes
   public static final int REQUEST_CONNECT_DEVICE = 1;
   public static final int REQUEST_ENABLE_BT = 2;
   
    // Name for the SDP record when creating server socket
    private static final String NAME = "BluetoothChat";

    // ArduinoとAndroidで通信部分を分け，それぞれに対してsocketを確立して通信
    // 共通部分
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    // mode of communication
    public static final int MATE = 0;
    public static final int APP = 1;
    public static final int SERVER = 2;
    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    
    // Arduinoとの通信部分
	public static final UUID MATE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	public static final String MATE_DEVICE = "FireFly-E3FC";
    private AcceptThread mMateAcceptThread;
    private ConnectThread mMateConnectThread;
    private ConnectedThread mMateConnectedThread;
    private int mMateState;
	
	// Androidとの通信部分
	public static final UUID APP_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    public static final String APP_DEVICE = "Aratouch";
    private AcceptThread mAppAcceptThread;
    private ConnectThread mAppConnectThread;
    private ConnectedThread mAppConnectedThread;
    private int mAppState;



    /* ---------------------------------------------------------------------------- */
    public BluetoothService(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mMateState = STATE_NONE;
        mAppState = STATE_NONE;
        mHandler = handler;
    }

    /* ---------------------------------------------------------------------------- */
    private synchronized void setState(int state, int mode) {
    	switch(mode){
    	case MATE:
            if (D) Log.d(TAG, "setState() " + mMateState + " -> " + state);
            mMateState = state;
    		break;
    	case APP:
            if (D) Log.d(TAG, "setState() " + mAppState + " -> " + state);
            mAppState = state;
    		break;
    	}

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    public synchronized int getState(int mode) {
    	switch(mode){
    	case MATE:
    		return mMateState;
    	case APP:
    		return mAppState;
    	}
    	return STATE_NONE;
    }

    /* ---------------------------------------------------------------------------- */
    public synchronized void start() {
        if (D) Log.d(TAG, "start");
        
        // Cancel any thread attempting to make a connection
        if (mMateConnectThread != null) {
        	mMateConnectThread.cancel();
        	mMateConnectThread = null;
        }
        if (mAppConnectThread != null) {
        	mAppConnectThread.cancel();
        	mAppConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mMateConnectedThread != null) {
        	mMateConnectedThread.cancel();
        	mMateConnectedThread = null;
        }
        if (mAppConnectedThread != null) {
        	mAppConnectedThread.cancel();
        	mAppConnectedThread = null;
        }

        // Start the thread to listen on a BluetoothServerSocket
        if (mMateAcceptThread == null) {
            mMateAcceptThread = new AcceptThread(MATE);
            mMateAcceptThread.start();
        }
        if (mAppAcceptThread == null) {
            mAppAcceptThread = new AcceptThread(APP);
            mAppAcceptThread.start();
        }
        setState(STATE_LISTEN, MATE);
        setState(STATE_LISTEN, APP);
    }

    /* ---------------------------------------------------------------------------- */
    public synchronized void connect(BluetoothDevice device, int mode) {
        if (D) Log.d(TAG, "connect to: " + device);

        switch(mode){
        case MATE:
            // Cancel any thread attempting to make a connection
            if (mMateState == STATE_CONNECTING) {
                if (mMateConnectThread != null) {
                	mMateConnectThread.cancel();
                	mMateConnectThread = null;
                }
            }

            // Cancel any thread currently running a connection
            if (mMateConnectedThread != null) {
            	mMateConnectedThread.cancel();
            	mMateConnectedThread = null;
            }

            // Start the thread to connect with the given device
            mMateConnectThread = new ConnectThread(device, MATE);
            mMateConnectThread.start();
            setState(STATE_CONNECTING, MATE);
        	break;
        	
        case APP:
            // Cancel any thread attempting to make a connection
            if (mAppState == STATE_CONNECTING) {
                if (mAppConnectThread != null) {
                	mAppConnectThread.cancel();
                	mAppConnectThread = null;
                }
            }

            // Cancel any thread currently running a connection
            if (mAppConnectedThread != null) {
            	mAppConnectedThread.cancel();
            	mAppConnectedThread = null;
            }

            // Start the thread to connect with the given device
            mAppConnectThread = new ConnectThread(device, APP);
            mAppConnectThread.start();
            setState(STATE_CONNECTING, APP);
        	break;
        }
    }

    /* ---------------------------------------------------------------------------- */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device, int mode) {
        if (D) Log.d(TAG, "connected");

        switch(mode){
        case MATE:
            // Cancel the thread that completed the connection
            if (mMateConnectThread != null) {
            	mMateConnectThread.cancel();
            	mMateConnectThread = null;
            }

            // Cancel any thread currently running a connection
            if (mMateConnectedThread != null) {
            	mMateConnectedThread.cancel();
            	mMateConnectedThread = null;
            }

            /*
            // Cancel the accept thread because we only want to connect to one device
            if (mMateAcceptThread != null) {
            	mMateAcceptThread.cancel();
            	mMateAcceptThread = null;
            }
            */

            // Start the thread to manage the connection and perform transmissions
            mMateConnectedThread = new ConnectedThread(socket, MATE);
            mMateConnectedThread.start();

            setState(STATE_CONNECTED, MATE);
        	break;
        	
        case APP:
            // Cancel the thread that completed the connection
            if (mAppConnectThread != null) {
            	mAppConnectThread.cancel();
            	mAppConnectThread = null;
            }

            // Cancel any thread currently running a connection
            if (mAppConnectedThread != null) {
            	mAppConnectedThread.cancel();
            	mAppConnectedThread = null;
            }

            /*
            // Cancel the accept thread because we only want to connect to one device
            if (mAppAcceptThread != null) {
            	mAppAcceptThread.cancel();
            	mAppAcceptThread = null;
            }
            */

            // Start the thread to manage the connection and perform transmissions
            mAppConnectedThread = new ConnectedThread(socket, APP);
            mAppConnectedThread.start();

            setState(STATE_CONNECTED, APP);
        	break;
        }

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /* ---------------------------------------------------------------------------- */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");
        // Cancel any thread attempting to make a connection
        if (mMateConnectThread != null) {
        	mMateConnectThread.cancel();
        	mMateConnectThread = null;
        }
        if (mAppConnectThread != null) {
        	mAppConnectThread.cancel();
        	mAppConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mMateConnectedThread != null) {
        	mMateConnectedThread.cancel();
        	mMateConnectedThread = null;
        }
        if (mAppConnectedThread != null) {
        	mAppConnectedThread.cancel();
        	mAppConnectedThread = null;
        }

        // Start the thread to listen on a BluetoothServerSocket
        if (mMateAcceptThread == null) {
            mMateAcceptThread.cancel();
            mMateAcceptThread = null;
        }
        if (mAppAcceptThread == null) {
            mAppAcceptThread.cancel();
            mAppAcceptThread = null;
        }
        setState(STATE_NONE, MATE);
        setState(STATE_NONE, APP);
    }

    /* ---------------------------------------------------------------------------- */
    public void write(byte[] out, int mode) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
        	switch(mode){
        	case MATE:
        		if(mMateState != STATE_CONNECTED) return;
        		r = mMateConnectedThread;
        		break;
        	case APP:
        		if(mAppState != STATE_CONNECTED) return;
        		r = mAppConnectedThread;
        		break;
        	default:
        		return;
        	}
        }
        // Perform the write unsynchronized
        r.write(out);
    }    
    
    /* ---------------------------------------------------------------------------- */
    private void connectionFailed(int mode) {
        setState(STATE_LISTEN, mode);

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    private void connectionLost(int mode) {
        setState(STATE_LISTEN, mode);

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /* ---------------------------------------------------------------------------- */
   /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        // Mode for connecting
        private int mode;

        public AcceptThread(int mode) {
            BluetoothServerSocket tmp = null;
            this.mode = mode;

            // Create a new listening server socket
            try {
            	if(mode == MATE){
            		tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME, MATE_UUID);
            	} else if(mode == APP){
            		tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME, APP_UUID);
            	}
            } catch (IOException e) {
                Log.e(TAG, "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            if (D) Log.d(TAG, "BEGIN mAcceptThread" + this);
            setName("AcceptThread");
            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while ( (mode == MATE && mMateState != STATE_CONNECTED)
            		|| (mode == APP && mAppState != STATE_CONNECTED) ) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothService.this) {
                    	int state;
                    	switch(mode){
                    	case MATE:
                    		state = mMateState;
                    		break;
                    	case APP:
                    		state = mAppState;
                    		break;
                    	default:
                    		return;
                    	}
                    	
                        switch (state) {
                        case STATE_LISTEN:
                        case STATE_CONNECTING:
                            // Situation normal. Start the connected thread.
                            connected(socket, socket.getRemoteDevice(), mode);
                            break;
                        case STATE_NONE:
                        case STATE_CONNECTED:
                            // Either not ready or already connected. Terminate new socket.
                            try {
                                socket.close();
                            } catch (IOException e) {
                                Log.e(TAG, "Could not close unwanted socket", e);
                            }
                            break;
                        }
                    }
                }
            }
            if (D) Log.i(TAG, "END mAcceptThread");
        }

        public void cancel() {
            if (D) Log.d(TAG, "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }
        }
    }


    /* ---------------------------------------------------------------------------- */
    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        // Mode for connecting
        private int mode;

        public ConnectThread(BluetoothDevice device, int mode) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            this.mode = mode;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
            	if(mode == MATE){
                    tmp = device.createRfcommSocketToServiceRecord(MATE_UUID);
            	} else if(mode == APP){
                    tmp = device.createRfcommSocketToServiceRecord(APP_UUID);
            	}
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                connectionFailed(mode);
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                // Start the service over to restart listening mode
                BluetoothService.this.start();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothService.this) {
            	switch(mode){
            	case MATE:
            		mMateConnectThread = null;
            		break;
            	case APP:
            		mAppConnectThread = null;
            		break;
            	default:
            		return;
            	}
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mode);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }


    /* ---------------------------------------------------------------------------- */
    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        // Mode for connecting
        private int mode;

        public ConnectedThread(BluetoothSocket socket, int mode) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            this.mode = mode;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[2048];
            int bytes;

            // Compose 9 bytes of message over the loops
            Vector<Byte> fragment = new Vector<Byte>();
            
            // Keep listening to the InputStream while connected
            while (true) {                
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    switch(mode){
                    case MATE:
	                    // byte buffer to send in handler
	                    byte[] ratedBuffer = new byte[Receiver.MESSAGE_SIZE];
                    
	                    // check if rated buffer is already composed
	                    boolean isBufferComposed = false;
	                    
	                    // add elements to fragment
	                    for(int i = 0; i < bytes; i++){
	                    	fragment.add(buffer[i]);
	                    }
	                    
	                    // search tag 0x1(start) and 0x2(end) descend
	                    byte lastTag = 0x1;
	                    int lastIndex = fragment.size();
	
	                    for(int i = fragment.size()-1; i >= 0; i--){
	                    	Byte b = (Byte)fragment.elementAt(i);
	                    	
	                    	switch(b){
	                    	case 0x1:
	                    		// if 0x1 comes after 0x2, check length
	                    		if(lastTag == 0x2){
	                    			if(lastIndex - i == Receiver.MESSAGE_SIZE){
	                                	for(int j = i; j < lastIndex; j++){
	                                		ratedBuffer[j-i] = fragment.elementAt(j);
	                                	}
	                                	isBufferComposed = true;
	                    			}
	                    		}
	                    		lastTag = 0x1;
	                    		lastIndex = i;
	                    		break;
	                    	case 0x2:
	                    		lastTag = 0x2;
	                    		lastIndex = i;
	                    		break;
	                    	default:
	                    		break;
	                    	}
	                    	
	                    	if(isBufferComposed){
	                    		break;
	                    	}
	                    }
	                    // Send mode with message.obj
	                    // Use the arg2 as the 
	                    // Send the obtained bytes to the UI Activity
	                    if(isBufferComposed){
		                    mHandler.obtainMessage(MESSAGE_READ, Receiver.MESSAGE_SIZE, mode, ratedBuffer)
		                    			.sendToTarget();
	                    }
	                    break;
	                    
                    case APP:
	                    mHandler.obtainMessage(MESSAGE_READ, Receiver.MESSAGE_SIZE, mode, buffer)
            			.sendToTarget();
                    	break;
	                }
                    
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost(mode);
                    break;
                }
            }
        }
        
        /* -------------------------------------------------------------- */
        /**
         * This code does not work but preserved for sometime to use...
         */
        @Deprecated
        private void readLoopVer2(){
            byte[] buffer = new byte[2048];
            int bytes;

            // Compose 9 bytes of message over the loops
            Vector<Byte> fragment = new Vector<Byte>();
            
            // Keep listening to the InputStream while connected
            while (true) {                
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // byte buffer to send in handler
                    byte[] ratedBuffer = new byte[Receiver.MESSAGE_SIZE];
                    // check if rated buffer is already composed
                    boolean isBufferComposed = false;
                    
                    // add elements to fragment
                    for(int i = 0; i < bytes; i++){
                    	fragment.add(buffer[i]);
                    }
                    
                    // search tag 0x1 from the end of vector
                    int firstIndex = fragment.size();
                    for(int i = fragment.size()-1; i >= 0; i--){
                    	Byte b = (Byte)fragment.elementAt(i);
                    	if(b == 0x1){
                    		firstIndex = i;
                    		break;
                    	}
                    }
                    
                    // first search of array
                    if(firstIndex == fragment.size() - Receiver.MESSAGE_SIZE){
                    	for(int i = firstIndex; i < fragment.size(); i++){
                    		ratedBuffer[i-firstIndex] = fragment.elementAt(i);
                    	}
                    	// clear vector
                    	fragment.clear();
                    	isBufferComposed = true;
                    }
                    
                	// second search of tag 0x1
                    int secondIndex = fragment.size() - firstIndex;
                    if(!isBufferComposed){
                    	for(int i = fragment.size() - firstIndex - 1; i >= 0; i--){
                        	Byte b = (Byte)fragment.elementAt(i);
                        	if(b == 0x1){
                    			secondIndex = i;
                    			break;
                    		}
                    	}
                    }
                    
                    // second search of array
                    if(firstIndex - secondIndex == Receiver.MESSAGE_SIZE){
                    	for(int i = secondIndex; i < firstIndex; i++){
                    		ratedBuffer[i-secondIndex] = fragment.elementAt(i);
                    	}
                    	// clear vector
                    	fragment.clear();
                    	isBufferComposed = true;
                    }

                    // Send mode with message.obj
                    // Use the arg2 as the 
                    // Send the obtained bytes to the UI Activity
                    if(isBufferComposed){
	                    mHandler.obtainMessage(MESSAGE_READ, Receiver.MESSAGE_SIZE, mode, ratedBuffer)
	                    			.sendToTarget();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost(mode);
                    break;
                }
            }
        }
        
        /* -------------------------------------------------------------- */
        /**
         * This code does not work but preserved for sometime to use...
         */
        @Deprecated
        private void readLoop(){ 
            byte[] buffer = new byte[2048];
            int bytes;

            // Compose 9 bytes of message over the loops
            int composed = 0;
            byte[] fragment = new byte[Receiver.MESSAGE_SIZE];
            
            // Keep listening to the InputStream while connected
            while (true) {
                boolean isBufferComposed = false;
                
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // byte buffer to send handler
                    byte[] ratedBuffer = new byte[Receiver.MESSAGE_SIZE];
                    
                    String readMsg = new String(buffer, 0, bytes);
                    
                    int index = -1;
                    for(int i = 0; i < bytes; i++){
                    	if(buffer[i] == 0x1){
                    		index = i;
                    		break;
                    	}
                    }
                    
                    if(index == 0){
                    	// reset fragment
                    	fragment = new byte[Receiver.MESSAGE_SIZE];
                    	composed = 0;
                    	
                    	if(bytes >= Receiver.MESSAGE_SIZE){
                    		for(int i = 0; i < Receiver.MESSAGE_SIZE; i++){
                    			ratedBuffer[i] = buffer[i];
                    		}
                    		// search the 0x1 from the rest
                    		int lastIndex = -1;
                    		for(int i = bytes-1; i >= Receiver.MESSAGE_SIZE; i--){
                            	if(buffer[index] == 0x1){
                            		lastIndex = i;
                            		break;
                            	}
                    		}
                    		// set the rest buffer to fragment if lastIndex exists in last 8 of the rest buffer
                    		if(lastIndex > bytes - Receiver.MESSAGE_SIZE){
	                    		int ret = bytes - lastIndex;
	                    		for(int i = 0; i < ret; i++){
	                    			fragment[i] = buffer[bytes - ret + i];
	                    		}
	                    		composed = ret;
                    		}
                    		
                    		isBufferComposed = true;
                    	} else {
                    		// set the rest buffer to fragment
                    		for(int i = 0; i < bytes; i++){
                    			fragment[i] = buffer[i];
                    		}
                    		composed = bytes;
                    		isBufferComposed = false;
                    	}
                    } else {
                    	// Is rated buffer composed from fragment
                    	
                    	if(composed > 0){
                    		if(index > 0){
                    			if(composed+index >= Receiver.MESSAGE_SIZE){
                    				for(int i = 0; i < composed; i++){
                    					ratedBuffer[i] = fragment[i];
                    				}
                    				for(int i = composed; i < Receiver.MESSAGE_SIZE; i--){
                    					ratedBuffer[i] = buffer[i-composed];
                    				}
                    				isBufferComposed = true;
                    			}
                				
                            	// reset fragment
                            	fragment = new byte[Receiver.MESSAGE_SIZE];
                            	composed = 0;
                    		} else {
                    			if(composed+bytes >= Receiver.MESSAGE_SIZE){
                    				for(int i = 0; i < composed; i++){
                    					ratedBuffer[i] = fragment[i];
                    				}
                    				for(int i = composed; i < Receiver.MESSAGE_SIZE; i++){
                    					ratedBuffer[i] = buffer[i-composed];
                    				}
                    				
                                	// reset fragment
                                	fragment = new byte[Receiver.MESSAGE_SIZE];
                                	composed = 0;
                    			} else {
                    				for(int i = composed; i < composed+bytes; i++){
                    					fragment[i] = buffer[i-composed];
                    				}
                    				composed = composed+bytes;
                    			}
                    			
                            	// go to next loop
                            	continue;
                    		}
                    	}
                    	
                    	// if index exists in the message, compose fragment
                    	if(index > 0){
                    		int ret = bytes - index;
                        	
                        	if(ret >= Receiver.MESSAGE_SIZE){
                        		// only if buffer is not composed with the fragment, make rated buffer
                        		if(!isBufferComposed){
	                        		for(int i = 0; i < Receiver.MESSAGE_SIZE; i++){
	                        			ratedBuffer[i] = buffer[index+i];
	                        		}
	                        		isBufferComposed = true;
                        		}
                        		// search the 0x1 from the rest
                        		int lastIndex = -1;
                        		for(int i = bytes-1; i >= Receiver.MESSAGE_SIZE; i++){
                                	if(buffer[index] == 0x1){
                                		lastIndex = i;
                                		break;
                                	}
                        		}
                        		// set the rest buffer to fragment if lastIndex exists in last 8 of the rest buffer
                        		if(lastIndex > bytes - Receiver.MESSAGE_SIZE){
    	                    		int retret = bytes - lastIndex;
    	                    		for(int i = 0; i < retret; i++){
    	                    			fragment[i] = buffer[bytes - retret + i];
    	                    		}
    	                    		composed = retret;
                        		}
                        	} else {
                        		// set the rest buffer to fragment
                        		for(int i = 0; i < ret; i++){
                        			fragment[i] = buffer[index+i];
                        		}
                        		composed = ret;
                        	}
                    	}
                    }
                    
                    if(D) Log.i(TAG, "composed fragment = "+composed);

                    // Send mode with message.obj
                    // Use the arg2 as the 
                    // Send the obtained bytes to the UI Activity
                    if(isBufferComposed){
	                    mHandler.obtainMessage(MESSAGE_READ, Receiver.MESSAGE_SIZE, mode, ratedBuffer)
	    	                            .sendToTarget();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost(mode);
                    break;
                }
            }
        }
        /* -------------------------------------------------------------- */

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}