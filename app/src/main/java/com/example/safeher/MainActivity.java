package com.example.safeher;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Connect the Java Activity with the XML layout
        setContentView(R.layout.activity_main);

        // Find the Get Started button
        Button getStartedButton =
                findViewById(R.id.getStartedButton);

        // Handle button click
        getStartedButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivity.this,
                    LoginActivity.class
            );
            startActivity(intent);
        });
    }
}
