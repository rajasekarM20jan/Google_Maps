package com.example.googlemaps;

import static com.example.googlemaps.MainActivity.checkGPSStatus;
import static com.example.googlemaps.MainActivity.isNetworkConnected;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MapPage extends AppCompatActivity {
    LocationManager locationManager;
    ImageView refreshButton,ZoomOut,ZoomIn;
    String apiKey;
    private float currentZoomLevel;
    RequestQueue requestQueue;
    private GoogleMap mMap;
    Geocoder geocoder;
    LatLng lastLatLng;
    AutocompleteSessionToken token;
    CardView listViewHolder;
    ListView placesListView;
    PlacesClient placesClient;
    EditText search_bar_text_view;
    LatLng currentLatLng;
    MapPage activity;
    PlacesAdapter adapter;
    List<String> places = new ArrayList<>();
    private FusedLocationProviderClient fusedLocationProviderClient;
    MarkerOptions markerOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_page);
        refreshButton=findViewById(R.id.refreshButton);
        ZoomIn=findViewById(R.id.ZoomIn);
        ZoomOut=findViewById(R.id.ZoomOut);
        placesListView=findViewById(R.id.placesListView);
        listViewHolder=findViewById(R.id.listViewHolder);
        search_bar_text_view=findViewById(R.id.search_bar_text_view);
        activity=this;
        geocoder=new Geocoder(this);
        apiKey="AIzaSyA6ALVPqgd1jyJ0ODOiHvdriutXitlFDLc";
        refreshButton.setOnClickListener(l->{
            search_bar_text_view.setText("");
            search_bar_text_view.clearFocus();
            init();
        });
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
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MapPage.this);
                            if (ActivityCompat.checkSelfPermission(MapPage.this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    && ActivityCompat.checkSelfPermission(MapPage.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
                                    if (location != null) {
                                        currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                                        System.out.println("currentLatLng "+currentLatLng);
                                        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                                                .findFragmentById(R.id.map);
                                        mapFragment.getMapAsync(new OnMapReadyCallback() {
                                            @Override
                                            public void onMapReady(GoogleMap googleMap) {
                                                mMap=googleMap;
                                                mMap.clear();
                                                // Add a marker at current location fetched previously and animate the camera
                                                markerOptions = new MarkerOptions();
                                                markerOptions.position(currentLatLng);
                                                markerOptions.title("You");
                                                markerOptions.draggable(true);
                                                mMap.addMarker(markerOptions);
                                                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(currentLatLng, 18);
                                                mMap.animateCamera(cameraUpdate);

                                                mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
                                                    @Override
                                                    public void onMarkerDragStart(@NonNull Marker marker) {
                                                        // Do nothing
                                                    }

                                                    @Override
                                                    public void onMarkerDrag(@NonNull Marker marker) {
                                                        // Do nothing
                                                    }

                                                    @Override
                                                    public void onMarkerDragEnd(@NonNull Marker marker) {
                                                        LatLng position = marker.getPosition();
                                                        double latitude = position.latitude;
                                                        double longitude = position.longitude;
                                                        DecimalFormat df=new DecimalFormat("#.####");
                                                        String title = "Lat: " + df.format(latitude) + ", Lng: " + df.format(longitude);
                                                        String addressLine="";
                                                        try {
                                                            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                                                            if (addresses != null && addresses.size() > 0) {
                                                                Address address = addresses.get(0);
                                                                String addressString = address.getAddressLine(0);
                                                                if (addressString != null && !addressString.isEmpty()) {
                                                                    addressLine = "(" + addressString + ")";
                                                                }
                                                            }
                                                        } catch (IOException e) {
                                                            e.printStackTrace();
                                                        }
                                                        marker.setTitle(title + addressLine);
                                                        marker.showInfoWindow();
                                                    }
                                                });
                                                search_bar_text_view.setOnKeyListener(new View.OnKeyListener() {
                                                    @Override
                                                    public boolean onKey(View view, int i, KeyEvent keyEvent) {
                                                        if(i== 66){
                                                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                                            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

                                                            mMap.clear();
                                                            placesClient = Places.createClient(MapPage.this);
                                                            token = AutocompleteSessionToken.newInstance();

                                                            FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                                                                    .setTypeFilter(TypeFilter.CITIES)
                                                                    .setTypeFilter(TypeFilter.ADDRESS)
                                                                    .setTypeFilter(TypeFilter.ESTABLISHMENT)
                                                                    .setSessionToken(token)
                                                                    .setCountries("IN")
                                                                    .setQuery(search_bar_text_view.getText().toString().trim()) // Replace with your actual search query
                                                                    .build();
                                                            placesClient.findAutocompletePredictions(request)
                                                                    .addOnSuccessListener((response) -> {
                                                                        for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
                                                                            String placeId = prediction.getPlaceId();
                                                                            // Step 2: Fetch the details of the place using the place ID
                                                                            PlacesClient placesClient2 = Places.createClient(MapPage.this);
                                                                            List<Place.Field> placeFields = Arrays.asList(Place.Field.LAT_LNG);

                                                                            FetchPlaceRequest request2 = FetchPlaceRequest.builder(placeId, placeFields).build();

                                                                            placesClient2.fetchPlace(request2)
                                                                                    .addOnSuccessListener((response2) -> {
                                                                                        Place place = response2.getPlace();

                                                                                        // Step 3: Retrieve the LatLng coordinates of the place
                                                                                        LatLng latLng = place.getLatLng();

                                                                                        markerOptions = new MarkerOptions();
                                                                                        markerOptions.position(latLng);
                                                                                        markerOptions.title(prediction.getFullText(null).toString());
                                                                                        markerOptions.draggable(true);
                                                                                        mMap.addMarker(markerOptions);
                                                                                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                                                                        builder.include(currentLatLng);
                                                                                        builder.include(latLng);
                                                                                        LatLngBounds bounds = builder.build();
                                                                                        listViewHolder.setVisibility(View.GONE);
                                                                                        placesListView.setVisibility(View.GONE);
                                                                                        int padding=100;
                                                                                        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                                                                                        mMap.animateCamera(cameraUpdate);
                                                                                    })
                                                                                    .addOnFailureListener((exception) -> {
                                                                                        // Handle error
                                                                                    });

                                                                        }

                                                                    })
                                                                    .addOnFailureListener((exception) -> {
                                                                        // Handle error
                                                                    });
                                                        }
                                                        return false;
                                                    }
                                                });

                                                search_bar_text_view.addTextChangedListener(new TextWatcher() {
                                                    @Override
                                                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                                                    }

                                                    @Override
                                                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                                                        if(search_bar_text_view.length()>3){
                                                            placesClient = Places.createClient(MapPage.this);
                                                            token = AutocompleteSessionToken.newInstance();

                                                            List<String> typesFilter=new ArrayList<>();
                                                            typesFilter.add("ADDRESS");
                                                            typesFilter.add("ESTABLISHMENT");
                                                            typesFilter.add("CITIES");

                                                            FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                                                                    .setTypeFilter(TypeFilter.CITIES)
                                                                    .setSessionToken(token)
                                                                    .setCountries("IN")
                                                                    .setQuery(search_bar_text_view.getText().toString().trim()) // Replace with your actual search query
                                                                    .build();

                                                            placesClient.findAutocompletePredictions(request)
                                                                    .addOnSuccessListener((response) -> {
                                                                        places=new ArrayList<>();
                                                                        for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {

                                                                            places.add(prediction.getFullText(null).toString());
                                                                        }
                                                                        adapter=new PlacesAdapter( MapPage.this,places,activity);
                                                                        listViewHolder.setVisibility(View.VISIBLE);
                                                                        placesListView.setVisibility(View.VISIBLE);
                                                                        placesListView.setAdapter(adapter);
                                                                        adapter.notifyDataSetChanged(); // Notify the adapter that data has changed
                                                                    })
                                                                    .addOnFailureListener((exception) -> {
                                                                        // Handle error
                                                                    });
                                                        }else{
                                                            listViewHolder.setVisibility(View.GONE);
                                                        }
                                                    }

                                                    @Override
                                                    public void afterTextChanged(Editable editable) {

                                                    }
                                                });

                                            }
                                        });
                                    }
                                });
                            }else{
                                init();
                            }
                        }
                    });
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

    public void clearMap(){
        mMap.clear();
    }

    public void getDirectionsOverListClick(String placeID,String placeName){
        if(isNetworkConnected(MapPage.this)){
            if(checkGPSStatus(MapPage.this)){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        search_bar_text_view.clearFocus();
                        listViewHolder.setVisibility(View.GONE);
                        PlacesClient placesClient = Places.createClient(MapPage.this);
                        List<Place.Field> placeFields = Arrays.asList(Place.Field.LAT_LNG);
                        FetchPlaceRequest request = FetchPlaceRequest.builder(placeID, placeFields).build();
                        placesClient.fetchPlace(request)
                                .addOnSuccessListener((response) -> {
                                    Place place = response.getPlace();
                                    LatLng destinationLatLng = place.getLatLng();
                                    mMap.clear();
                                    // Add a marker to the map at the selected destination
                                    mMap.addMarker(markerOptions);
                                    mMap.addMarker(new MarkerOptions()
                                            .position(currentLatLng)
                                            .title("You"));
                                    mMap.addMarker(new MarkerOptions()
                                            .position(destinationLatLng)
                                            .title(placeName));
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

                                    JsonObjectRequest jRequest = new JsonObjectRequest(Request.Method.GET, requestUrl, null,
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
                                    requestQueue.add(jRequest);

                                })
                                .addOnFailureListener((exception) -> {
                                    // Handle error
                                });
                    }
                });
            }
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