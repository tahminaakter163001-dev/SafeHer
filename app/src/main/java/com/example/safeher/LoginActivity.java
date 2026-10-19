package com.example.safeher;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText usernameInput = findViewById(R.id.usernameInput);
        EditText passwordInput = findViewById(R.id.passwordInput);
        Button loginButton = findViewById(R.id.loginButton);
        TextView registerPrompt = findViewById(R.id.registerPrompt);

        DatabaseHelper databaseHelper = new DatabaseHelper(this);
        SessionManager sessionManager = new SessionManager(this);

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

            User user = databaseHelper.getAuthenticatedUser(
                    email, password
            );

            if (user == null) {

                Toast.makeText(this,
                        "Incorrect password",
                        Toast.LENGTH_SHORT).show();

                return;
            }

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
        });

        registerPrompt.setOnClickListener(v -> {
            Intent intent = new Intent(
                    LoginActivity.this,
                    RegisterActivity.class
            );
            startActivity(intent);
        });
    }
}
