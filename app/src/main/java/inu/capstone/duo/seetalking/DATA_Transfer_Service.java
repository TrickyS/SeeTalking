package inu.capstone.duo.seetalking;

import android.app.Service;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;

import org.jtransforms.dct.DoubleDCT_1D;
import org.jtransforms.fft.DoubleFFT_1D;
import org.jtransforms.fft.FloatFFT_1D;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class DATA_Transfer_Service extends Service {

    private static final String TAG = "DATA_Transfer";




    //객체반환
    private final IBinder mBinder = new LootBinder();

    //Socket save
    private BluetoothSocket btsavesocket;

    private InputStream BtInStream;
    private OutputStream BtOutStream;

    public DATA_Transfer_Service() {
    }


    //Socket 전달
    //*~
    public void Call_Socket(BluetoothSocket U){
        btsavesocket = U;
    }
    public BluetoothSocket Call_Socket(){
        return btsavesocket;
    }
    public void Shut_Down_Socket(){
        if(btsavesocket!=null) {
            try {
                btsavesocket.close();

            }catch (IOException e2){
                Log.e(TAG, "close of connect socket failed", e2);
            }
        }
        ThreadisOn = false;
        btsavesocket = null;
    }
    //~*


    //Thread Controler
    Background_Calculate_Thread Thread = new Background_Calculate_Thread();
    boolean ThreadAssister = false;
    boolean ThreadisOn = false;
    boolean ServiceisEnd = false;

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();
        btsavesocket=null;
        ServiceisEnd = false;
        Thread.start();

    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(btsavesocket!=null) {
            try {
                btsavesocket.close();

            }catch (IOException e2){
                Log.e(TAG, "close of connect socket failed", e2);
            }
        }
        ThreadAssister = false;
        ThreadisOn = false;
        ServiceisEnd = true;
    }

    //Need for bind
    //*~
    public class LootBinder extends Binder {
        DATA_Transfer_Service getService() {
            return DATA_Transfer_Service.this;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //return mMessenger.getBinder();
        return mBinder;
    }
    //~*




    //여기선 쓰이지 않는다.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    private class Background_Calculate_Thread extends Thread{
        public Background_Calculate_Thread(){
        }

        @Override
        public void run() {
            super.run();
            while(!ServiceisEnd){
                Log.d(TAG,"DataBackground is running");

                //진동준비
                Vibrator vibrator;
                vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

                //Bluetooth 연결을 확인

                while(true){
                    if(btsavesocket!=null){
                        ThreadAssister = true;
                        break;
                    }
                }

                //Stream 생성
                InputStream tmpIn=null;
                OutputStream tmpOut=null;
                while(ThreadAssister) {
                    if (btsavesocket!=null) {
                        ThreadisOn = true;
                        Log.d(TAG, "Socket is Creating...");
                        //BluetoothSocket의 inputStream 과 outputStream을 얻는다.
                        try {
                            tmpIn = btsavesocket.getInputStream();
                            tmpOut = btsavesocket.getOutputStream();
                        } catch (IOException e) {
                            Log.e(TAG, "temp Socket is not created", e);
                        }
                        BtInStream = tmpIn;
                        BtOutStream = tmpOut;
                        break;
                    }
                    else{
                        Log.e(TAG,"Socket Creating is Failed!, Stop the work");
                        ThreadisOn = false;
                        ThreadAssister = false;
                    }
                }


                // Keep listening to the InputStream while connected (데이터 수신)
                byte [] readBuffer = new byte[1024];
                int readBufferPosition = 0;

                //FFT 파라미터 선언
                final long N = 512;                                                                   //Sample 개수 설정
                final double sampling_rate = 8924.826;                                               //Sampling rate 설정
                double[] arr = new double[(2 * (int)N)];
                DoubleFFT_1D FFT = new DoubleFFT_1D(N);

                int arraycount = 0;

                //Mel Frequency & Filter setting
                final double[] mel_frequency = new double[] {0 ,89.90, 179.8, 269.7, 359.6, 449.5, 539.5, 629.4, 719.3, 809.2, 899.1 ,989.0, 1078.9, 1168.8, 1258.7, 1348.7, 1438.6,
                        1528.5, 1618.4, 1708.3, 1798.2, 1888.1, 1978.0, 2067.9, 2157.8, 2247.7};            // mel frequency 설정
                int[] mel_filter = new int[mel_frequency.length];
                for(int i = 0; i<mel_frequency.length; i++){
                    mel_filter[i] = (int)Math.floor((N * (700.0 * (Math.exp(mel_frequency[i]/1125.0) - 1.0))) / sampling_rate);
                    Log.d(TAG,String.valueOf(mel_filter[i]));
                }

                //Mel Filter X FFT
                final int filter_index = mel_frequency.length;
                double[] filtered_data = new double[filter_index];

                //DCT 파라미터 선언
                DoubleDCT_1D DCT = new DoubleDCT_1D(filter_index);



                Log.v(TAG,"Thread state : "  + String.valueOf(ThreadisOn) + " & " + String.valueOf(ThreadAssister));
                while (ThreadisOn) {
                    try {
                        int bytesAvailable = BtInStream.available();
                        if (bytesAvailable > 0) {
                            byte[] packetBytes = new byte[bytesAvailable];
                            // Read from the InputStream
                            BtInStream.read(packetBytes);

                            for (int i = 0; i < bytesAvailable; i++) {

                                byte b = packetBytes[i];
                                if (b == '\n') {

                                    byte[] encodedBytes = new byte[readBufferPosition];

                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    String recvMessage = new String(encodedBytes, "UTF-8");



                                    if (recvMessage.length() >= 2 && Integer.parseInt(recvMessage) < 1023) {
                                        int recvdata = Integer.parseInt(recvMessage);
                                        arr[2 * arraycount] = ((double) recvdata * 3.35 / 1024.00) - 1.3000;
                                        arraycount++;
                                        if (arraycount > N - 1) {
                                            arraycount = 0;
                                            FFT.complexForward(arr);
                                            for (int Q = 0; Q < (2 * (int) N) - 1; Q += 2) {
                                                arr[Q / 2] = 2.00 * ((Math.sqrt(Math.pow(arr[Q], 2) + Math.pow(arr[Q + 1], 2))) / ((double) N));
                                            }
                                                /*
                                                //Log.d(TAG,recvMessage);  //테스트용
                                                Log.d(TAG,"Start");
                                                for (int Q2 = 0; Q2< 257; Q2++){
                                                    //Log.d(TAG,String.valueOf(arr[Q2])); //테스트용
                                                    Log.d(TAG,String.format("%.4f",(sampling_rate/512.00)*Q2) +"Hz : " + String.format("%.12f",arr[Q2]));
                                                }

                                                if(arr[93]>0.0006 && arr[47]>0.0005 && arr[138] > 0.0003 && arr[139] > 0.0034 && arr[184] > 0.00015 && arr[238] > 0.000155 && arr[21] < 0.00005){
                                                    vibrator.vibrate(1000);
                                                }
                                                Log.d(TAG,"End");
                                                */


                                            for (int Q2 = 0; Q2 < filter_index; Q2++) {
                                                filtered_data[Q2] = Math.log10(arr[mel_filter[Q2]]);
                                            }
                                            DCT.forward(filtered_data,true);

                                            for (int Q2 = 0; Q2< filter_index; Q2++) {
                                                Log.d(TAG,String.valueOf(Q2) + " : " + String.valueOf(filtered_data[Q2]));
                                            }



                                            for (int Q3 = 0; Q3 < 1024; Q3++) {
                                                arr[Q3] = 0;
                                            }
                                        }
                                    }


                                    readBufferPosition = 0;
                                } else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "disconnected", e);
                    }
                }
            }
        }
    }



}