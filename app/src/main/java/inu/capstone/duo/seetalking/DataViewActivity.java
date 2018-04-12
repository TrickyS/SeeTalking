package inu.capstone.duo.seetalking;

import android.app.ActivityManager;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DataViewActivity extends AppCompatActivity {

    private static final String TAG="DataViewActivity";

    //BindService Need following thing // *바인드를 시작함에 있어 다른 Activity의 ServiceConnect 변수명은 서로 겹치면 안됨,
    //*~
    boolean isService=false;
    private DATA_Transfer_Service Warden=null; // *위 내용에 있어 수신자도 동일
    private ServiceConnection DataViewmConnection=new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DATA_Transfer_Service.LootBinder keeper= (DATA_Transfer_Service.LootBinder) service;
            Warden=keeper.getService();
            isService = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Warden=null;
            isService=false;
        }
    };
    //~*


    //Layout
    private TextView tempanswer;
    private TextView connection_state;
    private Button setbindbuttton;


    //Socket , IN or OUT Stream
    private InputStream BtInStream;
    private OutputStream BtOutStream;
    private BluetoothSocket BtSocket;
    private String recvMessage;

    //Thread, ThreadisOn은 Thread 작동여부, ThreadAssister BindService 실행이 다될때까지 Bind객체 참조 지연도움 (Thread에있음)
    private DataViewThread DataViewThread=null;
    private boolean ThreadisOn;
    private boolean ThreadAssister;


    private int BluetoothState;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_view);
        Log.d(TAG,"onCreate");

        BluetoothState=100; // 기본값 초기화
        /*
        100 : Not connect
        101 : connecting...
        102 : connected
        */

        tempanswer=(TextView)findViewById(R.id.tempanswer);
        connection_state=(TextView)findViewById(R.id.connection_state);

    }

    /*
    DataVIewActivity가 화면에 보인 상태에서만 Service와 bind한다.
    Acitivity 생명주기는
    처음 생성시 onCreate -> onStart -> onResume -> onStop -> onDestroy
    앱을 중단했을시 (ex. 홈버튼 클릭) onStop -> (다시실행) -> onStart -> onResume
    이므로
    각각 onStart와 onStop 에서 bind, unbind 한다.
     */
    @Override
    protected void onStart() {
        super.onStart();
        Log.v(TAG,"onStart Create");


        //ServiceBind Start , and Running Check
        serviceBind_and_RunningCheck();

        //Thread ready and Start , Bluetooth data를 수신한다.
        //*~
        ThreadAssister=true;
        ThreadisOn=false;
        DataViewThread=new DataViewThread();
        DataViewThread.execute();
        //~*



    }

    //Bind 게시 //isServiceRunning : 서비스의 작동 여부를 확인 , Bluetooth 여부 확인
    //*~
        /*
            Bind는 시작과 동시에 혹은 우리가 생각하는 짧은 그 순간 이루어지지 않는다. //상당히 김
            굉장히 느리고 개발자가 중간에 Bind가 끝난줄 알고 사용하려 했을때, Bind가 이루어지지 않은 경우
            바로 null Object reference error를 볼 수 있다.
            BIndService를 지시하고 끝났는지만 알 수 있으므로 중간에 이루어지고 있는 그 순간을 알 수 없다.
            DataViewThread에서는 ThreadAssister가 이 오류가 나타나지 않게 하는 역할을 한다.
        */
    private void serviceBind_and_RunningCheck() {
        if(!isService&&isServiceRunning()) {
            Log.v(TAG,"Service has Activating");
            Log.d(TAG,"Activate BindService");
            Intent Bind_Postman2 = new Intent(DataViewActivity.this, DATA_Transfer_Service.class);
            bindService(Bind_Postman2, this.DataViewmConnection, BIND_AUTO_CREATE);
            //startService(Bind_Postman2);

            //Bluetooth 작동여부
            Intent P1=getIntent();
            BluetoothState=P1.getExtras().getInt("state");
            switch(BluetoothState){
                case 100:
                case 101:
                    Log.d(TAG,"Device isn't connected to Bluetooth");
                    Toast.makeText(getApplicationContext(),"Device isn't connected to Bluetooth",Toast.LENGTH_LONG).show();
                    connection_state.setText("Bluetooth OFF");
                    break;
                case 102:
                    break;

            }

        }
        else {
            Log.d(TAG,"Service isn't executed");
            Toast.makeText(getApplicationContext(),"Device isn't executed Service",Toast.LENGTH_LONG).show();
            connection_state.setText("Service OFF");
        }
    }
    //~*

    @Override
    protected void onResume() {
        super.onResume();
        //Log.v(TAG,"onResume Create");

    }


    //onStop Bind와 Thread 모두 중지됨.
    @Override
    protected void onStop() {
        super.onStop();
        Log.v(TAG,"onStop Create");
        try{
            BtSocket.close();
        }catch (IOException e){
            Log.e(TAG,"Close Fail");
        }

        ThreadisOn=false;
        ThreadAssister=false;

        if(isService)
        {
            unbindService(DataViewmConnection);
            isService=false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v(TAG,"onDestroy");

    }


    private class DataViewThread extends AsyncTask< Void ,String ,Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {

            InputStream tmpIn=null;
            OutputStream tmpOut=null;

            Log.d(TAG,"DataBackground is running");
            while((!ThreadisOn)&&ThreadAssister) {
                if (isService && (BluetoothState == 102)) {
                    ThreadisOn = true;
                    /*
                    BtInStream=Warden.Call_InSocket();
                    BtOutStream=Warden.Call_OutSocket();
                    */
                    BtSocket = Warden.Call_Socket();
                    Log.d(TAG, "Socket is Creating...");

                    //BluetoothSocket의 inputStream 과 outputStream을 얻는다.
                    try {
                        tmpIn = BtSocket.getInputStream();
                        tmpOut = BtSocket.getOutputStream();
                    } catch (IOException e) {
                        Log.e(TAG, "temp Socket is not created", e);
                    }

                    BtInStream = tmpIn;
                    BtOutStream = tmpOut;

                }
            }

            // Keep listening to the InputStream while connected
            // 데이터 수신

            byte [] readBuffer = new byte[1024];
            int readBufferPosition = 0;
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
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0,
                                            encodedBytes.length);
                                    recvMessage = new String(encodedBytes, "UTF-8");

                                    readBufferPosition = 0;
                                    publishProgress(recvMessage);
                                    //Log.v(TAG,recvMessage);
                                    //Log.d(TAG, "recv message: " + recvMessage);

                                } else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    } catch (IOException e) {

                        Log.e(TAG, "disconnected", e);
                    }
            }

            return null;

        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            tempanswer.setText(values[0]);
            Log.v(TAG,values[0]);
        }


    }


    //Service 작동 여부 확인 true or false 반환
    public boolean isServiceRunning()
    {
        ActivityManager manager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
        {
            if (DATA_Transfer_Service.class.getName().equals(service.service.getClassName()))
                return true;
        }
        return false;
    }
}
