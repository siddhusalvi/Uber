package com.siddhu.uber;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.siddhu.uber.historyREcyclerView.HistoryAdapter;
import com.siddhu.uber.historyREcyclerView.HistoryObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    String customerOrDriver,userId ;
    RecyclerView mHistoryRecyclerView;
    RecyclerView.Adapter mHistoryAdapter;
    RecyclerView.LayoutManager mHistoryLayoutManager;
    ArrayList resultHistory = new ArrayList<HistoryObject>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        mHistoryRecyclerView = findViewById(R.id.historyRecyclerView);
        mHistoryRecyclerView.setNestedScrollingEnabled(false);

        mHistoryRecyclerView.setHasFixedSize(true);

        mHistoryLayoutManager = new LinearLayoutManager(HistoryActivity.this);

        mHistoryRecyclerView.setLayoutManager(mHistoryLayoutManager);

        mHistoryAdapter = new HistoryAdapter(getDataSetHistory(),HistoryActivity.this);
        mHistoryRecyclerView.setAdapter(mHistoryAdapter);

        customerOrDriver = getIntent().getExtras().getString("customerOrDriver");

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        getUserHistoryIds();


        mHistoryAdapter.notifyDataSetChanged();
    }

    private void getUserHistoryIds() {
        DatabaseReference userHistoryDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(customerOrDriver).child(userId).child("history");
        userHistoryDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    for(DataSnapshot history : snapshot.getChildren()){
                        fetchRideInformation(history.getKey());
                    }
                }
            }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    private void fetchRideInformation(String key) {
        DatabaseReference historyDatabase = FirebaseDatabase.getInstance().getReference().child("history").child(key);
        historyDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    String userId = snapshot.getKey();
                    Long timestamp = 0L;
                    for(DataSnapshot child :snapshot.getChildren()){
                        if(child.getKey().equals("timestamp")){
                            timestamp = Long.valueOf(child.getValue().toString());
                        }
                    }
                    HistoryObject obj = new HistoryObject(userId,getDate(timestamp) );
                    resultHistory.add(obj);
                    mHistoryAdapter.notifyDataSetChanged();
                }
            }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private String getDate(Long timestamp) {
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(timestamp * 1000);

        String date = android.text.format.DateFormat.format("dd-MM-yyyy hh:mm",cal).toString();
        return date;
    }


    public List<HistoryObject> getDataSetHistory(){
        return resultHistory;
    }
}
