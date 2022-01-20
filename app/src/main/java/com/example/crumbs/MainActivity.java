package com.example.crumbs;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.List;


public class MainActivity extends AppCompatActivity {

    public static final int DEFAULT_UPDATE_INTERVAL = 2;
    public static final int FAST_UPDATE_INTERVAL = 5;
    public static final int PERMISSION_FINE_LOCATION = 99;

    TextView tv_lat, tv_lon, tv_altitude, tv_speed, tv_address, tv_CrumbsCount;
    Button btn_update;
    Button btn_newCrumb, btn_showCrumbs, btn_mapCrumbs;
    boolean updateOn = false;

    //current location
    Location currentLocation;

    //list of saved locations
    List<Location> savedLocations;

    //location request is a config file for all settings related to FusedLocationProviderClient
    LocationRequest locationRequest;
    LocationCallback locationCallBack;

    //googles api for location services
    FusedLocationProviderClient fusedLocationProviderClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //variable ID
        tv_lat = findViewById(R.id.tv_lat);
        tv_lon = findViewById(R.id.tv_lon);
        tv_altitude = findViewById(R.id.tv_altitude);
        tv_speed = findViewById(R.id.tv_speed);
        tv_address = findViewById(R.id.tv_address);
        tv_CrumbsCount = findViewById(R.id.tv_CrumbsCount);
        btn_update = findViewById(R.id.btn_update);
        btn_newCrumb = findViewById(R.id.btn_newCrumb);
        btn_mapCrumbs = findViewById(R.id.btn_mapCrumbs);

        //set properties of locationrequest
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000 * DEFAULT_UPDATE_INTERVAL);
        locationRequest.setFastestInterval(1000 * FAST_UPDATE_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        //this is triggered whenever the update interval is met
        locationCallBack = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                //save the location
                updateUIValues(locationResult.getLastLocation());
            }
        };


        btn_newCrumb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //get the gps location
                //add new location to the list
                MyApplication myApplication = (MyApplication) getApplicationContext();
                savedLocations = myApplication.getMyLocations();
                savedLocations.add(currentLocation);

            }
        });


        btn_mapCrumbs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Integer.valueOf(String.valueOf(tv_CrumbsCount.getText())) == 0) {
                    Toast.makeText(MainActivity.this, "No crumbs have been dropped!", Toast.LENGTH_LONG).show();
                    finish();
                }

                Intent i = new Intent(MainActivity.this, MapsActivity.class);
                startActivity(i);
            }
        });


        btn_update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateGPS();
                startLocationUpdates();
            }
        });

        updateGPS();
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallBack, null);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSION_FINE_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    updateGPS();
                } else {
                    Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
    }

    private void updateGPS() {
        //get perms from user to track gps
        //get current location from fused client
        //update the UI

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //user has granted the permission
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    //we got permissions, update the ui
                    updateUIValues(location);
                    currentLocation = location;
                }
            });
        } else {
            //user has denied
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FINE_LOCATION);
            }
        }
    }

    private void updateUIValues(Location location) {
        //update the ui textview
        tv_lat.setText(String.valueOf(location.getLatitude()));
        tv_lon.setText(String.valueOf(location.getLongitude()));
        if (location.hasAltitude()) {
            tv_altitude.setText(String.valueOf(location.getAltitude()));
        } else {
            tv_altitude.setText("Not Available");
        }

        if (location.hasSpeed()) {
            tv_speed.setText(String.valueOf(location.getSpeed()));
        } else {
            tv_speed.setText("Not Available");
        }

        Geocoder geocoder = new Geocoder(MainActivity.this);

        try {
            List<Address> addressList = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            tv_address.setText(addressList.get(0).getAddressLine(0));

        } catch (Exception e) {
            tv_address.setText("Unable to fetch address");
        }

        MyApplication myApplication = (MyApplication) getApplicationContext();
        savedLocations = myApplication.getMyLocations();
        //show the number of crumbs
        tv_CrumbsCount.setText(Integer.toString(savedLocations.size()));


    }

}
