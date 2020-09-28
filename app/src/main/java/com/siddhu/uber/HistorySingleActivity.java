package com.siddhu.uber;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistorySingleActivity extends AppCompatActivity implements OnMapReadyCallback, RoutingListener{

    GoogleMap mMap;
    SupportMapFragment mMapFragment;
    String rideId,currentUserId,customerId,driverId,userDriverOrCustomer;
    TextView locationRide, distanceRide, dateRide, nameUser, phoneUser;
    DatabaseReference historyRideInfoDb;
    ImageView imageUser;
    LatLng pickupLocation,dropLocation;
    RatingBar mRatingBar;
    String distance;
    double ridePrice = 0;

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

        mRatingBar = findViewById(R.id.rating_bar);

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
                                displayCustomerRelatedObjects();
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

                        if(child.getKey().equals("rating")){
                            mRatingBar.setRating(Integer.valueOf(child.getValue().toString()));
                        }

                        if(child.getKey().equals("destination") ){
                            locationRide.setText(child.getValue().toString());
                        }


                        if(child.getKey().equals("distance") ){
                            distance = child.getValue().toString();
                            distanceRide.setText(distance.substring(0,Math.min(distance.length(),5)) + "km");
                            ridePrice = Double.valueOf(distance) * 0.5;
                        }


//                        if(child.getKey().equals("location")){
//                            pickupLocation = new LatLng(Double.valueOf(child.child("from").child("lat").getValue().toString()),Double.valueOf(child.child("from").child("lng").getValue().toString()));
//                            dropLocation = new LatLng(Double.valueOf(child.child("to").child("lat").getValue().toString()),Double.valueOf(child.child("to").child("lng").getValue().toString()));
//                            if(pickupLocation != new LatLng(0,0) && dropLocation != new LatLng(0,0)){
//                                //call function to draw route between there two points
//                            }
//                        }

                        if (child.getKey().equals("location")){
                            System.out.println(child.child("from").child("lat").getValue());

                            Double lat1,lat2,lng1,lng2;
                            lat1 = Double.valueOf(child.child("from").child("lat").getValue().toString());
                            lng1 = Double.valueOf(child.child("from").child("lang").getValue().toString());
                            lat2 = Double.valueOf(child.child("to").child("lat").getValue().toString());
                            lng2 =  Double.valueOf(child.child("to").child("lang").getValue().toString());

                            pickupLocation = new LatLng(lat1,lng1);
                            dropLocation = new LatLng(lat2,lng2);
//                            if(destinationLatLng != new LatLng(0,0)){
//                                getRouteToMarker();
//                            }
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

    public void displayCustomerRelatedObjects(){
        mRatingBar.setVisibility(View.VISIBLE);
        mRatingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                historyRideInfoDb.child("rating").setValue(rating);
                DatabaseReference mDriverRatingDB = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("rating");
                mDriverRatingDB.child(rideId).setValue("rating");

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

    private void getRouteToMarker() {
        Routing routing = new Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false)
                .waypoints(pickupLocation, dropLocation)
                .build();
        routing.execute();
    }
    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};
    @Override
    public void onRoutingFailure(RouteException e) {
        if(e != null) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }else {
            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onRoutingStart() {
    }
    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(pickupLocation);
        builder.include(dropLocation);
        LatLngBounds bounds = builder.build();

        int width = getResources().getDisplayMetrics().widthPixels;
        int padding = (int) (width*0.2);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);

        mMap.animateCamera(cameraUpdate);

        mMap.addMarker(new MarkerOptions().position(pickupLocation).title("pickup location"));
        mMap.addMarker(new MarkerOptions().position(dropLocation).title("destination"));

        if(polylines.size()>0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i <route.size(); i++) {

            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            Toast.makeText(getApplicationContext(),"Route "+ (i+1) +": distance - "+ route.get(i).getDistanceValue()+": duration - "+ route.get(i).getDurationValue(),Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onRoutingCancelled() {
    }
    private void erasePolylines(){
        for(Polyline line : polylines){
            line.remove();
        }
        polylines.clear();
    }
}