package inu.capstone.duo.seetalking.Retrofit;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface RetrofitInterface {
    @Headers({"Content-Type: application/json", "charset=utf8"})

    @GET("selectDB")
    Call<PostData> getData();

    @POST("test")
    Call<PostData> postData(@Body PostData postData);
}
