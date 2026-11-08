
package com.example.safeher;

import android.os.Bundle;
import android.content.Intent;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class EditProfileActivity extends AppCompatActivity {

    private EditText nameInput;
    private EditText emailInput;
    private EditText phoneInput;
    private EditText addressInput;
    private EditText dobInput;
    private EditText bloodGroupInput;

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
        phoneInput = findViewById(R.id.editPhoneInput);
        addressInput = findViewById(R.id.editAddressInput);
        dobInput = findViewById(R.id.editDobInput);
        bloodGroupInput = findViewById(R.id.editBloodGroupInput);

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

        // Load existing profile information
        nameInput.setText(user.getFullName());
        emailInput.setText(user.getEmail());
        phoneInput.setText(user.getPhoneNumber());
        addressInput.setText(user.getAddress());
        dobInput.setText(user.getDateOfBirth());
        bloodGroupInput.setText(user.getBloodGroup());

        saveButton.setOnClickListener(v -> saveProfile());

        cancelButton.setOnClickListener(v -> finish());
    }

    private void saveProfile() {

        String name = nameInput.getText().toString().trim();

        String email = emailInput.getText().toString()
                .trim()
                .toLowerCase(Locale.ROOT);

        String phone = phoneInput.getText().toString().trim();

        String address = addressInput.getText().toString().trim();

        String dob = dobInput.getText().toString().trim();

        String bloodGroup = bloodGroupInput.getText().toString()
                .trim()
                .toUpperCase(Locale.ROOT);

        // Validate name
        if (name.isEmpty() || name.length() < 2) {
            nameInput.setError("Enter at least 2 characters");
            nameInput.requestFocus();
            return;
        }

        // Validate email
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter a valid email address");
            emailInput.requestFocus();
            return;
        }

        // Validate phone number if provided
        if (!phone.isEmpty()) {

            String cleanPhone = phone.replaceAll("[\\s()-]", "");

            if (!cleanPhone.matches("\\+?[0-9]{7,15}")) {
                phoneInput.setError("Enter a valid phone number");
                phoneInput.requestFocus();
                return;
            }
        }

        // Validate date of birth if provided
        if (!dob.isEmpty() && !isValidDate(dob)) {
            dobInput.setError("Use date format YYYY-MM-DD");
            dobInput.requestFocus();
            return;
        }

        // Validate blood group if provided
        if (!bloodGroup.isEmpty() &&
                !bloodGroup.matches("^(A|B|AB|O)[+-]$")) {

            bloodGroupInput.setError(
                    "Use A+, A-, B+, B-, AB+, AB-, O+ or O-"
            );

            bloodGroupInput.requestFocus();
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

        // Check whether anything changed
        boolean noChanges =
                name.equals(currentUser.getFullName()) &&
                        email.equalsIgnoreCase(currentUser.getEmail()) &&
                        phone.equals(currentUser.getPhoneNumber()) &&
                        address.equals(currentUser.getAddress()) &&
                        dob.equals(currentUser.getDateOfBirth()) &&
                        bloodGroup.equalsIgnoreCase(currentUser.getBloodGroup());

        if (noChanges) {
            Toast.makeText(this,
                    "No changes to save",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Check duplicate email
        if (!email.equalsIgnoreCase(currentUser.getEmail()) &&
                databaseHelper.emailExists(email)) {

            emailInput.setError("This email is already registered");
            emailInput.requestFocus();
            return;
        }

        saveButton.setEnabled(false);
        saveButton.setText("Saving...");

        firestoreHelper.updateUserProfile(
                name,
                email,
                phone,
                address,
                dob,
                bloodGroup,
                new FirestoreHelper.OnProfileSaveListener() {

                    @Override
                    public void onSuccess() {

                        if (isFinishing() || isDestroyed()) return;

                        boolean updated =
                                databaseHelper.updateUserProfile(
                                        userId,
                                        name,
                                        email,
                                        phone,
                                        address,
                                        dob,
                                        bloodGroup
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

    private boolean isValidDate(String date) {

        SimpleDateFormat format =
                new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        format.setLenient(false);

        try {
            format.parse(date);
            return true;

        } catch (ParseException e) {
            return false;
        }
    }

    private void goToLogin() {

        Intent intent = new Intent(
                EditProfileActivity.this,
                LoginActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}
