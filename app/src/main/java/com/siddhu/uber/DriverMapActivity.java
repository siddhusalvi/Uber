package com.siddhu.uber;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Map;

public class DriverMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;

    private Button mLogout;

    private String customerId = "";

    Location mLastLocation;
    LocationRequest mLocatonRequest;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mLogout = findViewById(R.id.logout);
        Log.i("onCreate"," methood");
        Toast.makeText(getApplicationContext(),"In oncreate method",Toast.LENGTH_SHORT).show();
        getAssignedCustomer();



        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(DriverMapActivity.this,MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

    }

    //attached listener that will detect chanages made on firebaseDB and driver will get notified
    private void getAssignedCustomer(){
        Toast.makeText(getApplicationContext(),"Getting assigned customer",Toast.LENGTH_SHORT).show();
        Log.i("in","getAssignedCustomer");
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Log.i("driverid",driverId);
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("customerRideId");
        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
//                    Map<String ,Object> map = (Map<String, Object>) snapshot.getValue();
//                    if(map.get("customerRideId") != null){
                        Log.i("snapshot ","found");
                        customerId = snapshot.getValue().toString();
                        getAssignedCustomerPickupLocation();
//                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    private void getAssignedCustomerPickupLocation(){
        DatabaseReference assignedCustomerPickupLocationRef = FirebaseDatabase.getInstance().getReference().child("CustomerRequest").child(customerId).child("l");
        assignedCustomerPickupLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.i("onDataChanged","called");
                if(snapshot.exists()){
                    List<Object> map = (List<Object>)snapshot.getValue();
                    double locationLang , locationLat;
                    locationLang = locationLat = 0;
                    if(map.get(0) != null){
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if(map.get(1) != null){
                        locationLang = Double.parseDouble(map.get(0).toString());
                    }
                    LatLng driverLatlang = new LatLng(locationLat,locationLang);
                    mMap.addMarker(new MarkerOptions().position(driverLatlang).title("Pickup location"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        Toast.makeText(getApplicationContext(),"Map is ready ",Toast.LENGTH_SHORT).show();
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
    }

    protected synchronized void buildGoogleApiClient(){
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }
    @Override
    public void onLocationChanged(Location location) {
//        if(getApplicationContext() != null) {
            Toast.makeText(getApplicationContext(),"driver location updateing OnLocationChanged",Toast.LENGTH_SHORT).show();
            mLastLocation = location;
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(19));

            //writing location to the database
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("DriversAvailable");
            DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("DriversWorking");

            GeoFire geoFireAvailable = new GeoFire(refAvailable);
            GeoFire geoFireWorking = new GeoFire(refWorking);


            switch (customerId){
                case "":
                    Toast.makeText(getApplicationContext(),"CustomerId "+customerId,Toast.LENGTH_SHORT).show();

                    geoFireWorking.removeLocation(userId);
                    geoFireAvailable.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;
                default:
                    geoFireAvailable.removeLocation(userId);
                    Toast.makeText(getApplicationContext(),"CustomerId "+customerId,Toast.LENGTH_SHORT).show();
                    geoFireWorking.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;
            }
//        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocatonRequest = new LocationRequest();
        mLocatonRequest.setInterval(1000);
        mLocatonRequest.setFastestInterval(1000);
        mLocatonRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        //check permission here

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,mLocatonRequest,this);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onStop() {
        super.onStop();

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("DriverseAvailable");

        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId);
        Toast.makeText(getApplicationContext(),"Removed location"+userId,Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("DriverseAvailable");

        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId);
        Toast.makeText(getApplicationContext(),"Removed location"+userId,Toast.LENGTH_SHORT).show();
    }
}
