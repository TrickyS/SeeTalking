package inu.capstone.duo.seetalking;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.os.AsyncTask;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.naver.speech.clientapi.SpeechRecognitionResult;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import inu.capstone.duo.seetalking.Model.MorpModel;
import inu.capstone.duo.seetalking.Retrofit.ReqData;
import inu.capstone.duo.seetalking.Retrofit.ResData;
import inu.capstone.duo.seetalking.Retrofit.RetrofitConnect;
import inu.capstone.duo.seetalking.Retrofit.RetrofitInterface;
import inu.capstone.duo.seetalking.util.AudioWriterPCM;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TrainingActivity extends Activity {

    private static final String TAG = TrainingActivity.class.getSimpleName();
    private static final String CLIENT_ID = KeyClass.CLIENT_ID; // 음성인식 KEY
    private static final String ACCESS_KEY = KeyClass.API_KEY; // 형태소 분석 KEY
    private static final String ANALYSIS_CODE = "morp"; // 형태소 분석 CODE

    private TrainingActivity.RecognitionHandler handler;
    private NaverRecognizer naverRecognizer;
    private AudioWriterPCM writer;

    // Retrofit
    private RetrofitInterface retrofitInterface;
    private ReqData reqData; // RequestBody
    private ResData resData; // ResponseBody
    private String responsebody = null;

    // Firestore
    private FirebaseFirestore firebaseFirestore;
    private FirebaseUser mFirebaseUser; // 유저 UID 따오기
    private String mFirebaseUserUID;

    // Button, TextView, EditText, SurfaceView 선언
    private String mResult;
    private TextView txtResult;
    private Button btn_record;
    private Button btn_camera;
    private Button btn_typing; // JYD
    private EditText input_text;
    private EditText typing_text; // JYD
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private Camera camera;
    private static int count = 0; // 변환용
    private int[] CompareResult;

    private InputMethodManager keyboard; // JYD
    private boolean setKeyboard; // JYD
    private ComparisonSentence emComparison; // JYD

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_training);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); // 전체화면

        // 음성인식
        handler = new TrainingActivity.RecognitionHandler(this);
        naverRecognizer = new NaverRecognizer(this, handler, CLIENT_ID);

        // Retrofit, Firestore 초기화
        retrofitInterface = RetrofitConnect.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        mFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        mFirebaseUserUID = mFirebaseUser.getUid();

        /* ReqData 초기화
        ReqData.Argument argument = new ReqData.Argument(ANALYSIS_CODE, sentence2);
        reqData = new ReqData(ACCESS_KEY, argument);*/

        txtResult = (TextView) findViewById(R.id.txt_result);
        btn_record = (Button) findViewById(R.id.btn_record);
        btn_camera = (Button) findViewById(R.id.btn_camera);
        btn_typing = (Button) findViewById(R.id.btn_typing); // JYD
        input_text = (EditText) findViewById(R.id.input_text);
        typing_text = (EditText) findViewById(R.id.input_typing); // JYD

        surfaceView =  (SurfaceView) findViewById(R.id.surfaceView1);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(surfaceListener);

        // keyboard, JYD
        keyboard = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        setKeyboard = false;

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

                    new MorpAnalysisTask().execute(typing_text.getText().toString()); // 형태소 분석 수행

                    //JYD
                    if(mResult!=null&&typing_text.getText().toString().length()>0) {
                        emComparison = new ComparisonSentence(typing_text.getText().toString(), mResult);
                        if (emComparison.Mistake_call() != null) {
                            CompareResult = emComparison.Mistake_call();  // 이걸 int array로 받음됨
                        }
                    }
                }
            }
        });

        // JYD
        btn_typing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(setKeyboard && typing_text.getText().toString().length() > 0){
                    typing_text.clearFocus();
                    typing_text.setClickable(false);
                    typing_text.setFocusable(false);

                    keyboard.hideSoftInputFromWindow(typing_text.getWindowToken(), 0);
                    setKeyboard = false;
                } else {
                    typing_text.setVisibility(View.VISIBLE);
                    typing_text.setFocusableInTouchMode(true);
                    typing_text.setClickable(true);
                    typing_text.setFocusable(true);
                    typing_text.setText(null);
                    typing_text.requestFocus();
                    keyboard.showSoftInput(typing_text,0);
                    setKeyboard = true;
                }
            }
        });
    }

        /*[HTTP Request Body]
        {
        “access_key”: “YOUR_ACCESS_KEY”,
        “argument”: {
            “analysis_code”: “ANALYSIS_CODE”
            “text”: “YOUR_SENTENCE”
            }
        }
        */

    private class MorpAnalysisTask extends AsyncTask<String, Void, Void>{ // 형태소 분석은 다른 스레드에서 수행
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(String... strings) {
            // 형태소 분석
            ReqData.Argument argument = new ReqData.Argument(ANALYSIS_CODE, strings[0]); // ReqData 초기화
            reqData = new ReqData(ACCESS_KEY, argument);
            Call<ResData> langDataCall = retrofitInterface.langData(reqData);
            langDataCall.enqueue(new Callback<ResData>() {
                @Override
                public void onResponse(Call<ResData> call, Response<ResData> response) {
                    if (response.isSuccessful()) {
                        Gson gson = new Gson();
                        MorpModel morpModel = null;
                        ResData resData = response.body();
                        JsonObject jsonObject = resData.getReturn_object(); // API에서 돌려준 Json 원본
                        JsonArray midJson = (JsonArray) jsonObject.getAsJsonArray("sentence"); // sentence 부분 추출
                        JsonElement midElement = midJson.get(0);
                        JsonObject midObject = midElement.getAsJsonObject();
                        JsonArray finalJson = (JsonArray) midObject.getAsJsonArray("morp"); // morp 부분 추출
                        for (int i = 0; i < finalJson.size(); i++) {
                            JsonElement tempElement = finalJson.get(i);
                            JsonObject tempObject = (JsonObject) tempElement.getAsJsonObject();
                            morpModel = gson.fromJson(tempObject, MorpModel.class);
                            if (morpModel.getType().equals("NNG") || morpModel.getType().equals("NNP")) { // 형태소가 일반명사거나 고유명사면 저장
                                FirestoreInsert(mFirebaseUserUID, morpModel.getLemma());
                            } else {
                                continue;
                            }
                            Log.d(TAG, "count : " + i + ", word : " + morpModel.getLemma());
                        }
                        responsebody = gson.toJson(finalJson);
                        Log.d(TAG, "API returns " + resData.getResult());
                    } else {
                        Log.d(TAG, "Failed because response code : " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<ResData> call, Throwable t) {
                    Log.d(TAG, "retrofit 전송 실패");
                }
            });

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }


    // Firestore 데이터 저장
    private void FirestoreInsert(String mFirebaseUserUID, String lemma) {
        Map<String, String> insertData = new HashMap<>();
        insertData.put(mFirebaseUserUID, lemma);
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseFirestore.collection("words")
                .add(insertData)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "Firestore 저장 성공");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "Firestore 저장 실패");
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
                // 결과 중에서 한 줄만
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