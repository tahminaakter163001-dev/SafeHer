
package com.example.safeher;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private DatabaseHelper databaseHelper;
    private FirestoreHelper firestoreHelper;

    private EditText fullNameInput;
    private EditText emailInput;
    private EditText passwordInput;
    private Button registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        fullNameInput = findViewById(R.id.fullNameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.registerPasswordInput);

        registerButton = findViewById(R.id.registerButton);
        TextView backToLogin = findViewById(R.id.backToLogin);

        FirebaseApp.initializeApp(this);
        firebaseAuth = FirebaseAuth.getInstance();

        databaseHelper = new DatabaseHelper(this);
        firestoreHelper = new FirestoreHelper();

        registerButton.setOnClickListener(v -> validateRegistration());

        backToLogin.setOnClickListener(v -> finish());
    }

    private void validateRegistration() {

        String name = fullNameInput.getText().toString().trim();

        String email = emailInput.getText().toString()
                .trim()
                .toLowerCase(java.util.Locale.ROOT);

        String password = passwordInput.getText().toString();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {

            Toast.makeText(this,
                    "Please fill in all fields",
                    Toast.LENGTH_SHORT).show();

        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {

            Toast.makeText(this,
                    "Please enter a valid email address",
                    Toast.LENGTH_SHORT).show();

        } else if (password.length() < 8) {

            Toast.makeText(this,
                    "Password must contain at least 8 characters",
                    Toast.LENGTH_SHORT).show();

        } else if (databaseHelper.emailExists(email)) {

            showLocalAccountConfirmation(name, email, password);

        } else {

            createFirebaseAccount(name, email, password, false);
        }
    }

    // Ask before deleting an existing local account
    private void showLocalAccountConfirmation(
            String name,
            String email,
            String password) {

        new AlertDialog.Builder(this)
                .setTitle("Existing Local Account")
                .setMessage(
                        "This email already exists in your local database.\n\n"
                                + "If you continue, the old local account "
                                + "and its emergency contacts will be deleted "
                                + "after Firebase registration succeeds.\n\n"
                                + "Do you want to continue?"
                )
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete & Continue",
                        (dialog, which) -> {

                            createFirebaseAccount(
                                    name, email, password, true
                            );
                        })
                .show();
    }

    // Create Firebase account
    private void createFirebaseAccount(
            String name,
            String email,
            String password,
            boolean replaceLocalAccount) {

        registerButton.setEnabled(false);

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        // Delete old local account only after
                        // Firebase registration succeeds
                        if (replaceLocalAccount) {

                            boolean deleted =
                                    databaseHelper.deleteLocalAccountByEmail(email);

                            if (!deleted) {

                                Toast.makeText(this,
                                        "Firebase account created, but old local "
                                                + "account could not be deleted.",
                                        Toast.LENGTH_LONG).show();

                                registerButton.setEnabled(true);
                                return;
                            }
                        }

                        saveNewLocalAccount(name, email);

                    } else {

                        String errorMessage =
                                task.getException() != null
                                        ? task.getException().getMessage()
                                        : "Registration failed.";

                        Toast.makeText(this,
                                errorMessage,
                                Toast.LENGTH_LONG).show();

                        registerButton.setEnabled(true);
                    }
                });
    }

    // Save new local account and Firestore profile
    private void saveNewLocalAccount(String name, String email) {

        boolean localRegistered =
                databaseHelper.registerFirebaseUser(name, email);

        if (!localRegistered) {

            Toast.makeText(this,
                    "Firebase account created, but local profile creation failed.",
                    Toast.LENGTH_LONG).show();

            registerButton.setEnabled(true);
            return;
        }

        firestoreHelper.saveUserProfile(
                name,
                email,
                new FirestoreHelper.OnProfileSaveListener() {

                    @Override
                    public void onSuccess() {

                        Toast.makeText(
                                RegisterActivity.this,
                                "Account created successfully!",
                                Toast.LENGTH_SHORT
                        ).show();

                        finish();
                    }

                    @Override
                    public void onFailure(String error) {

                        Toast.makeText(
                                RegisterActivity.this,
                                "Account created, but cloud profile save failed: "
                                        + error,
                                Toast.LENGTH_LONG
                        ).show();

                        registerButton.setEnabled(true);
                    }
                }
        );
    }
}
