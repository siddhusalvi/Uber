package com.siddhu.uber;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
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
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    String destination = "not available";
    String userId;
    GeoQuery geoQuery;
    RatingBar mRatingBar;
    private GoogleMap mMap;
    private Button mLogout, mRequest, mSettings,mHistory,mSaveRide;
    private LatLng pickupLocation;
    private Boolean requestBol = false;
    private Marker pickupMarker;
    public boolean mLoadMapFlag = false;
    public boolean mButtonflag = false;
    private int radius = 1;
    private Boolean driverFound = false;
    private String driverFoundID;
    private Marker mDriverMarker;
    String DEFAULT = "Not Available";
    private String mDriverNameField = "Name : "+DEFAULT;
    private String mDriverCarField = "CAR : " + DEFAULT;
    private int mDriverPhoneField = 0;

    DatabaseReference mCustomerDatabaseRef;
    GeoFire geoFireCustomerRequest;


    private DatabaseReference driverLocationRef;
    private ValueEventListener driverLocationRefListener;

    private boolean debugFlag = true;


    private LinearLayout mDriverInfo;
    private LinearLayout mButtonLayout,mSaveLayout;
    private ImageView mDriverProfileImage;
    private TextView mDriverName, mDriverPhone, mDriverCar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Snackbar.make(findViewById(android.R.id.content),"Please wait a while map is loading :)", BaseTransientBottomBar.LENGTH_LONG).show();


        // Initialize the SDK
//        Places.initialize(getApplicationContext(),"dfasgsdgsdfgsdgsdghsdgsa");
//        // Create a new PlacesClient instance
//        PlacesClient placesClient = Places.createClient(this);
//
//        // Initialize the AutocompleteSupportFragment.
//        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
//                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);
//
//        // Specify the types of place data to return.
//        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME));
//
//        // Set up a PlaceSelectionListener to handle the response.
//        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
//            @Override
//            public void onPlaceSelected(@NotNull Place place) {
//                // TODO: Get info about the selected place.
//                destination = place.getName().toString();
////                Log.i(TAG, "Place: " + place.getName() + ", " + place.getId());
//            }
//
//            @Override
//            public void onError(@NotNull Status status) {
//                // TODO: Handle the error.
////                Log.i(TAG, "An error occurred: " + status);
//                Toast.makeText(getApplicationContext(),status.toString(),Toast.LENGTH_LONG).show();
//            }
//        });

        mButtonLayout = findViewById(R.id.buttonLayout);
        mSaveLayout = findViewById(R.id.saveButtonLayout);


        mLogout = findViewById(R.id.logout);
        mRequest = findViewById(R.id.request);
        mSettings = findViewById(R.id.settings);
        mHistory = findViewById(R.id.history);


        mDriverInfo = findViewById(R.id.driverInfo);
        mDriverProfileImage = findViewById(R.id.driverProfileImage);
        mDriverName = findViewById(R.id.driverName);
        mDriverPhone = findViewById(R.id.driverPhone);
        mDriverCar = findViewById(R.id.driverCar);
        mSaveRide = findViewById(R.id.completeRide);

        mRatingBar = findViewById(R.id.rating_bar);


        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(CustomerMapActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        mRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (requestBol) {
                    requestBol = false;
                    geoQuery.removeAllListeners();
                    if (driverLocationRef != null) {
                        driverLocationRef.removeEventListener(driverLocationRefListener);
                    }

                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("customerRequest");
                    if (ref != null) {
                        GeoFire geoFire = new GeoFire(ref);
                        geoFire.removeLocation(userId);
                    }

                    if (driverFoundID != null) {
                        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID).child("ActiveCustomer");;
                        driverRef.removeValue();
                        driverFoundID = null;
                    }
                    driverFound = false;
                    radius = 1;
                    if (pickupMarker != null) {
                        pickupMarker.remove();
                    }

                    if (mDriverMarker != null) {
                        mDriverMarker.remove();
                    }

                    mDriverInfo.setVisibility(View.GONE);
                    mDriverName.setText("");
                    mDriverCar.setText("");
                    mDriverPhone.setText("");
                    mDriverProfileImage.setImageResource(R.mipmap.ic_default_user);


                    mRequest.setText("call uber");


                } else {
                    requestBol = true;
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();


                    mCustomerDatabaseRef = FirebaseDatabase.getInstance().getReference().child("customerRequest");
                    geoFireCustomerRequest = new GeoFire(mCustomerDatabaseRef);
                    geoFireCustomerRequest.setLocation(userId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));

                    pickupLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    pickupMarker = mMap.addMarker(new MarkerOptions().position(pickupLocation).title("Pickup Here"));
