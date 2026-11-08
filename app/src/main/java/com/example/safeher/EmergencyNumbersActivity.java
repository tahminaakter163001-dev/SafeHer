
package com.example.safeher;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class EmergencyNumbersActivity extends AppCompatActivity {

    private LinearLayout numbersContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_numbers);

        numbersContainer = findViewById(R.id.numbersContainer);

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        addNumber("999", "National Emergency Service",
                "Police, Fire Service and Ambulance");

        addNumber("102", "Fire Service",
                "Fire emergencies and rescue assistance");

        addNumber("109", "Women and Children Helpline",
                "Support against violence towards women and children");

        addNumber("1098", "Child Helpline",
                "Help and protection for children");

        addNumber("1090", "Disaster Helpline",
                "Disaster early warning information");
    }

    private void addNumber(
            String number,
            String title,
            String description
    ) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(24, 20, 24, 20);

        android.widget.LinearLayout.LayoutParams cardParams =
                new android.widget.LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(0, 0, 0, 20);
        card.setBackgroundResource(R.drawable.emergency_number_card);
        card.setLayoutParams(cardParams);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextSize(17);
        titleView.setTextColor(0xFF8E3155);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView numberView = new TextView(this);
        numberView.setText(number);
        numberView.setTextSize(25);
        numberView.setTextColor(0xFFB32645);
        numberView.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView descriptionView = new TextView(this);
        descriptionView.setText(description);
        descriptionView.setTextSize(14);
        descriptionView.setTextColor(0xFF725665);

        Button callButton = new Button(this);
        callButton.setText("Call " + number);

        callButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    Intent.ACTION_DIAL,
                    Uri.parse("tel:" + number)
            );

            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(
                        this,
                        "Unable to open phone dialer.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        card.addView(titleView);
        card.addView(numberView);
        card.addView(descriptionView);
        card.addView(callButton);

        numbersContainer.addView(card);
    }
}
