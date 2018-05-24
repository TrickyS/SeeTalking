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
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import inu.capstone.duo.seetalking.DATA_Transfer_Service.LootBinder;
import inu.capstone.duo.seetalking.Model.StaticModel;
import inu.capstone.duo.seetalking.Model.UserModel;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

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
    private TextView txt_result;
    private Button btn_setting;
    private Button btn_training;
    private Button btn_speech;
    private Button btn_analyze;
    private Button enter_dataview; // JYD

    // Not Connected 대용 변수
    private boolean isbluetoothon; // JYD
    // 진동 설정
    private static final int ISVIBRATE = 10; // JYD
    private boolean isvibrate; // JYD
    // Setting 관련 진동 설정 & 로그아웃 request code
    private static final int SETTINGA = 9; // JYD //Request code
    private static final int VIBRATE = 10;
    private static final int GOTOOUT=11; // 인증 삭제 이후 나가지는 로그아웃 코드

    //Bluetooth 값 수신하는 Handler
    private final Handler ABtHandler = new bthandler(this);
    private final Handler ATransferHandler=new trasnferhandler(this);

    //BindService Need following thing
    //*~
    private boolean isService=false;                //BindService 실행 여부를 판단 *중요
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

    // Firestore 데이터 긁어오기
    private String mFirebaseUserUID;
    private FirebaseFirestore firebaseFirestore;
    private ArrayList<String> countText;

    // 구글 로그인
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
       // 퍼미션 체크
        if (hasPermissions()) {
            //Toast.makeText(getApplicationContext(), "권한 승인", Toast.LENGTH_SHORT).show();
        } else {
            requestPerms();
        }

        mAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mAuth.getCurrentUser(); // 로그인 확인
        mFirebaseUserUID = mFirebaseUser.getUid(); // FirestoreGet 함수 돌리기 위한 변수

        // 구글 로그인 구성
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        if(GoogleSignIn.getLastSignedInAccount(this) == null || mFirebaseUser == null) { // 로그인이 되어있지 않으면 로그인 화면으로
            startActivity(new Intent(this, LoginActivity.class));
            //finish();
        } else { // 로그인 되어 있으면 프로필 저장하기
            userModel = new UserModel();
            userModel.userName = mFirebaseUser.getDisplayName();
            userModel.profileImageUrl = mFirebaseUser.getPhotoUrl().toString();
            userModel.uid = mFirebaseUser.getUid();
            FirebaseDatabase.getInstance().getReference().child("users").child(userModel.uid).setValue(userModel);
            Toast.makeText(this, "hello! " + userModel.userName, Toast.LENGTH_SHORT).show();
        }

        // bluetooth & vibrate setting
        btService=null;
        BluetoothState=100; //기본값 설정
        isvibrate = true;
        isbluetoothon = false;

        // 버튼 4개
        btn_training = (Button)findViewById(R.id.btn_training);
        btn_speech = (Button)findViewById(R.id.btn_speech);
        btn_analyze = (Button)findViewById(R.id.btn_analyze);
        btn_setting=(Button)findViewById(R.id.btn_setting);


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
                Intent intent = new Intent(getApplicationContext(), AnalyzeActivity.class);
                startActivity(intent);
            }
        });

        btn_setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                Log.d(TAG, "초기 : " + String.valueOf(isvibrate));
                intent.removeExtra("VIBRATE");
                intent.putExtra("VIBRATE", isvibrate);
                startActivityForResult(intent, SETTINGA);
                Log.d(TAG,"Vibrate state : " + String.valueOf(isvibrate));
            }
        });

    }

    // Action Bar 메뉴 버튼
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    // Action Bar 메뉴마다 기능
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.action_bluetoothconn:
                if (!isbluetoothon) {
                    Log.d(TAG, "Button Activate");
                    if (btService.getDeviceState()) {
                        btService.enableBluetooth();

                        //bindService Start,-> Service Start : Service 초기 시작
                        if (!isService) {
                            Log.d(TAG, "Service is Opening");
                            Intent Bind_Postman = new Intent(MainActivity.this, DATA_Transfer_Service.class);
                            bindService(Bind_Postman, mConnection, Context.BIND_AUTO_CREATE);
                        }
                    } else {
                        finish();
                    }
                } else if (isbluetoothon) {
                    btService.stop();
                    ms.Shut_Down_Socket();
                }
                return true;
            case R.id.action_logout:
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                FirebaseAuth.getInstance().signOut(); // Firebase 로그아웃
                if(GoogleSignIn.getLastSignedInAccount(this) != null) {
                    mGoogleSignInClient.signOut(); // google signout
                }
                startActivity(intent);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    protected void onStart() {
        super.onStart();

        // 검색을 합시다
        FirestoreGet(mFirebaseUserUID);

        if(GoogleSignIn.getLastSignedInAccount(this) == null){
            Log.i("onStart", "구글 로그인 OFF");
            startActivity(new Intent(this, LoginActivity.class));
        } else{
            Log.i("onStart", "구글 로그인 ON");
        }

        //Bluetooth Activate check and ready
        if(btService==null){
            btService=new BluetoothService(this, ABtHandler,ATransferHandler);
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
                } else{
                    Log.d(TAG,"Bluetooth is not enable");
                }
                break;
            case REQUEST_CONNECT_DEVICE:
                if(resultCode== RESULT_OK){
                    btService.getDeviceInfo(data);
                }
                break;
            case SETTINGA:
                switch (resultCode) {
                    case ISVIBRATE:
                        if (data != null) {
                            isvibrate = data.getBooleanExtra("VIBRATE", true);
                            //서비스로 진동상태 보냄
                            if (isService) {
                                if (isvibrate) {
                                    ms.Take_ISVIBRATE(1000);
                                } else {
                                    ms.Take_ISVIBRATE(2000);
                                }
                            }
                        } else {
                            Log.d(TAG, "data == null");
                        }
                        break;
                    case GOTOOUT:
                        //블루투스 & 서비스 끔
                        if (isbluetoothon) {
                            btService.stop();
                            ms.Shut_Down_Socket();
                        }

                        // 로그아웃
                        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                        FirebaseAuth.getInstance().signOut(); // Firebase 로그아웃
                        if (GoogleSignIn.getLastSignedInAccount(this) != null) {
                            mGoogleSignInClient.signOut(); // google signout
                        }
                        startActivity(intent);
                        finish();
                        break;
                    default:
                        Log.d(TAG, "Setting OK not Change");
                        break;
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
                isbluetoothon=false;
                BluetoothState=100;
                break;
            case 1: //STATE_LISTEN
                BluetoothState=100;
                break;
            case 2: //Connecting..
                BluetoothState=101;
                break;
            case 3: //Connected
                isbluetoothon=true;
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
        //Log.d(TAG,"transferhandler : " + String.valueOf(isService) + String.valueOf(msg.what));
        switch (msg.what)
        {
            case TRANSFER_SOCKET:
                Log.d(TAG,"Socket is launched");
                BluetoothSocket H3=(BluetoothSocket)msg.obj;
                if (isService){
                    ms.Call_Socket(H3);
                }
                else{
                    Log.d(TAG,"나중에 생성");
                }
                break;
            default:
                Log.d(TAG,"Socket isn't launched");
                break;
        }
    }
    //~*

    //종료시 unBindService를 담당
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isService)
        {
            unbindService(mConnection);
            isService=false;
            Log.d(TAG,"Service is closed");
        }
        btService.stop();
    }

    private void FirestoreGet(final String mFirebaseUserUID){
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseFirestore.collection("words")
                //.whereEqualTo(mFirebaseUserUID, true)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()){
                            countText = new ArrayList<>();
                            for(DocumentSnapshot documentSnapshot : task.getResult()) {
                                countText.add(documentSnapshot.getString(mFirebaseUserUID));
                                //Log.d(TAG, countText.toString());
                                //Log.d(TAG, documentSnapshot.getId() + " => " + documentSnapshot.getString(mFirebaseUserUID));
                            }
                            StaticModel.setTestarray(countText);
                            Log.d("Firestore words", StaticModel.getTestarray().toString());
                        } else {
                            Log.d(TAG, "err : " + task.getException());
                        }
                    }
                });
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
