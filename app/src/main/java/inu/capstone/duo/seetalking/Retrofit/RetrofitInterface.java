package inu.capstone.duo.seetalking.Retrofit;

import com.google.gson.JsonArray;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface RetrofitInterface {
    @GET("{path}")
    Call<JsonArray> getData(@Path("path") String path);
}