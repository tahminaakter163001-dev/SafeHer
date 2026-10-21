package com.example.safeher;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class DashboardActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        sessionManager = new SessionManager(this);

        Button sosButton = findViewById(R.id.sosButton);
        Button safetyTipsButton = findViewById(R.id.safetyTipsButton);
        Button profileButton = findViewById(R.id.profileButton);
        Button logoutButton = findViewById(R.id.logoutButton);
        Button emergencyContactsButton = findViewById(R.id.emergencyContactsButton);

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

        logoutButton.setOnClickListener(v -> {
            sessionManager.clearSession();

            Intent intent = new Intent(
                    DashboardActivity.this,
                    LoginActivity.class
            );

            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);
            finish();
        });
    }
}
