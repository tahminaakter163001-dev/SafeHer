package com.example.safeher;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        Button sosButton = findViewById(R.id.sosButton);
        Button safetyTipsButton = findViewById(R.id.safetyTipsButton);
        Button profileButton = findViewById(R.id.profileButton);
        Button logoutButton = findViewById(R.id.logoutButton);

        sosButton.setOnClickListener(v -> {
            Toast.makeText(this,
                    "Emergency SOS feature is coming soon",
                    Toast.LENGTH_LONG).show();
        });

        safetyTipsButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    DashboardActivity.this,
                    SafetyTipsActivity.class
            );
            startActivity(intent);
        });

        profileButton.setOnClickListener(v -> {
            Toast.makeText(this,
                    "My Profile feature is coming soon",
                    Toast.LENGTH_SHORT).show();
        });

        logoutButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    DashboardActivity.this,
                    LoginActivity.class
            );

            intent.setFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP
            );

            startActivity(intent);
            finish();
        });
    }
}
