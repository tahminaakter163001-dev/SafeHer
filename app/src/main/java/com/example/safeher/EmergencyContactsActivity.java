package com.example.safeher;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class EmergencyContactsActivity extends AppCompatActivity {

    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;
    private EmergencyContactAdapter adapter;
    private ArrayList<EmergencyContact> contacts;

    private RecyclerView recyclerView;
    private TextView emptyText;
    private int currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_contacts);

        databaseHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        if (!sessionManager.isLoggedIn()) {
            goToLogin();
            return;
        }

        currentUserId = sessionManager.getUserId();

        recyclerView = findViewById(R.id.emergencyContactsRecyclerView);
        emptyText = findViewById(R.id.emptyContactsText);
        Button addButton = findViewById(R.id.addEmergencyContactButton);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        contacts = new ArrayList<>();

        adapter = new EmergencyContactAdapter(
                contacts,
                new EmergencyContactAdapter.ContactActionListener() {
                    @Override
                    public void onEdit(EmergencyContact contact) {
                        showContactDialog(contact);
                    }

                    @Override
                    public void onDelete(EmergencyContact contact) {
                        confirmDelete(contact);
                    }
                }
        );

        recyclerView.setAdapter(adapter);

        addButton.setOnClickListener(v -> showContactDialog(null));

        loadContacts();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (sessionManager != null && sessionManager.isLoggedIn()
                && adapter != null) {
            loadContacts();
        }
    }

    private void loadContacts() {
        contacts.clear();
        contacts.addAll(
                databaseHelper.getEmergencyContacts(currentUserId)
        );

        adapter.notifyDataSetChanged();

        boolean isEmpty = contacts.isEmpty();
        emptyText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void showContactDialog(EmergencyContact existingContact) {
        boolean isEditing = existingContact != null;

        EditText nameInput = new EditText(this);
        nameInput.setHint("Contact person's name");
        nameInput.setSingleLine(true);

        EditText phoneInput = new EditText(this);
        phoneInput.setHint("Phone number");
        phoneInput.setSingleLine(true);
        phoneInput.setInputType(InputType.TYPE_CLASS_PHONE);

        android.widget.LinearLayout layout =
                new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources()
                .getDisplayMetrics().density);
        layout.setPadding(padding, padding / 2, padding, 0);
        layout.addView(nameInput);
        layout.addView(phoneInput);

        if (isEditing) {
            nameInput.setText(existingContact.getName());
            phoneInput.setText(existingContact.getPhoneNumber());
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(isEditing
                        ? "Edit Emergency Contact"
                        : "Add Emergency Contact")
                .setView(layout)
                .setNegativeButton("Cancel", null)
                .setPositiveButton(isEditing ? "Save" : "Add", null)
                .create();

        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(v -> {
                        String name = nameInput.getText()
                                .toString().trim();
                        String phone = phoneInput.getText()
                                .toString().trim();

                        if (name.length() < 2) {
                            nameInput.setError(
                                    "Enter at least 2 characters");
                            nameInput.requestFocus();
                            return;
                        }

                        if (!isValidPhone(phone)) {
                            phoneInput.setError(
                                    "Enter a valid phone number");
                            phoneInput.requestFocus();
                            return;
                        }

                        boolean success;

                        if (isEditing) {
                            success = databaseHelper.updateEmergencyContact(
                                    existingContact.getId(),
                                    currentUserId,
                                    name,
                                    phone
                            );
                        } else {
                            success = databaseHelper.addEmergencyContact(
                                    currentUserId,
                                    name,
                                    phone
                            ) != -1;
                        }

                        if (success) {
                            Toast.makeText(this,
                                    isEditing
                                            ? "Contact updated"
                                            : "Contact saved",
                                    Toast.LENGTH_SHORT).show();
                            loadContacts();
                            dialog.dismiss();
                        } else {
                            Toast.makeText(this,
                                    "Could not save contact. Please try again.",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });

        dialog.show();
    }

    private boolean isValidPhone(String phone) {
        String normalized = phone.replaceAll("[\\s()-]", "");

        return normalized.matches("\\+?[0-9]{7,15}");
    }

    private void confirmDelete(EmergencyContact contact) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Contact?")
                .setMessage("Remove " + contact.getName()
                        + " from your emergency contacts?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    boolean deleted = databaseHelper.deleteEmergencyContact(
                            contact.getId(), currentUserId);

                    if (deleted) {
                        Toast.makeText(this, "Contact deleted",
                                Toast.LENGTH_SHORT).show();
                        loadContacts();
                    } else {
                        Toast.makeText(this,
                                "Could not delete contact",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void goToLogin() {
        Intent intent = new Intent(
                EmergencyContactsActivity.this, LoginActivity.class);
        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
