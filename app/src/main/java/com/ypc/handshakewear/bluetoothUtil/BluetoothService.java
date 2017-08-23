package com.ypc.handshakewear.bluetoothUtil;

/**
 * @author John Watkinson
 */

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import com.ypc.handshakewear.BluetoothBaseActivity;
import com.ypc.handshakewear.ReconcilationData;
import com.ypc.handshakewear.sensor.SensorData;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BluetoothService {
    // Debugging
    private static final String TAG = "BluetoothService";
    private static final boolean D = true;

    // Name for the SDP record when creating server socket
    private static final String NAME_SECURE = "BluetoothSensorData";

    // Unique UUID for this application
    private static final UUID MY_UUID_SECURE = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mSecureAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    /**
     * Constructor. Prepares a new BluetoothSync session.
     * @param handler A Handler to send messages back to the UI Activity
     */
    public BluetoothService( Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();

        mState = STATE_NONE;
        mHandler = handler;
    }

    /**
     * Set the current state of the chat connection
     *
     * @param state An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) {
            Log.d(TAG, "setState() " + mState + " -> " + state);
        }
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(BluetoothBaseActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        if (D) {
            Log.d(TAG, "start");
        }

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_LISTEN);

    }

    public synchronized void startServer() {
        // Start the thread to listen on a BluetoothServerSocket
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread(true);
            mSecureAcceptThread.start();
        }
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param address The BluetoothDevice mac address to connect
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    public synchronized void connect(String address, boolean secure) {
        if (D) {
            Log.d(TAG, "connect to: " + address);
        }

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(mAdapter.getRemoteDevice(address), secure);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device, final String socketType, boolean server) {
        if (D) {
            Log.d(TAG, "connected, Socket Type:" + socketType);
        }

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one device
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        /*Message msg = mHandler.obtainMessage(MasterActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(MasterActivity.DEVICE_NAME, device.getName());
        bundle.putBoolean(MasterActivity.IS_SERVER, server);
        msg.setData(bundle);
        mHandler.sendMessage(msg);*/

        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) {
            Log.d(TAG, "stop");
        }

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     */
    public void write(int code, SensorData sensorData) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) {
                return;
            }
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(code, sensorData);
    }
    public void write(int code,List<Integer> list){
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) {
                return;
            }
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(code,list);
    }
    public void write(int code,ReconcilationData data){
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) {
                return;
            }
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(code,data);
    }

    public void write(int code,boolean isEven){
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) {
                return;
            }
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(code,isEven);
    }

    public void write(int code,int length){
        ConnectedThread r;
        synchronized (this){
            if (mState != STATE_CONNECTED) {
                return;
            }
            r = mConnectedThread;
        }
        r.write(code,length);
    }

    public void write(int code){
        ConnectedThread r;
        synchronized (this){
            if (mState != STATE_CONNECTED) {
                return;
            }
            r = mConnectedThread;
        }
        r.write(code);
    }


    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        /*Message msg = mHandler.obtainMessage(BluetoothBaseActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MasterActivity.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);*/

        // Start the service over to restart listening mode
        BluetoothService.this.start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
        /*Message msg = mHandler.obtainMessage(MasterActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MasterActivity.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);*/

        // Start the service over to restart listening mode
        BluetoothService.this.start();
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread(boolean secure) {
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            try {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, MY_UUID_SECURE);
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            if (D) {
                Log.d(TAG, "Socket Type: " + mSocketType +
                        "BEGIN mAcceptThread" + this);
            }
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket = null;

            // Listen to the server socket if we&#39;re not connected
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice(), mSocketType, true);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
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
            if (D) {
                Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);
            }

        }

        public void cancel() {
            if (D) {
                Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
            }
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
//                Method m = device.getClass().getMethod(&quot;createRfcommSocket&quot;, new Class[] { int.class });
//                tmp = (BluetoothSocket) m.invoke(device, 1);
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Socket Type: " + mSocketType + " create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we&#39;re done
            synchronized (BluetoothService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType, false);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final DataInputStream mmInStream;
        private final DataOutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            Log.d(TAG, "create ConnectedThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = new DataInputStream(tmpIn);
            mmOutStream = new DataOutputStream(tmpOut);
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    int code = mmInStream.readInt();
                    System.out.println(String.format("code:%d",code));
                    //Toast.makeText(mContext,"index rec",Toast.LENGTH_SHORT).show();
                    if (code == BluetoothBaseActivity.CODE_POSTS) {
                        // todo - process received posts one at a time, otherwise too much memory used
                        SensorData sensorData = new SensorData();
                        sensorData.read(mmInStream);
                        // Send the obtained bytes to the UI Activity
                        mHandler.obtainMessage(BluetoothBaseActivity.MESSAGE_READ, code, -1, sensorData).sendToTarget();
                    }else if(code==BluetoothBaseActivity.CODE_END||code==BluetoothBaseActivity.CODE_FINISH){
                        mHandler.obtainMessage(BluetoothBaseActivity.MESSAGE_READ,code,-1,null).sendToTarget();
                    }else if(code==BluetoothBaseActivity.CODE_START){
                        int length=mmInStream.readInt();
                        mHandler.obtainMessage(BluetoothBaseActivity.MESSAGE_READ,code,-1,length).sendToTarget();
                    }
                    else if(code==BluetoothBaseActivity.CODE_MASTER_INDEX){
                        System.out.println(String.format("commit code:%d",code));
                        int n=mmInStream.readInt();
                        List<Integer> list=new LinkedList<>();
                        for(int i=0;i<n;i++){
                            list.add(mmInStream.readInt());
                        }
                        mHandler.obtainMessage(BluetoothBaseActivity.MESSAGE_READ,code,-1,list).sendToTarget();
                    }
                    else if(code==BluetoothBaseActivity.CODE_CASCADE){
                        ReconcilationData reconcilationData=new ReconcilationData(0,0,0,0);
                        reconcilationData.read(mmInStream);
                        mHandler.obtainMessage(BluetoothBaseActivity.MESSAGE_READ,code,-1,reconcilationData).sendToTarget();
                    }
                    else if(code==BluetoothBaseActivity.CODE_ISEVEN){
                        boolean isEven=mmInStream.readBoolean();
                        mHandler.obtainMessage(BluetoothBaseActivity.MESSAGE_READ,code,-1,isEven).sendToTarget();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    //connectionLost();
                    // Start the service over to restart listening mode
                    //BluetoothService.this.start();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         */
        public void write(int code, SensorData sensorData) {
            try {
                mmOutStream.writeInt(code);
                sensorData.write(mmOutStream);
                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(BluetoothBaseActivity.MESSAGE_WRITE, code, -1, sensorData).sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        /**
          * Write start code
        */
        public void write(int code){
            try{
                mmOutStream.writeInt(code);
                mHandler.obtainMessage(BluetoothBaseActivity.MESSAGE_WRITE,code).sendToTarget();

            }catch (IOException e){
                Log.e(TAG, "Exception during write", e);
            }
        }
        public void write(int code,List<Integer> list){
            try {
                mmOutStream.writeInt(code);
                mmOutStream.writeInt(list.size());
                for(int i:list)
                    mmOutStream.writeInt(i);
                mHandler.obtainMessage(BluetoothBaseActivity.MESSAGE_WRITE,code,-1,list).sendToTarget();
            }
            catch (IOException e){
                Log.e(TAG, "Exception during write", e);
            }
        }
        public void write(int code,ReconcilationData data){
            try {
                mmOutStream.writeInt(code);
                data.write(mmOutStream);
                mHandler.obtainMessage(BluetoothBaseActivity.MESSAGE_WRITE,code,-1,data).sendToTarget();
            }
            catch (IOException e){
                Log.e(TAG, "Exception during write", e);
            }
        }
        public void write(int code,boolean isEven){
            try {
                mmOutStream.writeInt(code);
                mmOutStream.writeBoolean(isEven);
                mHandler.obtainMessage(BluetoothBaseActivity.MESSAGE_WRITE,code).sendToTarget();
            }
            catch (IOException e){
                Log.e(TAG, "Exception during write", e);
            }
        }
        public void write(int code,int length){
            try {
                mmOutStream.writeInt(code);
                mmOutStream.writeInt(length);
                mHandler.obtainMessage(BluetoothBaseActivity.MESSAGE_WRITE,code).sendToTarget();
            }
            catch (IOException e){
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

