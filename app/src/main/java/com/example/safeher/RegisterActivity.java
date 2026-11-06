
package com.example.safeher;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        EditText fullNameInput = findViewById(R.id.fullNameInput);
        EditText emailInput = findViewById(R.id.emailInput);
        EditText passwordInput =
                findViewById(R.id.registerPasswordInput);

        Button registerButton = findViewById(R.id.registerButton);
        TextView backToLogin = findViewById(R.id.backToLogin);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        firebaseAuth = FirebaseAuth.getInstance();

        databaseHelper = new DatabaseHelper(this);

        registerButton.setOnClickListener(v -> {

            String name = fullNameInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
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

                Toast.makeText(this,
                        "This email already exists in your local accounts.",
                        Toast.LENGTH_LONG).show();

            } else {

                registerButton.setEnabled(false);

                firebaseAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, task -> {

                            if (task.isSuccessful()) {

                                boolean localRegistered =
                                        databaseHelper.registerFirebaseUser(
                                                name, email
                                        );

                                if (localRegistered) {

                                    Toast.makeText(this,
                                            "Account created successfully!",
                                            Toast.LENGTH_SHORT).show();

                                    finish();

                                } else {

                                    Toast.makeText(this,
                                            "Firebase account created, but local profile creation failed.",
                                            Toast.LENGTH_LONG).show();

                                    registerButton.setEnabled(true);
                                }

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
        });

        backToLogin.setOnClickListener(v -> finish());
    }
}
