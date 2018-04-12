package inu.capstone.duo.seetalking;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.naver.speech.clientapi.SpeechRecognitionResult;
import inu.capstone.duo.seetalking.util.AudioWriterPCM;
import java.lang.ref.WeakReference;
import java.util.List;

public class TrainingActivity extends Activity {

    private static final String TAG = TrainingActivity.class.getSimpleName();
    private static final String CLIENT_ID = KeyClass.CLIENT_ID;

    private TrainingActivity.RecognitionHandler handler;
    private NaverRecognizer naverRecognizer;

    // 결과물, 버튼
    private String mResult;
    private TextView txtResult;
    private Button btn_record;
    private Button btn_camera;

    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private Camera camera;
    private static int count = 0; // 변환용

    private AudioWriterPCM writer;

    // 2. 그다음 시작하는게 onCreate. Activity 생성시 잴 먼저 실행된다.
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_training);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); // 전체화면

        handler = new TrainingActivity.RecognitionHandler(this);
        naverRecognizer = new NaverRecognizer(this, handler, CLIENT_ID);

        txtResult = (TextView) findViewById(R.id.txt_result);
        btn_record = (Button) findViewById(R.id.btn_record);
        btn_camera = (Button) findViewById(R.id.btn_camera);
        surfaceView =  (SurfaceView) findViewById(R.id.surfaceView1);

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(surfaceListener);
        //surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        findViewById(R.id.btn_camera).setOnClickListener(new Button.OnClickListener() {
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