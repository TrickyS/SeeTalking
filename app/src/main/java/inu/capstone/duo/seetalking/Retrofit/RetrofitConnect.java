package inu.capstone.duo.seetalking.Retrofit;

import inu.capstone.duo.seetalking.KeyClass;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitConnect {
    private static final boolean DEBUG = false;
    private static final String TAG = "Retrofit Debug";
    private static RetrofitConnect instance;
    private static Retrofit retrofit = null;
    private static RetrofitInterface retrofitInterface = null;

    private RetrofitConnect(){
        retrofit = new Retrofit.Builder()
                .baseUrl(KeyClass.SERVER_IP)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        retrofitInterface = (RetrofitInterface)retrofit.create(RetrofitInterface.class);
    }

    public static RetrofitInterface getInstance(){
        if (instance == null){
            instance = new RetrofitConnect();
        }
        RetrofitConnect localRetrofitClass = instance;
        return  retrofitInterface;
    }
}
