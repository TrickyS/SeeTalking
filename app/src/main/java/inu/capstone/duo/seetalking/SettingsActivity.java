package inu.capstone.duo.seetalking;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class SettingsActivity extends Activity {

    static final String TAG = SettingsActivity.class.getSimpleName();
    private Switch swch_vibrate;
    private Button btn_signdelete;


    //Firebase set
    private FirebaseUser mFirebaseUser;


    // Request code
    private static final int SETTINGA = 9; // JYD //Request code
    private static final int VIBRATE = 10;
    private static final int GOTOOUT=11; //인증삭제 이후 나가지는 로그아웃코드

    private boolean isvibrate;
    private Intent Intent;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);



        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();


        final AuthCredential credential = GoogleAuthProvider.getCredential(GoogleSignIn.getLastSignedInAccount(this).getIdToken(),null);

        btn_signdelete = (Button) findViewById(R.id.btn_signdelete);
        mFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        //진동셋
        swch_vibrate = (Switch) findViewById(R.id.switch_vibrate);
        Intent = getIntent();
        isvibrate = Intent.getBooleanExtra("VIBRATE", true);
        CheckState(isvibrate);
        swch_vibrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (swch_vibrate.isChecked()) {;
                    //swch_vibrate.setChecked(false);
                    isvibrate = true;
                    Intent.putExtra("VIBRATE", isvibrate);
                    setResult(VIBRATE, Intent);
                    Log.d(TAG, "Vibrate state : " + String.valueOf(isvibrate));
                    //Log.d(TAG,"Vibrate state : " + String.valueOf(isvibrate));
                } else {
                    //swch_vibrate.setChecked(true);
                    isvibrate = false;
                    Intent.putExtra("VIBRATE", isvibrate);
                    setResult(VIBRATE, Intent);
                    Log.d(TAG, "Vibrate state : " + String.valueOf(isvibrate));

                    //Log.d(TAG,"Vibrate state : " + String.valueOf(isvibrate));


                }
            }
        });
        btn_signdelete.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                mFirebaseUser.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        mFirebaseUser.delete()
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(task.isSuccessful()){
                                            Log.d(TAG,"User account delete");
                                            Intent gooutintnet = new Intent();
                                            setResult(GOTOOUT,gooutintnet);
                                            finish();
                                        }
                                    }
                                });
                    }
                });
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();


    }

    @Override
    protected void onStop() {
        super.onStop();

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        finish();
    }
    //처음들어갔을때 온오프 셋
    private void CheckState(boolean state) {
        Log.d(TAG,"Checkstate : " + String.valueOf(state));
        if (state) {
            swch_vibrate.setChecked(true);
        } else {
            swch_vibrate.setChecked(false);
        }
    }
}