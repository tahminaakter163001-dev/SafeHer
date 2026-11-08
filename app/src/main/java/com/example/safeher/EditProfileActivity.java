
package com.example.safeher;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Intent;
import android.util.Patterns;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class EditProfileActivity extends AppCompatActivity {

    private EditText nameInput;
    private EditText emailInput;

    private Button saveButton;

    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;
    private FirestoreHelper firestoreHelper;

    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        nameInput = findViewById(R.id.editNameInput);
        emailInput = findViewById(R.id.editEmailInput);

        saveButton = findViewById(R.id.saveProfileButton);
        Button cancelButton = findViewById(R.id.cancelEditButton);

        databaseHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);
        firestoreHelper = new FirestoreHelper();

        if (!sessionManager.isLoggedIn()) {
            goToLogin();
            return;
        }

        userId = sessionManager.getUserId();

        User user = databaseHelper.getUserById(userId);

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
                .toLowerCase(Locale.ROOT);

        if (name.isEmpty() || name.length() < 2) {
            nameInput.setError("Enter at least 2 characters");
            nameInput.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter a valid email address");
            emailInput.requestFocus();
            return;
        }

        User currentUser = databaseHelper.getUserById(userId);

        if (currentUser == null) {
            Toast.makeText(this,
                    "Account not found. Please log in again.",
                    Toast.LENGTH_LONG).show();
            sessionManager.clearSession();
            goToLogin();
            return;
        }

        if (name.equals(currentUser.getFullName())
                && email.equalsIgnoreCase(currentUser.getEmail())) {

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

        saveButton.setEnabled(false);
        saveButton.setText("Saving...");

        firestoreHelper.updateUserProfile(
                name,
                email,
                new FirestoreHelper.OnProfileSaveListener() {

                    @Override
                    public void onSuccess() {

                        if (isFinishing() || isDestroyed()) return;

                        boolean updated =
                                databaseHelper.updateUserProfile(
                                        userId, name, email
                                );

                        if (updated) {

                            sessionManager.saveUser(
                                    userId, name, email
                            );

                            Toast.makeText(
                                    EditProfileActivity.this,
                                    "Profile updated successfully!",
                                    Toast.LENGTH_LONG
                            ).show();

                            finish();

                        } else {

                            saveButton.setEnabled(true);
                            saveButton.setText("Save Changes");

                            Toast.makeText(
                                    EditProfileActivity.this,
                                    "Cloud updated, but local profile update failed.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(String error) {

                        if (isFinishing() || isDestroyed()) return;

                        saveButton.setEnabled(true);
                        saveButton.setText("Save Changes");

                        Toast.makeText(
                                EditProfileActivity.this,
                                error,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
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
