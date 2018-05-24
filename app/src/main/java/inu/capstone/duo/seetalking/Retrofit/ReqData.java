package inu.capstone.duo.seetalking.Retrofit;

import com.google.gson.annotations.SerializedName;

public class ReqData {

    @SerializedName("access_key")
    private String access_key;

    @SerializedName("argument")
    private Argument argument;

    public ReqData(String access_key, Argument argument){
        this.access_key = access_key;
        this.argument = argument;
    }

    public static class Argument{
        @SerializedName("analysis_code")
        public String analysis_code;
        @SerializedName("text")
        public String text;

        public Argument(String analysis_code, String text) {
            this.analysis_code = analysis_code;
            this.text = text;
        }
    }


    public Argument getArgument() {
        return argument;
    }

    public void setArgument(Argument argument) {
        this.argument = argument;
    }

    public String getAccess_key() {
        return access_key;
    }

    public void setAccess_key(String access_key) {
        this.access_key = access_key;
    }

}
