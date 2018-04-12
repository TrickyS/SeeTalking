package inu.capstone.duo.seetalking.Retrofit;

import inu.capstone.duo.seetalking.KeyClass;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitConnect {
    private static final boolean DEBUG = false;
    private static final String TAG = "Retrofit Debug";
    private static RetrofitConnect instance;
    private static Retrofit retrofit = null;
    private static RetrofitInterface retrofitInterface = null;

    static Retrofit getClient(){
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        retrofit = new Retrofit.Builder()
                .baseUrl(KeyClass.SERVER_IP)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        return retrofit;
    }
}
