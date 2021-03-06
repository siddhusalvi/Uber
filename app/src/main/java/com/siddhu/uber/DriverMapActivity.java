package com.siddhu.uber;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DriverMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LatLng mMylocationLatLang,mPickupLocationLatLang;
    LocationRequest mLocationRequest;
    Marker pickUpMarker, mMylocation;
    DatabaseReference assignedCustomerRef;
    String DEFAULT = "Not Available";
    private GoogleMap mMap;
    private Button mLogout, mSettings;
    private String customerId = "";
    private String destination = "";
    private Boolean isLoggingOut = false;
    private LinearLayout mCustomerInfo;
    private ImageView mCustomerProfileImage;
    private TextView mCustomerName, mCustomerPhone, mCustomerDestination;
    private DatabaseReference assignedCustomerPickupLocationRef;
    private ValueEventListener assignedCustomerPickupLocationRefListener;
    private boolean mLoadMapFlag = true;
    private boolean debugFlag = true;
    private String mCustomerNameField = DEFAULT;
    private String mCustomerDestinationField = DEFAULT;
    private int mCustomerPhoneField = 0;
    private ProgressDialog progressDialog;
    private List<Polyline> polylines;
    private Polyline mPolyline;
    boolean customerFoundFlag = false;
    Switch workStatus;
    float rideDistance = 0;
    Location driverFirstLocation;
    Button mHistory;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map);


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        polylines = new ArrayList<>();

        mCustomerInfo = findViewById(R.id.customerInfo);

        mCustomerProfileImage = findViewById(R.id.customerProfileImage);

        mCustomerName = findViewById(R.id.cutomerName);
        mCustomerPhone = findViewById(R.id.cutomerPhone);
        mCustomerDestination = findViewById(R.id.cutomerDestination);
        mHistory = findViewById(R.id.history);

        mLogout = findViewById(R.id.logout);
        mSettings = findViewById(R.id.settings);
        workStatus = findViewById(R.id.working);

        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DriverMapActivity.this, DriverSettingsActivity.class);
                startActivity(intent);
                return;
            }
        });

        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLoggingOut = true;
                disconnectDriver();
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(DriverMapActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        workStatus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    connectDriver();
                }else{
                    disconnectDriver();
                }
            }
        });

        mHistory.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(DriverMapActivity.this,HistoryActivity.class);
                        intent.putExtra("customerOrDriver","Drivers");
                        startActivity(intent);
                    }
                }
        );


    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mLoadMapFlag) {
            String msg = "onResume";
            message(msg, 0);
            buildGoogleApiClient();
            onLocationChanged(mLastLocation);
        }
        getAssignedCustomer();
    }

    private void getAssignedCustomer() {
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("ActiveCustomer").child("customerRideId");

//        assignedCustomerRef.setValue(driverId, new DatabaseReference.CompletionListener() {
//            @Override
//            public void onComplete(DatabaseError databaseError, DatabaseReference dataReference) {
//                message(databaseError.getMessage());
//            }
//        });


        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    customerId = dataSnapshot.getValue().toString();
                    customerFoundFlag = true;
                    String msg = "Customer found" + customerId;
                    message(msg, 0);
                    getAssignedCustomerPickupLocation();
                    getAssignedCustomerDestination();
                    getAssignedCustomerInfo();

                } else {
                    recordRide();
                    customerId = "";
                    if (pickUpMarker != null) {
                        pickUpMarker.remove();
                    }
                    if (assignedCustomerPickupLocationRefListener != null) {
                        assignedCustomerRef.removeEventListener(assignedCustomerPickupLocationRefListener);
                    }
                    rideDistance = 0;
                    mCustomerInfo.setVisibility(View.GONE);
                    mCustomerName.setText("");
                    mCustomerPhone.setText("");
                    mCustomerDestination.setText("Destination : ");
                    mCustomerProfileImage.setImageResource(R.mipmap.ic_default_user);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void getAssignedCustomerPickupLocation()  {

        assignedCustomerPickupLocationRef = FirebaseDatabase.getInstance().getReference().child("customerRequest").child(customerId).child("l");
        assignedCustomerPickupLocationRefListener = assignedCustomerPickupLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && !customerId.equals("")) {
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    if (map.get(0) != null) {
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1) != null) {
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng pickupLatlang = new LatLng(locationLat, locationLng);
                    if (pickUpMarker != null) {
                        pickUpMarker.remove();
                    }
                    pickUpMarker = mMap.addMarker(new MarkerOptions().position(pickupLatlang).title("pickup location"));
//                            .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickupmarker)));
                    mMylocationLatLang = new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude());
                    mPickupLocationLatLang = pickupLatlang;

//                    mPolyline = mMap.addPolyline(new PolylineOptions()
//                            .clickable(true)
//                            .add(mMylocationLatLang,mPickupLocationLatLang));


                } else {
                    message("Customer Found but unable to get its location", 0);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                message(databaseError.getMessage(), 1);
            }
        });
    }

    public void connectDriver(){
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    private void getAssignedCustomerDestination() {
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        final DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("customerRequest").child("destination");
        assignedCustomerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String destination = dataSnapshot.getValue().toString();
                    mCustomerDestination.setText("Destination :" + destination);
                } else {
                    mCustomerDestination.setText("Destination : ");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void getAssignedCustomerInfo() {
        mCustomerInfo.setVisibility(View.VISIBLE);
        DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId);
        mCustomerDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                    if (map.get("name") != null) {
                        mCustomerNameField = map.get("name").toString();
                        mCustomerName.setText(mCustomerNameField);
                    }
                    if (map.get("phone") != null) {
                        mCustomerPhoneField = Integer.valueOf(map.get("phone").toString());
                        mCustomerPhone.setText(String.valueOf(mCustomerPhoneField));
                    }
                    if (map.get("profileImageUrl") != null) {
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(mCustomerProfileImage);
                    } else {
                        mCustomerProfileImage.setImageResource(R.mipmap.ic_default_passenger);
                    }

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
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (getApplicationContext() != null) {

            if(driverFirstLocation ==null){
                driverFirstLocation = location;
            }

            if(!customerId.equals("")){
                 rideDistance += driverFirstLocation.distanceTo(location)/1000;
            }

            mLastLocation = location;
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

            String msg = "Location " + latLng.toString();
            message(msg, 0);

            if (mLastLocation != null) {
                if (mMylocation != null) {
                    mMylocation.remove();
                }
                LatLng pickupLatlang = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                mMylocation = mMap.addMarker(new MarkerOptions().position(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude())).title("MyLocation"));
            }


            //flag to load map first time
            if (mLoadMapFlag) {
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
                mLoadMapFlag = false;
            }
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("driversAvailable");
            DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("driversWorking");
            GeoFire geoFireAvailable = new GeoFire(refAvailable);
            GeoFire geoFireWorking = new GeoFire(refWorking);

            switch (customerId) {
                case "":
                    geoFireWorking.removeLocation(userId);
                    geoFireAvailable.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));

                    break;

                default:
                    geoFireAvailable.removeLocation(userId);
                    geoFireWorking.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

//        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            return;
//        }
//        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

//    private void erasePolyLines() {
//        for (Polyline line : polylines) {
//            line.remove();
//        }
//        polylines.clear();
//    }

    private void disconnectDriver() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference availableRef = FirebaseDatabase.getInstance().getReference("driversAvailable");
        DatabaseReference workingRef = FirebaseDatabase.getInstance().getReference("driversWorking");

        GeoFire geoFireAvailbale = new GeoFire(availableRef);
        geoFireAvailbale.removeLocation(userId);

        GeoFire geoFireWorking = new GeoFire(workingRef);
        geoFireWorking.removeLocation(userId);

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isLoggingOut) {
            disconnectDriver();
        }
    }

    public void recordRide(){
        if(!customerFoundFlag){
            return;
        }
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverHistoryRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("history");
        DatabaseReference customerHistoryRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId).child("history");
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference().child("history");
        String requestID = historyRef.push().getKey();

        driverHistoryRef.child(requestID).setValue(true);
        customerHistoryRef.child(requestID).setValue(true);

        HashMap map = new HashMap();
        map.put("driver",driverId);
        map.put("customer",customerId);
        map.put("rating",0);
        map.put("destination",destination);
        map.put("location/from/lat",mPickupLocationLatLang);
        map.put("location/from/lat",mPickupLocationLatLang.latitude);
        map.put("location/from/lang",mPickupLocationLatLang.longitude);
        //Update due to disables direction api we cannot set drop location
        map.put("location/to/lat",mMylocationLatLang.latitude);
        map.put("location/to/lang",mMylocationLatLang.longitude);
        map.put("timestamp",getCurrentTimstamp());
        map.put("distance",rideDistance);
        historyRef.child(requestID).updateChildren(map);
        customerFoundFlag = false;
    }

    private Long getCurrentTimstamp() {
        return  System.currentTimeMillis()/1000;
    }

    public void message(String msg, int code) {
        if (code == 1 || debugFlag) {
//          Snackbar.make(findViewById(android.R.id.content),msg, BaseTransientBottomBar.LENGTH_SHORT).show();
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();

            Log.i("Information : ", msg);
        }
    }

    //---------------------------------------------------------------------------------------------------------------------------------------------------

}/*
In some phone map took more than minute to load functionality needed to hide button until map load
 */