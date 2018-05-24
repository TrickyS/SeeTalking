package inu.capstone.duo.seetalking.Model;

import com.google.gson.annotations.SerializedName;

public class MorpModel {

    @SerializedName("type")
    private String type;

    @SerializedName("lemma")
    private String lemma;

    public MorpModel(String type, String lemma){
        this.type = type;
        this.lemma = lemma;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLemma() {
        return lemma;
    }

    public void setLemma(String lemma) {
        this.lemma = lemma;
    }
}
