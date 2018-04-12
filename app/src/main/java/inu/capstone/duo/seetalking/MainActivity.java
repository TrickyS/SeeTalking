package inu.capstone.duo.seetalking;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.lang.ref.WeakReference;
import inu.capstone.duo.seetalking.DATA_Transfer_Service.LootBinder;
import inu.capstone.duo.seetalking.Model.UserModel;

public class MainActivity extends Activity {
    private static final String TAG="Main";

    //Bluetooth
    private BluetoothService btService;
    private int BluetoothState;  // Bluetooth 상태, Intent로 DataViewActivity에 상태전달
    /*
    100 : Not connect
    101 : connecting...
    102 : connected
     */

    //Intent request code
    private static final int REQUEST_CONNECT_DEVICE=1;
    private static final int REQUEST_ENABLE_BT=2;

    //transferHandler request code
    private static final int IN_SOCKET=3;
    private static final int OUT_SOCKET=4;
    private static final int TRANSFER_SOCKET=5;

    //layout
    private Button btn_connect;
    private Button enter_dataview;
    private TextView txt_result;
    private Button btn_setting;
    private Button btn_training;
    private Button btn_speech;
    private Button btn_analyze;
    private Button btn_logout;


    //Bluetooth 값 수신하는 Handler
    private final Handler ABtHandler = new bthandler(this);
    private final Handler ATransferHandler=new trasnferhandler(this);

    //BindService Need following thing
    //*~
    boolean isService=false;                //BindService 실행 여부를 판단 *중요
    private DATA_Transfer_Service ms=null;
    private ServiceConnection mConnection=new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LootBinder mb= (LootBinder) service;
            ms=mb.getService();

