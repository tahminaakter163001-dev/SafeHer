
package com.example.safeher;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.ArrayList;
import java.util.List;

public class EmergencyPlacesActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_CODE = 200;
    private static final long GEOCODING_DELAY = 300;

    private FusedLocationProviderClient fusedLocationClient;

    private TextView locationStatus;
    private TextView emptyMessage;
    private ProgressBar progressBar;
    private RecyclerView placesRecyclerView;
    private Button retryButton;

    private ArrayList<EmergencyPlace> nearbyPlaces;
    private EmergencyPlacesAdapter placesAdapter;

    private final Handler geocodingHandler =
            new Handler(Looper.getMainLooper());

    private boolean activityDestroyed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_places);

        locationStatus = findViewById(R.id.locationStatus);
        emptyMessage = findViewById(R.id.emptyMessage);
        progressBar = findViewById(R.id.progressBar);
        placesRecyclerView = findViewById(R.id.placesRecyclerView);
        retryButton = findViewById(R.id.retryButton);

        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        placesRecyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );

        retryButton.setOnClickListener(v -> checkLocationPermission());

        checkLocationPermission();
    }

    private void checkLocationPermission() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED) {

            getCurrentLocation();

        } else {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_CODE
            );
        }
    }

    private void getCurrentLocation() {

        showLoading();

        try {

            // First try to get the last known location.
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {

                        if (location != null) {

                            fetchEmergencyPlaces(
                                    location.getLatitude(),
                                    location.getLongitude()
                            );

                        } else {

                            requestFreshLocation();
                        }
                    })
                    .addOnFailureListener(e ->
                            requestFreshLocation()
                    );

        } catch (SecurityException e) {

            showError("Location permission is required.");
        }
    }

    private void requestFreshLocation() {

        if (activityDestroyed) return;

        try {

            fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
            ).addOnSuccessListener(location -> {

                if (activityDestroyed) return;

                if (location != null) {

                    fetchEmergencyPlaces(
                            location.getLatitude(),
                            location.getLongitude()
                    );

                } else {

                    showError(
                            "Unable to get your location. Please try again."
                    );
                }

            }).addOnFailureListener(e -> {

                if (!activityDestroyed) {
                    showError("Location error: " + e.getMessage());
                }
            });

        } catch (SecurityException e) {

            showError("Location permission is required.");
        }
    }

    private void fetchEmergencyPlaces(
            double latitude,
            double longitude
    ) {

        if (activityDestroyed) return;

        locationStatus.setText(
                "Searching emergency services within 3 km..."
        );

        EmergencyPlacesApi.fetchNearbyPlaces(
                latitude,
                longitude,
                new EmergencyPlacesApi.PlacesCallback() {

                    @Override
                    public void onSuccess(List<EmergencyPlace> places) {

                        if (activityDestroyed) return;

                        progressBar.setVisibility(View.GONE);
                        retryButton.setVisibility(View.GONE);

                        if (places.isEmpty()) {

                            placesRecyclerView.setAdapter(null);

                            emptyMessage.setText(
                                    "No emergency services found within 3 km."
                            );

                            emptyMessage.setVisibility(View.VISIBLE);

                            locationStatus.setText(
                                    "Search completed."
                            );

                        } else {

                            emptyMessage.setVisibility(View.GONE);

                            nearbyPlaces = new ArrayList<>(places);

                            placesAdapter =
                                    new EmergencyPlacesAdapter(nearbyPlaces);

                            placesRecyclerView.setAdapter(placesAdapter);

                            locationStatus.setText(
                                    "Found " + places.size()
                                            + " emergency services nearby."
                            );

                            // Load missing addresses in the background.
                            geocodeMissingAddresses(0);
                        }
                    }

                    @Override
                    public void onError(String error) {

                        if (!activityDestroyed) {
                            showError(
                                    "Unable to load nearby places: " + error
                            );
                        }
                    }
                }
        );
    }

    private void geocodeMissingAddresses(int startIndex) {

        if (activityDestroyed || nearbyPlaces == null) return;

        int index = startIndex;

        while (index < nearbyPlaces.size()) {

            String address = nearbyPlaces.get(index).getAddress();

            if (address == null || address.trim().isEmpty()) {
                break;
            }

            index++;
        }

        if (index >= nearbyPlaces.size()) {

            locationStatus.setText(
                    "Emergency services loaded: " + nearbyPlaces.size()
            );

            return;
        }

        final int currentIndex = index;

        EmergencyPlace place = nearbyPlaces.get(currentIndex);

        GeocodingHelper.getAddress(
                place.getLatitude(),
                place.getLongitude(),
                new GeocodingHelper.AddressCallback() {

                    @Override
                    public void onSuccess(String address) {

                        if (activityDestroyed) return;

                        placesAdapter.updatePlaceAddress(
                                currentIndex,
                                address
                        );

                        geocodingHandler.postDelayed(
                                () -> geocodeMissingAddresses(
                                        currentIndex + 1
                                ),
                                GEOCODING_DELAY
                        );
                    }

                    @Override
                    public void onError(String error) {

                        if (activityDestroyed) return;

                        geocodingHandler.postDelayed(
                                () -> geocodeMissingAddresses(
                                        currentIndex + 1
                                ),
                                GEOCODING_DELAY
                        );
                    }
                }
        );
    }

    private void showLoading() {

        progressBar.setVisibility(View.VISIBLE);
        emptyMessage.setVisibility(View.GONE);
        retryButton.setVisibility(View.GONE);

        locationStatus.setText("Finding your location...");
    }

    private void showError(String message) {

        if (activityDestroyed) return;

        progressBar.setVisibility(View.GONE);

        locationStatus.setText("Something went wrong.");

        emptyMessage.setText(message);
        emptyMessage.setVisibility(View.VISIBLE);

        retryButton.setVisibility(View.VISIBLE);

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == LOCATION_PERMISSION_CODE) {

            boolean granted = false;

            for (int result : grantResults) {

                if (result == PackageManager.PERMISSION_GRANTED) {
                    granted = true;
                    break;
                }
            }

            if (granted) {

                getCurrentLocation();

            } else {

                showError(
                        "Location permission denied. "
                                + "Please allow location access."
                );
            }
        }
    }

    @Override
    protected void onDestroy() {

        activityDestroyed = true;

        geocodingHandler.removeCallbacksAndMessages(null);

        super.onDestroy();
    }
}
