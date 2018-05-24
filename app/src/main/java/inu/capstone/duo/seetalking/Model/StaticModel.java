package inu.capstone.duo.seetalking.Model;

import java.util.ArrayList;

public class StaticModel {
    private static ArrayList<String> testarray = new ArrayList<>();

    public static ArrayList<String> getTestarray() {
        return testarray;
    }

    public static void setTestarray(ArrayList<String> testarray) {
        StaticModel.testarray = testarray;
    }
}
