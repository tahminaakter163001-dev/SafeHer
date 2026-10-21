
package com.example.safeher;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class EmergencySOSActivity extends AppCompatActivity {

    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;
    private LinearLayout contactsContainer;

    private final ArrayList<EmergencyContact> selectedContacts =
            new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_sos);

        Button backButton = findViewById(R.id.sosBackButton);
        TextView statusText = findViewById(R.id.sosStatusText);

        databaseHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        if (!sessionManager.isLoggedIn()) {
            finish();
            return;
        }

        contactsContainer =
                findViewById(R.id.sosContactsContainer);

        statusText.setText(
                "No emergency message has been sent."
        );

        backButton.setOnClickListener(v -> finish());

        loadTrustedContacts();
    }

    private void loadTrustedContacts() {
        contactsContainer.removeAllViews();
        selectedContacts.clear();

        int userId = sessionManager.getUserId();

        ArrayList<EmergencyContact> contacts =
                databaseHelper.getEmergencyContacts(userId);

        if (contacts.isEmpty()) {
            TextView emptyText = new TextView(this);

            emptyText.setText(
                    "No trusted contacts saved yet. " +
                            "Add contacts from the Dashboard first."
            );

            emptyText.setTextSize(15);
            emptyText.setPadding(8, 16, 8, 16);

            contactsContainer.addView(emptyText);
            return;
        }

        for (EmergencyContact contact : contacts) {
            CheckBox checkBox = new CheckBox(this);

            checkBox.setText(
                    contact.getName() + "\n" +
                            contact.getPhoneNumber()
            );

            checkBox.setTextSize(15);
            checkBox.setPadding(8, 8, 8, 8);

            checkBox.setOnCheckedChangeListener(
                    (button, isChecked) -> {
                        if (isChecked) {
                            if (!selectedContacts.contains(contact)) {
                                selectedContacts.add(contact);
                            }
                        } else {
                            selectedContacts.remove(contact);
                        }
                    }
            );

            contactsContainer.addView(checkBox);
        }
    }
}
