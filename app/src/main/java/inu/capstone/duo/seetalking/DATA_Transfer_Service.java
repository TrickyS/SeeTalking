package inu.capstone.duo.seetalking;

import android.app.Service;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import org.jtransforms.dct.DoubleDCT_1D;
import org.jtransforms.fft.DoubleFFT_1D;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DATA_Transfer_Service extends Service {

    private static final String TAG = DATA_Transfer_Service.class.getSimpleName();
    private static final int NOTICECODE = 15;

    //객체반환
    private final IBinder mBinder = new LootBinder();

    //Socket save
    private BluetoothSocket btsavesocket;

    private InputStream BtInStream;
    private OutputStream BtOutStream;


    //진동 가능?
    private boolean isvibrate = true;
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
    public void Take_ISVIBRATE(int temp){
        if(temp==1000){
            isvibrate =true;
        }
        else{
            isvibrate = false;
        }
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
        Toast.makeText(getApplicationContext(), "binding", Toast.LENGTH_SHORT).show();

        return mBinder;
    }
    //~*

    // Not used in here.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }
    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();

        isvibrate = true; //default 값
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

    private class Background_Calculate_Thread extends Thread {
        private void write(OutputStream mmOutStream,String message) {
            byte[] buffer = message.getBytes();
            try {
                // 값을 쓰는 부분(값을 보낸다)
                mmOutStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public Background_Calculate_Thread() {
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

                //실험용 임시 배열
                boolean exmp_start =false;  //true는 샘플링 스타트
                double[] exmp_temp = new double[filter_index];
                int circletime = 0;

                //상태값 저장 에너지값을 고려하지 않았을때

                double[] s_quite_state = new double[]{ 2.9346, 1.4987, 1.2000, 0.9435, 0.7229, 0.5368, 0.3963, 0.3305, 0.2680, 0.2216, 0.2137, 0.2189, 0.1879, 0.1836, 0.1988, 0.1849, 0.2012 };
                double[] s_car_state = new double[]{ 6.7852,0.7134,-0.2276,0.7906,-0.1706,0.4140,0.7080,-0.0007101,0.7050,0.5680,0.5975,0.0007053,-0.2666,0.3113,0.2749,0.3961,0.04586};
                double[] s_carloud_state = new double[]{9.5140,0.2683,-0.8183,0.5602,-0.5144,0.5135,0.4225,-0.07903,0.6590,0.5467,0.7622,0.09989,-0.3597,0.5324,0.2012,0.6494,-0.2293};
                double[] s_carloudd_state = new double[]{5.59939,0.022252,0.5335991,0.982376,0.204301,0.784989,0.526629,0.0326027,0.892070,0.38889,0.85406,0.21322,-0.02129,0.5735,0.2717,0.5136,-0.22173};


                double[] s_quite_state2 = new double[]{ 1.0, 0.5123, 0.3678, 0.2717, 0.20924, 0.15937, 0.14172, 0.16602, 0.183816, 0.1836, 0.16658, 0.1377, 0.09721, 0.06923, 0.03451, 0.04075, 0.05608 };
                //double[] s_car_state2 = new double[]{ 6.7852,0.7134,-0.2276,0.7906,-0.1706,0.4140,0.7080,-0.0007101,0.7050,0.5680,0.5975,0.0007053,-0.2666,0.3113,0.2749,0.3961,0.04586};
                double[] s_carloud_state2 = new double[]{1.0,-0.010512,-0.004069,0.017651,0.0178173,0.047750,0.042079,0.009397,0.119789,0.0327970,0.0791854,0.00216166,-0.0075046,0.0382546,0.0375632,0.0666337,-0.0375466};

                double distn_qs = 0;
                double distn_cs = 0;
                double distn_cls = 0;
                double distn_clsd = 0;
                double distn_qs2 = 0;
                double distn_cls2 = 0;


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
                                    int messagetime=0;


                                    //Log.d(TAG, recvMessage);  //테스트용
                                    for(int Q9 = 0; Q9 < 10; Q9++){
                                        if(recvMessage.contains(String.valueOf(Q9))){ messagetime +=1;}
                                    }
                                    //Log.d(TAG,"이건 "+String.valueOf(messagetime));
                                    if(messagetime<=0 || messagetime>3){
                                        break;
                                    }
                                    if(recvMessage.contains("&")||recvMessage.contains("'")||recvMessage.contains("w")||recvMessage.indexOf("�")!=-1 || recvMessage.indexOf("\b")!=-1 || recvMessage.indexOf("#")!=-1 || recvMessage.indexOf("\"")!=-1 || recvMessage.indexOf("?")!=-1 || recvMessage.indexOf("-")!=-1|| recvMessage.indexOf("\u001E")!=-1) {
                                        break;
                                    }

                                    try {
                                        Integer.parseInt(recvMessage);
                                    } catch (NumberFormatException e) {
                                        break;
                                    }

                                    if (recvMessage.length() >= 2 && Integer.parseInt(recvMessage) < 1023) {
                                        int recvdata = Integer.parseInt(recvMessage);
                                        arr[2 * arraycount] = ((double) recvdata * 3.35 / 1024.00) - 1.2500;
                                        arraycount++;
                                        if (arraycount > N - 1) {
                                            arraycount = 0;
                                            FFT.complexForward(arr);
                                            for (int Q = 0; Q < (2 * (int) N) - 1; Q += 2) {
                                                arr[Q / 2] = (2.00 * ((Math.sqrt(Math.pow(arr[Q], 2) + Math.pow(arr[Q + 1], 2))) / ((double) N))) * 10000.00;
                                            }
                                            //테스트출력 1
                                                /*
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
                                            //DCT
                                            DCT.forward(filtered_data, true);

                                            //테스트출력2
                                            /*
                                            for (int Q2 = 0; Q2< filter_index; Q2++) {
                                                Log.d(TAG,String.valueOf(Q2) + " : " + String.valueOf(filtered_data[Q2]));
                                            }
                                            */

                                            //샘플 수집 할지 말지 exmp_start 로 결정
                                            if (exmp_start == true) {
                                                if (circletime < 300) {
                                                    for (int Q3 = 0; Q3 < filter_index; Q3++) {
                                                        exmp_temp[Q3] = exmp_temp[Q3] + filtered_data[Q3];
                                                    }
                                                    circletime++;
                                                    Log.d(TAG, " A : " + String.valueOf(circletime));
                                                } else {
                                                    circletime = 0;
                                                    for (int Q3 = 0; Q3 < filter_index; Q3++) {
                                                        exmp_temp[Q3] = (exmp_temp[Q3] / 300.00);
                                                        Log.d(TAG, String.valueOf(Q3) + " : " + String.valueOf(exmp_temp[Q3]));
                                                    }
                                                    //이부분 에너지값 1로 평준화 시킬떄 쓰는거
                                                    /*
                                                    for (int Q3 = 1; Q3<filter_index; Q3++){
                                                        exmp_temp[Q3] = exmp_temp[Q3] / exmp_temp[0];
                                                        Log.d(TAG, String.valueOf(Q3) + " : " + String.valueOf(exmp_temp[Q3]));
                                                    }
                                                    exmp_temp[0] = exmp_temp[0]/exmp_temp[0];
                                                    Log.d(TAG, String.valueOf(0) + " : " + String.valueOf(exmp_temp[0]));
                                                    */
                                                    for (int Q3 = 0; Q3 < filter_index; Q3++) {
                                                        exmp_temp[Q3] = 0;
                                                    }
                                                }
                                            }

                                            //1번 조용상태 판단
                                            for (int Q3 = 0; Q3 < s_quite_state.length; Q3++) {
                                                distn_qs = distn_qs + Math.pow(filtered_data[Q3] - s_quite_state[Q3], 2);
                                            }
                                            distn_qs = 1.0 / (1 + Math.sqrt(distn_qs));
                                            Log.d(TAG, "QS : " + String.valueOf(distn_qs));

                                            //2번 차소리
                                            for (int Q3 = 0; Q3 < s_car_state.length; Q3++) {
                                                distn_cs = distn_cs + Math.pow(filtered_data[Q3] - s_car_state[Q3], 2);
                                            }
                                            distn_cs = 1.0 / (1 + Math.sqrt(distn_cs));
                                            Log.d(TAG, "CS : " + String.valueOf(distn_cs));

                                            //3번 큰차소리
                                            for (int Q3 = 0; Q3 < s_carloud_state.length; Q3++) {
                                                distn_cls = distn_cls + Math.pow(filtered_data[Q3] - s_carloud_state[Q3], 2);
                                            }
                                            distn_cls = 1.0 / (1 + Math.sqrt(distn_cls));
                                            Log.d(TAG, "CLS : " + String.valueOf(distn_cls));

                                            //4번 큰차소리 거리
                                            for (int Q3 = 0; Q3 < s_carloudd_state.length; Q3++) {
                                                distn_clsd = distn_clsd + Math.pow(filtered_data[Q3] - s_carloudd_state[Q3], 2);
                                            }
                                            distn_clsd = 1.0 / (1 + Math.sqrt(distn_clsd));
                                            Log.d(TAG, "CLSd : " + String.valueOf(distn_clsd));

                                            /*
                                            for(int Q4 = 1; Q4< s_quite_state2.length; Q4++){
                                                filtered_data[Q4] = filtered_data[Q4] / filtered_data[0];
                                            }
                                            filtered_data[0] = filtered_data[0]/filtered_data[0];
                                            //1번 조용상태 판단
                                            for (int Q3 = 0; Q3 < s_quite_state2.length; Q3++) {
                                                distn_qs2 = distn_qs2 + Math.pow(filtered_data[Q3] - s_quite_state2[Q3], 2);
                                            }
                                            distn_qs2 = 1.0 / (1 + Math.sqrt(distn_qs2));
                                            Log.d(TAG, "QS2 : " + String.valueOf(distn_qs));

                                            for (int Q3 = 0; Q3 < s_carloud_state2.length; Q3++) {
                                                distn_cls2 = distn_cls2 + Math.pow(filtered_data[Q3] - s_carloud_state2[Q3], 2);
                                            }
                                            distn_cls2 = 1.0 / (1 + Math.sqrt(distn_cls2));
                                            Log.d(TAG, "CLS2 : " + String.valueOf(distn_cls2));

                                            */


                                            if (distn_qs < 0.3500 && (distn_cs > 0.3550 || distn_cls > 0.3550 || distn_clsd > 0.3950)) {
                                                if(isvibrate) {
                                                    Log.d(TAG,"Vibrate = " + String.valueOf(isvibrate));
                                                    vibrator.vibrate(1000);

                                                    Handler mainHandler = new Handler(getMainLooper());

                                                    mainHandler.post(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            // Do your stuff here related to UI, e.g. show toast
                                                            Toast.makeText(getApplicationContext(), "경적소리!, 주변을 확인하세요", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });


                                                }
                                                String message = "A";
                                                write(BtOutStream, message);
                                            }



                                            for (int Q4 = 0; Q4 < 1024; Q4++) {
                                                arr[Q4] = 0;
                                            }
                                            distn_qs = 0;
                                            distn_cls = 0;
                                            distn_cs = 0;
                                            distn_cls2 = 0;
                                            distn_qs2 = 0;

                                        }
                                    }
                                    readBufferPosition = 0;
                                } else {
                                    readBuffer[readBufferPosition++] = b;
                                    if(readBufferPosition>100){
                                        readBufferPosition = 0;
                                        Log.d(TAG,"readBufferPosition Error");
                                    }


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