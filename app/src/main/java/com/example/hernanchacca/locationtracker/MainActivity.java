package com.example.hernanchacca.locationtracker;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    // Acquire a reference to the system Location Manager
    TrackerService localTrackerService;
    Intent intent;
    private static final int LOCATION_REQUEST_CODE = 122;
    private boolean autoSend = true;
    private boolean isBound = false;
    private Button senButton ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Switch appSwitch = (Switch) findViewById(R.id.switch1);
        Switch autoSendSwitch = (Switch) findViewById(R.id.switch2);
        Switch LocalizationSwitch = (Switch) findViewById(R.id.switch3);
        senButton = (Button) findViewById(R.id.senButton);

        appSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                //Tracking switch
                if (b) {
                    restarService();
                    Toast.makeText(getApplicationContext(), "App started", Toast.LENGTH_SHORT).show();
                }
                else {
                    stopTrakingService();
                    Toast.makeText(getApplicationContext(), "App Stopped", Toast.LENGTH_SHORT).show();
                }

            }
        });

        autoSendSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                // auto send information
                if (b) {
                    localTrackerService.restarAutoSend();
                    Toast.makeText(getApplicationContext(), "Auto Send Activated", Toast.LENGTH_SHORT).show();
                    senButton.setEnabled(false);
                } else {
                    Toast.makeText(getApplicationContext(), "Auto Send Desactivated", Toast.LENGTH_SHORT).show();
                    localTrackerService.stopAutoSend();
                    senButton.setEnabled(true);
                }
            }
        });

        LocalizationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                //switch app
                if (b) {
                    localTrackerService.startUpdatingLocation();
                    Toast.makeText(getApplicationContext(), "Localization Activated", Toast.LENGTH_SHORT).show();
                } else {
                    localTrackerService.stopUpdatingLocation();
                    Toast.makeText(getApplicationContext(), "Localization Desactivated", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Ask the user for Location Permissions
        String[] PERMISSIONS = {android.Manifest.permission.ACCESS_FINE_LOCATION};
        ActivityCompat.requestPermissions(this, PERMISSIONS, LOCATION_REQUEST_CODE);
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            System.out.println("NO PERMISOS");
            return;
        }

    }

    @Override
    protected void onStop() {

        super.onStop();
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }

    }
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        switch (requestCode) {
            case LOCATION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    restarService();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }

    }

    public void stopTrakingService() {

        // Stop the tracker service
        if (isBound) {
            unbindService(connection);
            stopService(intent);
            isBound = false;
        }

    }

    public void restarService() {

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            System.out.println("NO PERMISOS");
            return;
        }

        if (intent != null)
            return;
        intent = new Intent(this, TrackerService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
        System.out.println("Restarted");

    }

    // Service Connection  that connect with the TrackerService
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            TrackerService.LocalBinder binder = (TrackerService.LocalBinder) iBinder;
            localTrackerService = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isBound = false;
        }
    };


    public void sendInfoClick(View view){
        if (autoSend) {
            Toast.makeText(getApplicationContext(), "Auto Send is already activated", Toast.LENGTH_SHORT).show();
        }
        localTrackerService.sendOutDatedInfo();
        Toast.makeText(getApplicationContext(), "Information Sent", Toast.LENGTH_SHORT).show();
    }
}
