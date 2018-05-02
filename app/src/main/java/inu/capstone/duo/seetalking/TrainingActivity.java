package inu.capstone.duo.seetalking;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.naver.speech.clientapi.SpeechRecognitionResult;

import org.json.JSONObject;

import inu.capstone.duo.seetalking.Retrofit.PostData;
import inu.capstone.duo.seetalking.Retrofit.RetrofitConnect;
import inu.capstone.duo.seetalking.Retrofit.RetrofitInterface;
import inu.capstone.duo.seetalking.util.AudioWriterPCM;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import java.lang.ref.WeakReference;
import java.util.List;

public class TrainingActivity extends Activity {

    // 음성인식
    private static final String TAG = TrainingActivity.class.getSimpleName();
    private static final String CLIENT_ID = KeyClass.CLIENT_ID;
    private TrainingActivity.RecognitionHandler handler;
    private NaverRecognizer naverRecognizer;
    private AudioWriterPCM writer;

    // Retrofit
    private RetrofitInterface retrofitInterface;
    // 테스트
    private Button btn_test;

    // 결과물, 버튼
    private String mResult;
    private TextView txtResult;
    private Button btn_record;
    private Button btn_camera;
    private EditText input_text;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private Camera camera;
    private static int count = 0; // 변환용

    // 2. 그다음 시작하는게 onCreate. Activity 생성시 잴 먼저 실행된다.
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_training);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); // 전체화면

        // 음성인식
        handler = new TrainingActivity.RecognitionHandler(this);
        naverRecognizer = new NaverRecognizer(this, handler, CLIENT_ID);

        // Retrofit 초기화
        retrofitInterface = RetrofitConnect.getInstance();

        txtResult = (TextView) findViewById(R.id.txt_result);
        btn_record = (Button) findViewById(R.id.btn_record);
        btn_camera = (Button) findViewById(R.id.btn_camera);
        input_text = (EditText) findViewById(R.id.input_text);

        surfaceView =  (SurfaceView) findViewById(R.id.surfaceView1);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(surfaceListener);

        // Retrofit 값 보내기 테스트
        btn_test = (Button)findViewById(R.id.btn_test);

        btn_test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Connection2();
            }
        });

       btn_camera.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                count++;
                if(count>1) count=0;
                if(count==1) surfaceView.setVisibility(View.VISIBLE);
                else if(count==0) surfaceView.setVisibility(View.INVISIBLE);
                }
            }
        );

        btn_record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!naverRecognizer.getSpeechRecognizer().isRunning()) {
                    // Start button is pushed when SpeechRecognizer's state is inactive.
                    // Run SpeechRecongizer by calling recognize().
                    mResult = "";
                    txtResult.setText("연결중입니다.");
                    btn_record.setText(R.string.str_stop);
                    naverRecognizer.recognize();
                } else {
                    Log.d(TAG, "stop and wait Final Result");
                    btn_record.setEnabled(false);

                    naverRecognizer.getSpeechRecognizer().stop();
                }
            }
        });
    }

    private SurfaceHolder.Callback surfaceListener = new SurfaceHolder.Callback() {

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            // TODO Auto-generated method stub
            camera.release();
            camera = null;
            Log.i(TAG, "카메라 기능 해제");
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            // TODO Auto-generated method stub
            camera = Camera.open(1);
            Log.i(TAG, "카메라 미리보기 활성");

            try {
                camera.setPreviewDisplay(holder);
            }catch(Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,int height) {
            // TODO Auto-generated method stub
            Camera.Parameters parameters = camera.getParameters();
            parameters.setPreviewSize(width, height);
            camera.startPreview();
            Log.i(TAG,"카메라 미리보기 활성");

        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        // NOTE : initialize() must be called on start time.
        naverRecognizer.getSpeechRecognizer().initialize();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mResult = "";
        txtResult.setText("");
        btn_record.setText(R.string.str_start);
        btn_record.setEnabled(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // NOTE : release() must be called on stop time.
        naverRecognizer.getSpeechRecognizer().release();
    }

    // 테스트용
    /*public void Connection(){
        Call<JsonArray> getDataCall = retrofitInterface.getData();

        getDataCall.enqueue(new Callback<JsonArray>() {
            @Override
            public void onResponse(Call<JsonArray> call, Response<JsonArray> response) {
                if(response.isSuccessful()) { // 성공
                    JsonParser jsonParser = new JsonParser();
                    JsonArray jsonData = (JsonArray) jsonParser.parse(response.body().toString());
                    String resultData = jsonData.toString();
                    txtResult.setText(resultData);
                    Log.i("what the", "왜???");
                }
            }

            @Override
            public void onFailure(Call<JsonArray> call, Throwable t) {
                Log.i("failed", "Why?!!?!");
            }
        });
    }*/

    public void Connection2(){
        PostData postData = new PostData("hello", "world!");
        Call<PostData> postDataCall = retrofitInterface.postData(postData);
        postDataCall.enqueue(new Callback<PostData>() {
            @Override
            public void onResponse(Call<PostData> call, Response<PostData> response) {
                if(response.isSuccessful()) {
                    Gson gson = new Gson();
                    PostData receiveData = response.body();
                    String randtoken = receiveData.getRandtoken();
                    Toast.makeText(getApplicationContext(), "response code : " + response.code() + randtoken, Toast.LENGTH_LONG).show();
                    Log.i("result", randtoken);
                }
                else{
                    Toast.makeText(getApplicationContext(), "response code : " + response.code(), Toast.LENGTH_LONG).show();
                    Log.i("post", "failed");
                }
            }

            @Override
            public void onFailure(Call<PostData> call, Throwable t) {
                Log.i("failed", "Why?!!?!");
            }
        });
    }

    // Declare handler for handling SpeechRecognizer thread's Messages.
    static class RecognitionHandler extends Handler {
        private final WeakReference<TrainingActivity> mActivity;

        RecognitionHandler(TrainingActivity activity) {
            mActivity = new WeakReference<TrainingActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            TrainingActivity activity = mActivity.get();
            if (activity != null) {
                activity.handleMessage(msg);
            }
        }
    }

    // Handle speech recognition Messages.
    private void handleMessage(Message msg) {
        switch (msg.what) {
            case R.id.clientReady:
                // Now an user can speak.
                txtResult.setText("말하세요.");
                writer = new AudioWriterPCM(
                        Environment.getExternalStorageDirectory().getAbsolutePath() + "/NaverSpeechTest");
                writer.open("Test");
                break;

            case R.id.audioRecording:
                writer.write((short[]) msg.obj);
                break;

            case R.id.partialResult:
                // Extract obj property typed with String.
                mResult = (String) (msg.obj);
                txtResult.setText(mResult);
                break;

            case R.id.finalResult:
                // Extract obj property typed with String array.
                // The first element is recognition result for speech.
                SpeechRecognitionResult speechRecognitionResult = (SpeechRecognitionResult) msg.obj;
                List<String> results = speechRecognitionResult.getResults();
                StringBuilder strBuf = new StringBuilder();
                // 결과 중에서 한 줄만 따는 법을 나는 모르겠다 하하하하하하
                for(String result : results) {
                    strBuf.append(result);
                    strBuf.append("\n");
                    break;
                }
                mResult = strBuf.toString();
                txtResult.setText(mResult);
                break;

            case R.id.recognitionError:
                if (writer != null) {
                    writer.close();
                }

                mResult = "Error code : " + msg.obj.toString();
                txtResult.setText(mResult);
                btn_record.setText(R.string.str_start);
                btn_record.setEnabled(true);
                break;

            case R.id.clientInactive:
                if (writer != null) {
                    writer.close();
                }

                btn_record.setText(R.string.str_start);
                btn_record.setEnabled(true);
                break;
        }
    }
}