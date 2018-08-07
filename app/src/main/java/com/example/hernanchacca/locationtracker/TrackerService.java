package com.example.hernanchacca.locationtracker;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by hernanchacca on 11/29/17.
 */

public class TrackerService extends Service {

    int mStartMode;       // indicates how to behave if the service is killed
    IBinder mBinder = new LocalBinder();      // interface for clients that bind
    boolean mAllowRebind; // indicates whether onRebind should be used

    // Acquire a reference to the system Location Manager
    LocationManager locationManager ;
    LocationListener locationListener;
    private static final int LOCATION_REQUEST_CODE = 122;
    private boolean autoSend = true;
    private  boolean locationCheched = true;
    private String FILENAME = "locationdb";



    @Override
    public void onCreate() {

        // The service is being created
        // Locacion Manager is Created
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                if (autoSend)
                    sendInformation(location);
                else
                    saveInformation(location);

            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };

        if (locationCheched)
            startUpdatingLocation();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // The service is starting, due to a call to startService()
        startUpdatingLocation();
        return START_NOT_STICKY;

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return mBinder;

    }

    @Override
    public boolean onUnbind(Intent intent) {

        // All clients have unbound with unbindService()
        return mAllowRebind;

    }
    @Override
    public void onRebind(Intent intent) {

        // A client is binding to the service with bindService(),
        // after onUnbind() has already been called

    }
    @Override
    public void onDestroy() {

        // The service is no longer used and is being destroyed
        super.onDestroy();
        System.out.print("Destroyed");

    }

    // Send location information to a heroku app that show the data in a plataform
    // via RestFull API
    public void  sendInformation(Location location) {

        Map<String, String> obj = new HashMap<String, String>();
        JSONObject jsonObj  = null;

        try {

            obj.put("Lat", location.getLatitude() + "");
            obj.put("Alt", location.getAltitude() + "");
            obj.put("lon", location.getLongitude() + "");
            obj.put("spe", location.getSpeed() + "");
            obj.put("year", Calendar.getInstance().get(Calendar.YEAR) + "");
            obj.put("month", Calendar.getInstance().get(Calendar.MONTH) + "");
            obj.put("day", Calendar.getInstance().get(Calendar.DAY_OF_MONTH) + "");
            obj.put("hour", Calendar.getInstance().get(Calendar.HOUR) + "");
            jsonObj = new JSONObject(obj);

        } catch (Exception e) {
            // Somthing went wrong
        }

        sendPostRequest("https://radiant-atoll-24808.herokuapp.com/api/location/", jsonObj);
        System.out.println("Service Sent:" + location.getLatitude());

    }

    // If auto send is desactivated, location information is saved
    // until the user press send information.
    public void saveInformation(Location location) {
        Map<String, String> obj = new HashMap<String, String>();
        JSONObject jsonObj  = null;

        try {

            obj.put("Lat", location.getLatitude() + "");
            obj.put("Alt", location.getAltitude() + "");
            obj.put("lon", location.getLongitude() + "");
            obj.put("spe", location.getSpeed() + "");
            obj.put("year", Calendar.getInstance().get(Calendar.YEAR) + "");
            obj.put("month", Calendar.getInstance().get(Calendar.MONTH) + "");
            obj.put("day", Calendar.getInstance().get(Calendar.DAY_OF_MONTH) + "");
            obj.put("hour", Calendar.getInstance().get(Calendar.HOUR) + "");
            jsonObj = new JSONObject(obj);

        } catch (Exception e) {
            // Something went wrong
        }


        try {
            FileOutputStream fos = openFileOutput(FILENAME, Context.MODE_APPEND);
            fos.write(obj.toString().getBytes());
            fos.close();
        } catch (Exception e) {

        }

        // Saved locally to be sent later
        System.out.println("Service Saved:" + location.getLatitude());

    }

    // Start Location listener
    public void startUpdatingLocation() {

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            System.out.println("Service says NO PERMISOS");
            return;
        }

        // Register the listener with the Location Manager to receive location updates
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);

        //locationManager.removeUpdates(locationListener);
        System.out.println("Hola desde Servicio");
        locationCheched = true;
    }

    // Stop listening the location changes
    public void stopUpdatingLocation() {
        locationManager.removeUpdates(locationListener);
        locationCheched = false;
    }

    // Restart the auto send information
    public void restarAutoSend() {
        autoSend = true;
    }

    // Stop auto send information
    public void stopAutoSend() {
        autoSend = false;
    }

    // Init the Local Binder
    public class LocalBinder extends Binder {
        TrackerService getService() {
            return TrackerService.this;
        }
    }

    // Send all the information savend when auto send was desactivated
    public  void sendOutDatedInfo() {
        JSONObject jsonObj = null;
        //send saved information
        String string = "hello world!";

        try {

            FileInputStream fis = openFileInput(FILENAME);
            FileChannel fc = fis.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            /* Instead of using default, pass in a decoder. */
            fis.close();
            String jString = Charset.defaultCharset().decode(bb).toString();
            jsonObj = new JSONObject(jString);

            FileOutputStream fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);
            fos.write("".getBytes());
            fos.flush();

        } catch (Exception e) {

        }

        sendPostRequest("https://radiant-atoll-24808.herokuapp.com/api/location/", jsonObj);

    }

    // Post request wrapper
    public void sendPostRequest(String url, JSONObject jsonObj) {

        //String url = "https://radiant-atoll-24808.herokuapp.com/api/videos";
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                // Succesfull respponse
                System.out.print("Succesfull");
            }
        },
        new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // Error Response
                System.out.print(error.getMessage());
            }
        });

        // Access the RequestQueue through your singleton class.
        MySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest);

    }

}
