
package com.example.safeher;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class DashboardActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    private SystemEventReceiver systemEventReceiver;
    private boolean receiverRegistered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        sessionManager = new SessionManager(this);

        // Initialize System Event Receiver
        systemEventReceiver = new SystemEventReceiver();

        Button sosButton = findViewById(R.id.sosButton);
        Button safetyTipsButton = findViewById(R.id.safetyTipsButton);
        Button profileButton = findViewById(R.id.profileButton);
        Button logoutButton = findViewById(R.id.logoutButton);
        Button emergencyContactsButton =
                findViewById(R.id.emergencyContactsButton);

        Button nearbyServicesButton =
                findViewById(R.id.nearbyServicesButton);

        sosButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    DashboardActivity.this,
                    EmergencySOSActivity.class
            );
            startActivity(intent);
        });

        safetyTipsButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    DashboardActivity.this,
                    SafetyTipsActivity.class
            );
            startActivity(intent);
        });

        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    DashboardActivity.this,
                    ProfileActivity.class
            );
            startActivity(intent);
        });

        emergencyContactsButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    DashboardActivity.this,
                    EmergencyContactsActivity.class
            );
            startActivity(intent);
        });

        nearbyServicesButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    DashboardActivity.this,
                    EmergencyPlacesActivity.class
            );
            startActivity(intent);
        });

        logoutButton.setOnClickListener(v -> {
            sessionManager.clearSession();

            Intent intent = new Intent(
                    DashboardActivity.this,
                    LoginActivity.class
            );

            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        filter.addAction(Intent.ACTION_BATTERY_LOW);

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            registerReceiver(
                    systemEventReceiver,
                    filter,
                    Context.RECEIVER_EXPORTED
            );
        } else {
            registerReceiver(systemEventReceiver, filter);
        }

        receiverRegistered = true;
    }

    @Override
    protected void onStop() {
        if (receiverRegistered) {
            unregisterReceiver(systemEventReceiver);
            receiverRegistered = false;
        }

        super.onStop();
    }
}
