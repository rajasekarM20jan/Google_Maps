package com.example.googlemaps;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.ArrayList;
import java.util.List;

public class PlacesAdapter extends ArrayAdapter<String> {
    Context context;
    MapPage activity;
    AutocompleteSessionToken token;
    // Constructor
    public PlacesAdapter(Context context, List<String> places,MapPage activity) {
        super(context, 0, places);
        this.context=context;
        this.activity=activity;
    }

    // Override getView() to bind data to the views
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Inflate the list item layout
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.custom_list_place, parent, false);
        }

        // Get the current place object
        String place = getItem(position);

        // Bind the data to the views
        TextView textView = convertView.findViewById(R.id.textInputPlace);
        textView.setText(place);

        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activity.clearMap();
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    PlacesClient placesClient= Places.createClient(context);
                    token = AutocompleteSessionToken.newInstance();

                    FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                            .setTypeFilter(TypeFilter.CITIES)
                            .setSessionToken(token)
                            .setCountries("IN")
                            .setQuery(textView.getText().toString().trim()) // Replace with your actual search query
                            .build();
                    placesClient.findAutocompletePredictions(request)
                            .addOnSuccessListener((response) -> {
                                String placeId= response.getAutocompletePredictions().get(0).getPlaceId();
                                String placeName= response.getAutocompletePredictions().get(0).getFullText(null).toString();
                                activity.getDirectionsOverListClick(placeId,placeName);
                            })
                            .addOnFailureListener((exception) -> {
                                // Handle error
                            });
            }
        });

        return convertView;
    }
}