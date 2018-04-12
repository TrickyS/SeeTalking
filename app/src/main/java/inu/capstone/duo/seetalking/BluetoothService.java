/*
연결 순서 & 구동 방식

MainActivity onStart (초기화) -> btn_connect getDevice,enableBluetooth 실행 -> MainActivity onActivityResult SREQUEST_ENABLE_BT: -> scanDevice()실행 ->
DeviceListActivity 실행 (Activity 전환) -> ainActivity onActivityResult REQUEST_CONNECT_DEVICE: -> getDeviceInfo 실행 ->
connect -> connectThread -> connected -> connectedThread -> (소켓정보) MainActivity trasnferhandler (수신) -> DATA_Transfer_Service Service전달

*/
package inu.capstone.duo.seetalking;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


public class BluetoothService {
    // Debugging
    private static final String TAG="BluetoothService";

    //Intent request code
    private static final int REQUEST_CONNECT_DEVICE=1;
    private static final int REQUEST_ENABLE_BT=2;

    //transferHandler request code
    private static final int IN_SOCKET=3;
    private static final int OUT_SOCKET=4;
    private static final int TRANSFER_SOCKET=5;

    private BluetoothAdapter btAdapter;

    //Handler & MainActivity 수신객체
    private Activity mActivity;
    private Handler BtHandler;
    private Handler TransferHandler;

    //RFCOMM Protocol
    private static final UUID MY_UUID=UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    //상태를 나타내는 상태변수
    private int mState;
    private static final int STATE_NONE=0;
    private static final int STATE_LISTEN=1;
    private static final int STATE_CONNECTING=2;
    private static final int STATE_CONNECTED=3;


    // Constructor
    public BluetoothService(Activity ac, Handler h, Handler t){
        mActivity=ac;
        BtHandler =h;
        TransferHandler=t;

        //BluetoothAdepter 얻기
        btAdapter=BluetoothAdapter.getDefaultAdapter();

    }

    // Bluetooth 지원 여부 확인
    public boolean getDeviceState(){
        Log.d(TAG, "Check The Bluetooth support");

        if(btAdapter==null)
        {
            Log.d(TAG, "Bluetooth is not available");
            return false;
        }
        else{
            Log.d(TAG, "Bluetooth is available");
            return true;
        }
    }

    // Bluetooth 활성화 요청
    public void enableBluetooth(){
        Log.i(TAG, "Check the enable Bluetooth");
        if(btAdapter.isEnabled()){
            Log.d(TAG, "Bluetooth Enable now");
            scanDevice();
        }
        else{
            Log.d(TAG,"Bluetooth Enable Request");

            Intent i=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mActivity.startActivityForResult(i, REQUEST_ENABLE_BT);

        }
    }

    // Bluetooth 기기 검색, MainActivity -> DeviceListActivity
    public void scanDevice(){
        Log.d(TAG,"Scan Device");

        Intent severintent = new Intent(mActivity,DeviceListActivity.class);
        mActivity.startActivityForResult(severintent, REQUEST_CONNECT_DEVICE);
    }

    // 기기 정보 수신 , DeviceListActivity -> MainActivity
    public void getDeviceInfo(Intent data)
    {
        //Get the Device MAC Address
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        //Get the bluetooth Device Object
        BluetoothDevice device=btAdapter.getRemoteDevice(address);

        Log.d(TAG,"Get Device Info \n" + "address :" + address);

        connect(device);
    }

    //Bluetooth 상태 set , MainActivitiy BtHandler 관여
    private synchronized void setState(int state){
        Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;
        Message msg= BtHandler.obtainMessage(mState);
        BtHandler.sendMessage(msg);
    }

    //Bluetooth 상태 get
    public synchronized int getState() {
        return mState;
    }


