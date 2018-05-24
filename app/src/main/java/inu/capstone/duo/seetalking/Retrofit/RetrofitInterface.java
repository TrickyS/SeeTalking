package inu.capstone.duo.seetalking.Retrofit;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface RetrofitInterface {
    @Headers({"Content-Type: application/json", "charset=utf8"})

    // 형태소 분석기와 POST 통신
    @POST("WiseNLU")
    Call<ResData> langData(@Body ReqData reqData);
}
