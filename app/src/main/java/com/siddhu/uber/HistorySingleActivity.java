package com.siddhu.uber;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.RoutingListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

public class HistorySingleActivity extends AppCompatActivity implements OnMapReadyCallback, RoutingListener {

    GoogleMap mMap;
    SupportMapFragment mMapFragment;
    String rideId,currentUserId,customerId,driverId,userDriverOrCustomer;
    TextView locationRide, distanceRide, dateRide, nameUser, phoneUser;
    DatabaseReference historyRideInfoDb;
    ImageView imageUser;
    LatLng pickupLocation,dropLocation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_single);

        mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mMapFragment.getMapAsync(this);

        rideId = getIntent().getExtras().getString("rideId");

        locationRide = findViewById(R.id.rideLocation);
        distanceRide = findViewById(R.id.rideDistance);
        dateRide = findViewById(R.id.rideDate);
        nameUser = findViewById(R.id.name);
        phoneUser = findViewById(R.id.phone);
        imageUser = findViewById(R.id.userImage);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        historyRideInfoDb = FirebaseDatabase.getInstance().getReference().child("history").child(rideId);
        getRideInformation();


    }

    public  void getRideInformation(){
        historyRideInfoDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    for(DataSnapshot child :snapshot.getChildren()){
                        if(child.getKey().equals("customer")){
                            customerId = child.getValue().toString();
                            if(!customerId.equals(currentUserId)){
                                userDriverOrCustomer = "Drivers";
                                getUserInfomation("Customers",customerId);
                            }
                        }
                        if(child.getKey().equals("driver")){
                            driverId = child.getValue().toString();
                            if(!customerId.equals(currentUserId)){
                                userDriverOrCustomer = "Customers";
                                getUserInfomation("Drivers",driverId);

                            }
                        }
                        if(child.getKey().equals("timestamp")){
                         dateRide.setText(getDate(Long.valueOf(child.getValue().toString())));
                        }

                        if(child.getKey().equals("destination")){
                            locationRide.setText(getDate(Long.valueOf(child.getValue().toString())));
                        }


                        if(child.getKey().equals("location")){
                            pickupLocation = new LatLng(Double.valueOf(child.child("from").child("lat").getValue().toString()),Double.valueOf(child.child("from").child("long").getValue().toString()));
                            dropLocation = new LatLng(Double.valueOf(child.child("to").child("lat").getValue().toString()),Double.valueOf(child.child("to").child("long").getValue().toString()));
                            if(pickupLocation != new LatLng(0,0) && dropLocation != new LatLng(0,0)){
                                //call function to draw route between there two points
                            }
                        }



                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getApplicationContext(),error.getMessage(),Toast.LENGTH_SHORT).show();

            }
        });
    }

    private void getUserInfomation(String otherUserDriverOrCustomer, String otherUserId) {


        DatabaseReference otherUserDB = FirebaseDatabase.getInstance().getReference().child("Users").child(otherUserDriverOrCustomer).child(otherUserId);
        otherUserDB.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    Map<String,Object> map = (Map<String,Object>) snapshot.getValue();
                    if(map.get("name")!=null){
                        nameUser.setText(map.get("name").toString());
                    }

                    if(map.get("phone")!=null){
                        phoneUser.setText(map.get("phone").toString());
                    }

                    if(map.get("profileImageUrl")!=null){
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString());
                    }
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



    //Update Add routing logic which is not worked in driver map activity
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    @Override
    public void onRoutingFailure(RouteException e) {

    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> arrayList, int i) {

    }

    @Override
    public void onRoutingCancelled() {

    }
}