//                            .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickupmarker)) );

                    mRequest.setText("Getting your Driver....");

                    getClosestDriver();
                }
            }
        });

        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CustomerMapActivity.this, CustomerSettingsActivity.class);
                startActivity(intent);
            }
        });

        mHistory.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(CustomerMapActivity.this,HistoryActivity.class);
                        intent.putExtra("customerOrDriver","Customers");
                        startActivity(intent);
                    }
                }
        );


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
    protected void onResume() {
        super.onResume();
        if (requestBol) {
            getClosestDriver();
        }

        if (mLoadMapFlag) {
            String msg = "onResume";
            message(msg,0);
            buildGoogleApiClient();
            onLocationChanged(mLastLocation);
        }
    }

    private void getClosestDriver() {
        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference().child("driversAvailable");
        GeoFire geoFire = new GeoFire(driverLocation);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickupLocation.latitude, pickupLocation.longitude), radius);

        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!driverFound && requestBol) {
                    driverFound = true;
                    driverFoundID = key;

                    DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID).child("ActiveCustomer");
                    String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    HashMap map = new HashMap();
                    map.put("customerRideId", customerId);
                    map.put("destination", destination);
                    driverRef.updateChildren(map);
                    mRequest.setText("Looking for Driver Location....");
                    getDriverLocation();
                    getDriverInfo();

                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (!driverFound) {
                    radius++;
                    getClosestDriver();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                message(error.getMessage(),0);
            }
        });
    }

    private void getDriverLocation() {

        driverLocationRef = FirebaseDatabase.getInstance().getReference().child("driversWorking").child(driverFoundID).child("l");
        driverLocationRefListener = driverLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && requestBol) {
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    mRequest.setText("Driver Found");
                    if (map.get(0) != null) {
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1) != null) {
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng driverLatLng = new LatLng(locationLat, locationLng);
                    if (mDriverMarker != null) {
                        mDriverMarker.remove();
                    }
                    Location loc1 = new Location("");
                    loc1.setLatitude(pickupLocation.latitude);
                    loc1.setLongitude(pickupLocation.longitude);

                    Location loc2 = new Location("");
                    loc2.setLatitude(driverLatLng.latitude);
                    loc2.setLongitude(driverLatLng.longitude);

                    float distance = loc1.distanceTo(loc2);

                    if (distance < 100) {
                        mRequest.setText("Driver Arrived : " + distance);
                    } else {
                        mRequest.setText("Driver Found : " + distance);

                    }
                    mDriverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("your driver"));
//                            .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_truck)));
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

    }

    private void getDriverInfo() {
        mDriverInfo.setVisibility(View.VISIBLE);
        DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID);
        mCustomerDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                    if (map.get("name") != null) {
                        mDriverNameField = map.get("name").toString();
                    }
                    if (map.get("phone") != null) {
                        mDriverPhoneField = Integer.valueOf(map.get("phone").toString());
                    }
                    if (map.get("car") != null) {
                        mDriverCarField = map.get("car").toString();
                    }
                    if (map.get("profileImageUrl") != null) {
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(mDriverProfileImage);
                    }else{
                        mDriverProfileImage.setImageResource(R.mipmap.ic_default_driver);
                    }

                    int ratingSum = 0;
                    int ratingsTotal = 0;
                    for(DataSnapshot child : snapshot.child("rating").getChildren()){
                        ratingSum += Integer.valueOf(child.getValue().toString());
                        ratingsTotal += 1;
                    }

                    if(ratingsTotal != 0) {
                        mRatingBar.setRating((float)ratingSum/ratingsTotal);
                    }

                    mDriverName.setText(mDriverNameField);
                    mDriverPhone.setText(String.valueOf(mDriverPhoneField));
                    mDriverCar.setText(mDriverCarField);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public void onLocationChanged(Location location) {
        if (getApplicationContext() != null) {
            mLastLocation = location;
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            if (requestBol) {
                geoFireCustomerRequest.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
            }
            if (!mLoadMapFlag) {
                message("Updating map",0);
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(10));
                mLoadMapFlag = true;
            }


            if(mLastLocation != null && !mButtonflag){
                mButtonLayout.setVisibility(View.VISIBLE);
                mButtonflag = true;

            }

        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    protected void onStop() {
        DatabaseReference mCustomerDatabaseRef = FirebaseDatabase.getInstance().getReference().child("customerRequest");
        GeoFire geoFireCustomerRequest = new GeoFire(mCustomerDatabaseRef);
        geoFireCustomerRequest.removeLocation(userId);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    public void message(String msg,int code) {
        if(code == 1 || debugFlag){
//          Snackbar.make(findViewById(android.R.id.content),msg, BaseTransientBottomBar.LENGTH_SHORT).show();
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            Log.i("Information : ",msg);
        }
    }
}
