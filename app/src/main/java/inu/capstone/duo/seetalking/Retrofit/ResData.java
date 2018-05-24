package inu.capstone.duo.seetalking.Retrofit;

import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ResData {

    @SerializedName("result")
    @Expose
    private int result;

    @SerializedName("return_type")
    @Expose
    private String return_type;

    @SerializedName("return_object")
    @Expose
    private JsonObject return_object;

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public String getReturn_type() {
        return return_type;
    }

    public void setReturn_type(String return_type) {
        this.return_type = return_type;
    }

    public JsonObject getReturn_object() {
        return return_object;
    }

    public void setReturn_object(JsonObject return_object) {
        this.return_object = return_object;
    }

    public ResData(int result, String return_type, JsonObject return_object){
        this.result = result;
        this.return_type = return_type;
        this.return_object = return_object;
    }
}
