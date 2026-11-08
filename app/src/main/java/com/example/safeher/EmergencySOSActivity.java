
package com.example.safeher;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.ArrayList;

public class EmergencySOSActivity extends AppCompatActivity {

    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;
    private LinearLayout contactsContainer;

    private TextView statusText;
    private TextView messagePreview;
    private Button sendSOSButton;

    private String currentUserName = "SafeHer user";

    private final ArrayList<EmergencyContact> selectedContacts =
            new ArrayList<>();

    private FusedLocationProviderClient fusedLocationClient;
    private RealtimeDatabaseHelper realtimeDatabaseHelper;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    private double currentLatitude;
    private double currentLongitude;

    private boolean locationAvailable = false;
    private boolean isSOSPending = false;
    private boolean isTrackingActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_sos);

        databaseHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        realtimeDatabaseHelper = new RealtimeDatabaseHelper();

        if (!sessionManager.isLoggedIn()) {
            finish();
            return;
        }

        contactsContainer = findViewById(R.id.sosContactsContainer);
        statusText = findViewById(R.id.sosStatusText);
        messagePreview = findViewById(R.id.sosMessagePreview);
        sendSOSButton = findViewById(R.id.sendSOSButton);

        Button backButton = findViewById(R.id.sosBackButton);

        loadCurrentUserName();
        updateMessagePreview(null);

        statusText.setText("SOS is inactive. You are in control.");

        backButton.setOnClickListener(v -> goBack());

        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        goBack();
                    }
                }
        );

        sendSOSButton.setOnClickListener(v -> {
            if (isTrackingActive) {
                stopSOSTracking();
            } else {
                sendSOSAlert();
            }
        });

        loadTrustedContacts();
    }

    private void loadCurrentUserName() {

        User user = databaseHelper.getUserById(
                sessionManager.getUserId()
        );

        if (user != null &&
                user.getFullName() != null &&
                !user.getFullName().trim().isEmpty()) {

            currentUserName = user.getFullName().trim();

        } else if (sessionManager.getUserName() != null &&
                !sessionManager.getUserName().trim().isEmpty()) {

            currentUserName = sessionManager.getUserName().trim();
        }
    }

    private void goBack() {

        if (isTrackingActive) {
            Toast.makeText(
                    this,
                    "Please stop SOS tracking before leaving.",
                    Toast.LENGTH_LONG
            ).show();
        } else {
            finish();
        }
    }

    private void loadTrustedContacts() {

        contactsContainer.removeAllViews();
        selectedContacts.clear();

        ArrayList<EmergencyContact> contacts =
                databaseHelper.getEmergencyContacts(
                        sessionManager.getUserId()
                );

        if (contacts.isEmpty()) {

            TextView emptyText = new TextView(this);

            emptyText.setText(
                    "No trusted contacts found.\n" +
                            "Please add emergency contacts from your Dashboard."
            );

            emptyText.setTextSize(14);
            emptyText.setTextColor(
                    android.graphics.Color.parseColor("#725665")
            );
            emptyText.setPadding(12, 20, 12, 20);

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
            checkBox.setPadding(8, 10, 8, 10);
            checkBox.setButtonTintList(
                    android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#8E3155")
                    )
            );

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

        boolean fineGranted =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        boolean coarseGranted =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        if (fineGranted || coarseGranted) {
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

                if (isSOSPending) {
                    isSOSPending = false;
                    openSOSMessage();
                }

            } else {

                locationAvailable = false;
                isSOSPending = false;

                Toast.makeText(
                        this,
                        "Unable to get your location. Please enable GPS and try again.",
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

        realtimeDatabaseHelper.startSOS(
                currentLatitude,
                currentLongitude
        );

        Intent trackingIntent =
                new Intent(this, LocationTrackingService.class);

        if (android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.O) {

            startForegroundService(trackingIntent);

        } else {
            startService(trackingIntent);
        }

        isTrackingActive = true;

        sendSOSButton.setText("STOP SOS TRACKING");
        sendSOSButton.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#388E3C")
                )
        );

        statusText.setText(
                "SOS ACTIVE • Your live location is being shared."
        );

        statusText.setBackgroundColor(
                android.graphics.Color.parseColor("#E4F5E8")
        );

        statusText.setTextColor(
                android.graphics.Color.parseColor("#216B35")
        );

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
                        openSMSComposer(null);

                        Toast.makeText(
                                EmergencySOSActivity.this,
                                "Address unavailable. GPS coordinates will be included instead.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private String buildSOSMessage(String address) {

        String message =
                "EMERGENCY ALERT!\n\n" +
                        "Hello, this is " + currentUserName + ". " +
                        "I may be in an unsafe situation and need your help. " +
                        "Please check on me and help ensure my safety.\n\n" +
                        "MY CURRENT LOCATION:\n";

        if (address != null && !address.trim().isEmpty()) {
            message += "Address: " + address.trim() + "\n";
        }

        if (locationAvailable) {
            message +=
                    "GPS Coordinates: " +
                            currentLatitude + ", " + currentLongitude + "\n\n" +
                            "Google Maps:\n" +
                            "https://maps.google.com/?q=" +
                            currentLatitude + "," + currentLongitude;
        } else {
            message += "Location is currently unavailable.";
        }

        return message;
    }

    private void updateMessagePreview(String address) {

        if (messagePreview != null) {
            messagePreview.setText(buildSOSMessage(address));
        }
    }

    private void openSMSComposer(String address) {

        String message = buildSOSMessage(address);
        updateMessagePreview(address);

        ArrayList<String> phoneNumbers = new ArrayList<>();

        for (EmergencyContact contact : selectedContacts) {
            phoneNumbers.add(contact.getPhoneNumber());
        }

        String recipients = android.text.TextUtils.join(
                ";",
                phoneNumbers
        );

        Intent smsIntent = new Intent(Intent.ACTION_SENDTO);

        smsIntent.setData(
                Uri.parse("smsto:" + recipients)
        );

        smsIntent.putExtra("sms_body", message);

        try {
            startActivity(smsIntent);

        } catch (ActivityNotFoundException e) {

            Toast.makeText(
                    this,
                    "No SMS application found on this device.",
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

        sendSOSButton.setText("SEND SOS ALERT");

        sendSOSButton.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#D32F2F")
                )
        );

        statusText.setText(
                "SOS ended. Your live location tracking has stopped."
        );

        statusText.setBackgroundColor(
                android.graphics.Color.parseColor("#FCE7F3")
        );

        statusText.setTextColor(
                android.graphics.Color.parseColor("#8E3155")
        );

        Toast.makeText(
                this,
                "SOS tracking stopped successfully.",
                Toast.LENGTH_SHORT
        ).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
}
