package com.example.sdy60_ge5;

import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.provider.SyncStateContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import android.location.LocationManager;


import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.maps.android.SphericalUtil;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.Manifest;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.ToggleButton;

//import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;


public class TrackActivity extends FragmentActivity implements OnMapReadyCallback {

    //
    private static final int PERMISSIONS_REQUEST = 1;
    private static final String TAG = TrackActivity.class.getSimpleName();
    private HashMap<String, Marker> mMarkers = new HashMap<>();
    double distance, roundDistance;  // path walked
    String distanceString;
    ArrayList<LatLng> tempcoordList = new ArrayList<LatLng>(); // LatLngs of locations reeived


    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        distance = 0;
        roundDistance=0;
        distanceString=null;
        tempcoordList.clear();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        final Button startBtn = findViewById(R.id.StartBtn);
        final Button stopBtn = findViewById(R.id.StopBtn);

        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Please enable location services", Toast.LENGTH_SHORT).show();
            //finish();
        }
        // Check location permission is granted - if it is, start
        // the service, otherwise request the permission
        final int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        mMap.setMaxZoomPreference(16);
        loginToFirebase();

        startBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (permission == PackageManager.PERMISSION_GRANTED) {
                    startTrackerService();

                } else {
                    ActivityCompat.requestPermissions( TrackActivity.this ,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            PERMISSIONS_REQUEST);
                }
            }
        });

        final AlertDialog.Builder builderPedestrian = new AlertDialog.Builder(this);
        final AlertDialog.Builder builderWheelchair = new AlertDialog.Builder(this);
        final AlertDialog.Builder builderKlms = new AlertDialog.Builder(this);
        final AlertDialog.Builder builderExit = new AlertDialog.Builder (this);
        final TextView tView = new TextView(this);
        final TextView tView2 = new TextView(this);

        // STOP button functionality
        stopBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                PolylineOptions polylineOptions = new PolylineOptions();

                GlobalArr ga = (GlobalArr) getApplication();

                tempcoordList=ga.getCoordList();

                polylineOptions.addAll(tempcoordList);
                polylineOptions
                        .width(5)
                        .color(Color.RED);

                mMap.addPolyline(polylineOptions);

                String[] rates={"1.Εύκολο","2.Προσβάσιμο","3.Μέτριο","4.Δύσκολο","5.Επικίνδυνο"};


                tView.setTextSize(18);
                tView.setText("Πώς κρίνετε τη δυσκολία του μονοπατιού για ΠΕΖΟΥΣ;");

                builderPedestrian.setCustomTitle(tView)
                        .setItems(rates, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // User cancelled the dialog
                                }
                        });

                tView2.setTextSize(18);
                tView2.setText("Πώς κρίνετε τη δυσκολία του μονοπατιού για ΚΑΡΟΤΣΑΚΙΑ;");


                builderWheelchair.setCustomTitle(tView2)
                        .setItems(rates, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User cancelled the dialog
                            }
                        });


                distance = SphericalUtil.computeLength(tempcoordList);
                roundDistance = Math.round(distance);
                distanceString = String.valueOf(roundDistance);

               builderKlms.setTitle("Διασχίσατε απόσταση "+distanceString+" μέτρων")
                        .setMessage("Κερδίσατε αντίστοιχους πόντους")
                        .setPositiveButton("OK thanks", new DialogInterface.OnClickListener() {
                           public void onClick(DialogInterface dialog, int id) {

                          }
                      });

                builderExit.setMessage("Θέλετε να κλείσετε την εφαρμογή?")
                        .setCancelable(false)
                        .setPositiveButton("ΝΑΙ", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                stopTrackerService();
                                distance = 0;
                                finish();
                            }
                        })
                        .setNegativeButton("ΟΧΙ", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                distance = 0;
                                dialog.cancel();
                            }
                        });

                AlertDialog dialog4 = builderExit.create();
                dialog4.show();

                AlertDialog dialog1 = builderPedestrian.create();
                dialog1.show();

                AlertDialog dialog2 = builderWheelchair.create();
                dialog2.show();

               AlertDialog dialog3 = builderKlms.create();
                dialog3.show();

                }
        });
    }

    private void startTrackerService() {
        startService(new Intent(this, TrackerService.class));
        //finish();
    }

    private void stopTrackerService() {
        stopService(new Intent(this, TrackerService.class));
        //finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[]
            grantResults) {
        if (requestCode == PERMISSIONS_REQUEST && grantResults.length == 1
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Start the service when the permission is granted
            startTrackerService();
        } else {
            finish();
        }
    }

    private void loginToFirebase() {
        String email = getString(R.string.firebase_email);
        String password = getString(R.string.firebase_password);
        // Authenticate with Firebase and subscribe to updates
        FirebaseAuth.getInstance().signInWithEmailAndPassword(
                email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    subscribeToUpdates();
                    Log.d(TAG, "firebase auth success");
                } else {
                    Log.d(TAG, "firebase auth failed");
                }
            }
        });
    }

    private void subscribeToUpdates() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(getString(R.string.firebase_path));
        ref.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                setMarker(dataSnapshot);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                setMarker(dataSnapshot);
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.d(TAG, "Failed to read value.", error.toException());
            }
        });
    }

    private void setMarker(DataSnapshot dataSnapshot) {
        // When a location update is received, put or update
        // its value in mMarkers, which contains all the markers
        // for locations received, so that we can build the
        // boundaries required to show them all on the map at once

        String key = dataSnapshot.getKey();
        HashMap<String, Object> value = (HashMap<String, Object>) dataSnapshot.getValue();
        double lat = Double.parseDouble(value.get("latitude").toString());
        double lng = Double.parseDouble(value.get("longitude").toString());
        LatLng location = new LatLng(lat, lng);

        if (!mMarkers.containsKey(key)) {
            mMarkers.put(key, mMap.addMarker(new MarkerOptions().title(key).position(location)));
        } else {
            mMarkers.get(key).setPosition(location);
        }
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Marker marker : mMarkers.values()) {
            builder.include(marker.getPosition());
        }
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 300));
    }
}
