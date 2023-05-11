package com.example.googlemaps;

import static com.example.googlemaps.MainActivity.checkGPSStatus;
import static com.example.googlemaps.MainActivity.isNetworkConnected;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.Status;
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
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapPage extends AppCompatActivity {
    LocationManager locationManager;
    ImageView refreshButton,ZoomOut,ZoomIn;
    String apiKey;
    private float currentZoomLevel;
    RequestQueue requestQueue;
    private GoogleMap mMap;
    AutocompleteSupportFragment autocompleteFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_page);
        refreshButton=findViewById(R.id.refreshButton);
        ZoomIn=findViewById(R.id.ZoomIn);
        ZoomOut=findViewById(R.id.ZoomOut);
        apiKey="AIzaSyA6ALVPqgd1jyJ0ODOiHvdriutXitlFDLc";
        refreshButton.setOnClickListener(l->init());
        ZoomOut.setOnClickListener(l->{
            currentZoomLevel = mMap.getCameraPosition().zoom - 1f;
            mMap.moveCamera(CameraUpdateFactory.zoomTo(currentZoomLevel));
        });
        ZoomIn.setOnClickListener(l->{
            currentZoomLevel = mMap.getCameraPosition().zoom + 1f;
            mMap.moveCamera(CameraUpdateFactory.zoomTo(currentZoomLevel));
        });
        requestQueue = Volley.newRequestQueue(this);
        init();
        if (!Places.isInitialized()) {

            Places.initialize(getApplicationContext(), apiKey);
        }
    }

    void init() {
        try {
            if (isNetworkConnected(this)) {
                if (checkGPSStatus(this)) {
                    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    Criteria criteria = new Criteria();
                    if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                                .findFragmentById(R.id.map);
                        mapFragment.getMapAsync(new OnMapReadyCallback() {
                            @Override
                            public void onMapReady(GoogleMap googleMap) {
                                mMap=googleMap;
                                mMap.clear();
                                // Add a marker at current location fetched previously and animate the camera
                                MarkerOptions markerOptions = new MarkerOptions();
                                markerOptions.position(currentLatLng);
                                markerOptions.title("You");
                                mMap.addMarker(markerOptions);
                                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(currentLatLng, 18);
                                mMap.animateCamera(cameraUpdate);

                                autocompleteFragment =
                                        (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);
                                assert autocompleteFragment != null;
                                autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
                                autocompleteFragment.setCountries("IN"); // Restrict to the INDIA
                                autocompleteFragment.setTypeFilter(TypeFilter.ADDRESS); // Only return ADDRESS
                                autocompleteFragment.setTypeFilter(TypeFilter.CITIES); // Only return CITIES
                                autocompleteFragment.setHint("Enter a location");
                                autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                                    @Override
                                    public void onPlaceSelected(@NonNull Place place) {
                                        // Handle the selected place
                                        LatLng destinationLatLng = place.getLatLng();
                                        mMap.clear();
                                        // Add a marker to the map at the selected destination
                                        mMap.addMarker(markerOptions);
                                        mMap.addMarker(new MarkerOptions()
                                                .position(destinationLatLng)
                                                .title(place.getName()));
                                        // Move the camera to the selected destination
                                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                        builder.include(currentLatLng);
                                        builder.include(destinationLatLng);
                                        LatLngBounds bounds = builder.build();
                                        int padding=100;
                                        CameraUpdate cameraUpdate2 = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                                        mMap.animateCamera(cameraUpdate2);
                                        String origin = currentLatLng.latitude + "," + currentLatLng.longitude;
                                        String destination = destinationLatLng.latitude + "," + destinationLatLng.longitude;
                                        String requestUrl = "https://maps.googleapis.com/maps/api/directions/json?origin="
                                                + origin + "&destination=" + destination + "&key=" + apiKey;

                                        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, requestUrl, null,
                                                new Response.Listener<JSONObject>() {
                                                    @Override
                                                    public void onResponse(JSONObject response) {
                                                        // Parse the response to extract the polylines information
                                                        String encodedPolyline = "";
                                                        try {
                                                            JSONObject route = response.getJSONArray("routes").getJSONObject(0);
                                                            JSONObject polyline = route.getJSONObject("overview_polyline");
                                                            encodedPolyline = polyline.getString("points");
                                                        } catch (JSONException e) {
                                                            e.printStackTrace();
                                                        }

                                                        // Draw the polylines on the map using the Polyline class
                                                        List<LatLng> decodedPolyline = decodePolyline(encodedPolyline);
                                                        PolylineOptions polylineOptions = null;

                                                        polylineOptions = new PolylineOptions()
                                                                .addAll(decodedPolyline)
                                                                .color(Color.BLUE)
                                                                .width(10);

                                                        mMap.addPolyline(polylineOptions);
                                                    }
                                                },
                                                new Response.ErrorListener() {
                                                    @Override
                                                    public void onErrorResponse(VolleyError error) {
                                                        Log.e(null, "Directions request failed.");
                                                    }
                                                });
                                        //used for adding the JsonObject Request to Volley
                                        requestQueue.add(request);

                                    }

                                    @Override
                                    public void onError(@NonNull Status status) {
                                        // Handle error
                                    }
                                });


                            }
                        });
                    }else{
                        MainActivity m=new MainActivity();
                        m.requestPermissions();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            AlertDialog.Builder a=new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setMessage("Oops! Something went wrong. Please Try Again after sometime")
                    .setNegativeButton("OK", (dialog, which) -> {
                        finishAffinity();
                    });
            a.show();
        }
    }

    //Method to decode the encoded polyline from API response
    private List<LatLng> decodePolyline(String encodedPolyline) {
        List<LatLng> points = new ArrayList<>();
        int index = 0, len = encodedPolyline.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;

            do {
                b = encodedPolyline.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);

            int dLat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dLat;

            shift = 0;
            result = 0;

            do {
                b = encodedPolyline.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);

            int dLng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dLng;

            LatLng point = new LatLng((double) lat / 1E5, (double) lng / 1E5);
            points.add(point);
        }

        return points;
    }


    @Override
    public void onBackPressed() {
        finishAffinity();
    }
}