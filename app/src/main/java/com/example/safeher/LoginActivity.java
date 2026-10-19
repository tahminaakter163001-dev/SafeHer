package com.example.safeher;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Intent;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText usernameInput = findViewById(R.id.usernameInput);
        EditText passwordInput = findViewById(R.id.passwordInput);
        Button loginButton = findViewById(R.id.loginButton);


        DatabaseHelper databaseHelper = new DatabaseHelper(this);

        loginButton.setOnClickListener(v -> {
            String email = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this,
                        "Please enter email and password",
                        Toast.LENGTH_SHORT).show();

            } else if (!databaseHelper.emailExists(email)) {
                Toast.makeText(this,
                        "No account found with this email",
                        Toast.LENGTH_SHORT).show();

            } else if (!databaseHelper.checkUser(email, password)) {
                Toast.makeText(this,
                        "Incorrect password",
                        Toast.LENGTH_SHORT).show();

            }
            else {
                Toast.makeText(LoginActivity.this,
                        "Login successful!",
                        Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(
                        LoginActivity.this,
                        DashboardActivity.class
                );

                startActivity(intent);
                finish();
            }

        });
        TextView registerPrompt = findViewById(R.id.registerPrompt);

        registerPrompt.setOnClickListener(v -> {
            Intent intent = new Intent(
                    LoginActivity.this,
                    RegisterActivity.class
            );
            startActivity(intent);
        });
    }
}

