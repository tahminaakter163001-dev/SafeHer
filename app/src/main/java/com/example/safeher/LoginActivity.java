package com.example.safeher;

import android.os.Bundle;
import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText usernameInput = findViewById(R.id.usernameInput);
        EditText passwordInput = findViewById(R.id.passwordInput);

        Button loginButton = findViewById(R.id.loginButton);
        TextView registerPrompt = findViewById(R.id.registerPrompt);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        firebaseAuth = FirebaseAuth.getInstance();

        databaseHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        loginButton.setOnClickListener(v -> {

            String email = usernameInput.getText().toString()
                    .trim()
                    .toLowerCase(java.util.Locale.ROOT);

            String password = passwordInput.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {

                Toast.makeText(this,
                        "Please enter email and password",
                        Toast.LENGTH_SHORT).show();

                return;
            }

            if (!databaseHelper.emailExists(email)) {

                Toast.makeText(this,
                        "No account found with this email",
                        Toast.LENGTH_SHORT).show();

                return;
            }

            loginButton.setEnabled(false);

            // Check whether this is a Firebase account
            if (databaseHelper.isFirebaseAccount(email)) {

                // Firebase login
                firebaseAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, task -> {

                            if (task.isSuccessful()) {

                                User user = databaseHelper.getUserByEmail(email);

                                if (user != null) {

                                    openDashboard(user);

                                } else {

                                    Toast.makeText(this,
                                            "Account profile not found locally.",
                                            Toast.LENGTH_LONG).show();

                                    loginButton.setEnabled(true);
                                }

                            } else {

                                String errorMessage =
                                        task.getException() != null
                                                ? task.getException().getMessage()
                                                : "Login failed.";

                                Toast.makeText(this,
                                        errorMessage,
                                        Toast.LENGTH_LONG).show();

                                loginButton.setEnabled(true);
                            }
                        });

            } else {

                // Existing SQLite login
                User user = databaseHelper.getAuthenticatedUser(
                        email, password
                );

                if (user != null) {

                    openDashboard(user);

                } else {

                    Toast.makeText(this,
                            "Incorrect password",
                            Toast.LENGTH_SHORT).show();

                    loginButton.setEnabled(true);
                }
            }
        });

        registerPrompt.setOnClickListener(v -> {

            Intent intent = new Intent(
                    LoginActivity.this,
                    RegisterActivity.class
            );

            startActivity(intent);
        });
    }

    private void openDashboard(User user) {

        sessionManager.saveUser(
                user.getId(),
                user.getFullName(),
                user.getEmail()
        );

        Toast.makeText(this,
                "Login successful!",
                Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(
                LoginActivity.this,
                DashboardActivity.class
        );

        startActivity(intent);
        finish();
    }
}