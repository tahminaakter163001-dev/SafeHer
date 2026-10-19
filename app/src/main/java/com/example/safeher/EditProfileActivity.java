package com.example.safeher;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Intent;
import android.util.Patterns;

import androidx.appcompat.app.AppCompatActivity;

public class EditProfileActivity extends AppCompatActivity {

    private EditText nameInput;
    private EditText emailInput;

    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        nameInput = findViewById(R.id.editNameInput);
        emailInput = findViewById(R.id.editEmailInput);

        Button saveButton = findViewById(R.id.saveProfileButton);
        Button cancelButton = findViewById(R.id.cancelEditButton);

        databaseHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        if (!sessionManager.isLoggedIn()) {
            goToLogin();
            return;
        }

        User user = databaseHelper.getUserById(
                sessionManager.getUserId()
        );

        if (user == null) {
            sessionManager.clearSession();
            goToLogin();
            return;
        }

        nameInput.setText(user.getFullName());
        emailInput.setText(user.getEmail());

        saveButton.setOnClickListener(v -> saveProfile());

        cancelButton.setOnClickListener(v -> finish());
    }

    private void saveProfile() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString()
                .trim()
                .toLowerCase(java.util.Locale.ROOT);

        if (name.isEmpty()) {
            nameInput.setError("Name is required");
            nameInput.requestFocus();
            return;
        }

        if (name.length() < 2) {
            nameInput.setError("Enter at least 2 characters");
            nameInput.requestFocus();
            return;
        }

        if (email.isEmpty()
                || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter a valid email address");
            emailInput.requestFocus();
            return;
        }

        int userId = sessionManager.getUserId();

        User currentUser = databaseHelper.getUserById(userId);

        if (currentUser == null) {
            Toast.makeText(this,
                    "Account not found. Please log in again.",
                    Toast.LENGTH_LONG).show();
            sessionManager.clearSession();
            goToLogin();
            return;
        }

        if (email.equalsIgnoreCase(currentUser.getEmail())
                && name.equals(currentUser.getFullName())) {
            Toast.makeText(this,
                    "No changes to save",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (!email.equalsIgnoreCase(currentUser.getEmail())
                && databaseHelper.emailExists(email)) {
            emailInput.setError("This email is already registered");
            emailInput.requestFocus();
            return;
        }

        boolean updated = databaseHelper.updateUserProfile(
                userId, name, email
        );

        if (updated) {
            sessionManager.saveUser(userId, name, email);

            Toast.makeText(this,
                    "Profile updated successfully",
                    Toast.LENGTH_SHORT).show();

            finish();
        } else {
            Toast.makeText(this,
                    "Could not update profile. Please try again.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void goToLogin() {
        Intent intent = new Intent(
                EditProfileActivity.this,
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