            isService = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            ms=null;
            isService=false;
        }
    };
    //~*

    // LoginActivity에서 로그인 유무 확인
    private FirebaseAuth mAuth;
    private FirebaseUser mFirebaseUser;
    private UserModel userModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
       // 퍼미션 체크
        if (hasPermissions()) {
            Toast.makeText(getApplicationContext(), "권한 승인", Toast.LENGTH_SHORT).show();
        } else {
            requestPerms();
        }

        mAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mAuth.getCurrentUser(); // 로그인 확인

        if(mFirebaseUser == null) { // 로그인이 되어있지 않으면 로그인 화면으로
           startActivity(new Intent(this, LoginActivity.class));
           finish();
        } else { // 로그인 되어 있으면 프로필 저장하기 이걸 좀 더 최적화하고 싶은데...
            userModel = new UserModel();
            userModel.userName = mFirebaseUser.getDisplayName();
            userModel.profileImageUrl = mFirebaseUser.getPhotoUrl().toString();
            userModel.uid = mFirebaseUser.getUid();

            FirebaseDatabase.getInstance().getReference().child("users").child(userModel.uid).setValue(userModel);
        }

        btService=null;
        BluetoothState=100; //기본값 설정

        //Main layout
        btn_connect=(Button)findViewById(R.id.btn_bluetooth);
        txt_result=(TextView)findViewById(R.id.dataview);       //connect 결과
        btn_training = (Button)findViewById(R.id.btn_training);
        btn_speech = (Button)findViewById(R.id.btn_speech);
        btn_analyze = (Button)findViewById(R.id.btn_analyze);
        btn_setting=(Button)findViewById(R.id.btn_setting);
        btn_logout=(Button)findViewById(R.id.btn_logout);

        //Bluetooth Connect Button
        //*~
        btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (txt_result.getText().toString().equals("Not Connect")) {
                    Log.d(TAG, "Button Activate");
                    if (btService.getDeviceState()) {
                        btService.enableBluetooth();
                    } else {
                        finish();
                    }
                } else if (txt_result.getText().toString().equals("Connected")) {
                    btService.stop();
                    ms.Shut_Down_Socket();
                }
            }
        });
        //~*

        btn_training.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), TrainingActivity.class);
                startActivity(intent);
            }
        });

        btn_speech.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), NavigationActivity.class);
                startActivity(intent);
            }
        });

        btn_analyze.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(intent);
            }
        });

        btn_setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(intent);
            }
        });

        btn_logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                FirebaseAuth.getInstance().signOut(); // 로그아웃
                mFirebaseUser = FirebaseAuth.getInstance().getCurrentUser(); // 로그인 확인
                if(mFirebaseUser == null) { // 로그인이 되어있지 않으면 로그인 화면으로
                    startActivity(intent);
                    finish();
                }
            }
        });

    }


    @Override
    protected void onStart() {
        super.onStart();

        //Bluetooth Activate check and ready
        if(btService==null){
            btService=new BluetoothService(this, ABtHandler,ATransferHandler);
        }

        //bindService Start,-> Service Start : Service 초기 시작
        if(!isService) {
            Log.d(TAG,"MainActivity go to bind");
            Intent Bind_Postman = new Intent(MainActivity.this, DATA_Transfer_Service.class);
            bindService(Bind_Postman, mConnection, Context.BIND_AUTO_CREATE);

            Log.d(TAG, "Service is Open");
        }

    }

    //Bluetooth 상태 체크, REQUEST_ENABLE_BT -> Bluetooth 존재 여부 확인 및 스캔 지시, REQUEST_CONNECT_DEVICE -> (DeviceListActivity) 종료후 여기로 반환
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Log.d(TAG, "onActivityResult " + resultCode);
        Log.d(TAG, "onActivityrequestCode " + requestCode);

        switch (requestCode){
            case REQUEST_ENABLE_BT:
                if(resultCode== RESULT_OK){
                    btService.scanDevice();
                }
                else{
                    Log.d(TAG,"Bluetooth is not enable");
                }
                break;
            case REQUEST_CONNECT_DEVICE:
                if(resultCode== RESULT_OK){
                    btService.getDeviceInfo(data);
                }
                break;
        }
    }

    //Handler  : BluetoothService 관여, Textview txt_result & Button btn_connect 의 Text값을 변경함
    //*~
    /*
        bthandler의 Text값 변경은 신중해야 한다.
        onStart의 btn_connect 버튼구동은 기존 Text값에 의존하기 때문에, 추후 .xml에서 text값을 변경하려 한다면
        .xml, btn_connect SetonClickListener, bthandlerMessage 3개 모두를 변경해야한다.
     */
    private static class bthandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public bthandler(MainActivity activity) {
            mActivity = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if(activity != null) {
                activity.bthandlerMessage(msg);
            }
        }
    }
    private void bthandlerMessage(Message msg) {

        switch(msg.what)
        {
            case 0: //STATE_NONE
                txt_result.setText("Not Connect");
                btn_connect.setText("Connect");
                BluetoothState=100;
                break;
            case 1: //STATE_LISTEN
                txt_result.setText("State Listen");
                BluetoothState=100;
                break;
            case 2: //Connecting..
                txt_result.setText("Connecting..");
                BluetoothState=101;
                break;
            case 3: //Connected
                txt_result.setText("Connected");
                btn_connect.setText("DisConnect");
                BluetoothState=102;
                break;
        }
    }
    //~*


    //Handler : BluetoothService에 관여, ConnectedThread의 마지막 Socket을 MainActivity로 가져옴.
    //*~
    private static class trasnferhandler extends Handler{
        private final WeakReference<MainActivity> mActivity;

        public trasnferhandler(MainActivity activity) {
            mActivity = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if(activity != null) {
                activity.transferhandlerMessage(msg);
            }
        }
    }
    private void transferhandlerMessage(Message msg) {
        //String Tmsg=msg.getData().getString("key");
        long T1=System.currentTimeMillis();
        long T2=T1+1000;
        for(;T1<T2; T1++)
        {
            if(isService)
            {
                switch (msg.what)
                {
                    /*
                    case IN_SOCKET:
                        Log.d(TAG,"Socket is launhed : INPUTSTREAM");
                        InputStream H1=(InputStream) msg.obj;
                        //ms.Call_InSocket(H1);

                        break;
                    case OUT_SOCKET:
                        Log.d(TAG,"Socket is launched : OUTPUTSTREAM");
                        OutputStream H2=(OutputStream)msg.obj;
                        //ms.Call_OutSocket(H2);
                        break;
                    */
                    case TRANSFER_SOCKET:
                        Log.d(TAG,"Socket is launched");
                        BluetoothSocket H3=(BluetoothSocket)msg.obj;
                        ms.Call_Socket(H3);
                        break;

                    default:
                        Log.d(TAG,"Socket isn't launched");
                        break;

                }

                break;
            }
            else {
                T2++;
            }
        }
    }
    //~*

    //종료시 unBindService를 담당
    @Override
    protected void onDestroy() {
        super.onDestroy();
        btService.stop();
        if(isService)
        {
            unbindService(mConnection);
            isService=false;
            Log.d(TAG,"Service is closed");
        }
    }


    // 퍼미션 여부 확인
    private boolean hasPermissions(){
        int res;

        String[] permissions = new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

        for(String perms : permissions) {
            res = checkCallingOrSelfPermission(perms);

            if(!(res == PackageManager.PERMISSION_GRANTED))
                return false;
        }
        return true;
    }

    // 퍼미션 요청
    private void requestPerms() {
        String[] permissions = new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, 0);
        }
    }
}
