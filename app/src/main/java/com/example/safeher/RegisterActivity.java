package com.example.safeher;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

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
        DatabaseHelper databaseHelper = new DatabaseHelper(this);
        registerButton.setOnClickListener(v -> {
            String name = fullNameInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString();

            if (name.isEmpty() || email.isEmpty()
                    || password.isEmpty()) {
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

            } else {
                boolean registered = databaseHelper.registerUser(
                        name, email, password
                );

                if (registered) {
                    Toast.makeText(this,
                            "Account created successfully!",
                            Toast.LENGTH_SHORT).show();

                    finish();
                } else {
                    Toast.makeText(this,
                            "Registration failed. Email may already exist.",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        backToLogin.setOnClickListener(v -> finish());
    }
}