    //Bluetooth 연결 중단, 초기화 Connect, Connected thread . cansel() 모두 포함
    public synchronized void start() {
        Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread == null) {

        } else {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread == null) {

        } else {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_NONE);
    }

    //ConnectThread 초기화 device 모든 연결 제거
    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread == null) {

            } else {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread == null) {

        } else {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);

        mConnectThread.start();


        setState(STATE_CONNECTING);
        Message msg= BtHandler.obtainMessage(STATE_CONNECTING);
        BtHandler.sendMessage(msg);
    }

    //ConnectedThread 초기화
    public synchronized void connected(BluetoothSocket socket,
                                       BluetoothDevice device) {
        Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (mConnectThread == null) {

        } else {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread == null) {

        } else {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        setState(STATE_CONNECTED);
    }

    //모든 thread stop
    public synchronized void stop() {
        Log.d(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_NONE);
    }

    // 값을 쓰는 부분
    public void write(byte[] out) { // Create temporary object
        ConnectedThread r; // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED)
                return;
            r = mConnectedThread;
        } // Perform the write unsynchronized r.write(out); }
    }

    // 연결 실패했을때
    private void connectionFailed() {
        setState(STATE_LISTEN);

    }

    // 연결을 잃었을때
    private void connectionLost() {
        setState(STATE_LISTEN);
    }


    private class ConnectThread extends Thread{
        private BluetoothSocket mmSocket;
        private BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device){
            mmDevice=device;
            BluetoothSocket tmp=null;
            try{
                tmp=device.createRfcommSocketToServiceRecord(MY_UUID);
            }catch(IOException e){
                Log.e(TAG,"create() failed",e);
            }
            mmSocket=tmp;
        }
        public void run(){
            Log.i(TAG,"BEGiN mConnectThread");
            setName("ConnectThread");

            //연결 시작하기 전에는 항상 기기검색 중지
            //기기 검색 계속되면 연결속도가 지연
            btAdapter.cancelDiscovery();


            // bluetoothSocket 연결시도
            try{
                //BluetoothSocket 연결시도에 대한 return 값은 success 또는 exception이다.
                mmSocket.connect();
                Log.d(TAG,"Connect Success");
            }catch(IOException e){
                connectionFailed();
                Log.d(TAG,"Connect Fail" );
                try{
                    mmSocket.close();
                }catch (IOException e2){
                    Log.e(TAG,"unable to close() socket during connection failure",e2);
                }
                //연결중 혹은 대기상태인 메소드 호출

                BluetoothService.this.start();
                return;
            }

            //ConnectThread reset
            synchronized (BluetoothService.this){
                mConnectThread=null;
            }
            connected(mmSocket,mmDevice);
        }

        public void cancel(){
            try{
                mmSocket.close();
            }catch(IOException e){
                Log.e(TAG, "close of connect socket failed", e);
            }
        }

    }
    private class ConnectedThread extends  Thread{
        private BluetoothSocket mmSocket;
        private InputStream mmInStream;
        private OutputStream mmOutStream;

        private Message Postman;

        public ConnectedThread(BluetoothSocket socket){
            Log.d(TAG,"create ConnectThread");
            mmSocket=socket;
            InputStream tmpIn=null;
            OutputStream tmpOut=null;


            //BluetoothSocket의 inputStream 과 outputStream을 얻는다.
            try{
                tmpIn=socket.getInputStream();
                tmpOut=socket.getOutputStream();
            }catch(IOException e){
                Log.e(TAG,"temp Socket is not created",e);
            }

            mmInStream=tmpIn;
            mmOutStream=tmpOut;

            Postman=Message.obtain(TransferHandler,TRANSFER_SOCKET,mmSocket);
            TransferHandler.sendMessage(Postman);


            //혹시 필요할지 모를까봐 남겨둠
            /*
            Postman=Message.obtain(TransferHandler,IN_SOCKET,mmInStream);
            TransferHandler.sendMessage(Postman);

            Postman=Message.obtain(TransferHandler,OUT_SOCKET,mmOutStream);
            TransferHandler.sendMessage(Postman);
            */
        }

        public void write(byte[] buffer) {
            try {
                // 값을 쓰는 부분(값을 보낸다)
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

