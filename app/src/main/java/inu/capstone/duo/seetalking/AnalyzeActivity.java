package inu.capstone.duo.seetalking;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import inu.capstone.duo.seetalking.Model.StaticModel;

public class AnalyzeActivity extends Activity {

    private static final String TAG = AnalyzeActivity.class.getSimpleName();

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private RecyclerViewAdapter mRecyclerViewAdapter;
    private FirebaseFirestore firebaseFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analyze);

        ArrayList<String> testarray = StaticModel.getTestarray();
        Map<String, Long> counts = testarray.stream().collect(Collectors.groupingBy(e -> e, Collectors.counting()));

        Map<String, Long> sort_count = new LinkedHashMap<>();
        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEachOrdered(x -> sort_count.put(x.getKey(), x.getValue()));

        Log.d(TAG, testarray.toString());
        Log.d("counts", counts.toString());
        Log.d("sort_count", sort_count.toString());


        // Firestore 초기화
        firebaseFirestore = FirebaseFirestore.getInstance();

        // RecyclerView 세팅
        mRecyclerView = (RecyclerView) findViewById(R.id.analyzeactivity_recyclerview);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        mRecyclerViewAdapter = new RecyclerViewAdapter(sort_count);
        mRecyclerView.setAdapter(mRecyclerViewAdapter);
    }

    public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewHolder> {
        private Map<String, Long> mitem;
        private List<String> mitem_keyset;
        private List<Long> mitem_value;
        private Context mContext;

        public RecyclerViewAdapter(Map<String, Long> itemList){
            mitem = itemList;
            mitem_keyset = new ArrayList(mitem.keySet());
            mitem_value = new ArrayList(mitem.values());
            // collect가 문제였어...
            // mitem_keyset = mitem.keySet().stream().collect(Collectors.toList());
            // mitem_value = mitem.values().stream().collect(Collectors.toList());
        }

        // 필수 오버라이드 : View 생성, ViewHolder 호출
        @NonNull
        @Override
        public RecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ranking, parent, false);
            mContext = parent.getContext();
            RecyclerViewHolder holder = new RecyclerViewHolder(v);
            return holder;
        }

        // 필수 오버라이드 : 재활용되는 View가 호출, Adapter가 해당 position에 해당하는 데이터를 결합
        @Override
        public void onBindViewHolder(@NonNull RecyclerViewHolder holder, int position) {
            holder.mWord.setText(mitem_keyset.get(position));
            holder.mWordCount.setText(mitem_value.get(position).toString() + "번");
        }

        // 필수 오버라이드 : 데이터 개수 반환
        @Override
        public int getItemCount() {
            //return mitem.size();
            if(mitem.size() > 5){
                return 5;
            } else{
               return mitem.size();
            }
        }
    }

    public class RecyclerViewHolder extends RecyclerView.ViewHolder{
        public TextView mWord;
        public TextView mWordCount;
        public RecyclerViewHolder(View itemView){
            super(itemView);
            mWord = (TextView) itemView.findViewById(R.id.text_word);
            mWordCount = (TextView) itemView.findViewById(R.id.text_wordcount);
        }
    }
}
