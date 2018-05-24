package inu.capstone.duo.seetalking;

import android.util.Log;

public class ComparisonSentence {

    private static final String TAG = ComparisonSentence.class.getSimpleName();

    private String base_sentence;
    private String other_sentence;

    private char[] caric_base_sentence;
    private char[] caric_other_sentence;

    private int[] mistake_save;

    public ComparisonSentence(String base, String other){
        base_sentence = base;
        other_sentence = other;

        caric_base_sentence = base_sentence.toCharArray();
        caric_other_sentence = other_sentence.toCharArray();
        if(ChackLetter(caric_base_sentence)&&(ChackLetter(caric_other_sentence))){
            caric_base_sentence = RemoveSpacing(base_sentence);
            caric_other_sentence = RemoveSpacing(other_sentence);

            mistake_save=ComparisonM(caric_base_sentence,caric_other_sentence);
        }
        else{
            Log.d(TAG,"Stop comparion");
        }

    }

    public int[] Mistake_call(){
        if(mistake_save == null){
            Log.e(TAG,"mistake_save == null , Return 거부");
            return null;
        }
        else{
            return mistake_save;
        }
    }

    private boolean ChackLetter(char[] target){
        for(int i=0; i<target.length; i++){
            //Log.d(TAG,String.valueOf(Integer.toHexString((int)target[i])));
            if(!(('\uac00'<=(int)target[i] && '\uD7AF'>=(int)target[i]) || '\u0020'==(int)target[i])){
                Log.d(TAG,"This isn't hangul");
                return false;
            }
        }
        return true;
    }


    private char[] RemoveSpacing(String target){
        target = target.replaceAll(" ","%");
        Log.d("Comp-C-RemovieSpacing", target);
        return target.toCharArray();
    }

    private int[] ComparisonM(char[] base, char[] other) {

        //50개 까지 인식가능
        int[] countfor= new int[50];
        int ct0 = 0;
        int i = 0, j = 0, temp = 0;

        if (base.length >= other.length && other.length>0) {
            Log.d(TAG,"이거실행");
            j = i;
            Log.d(TAG,String.valueOf(base.length));
            for (i = 0; i < base.length; i++) {
                temp = j;
                Log.d(TAG,"i : " + String.valueOf(i) + " : j : "+ String.valueOf(j) + " 1 : " + String.valueOf(base[i]) +":  2 : "+ String.valueOf(other[j]));
                if (j > other.length - 1) {
                    countfor[i] = 1;
                    Log.d(TAG,"1");
                } else if (i == base.length -1 && j < other.length - 1) {

                    i = temp - 1;
                    j++;
                    Log.d(TAG,"2");
                    /*
                } else if (base[i] == other[j]) {
                    countfor[i] = 0;
                    */
                } else if((base[i] == '%')&&(other[j] == '%')) {

                    j++;
                    countfor[i] = 0;
                    Log.d(TAG, "3");
                } else if(((base[i] == '%'))){
                    countfor[i] = 0;
                    Log.d(TAG, "6");
                }else if( (other[j] == '%')){

                    j++;
                    Log.d(TAG,"4");
                } else if ((base[i] != other[j])) {
                    countfor[i] = 1;
                    Log.d(TAG,"5");

                } else {

                    j++;
                    countfor[i] = 0;
                    Log.d(TAG,"6");

                }
            }
        }
        else if(base.length < other.length && other.length>0){
            Log.d(TAG,"이거실행2");
            j = i;
            Log.d(TAG,String.valueOf(base.length));
            for (i = 0; i < other.length; i++) {
                temp = j;
                Log.d(TAG,"i : " + String.valueOf(i) + " : j : "+ String.valueOf(j) + " 1 : " + String.valueOf(base[j]) +":  2 : "+ String.valueOf(other[i]));
                if(j>base.length-1){
                    break;
                }
                else if (i == other.length -1 && j < base.length - 1) {
                    i = 0;
                    j++;
                    Log.d(TAG,"2");
                    /*
                } else if (base[i] == other[j]) {
                    countfor[i] = 0;
                    */
                } else if((base[j] == '%')&&(other[i] == '%')) {
                    countfor[j] = 0;
                    j++;

                    Log.d(TAG, "3");
                } else if(((base[j] == '%'))){
                    countfor[j] = 0;
                    j++;

                    Log.d(TAG, "6");
                }else if( (other[i] == '%')){

                    Log.d(TAG,"4");
                } else if ((base[j] != other[i])) {
                    countfor[j] = 1;
                    Log.d(TAG,"5");

                } else {

                    countfor[j] = 0;
                    j++;
                    Log.d(TAG,"6");

                }
            }

        }


        for(int Q=0; Q<base.length; Q++) {
            Log.d(TAG, String.valueOf(countfor[Q]));
        }
        return countfor;
    }
}
