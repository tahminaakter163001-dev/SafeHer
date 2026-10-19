package com.example.safeher;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    private TextView nameText;
    private TextView emailText;

    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        nameText = findViewById(R.id.profileName);
        emailText = findViewById(R.id.profileEmail);

        Button editButton = findViewById(R.id.editProfileButton);
        Button backButton = findViewById(R.id.backButton);

        databaseHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        if (!sessionManager.isLoggedIn()) {
            goToLogin();
            return;
        }

        loadProfile();

        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    ProfileActivity.this,
                    EditProfileActivity.class
            );
            startActivity(intent);
        });

        backButton.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (databaseHelper != null && sessionManager != null
                && sessionManager.isLoggedIn()) {
            loadProfile();
        }
    }

    private void loadProfile() {
        int userId = sessionManager.getUserId();
        User user = databaseHelper.getUserById(userId);

        if (user == null) {
            Toast.makeText(this,
                    "Unable to load profile. Please log in again.",
                    Toast.LENGTH_LONG).show();

            sessionManager.clearSession();
            goToLogin();
            return;
        }

        nameText.setText(user.getFullName());
        emailText.setText(user.getEmail());

        // Keep session data in sync with the database.
        sessionManager.saveUser(
                user.getId(),
                user.getFullName(),
                user.getEmail()
        );
    }

    private void goToLogin() {
        Intent intent = new Intent(
                ProfileActivity.this,
                LoginActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}
