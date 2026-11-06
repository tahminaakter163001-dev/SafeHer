
package com.example.safeher;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.Priority;
import androidx.activity.OnBackPressedCallback;

public class EmergencySOSActivity extends AppCompatActivity {

    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;
    private LinearLayout contactsContainer;

    private final ArrayList<EmergencyContact> selectedContacts =
            new ArrayList<>();
    private FusedLocationProviderClient fusedLocationClient;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private double currentLatitude;
    private double currentLongitude;
    private boolean locationAvailable = false;
    private boolean isSOSPending = false;
    private RealtimeDatabaseHelper realtimeDatabaseHelper;
    private boolean isTrackingActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_sos);

        Button backButton = findViewById(R.id.sosBackButton);
        TextView statusText = findViewById(R.id.sosStatusText);

        databaseHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);
        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);
        realtimeDatabaseHelper = new RealtimeDatabaseHelper();

        if (!sessionManager.isLoggedIn()) {
            finish();
            return;
        }

        contactsContainer =
                findViewById(R.id.sosContactsContainer);

        statusText.setText(
                "No emergency message has been sent."
        );

        backButton.setOnClickListener(v -> {

            if (isTrackingActive) {

                Toast.makeText(
                        this,
                        "Please stop SOS tracking before leaving.",
                        Toast.LENGTH_LONG
                ).show();

            } else {
                finish();
            }
        });
        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {

                    @Override
                    public void handleOnBackPressed() {

                        if (isTrackingActive) {

                            Toast.makeText(
                                    EmergencySOSActivity.this,
                                    "Please stop SOS tracking before leaving.",
                                    Toast.LENGTH_LONG
                            ).show();

                        } else {
                            finish();
                        }
                    }
                }
        );
        // SOS button click listener
        Button sendSOSButton =
                findViewById(R.id.sendSOSButton);

        sendSOSButton.setOnClickListener(v -> {

            if (isTrackingActive) {
                stopSOSTracking();
            } else {
                sendSOSAlert();
            }
        });

        loadTrustedContacts();
    }

    // Load trusted contacts from SQLite
    private void loadTrustedContacts() {

        contactsContainer.removeAllViews();
        selectedContacts.clear();

        int userId = sessionManager.getUserId();

        ArrayList<EmergencyContact> contacts =
                databaseHelper.getEmergencyContacts(userId);

        if (contacts.isEmpty()) {

            TextView emptyText = new TextView(this);

            emptyText.setText(
                    "No trusted contacts saved yet. " +
                            "Add contacts from the Dashboard first."
            );

            emptyText.setTextSize(15);
            emptyText.setTextColor(
                    android.graphics.Color.parseColor("#33232B")
            );
            emptyText.setPadding(8, 16, 8, 16);

            contactsContainer.addView(emptyText);
            return;
        }

        for (EmergencyContact contact : contacts) {

            CheckBox checkBox = new CheckBox(this);

            checkBox.setText(
                    contact.getName() + "\n" +
                            contact.getPhoneNumber()
            );

            checkBox.setTextSize(15);
            checkBox.setTextColor(
                    android.graphics.Color.parseColor("#33232B")
            );
            checkBox.setPadding(8, 8, 8, 8);

            checkBox.setOnCheckedChangeListener(
                    (button, isChecked) -> {

                        if (isChecked) {

                            if (!selectedContacts.contains(contact)) {
                                selectedContacts.add(contact);
                            }

                        } else {
                            selectedContacts.remove(contact);
                        }
                    }
            );

            contactsContainer.addView(checkBox);
        }
    }

    // Prepare SOS message and open SMS application
    private void sendSOSAlert() {

        if (selectedContacts.isEmpty()) {
            Toast.makeText(
                    this,
                    "Please select at least one trusted contact.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        isSOSPending = true;
        locationAvailable = false;

        checkLocationPermission();
    }
    private void checkLocationPermission() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
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
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        }
    }

    private void getCurrentLocation() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
        ).addOnSuccessListener(location -> {

            if (location != null) {

                currentLatitude = location.getLatitude();
                currentLongitude = location.getLongitude();
                locationAvailable = true;

                Toast.makeText(
                        this,
                        "Current location obtained successfully!",
                        Toast.LENGTH_SHORT
                ).show();

                if (isSOSPending) {
                    isSOSPending = false;
                    openSOSMessage();
                }
            } else {

                locationAvailable = false;
                isSOSPending = false;

                Toast.makeText(
                        this,
                        "Unable to get location. Please enable GPS.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }).addOnFailureListener(error -> {

            locationAvailable = false;
            isSOSPending = false;

            Toast.makeText(
                    this,
                    "Location error: " + error.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        });
    }

    private void openSOSMessage() {

        // Start saving SOS information in Firebase
        realtimeDatabaseHelper.startSOS(
                currentLatitude,
                currentLongitude
        );

        // Start continuous GPS tracking
        Intent trackingIntent =
                new Intent(this, LocationTrackingService.class);

        if (android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.O) {

            startForegroundService(trackingIntent);

        } else {
            startService(trackingIntent);
        }

        isTrackingActive = true;

        Button sendSOSButton =
                findViewById(R.id.sendSOSButton);

        sendSOSButton.setText("Stop SOS");

        sendSOSButton.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#388E3C")
                )
        );

        TextView statusText =
                findViewById(R.id.sosStatusText);

        statusText.setText(
                "SOS Active - Live location tracking is running."
        );

        // Get readable address from API
        GeocodingHelper.getAddress(
                currentLatitude,
                currentLongitude,
                new GeocodingHelper.AddressCallback() {

                    @Override
                    public void onSuccess(String address) {

                        openSMSComposer(address);
                    }

                    @Override
                    public void onError(String error) {

                        // If API fails, send coordinates only
                        openSMSComposer(null);

                        Toast.makeText(
                                EmergencySOSActivity.this,
                                "Address could not be fetched. Sending GPS coordinates instead.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void openSMSComposer(String address) {

        String message =
                "EMERGENCY SOS! I may be in danger. " +
                        "Please contact me as soon as possible.\n\n" +
                        "My current location:\n";

        if (address != null && !address.isEmpty()) {

            message +=
                    "Address: " + address + "\n\n";
        }

        message +=
                "GPS Coordinates: " +
                        currentLatitude + ", " + currentLongitude +
                        "\n\n" +
                        "Google Maps:\n" +
                        "https://maps.google.com/?q=" +
                        currentLatitude + "," + currentLongitude;

        ArrayList<String> phoneNumbers = new ArrayList<>();

        for (EmergencyContact contact : selectedContacts) {
            phoneNumbers.add(contact.getPhoneNumber());
        }

        String recipients = android.text.TextUtils.join(
                ";",
                phoneNumbers
        );

        Intent smsIntent =
                new Intent(Intent.ACTION_SENDTO);

        smsIntent.setData(
                Uri.parse("smsto:" + recipients)
        );

        smsIntent.putExtra("sms_body", message);

        try {

            startActivity(smsIntent);

        } catch (android.content.ActivityNotFoundException e) {

            Toast.makeText(
                    this,
                    "No SMS application found.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {

            boolean permissionGranted = false;

            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    permissionGranted = true;
                    break;
                }
            }

            if (permissionGranted) {

                getCurrentLocation();

            } else {

                isSOSPending = false;

                Toast.makeText(
                        this,
                        "Location permission denied. SOS message was not prepared.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }
    private void stopSOSTracking() {

        Intent trackingIntent =
                new Intent(this, LocationTrackingService.class);

        stopService(trackingIntent);

        realtimeDatabaseHelper.stopSOS();

        isTrackingActive = false;

        Button sendSOSButton = findViewById(R.id.sendSOSButton);
        sendSOSButton.setText("Send SOS");
        sendSOSButton.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#D32F2F")
                )
        );

        TextView statusText = findViewById(R.id.sosStatusText);
        statusText.setText("SOS ended. Live location tracking stopped.");

        Toast.makeText(
                this,
                "SOS stopped successfully.",
                Toast.LENGTH_SHORT
        ).show();
    }
}
